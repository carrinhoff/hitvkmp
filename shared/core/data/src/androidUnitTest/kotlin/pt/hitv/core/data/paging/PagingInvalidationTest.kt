package pt.hitv.core.data.paging

import app.cash.paging.PagingSource
import app.cash.paging.PagingSourceLoadParams
import app.cash.paging.PagingSourceLoadResult
import app.cash.paging.PagingSourceLoadResultPage
import app.cash.paging.PagingState
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pt.hitv.core.database.HitvDatabase
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Proves paged lists actually refresh when the database changes.
 *
 * Room gives the original this via its `InvalidationTracker`: a Room-backed `PagingSource`
 * self-invalidates when a table it reads is written, so a finished sync reaches the screen. Every
 * paging source in the port read with a one-shot `executeAsList()` and registered nothing, so a
 * completed sync did not update any visible list — new channels missing, removed ones still shown,
 * a toggled favourite absent from Favourites until the screen was destroyed and recreated. On iOS
 * that is the normal case rather than an edge case, because the BGTask sync finishes while the app
 * is suspended and the user returns to a list that predates it.
 *
 * The mechanism is worth a test of its own because every way it can break is silent. SQLDelight
 * notifies listeners under string "query keys" — the table names emitted by the generated
 * `notifyQueries { emit("Channel") }`. A typo, or a table renamed in the `.sq`, does not fail to
 * compile and does not throw: the listener simply never fires and the screen quietly goes back to
 * being stale.
 */
class PagingInvalidationTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: HitvDatabase

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HitvDatabase.Schema.create(driver)
        db = HitvDatabase(driver)
    }

    @AfterTest
    fun tearDown() = driver.close()

    private class FakePagingSource : PagingSource<Int, String>() {
        override suspend fun load(params: PagingSourceLoadParams<Int>): PagingSourceLoadResult<Int, String> =
            PagingSourceLoadResultPage(emptyList<String>(), null, null)

        override fun getRefreshKey(state: PagingState<Int, String>): Int? = null
    }

    private fun insertChannel(name: String = "Sport TV", userId: Long = 1L) {
        db.channelQueries.insertOrIgnore(
            name = name,
            streamUrl = "http://example/$name",
            streamIcon = "",
            epgChannelId = name.lowercase(),
            categoryCreatorId = "1",
            isFavorite = 0L,
            licenseKey = null,
            userId = userId,
            lastViewedTimestamp = 0L,
            lastUpdated = 0L,
            lastSeen = 0L,
            contentHash = null,
            syncVersion = 1L,
            tvArchive = 0L,
            tvArchiveDuration = 0L,
            catchupType = null,
            catchupSource = null,
        )
    }

    private fun insertMovie(name: String = "A Movie") {
        db.movieQueries.insertOrIgnore(
            name = name,
            streamId = "1",
            streamIcon = "",
            rating = "0",
            added = "0",
            categoryCreatorId = "1",
            containerExtension = "mp4",
            isFavorite = 0L,
            userId = 1L,
            lastViewedTimestamp = 0L,
            lastUpdated = 0L,
            lastSeen = 0L,
            contentHash = null,
            syncVersion = 1L,
        )
    }

    @Test
    fun `a write to a watched table invalidates the source`() {
        val source = FakePagingSource()
        source.invalidateOnChangeTo(driver, PagedTables.CHANNEL)
        assertFalse(source.invalid)

        insertChannel()

        assertTrue(source.invalid, "the paging source did not notice the channel write")
    }

    @Test
    fun `the table name constants match what SQLDelight actually emits`() {
        // The whole failure mode this guards: a key that never matches fires nothing, silently.
        // Asserting per-table rather than trusting the constants keeps a rename in the .sq honest.
        val channelSource = FakePagingSource().also { it.invalidateOnChangeTo(driver, PagedTables.CHANNEL) }
        val movieSource = FakePagingSource().also { it.invalidateOnChangeTo(driver, PagedTables.MOVIE) }

        insertChannel()
        insertMovie()

        assertTrue(channelSource.invalid, "PagedTables.CHANNEL does not match the emitted key")
        assertTrue(movieSource.invalid, "PagedTables.MOVIE does not match the emitted key")
    }

    @Test
    fun `an unrelated table does not invalidate`() {
        // Over-invalidation is not harmless: every spurious invalidation restarts paging and
        // scrolls the user's list back to the top.
        val source = FakePagingSource()
        source.invalidateOnChangeTo(driver, PagedTables.MOVIE)

        insertChannel()

        assertFalse(source.invalid, "a Channel write invalidated a Movie-only source")
    }

    @Test
    fun `a source watching several tables reacts to any of them`() {
        val source = FakePagingSource()
        source.invalidateOnChangeTo(driver, PagedTables.CHANNEL, PagedTables.CUSTOM_GROUP_CHANNEL)

        db.customGroupQueries.insertGroup(
            groupName = "G", groupIcon = null, createdAt = 0L, updatedAt = 0L,
            sortOrder = 0L, isPinned = 0L, isHidden = 0L, isDefault = 0L,
        )
        db.customGroupQueries.addChannelToGroup(
            groupId = 1L, channelId = 1L, channelUserId = 1L, position = 0L, addedAt = 0L,
        )

        assertTrue(source.invalid, "group membership changes must refresh the channel list")
    }

    @Test
    fun `favouriting a channel invalidates - the visible symptom`() {
        insertChannel()
        val source = FakePagingSource()
        source.invalidateOnChangeTo(driver, PagedTables.CHANNEL)

        db.channelQueries.updateFavorite(1L, "Sport TV", 1L, "1")

        assertTrue(source.invalid, "the Favourites list would not have updated")
    }

    @Test
    fun `the listener is removed once the source is invalidated`() {
        // Paging builds a fresh source after each invalidation, so a listener that outlives its
        // source accumulates one dead registration per refresh for the life of the process.
        val source = FakePagingSource()
        source.invalidateOnChangeTo(driver, PagedTables.CHANNEL)

        var cleanupRan = false
        source.registerInvalidatedCallback { cleanupRan = true }

        insertChannel("First")
        assertTrue(source.invalid)
        assertTrue(cleanupRan, "invalidated callbacks did not run — the listener would leak")

        // Further writes must not throw against the deregistered listener.
        insertChannel("Second")
    }
}
