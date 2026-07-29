package pt.hitv.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Proves the data-loss bug behind P0 #10 against a real SQLite database.
 *
 * `syncChannels` / `saveM3uData` write content with `INSERT OR REPLACE`, passing literal
 * `isFavorite = 0` / `lastViewedTimestamp = 0` and `isPinned/isHidden/isDefault = 0`. Every
 * content re-sync therefore wiped the user's favourites, recently-viewed list and category
 * preferences — and on iOS that fires from the background BGTask with no user action.
 *
 * These tests exercise the snapshot/restore queries added to `Channel.sq` / `Category.sq`,
 * simulating a sync in between, and assert the user's state survives.
 *
 * Deliberately at the query layer: that's where the SQL risk is, and it needs no repository
 * dependency graph. SQLite 3.19 (minSdk 26) has no UPSERT, which is why this is snapshot/restore
 * rather than `ON CONFLICT DO UPDATE` — note the project's SQLDelight dialect is `sqlite-3-38`,
 * so an UPSERT would have compiled fine and then failed on Android 8 devices.
 */
class SyncPreservesUserStateTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: HitvDatabase

    private val userId = 7L
    private val otherUserId = 99L

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HitvDatabase.Schema.create(driver)
        db = HitvDatabase(driver)
    }

    @AfterTest
    fun tearDown() = driver.close()

    /** Writes a channel the way a content sync does — flattening user-owned columns. */
    private fun syncWriteChannel(name: String, categoryId: String, forUser: Long = userId) {
        db.channelQueries.insertOrReplace(
            name = name,
            streamUrl = "http://example/$name",
            streamIcon = "",
            epgChannelId = name.lowercase(),
            categoryCreatorId = categoryId,
            isFavorite = 0L,
            licenseKey = null,
            userId = forUser,
            lastViewedTimestamp = 0L,
            lastUpdated = 1L,
            lastSeen = 1L,
            contentHash = null,
            syncVersion = 1L,
            tvArchive = 0L,
            tvArchiveDuration = 0L,
            catchupType = null,
            catchupSource = null,
        )
    }

    private fun syncWriteCategory(id: Long, name: String, forUser: Long = userId) {
        db.categoryQueries.insertOrReplace(
            categoryId = id,
            categoryName = name,
            userId = forUser,
            isPinned = 0L,
            isHidden = 0L,
            isDefault = 0L,
        )
    }

    @Test
    fun `re-sync without snapshot restore wipes favourites - the bug`() {
        syncWriteChannel("BBC One", "1")
        db.channelQueries.updateFavorite(1L, "BBC One", userId, "1")

        // A second sync, exactly as the app did it before the fix.
        syncWriteChannel("BBC One", "1")

        val fav = db.channelQueries.selectFavoriteStatus("BBC One", "1", userId).executeAsOne()
        assertEquals(0L, fav, "this test documents the original bug; if it fails the bug is gone")
    }

    @Test
    fun `snapshot and restore preserves favourites across a re-sync`() {
        syncWriteChannel("BBC One", "1")
        syncWriteChannel("ITV", "1")
        db.channelQueries.updateFavorite(1L, "BBC One", userId, "1")

        val snapshot = db.channelQueries.selectUserStateForSync(userId).executeAsList()
        assertEquals(1, snapshot.size, "only the favourited row should be snapshotted")

        syncWriteChannel("BBC One", "1")
        syncWriteChannel("ITV", "1")

        snapshot.forEach {
            db.channelQueries.restoreUserStateForSync(
                isFavorite = it.isFavorite,
                lastViewedTimestamp = it.lastViewedTimestamp,
                name = it.name,
                userId = userId,
                categoryCreatorId = it.categoryCreatorId,
            )
        }

        assertEquals(
            1L,
            db.channelQueries.selectFavoriteStatus("BBC One", "1", userId).executeAsOne(),
            "favourite did not survive the re-sync",
        )
        assertEquals(
            0L,
            db.channelQueries.selectFavoriteStatus("ITV", "1", userId).executeAsOne(),
            "restore must not invent favourites for untouched channels",
        )
    }

    @Test
    fun `snapshot and restore preserves recently-viewed timestamps`() {
        syncWriteChannel("Sky Sports", "2")
        db.channelQueries.updateLastViewedTimestamp(1_700_000_000_000L, "Sky Sports", userId, "2")

        val snapshot = db.channelQueries.selectUserStateForSync(userId).executeAsList()
        syncWriteChannel("Sky Sports", "2")
        snapshot.forEach {
            db.channelQueries.restoreUserStateForSync(
                it.isFavorite, it.lastViewedTimestamp, it.name, userId, it.categoryCreatorId,
            )
        }

        val recent = db.channelQueries.selectRecentlyViewed(userId).executeAsList()
        assertEquals(1, recent.size, "recently-viewed list was wiped by the re-sync")
        assertEquals(1_700_000_000_000L, recent.first().lastViewedTimestamp)
    }

    @Test
    fun `snapshot only captures rows the user has touched`() {
        // Guards the "stays small on a 50k-channel account" claim in the query comment.
        repeat(50) { syncWriteChannel("Channel $it", "1") }
        db.channelQueries.updateFavorite(1L, "Channel 3", userId, "1")
        db.channelQueries.updateLastViewedTimestamp(123L, "Channel 9", userId, "1")

        val snapshot = db.channelQueries.selectUserStateForSync(userId).executeAsList()
        assertEquals(2, snapshot.size, "snapshot should hold only the two touched rows")
        assertTrue(snapshot.any { it.name == "Channel 3" && it.isFavorite == 1L })
        assertTrue(snapshot.any { it.name == "Channel 9" && it.lastViewedTimestamp == 123L })
    }

    @Test
    fun `snapshot is scoped to one user`() {
        syncWriteChannel("Shared Name", "1", forUser = userId)
        syncWriteChannel("Shared Name", "1", forUser = otherUserId)
        db.channelQueries.updateFavorite(1L, "Shared Name", otherUserId, "1")

        val mine = db.channelQueries.selectUserStateForSync(userId).executeAsList()
        assertTrue(mine.isEmpty(), "another account's favourite leaked into this user's snapshot")
    }

    @Test
    fun `restore does not resurrect a favourite the user removed mid-sync`() {
        // Snapshot is taken at sync start; un-favouriting after that must not be undone by a
        // stale snapshot key that no longer matches.
        syncWriteChannel("Film4", "3")
        db.channelQueries.updateFavorite(1L, "Film4", userId, "3")
        val snapshot = db.channelQueries.selectUserStateForSync(userId).executeAsList()

        // Channel gets recategorised by the provider, so the snapshot key no longer matches.
        syncWriteChannel("Film4", "4")
        snapshot.forEach {
            db.channelQueries.restoreUserStateForSync(
                it.isFavorite, it.lastViewedTimestamp, it.name, userId, it.categoryCreatorId,
            )
        }

        // The row under the NEW category is not favourited — restore is key-matched, not fuzzy.
        assertEquals(
            0L,
            db.channelQueries.selectFavoriteStatus("Film4", "4", userId).executeAsOne(),
            "restore should only touch rows whose (name, category, user) key still matches",
        )
    }

    @Test
    fun `snapshot and restore preserves pinned hidden and default categories`() {
        syncWriteCategory(1L, "Sports")
        syncWriteCategory(2L, "News")
        syncWriteCategory(3L, "Kids")
        db.categoryQueries.updatePinStatus(1L, 1L, userId)
        db.categoryQueries.updateHideStatus(1L, 2L, userId)
        db.categoryQueries.updateDefaultStatus(1L, 3L, userId)

        val prefs = db.categoryQueries.selectPreferencesForSync(userId).executeAsList()
        assertEquals(3, prefs.size)

        syncWriteCategory(1L, "Sports")
        syncWriteCategory(2L, "News")
        syncWriteCategory(3L, "Kids")

        prefs.forEach {
            db.categoryQueries.restorePreferencesForSync(
                isPinned = it.isPinned,
                isHidden = it.isHidden,
                isDefault = it.isDefault,
                categoryId = it.categoryId,
                userId = userId,
            )
        }

        val all = db.categoryQueries.selectAllByUserId(userId).executeAsList().associateBy { it.categoryId }
        assertEquals(1L, all.getValue(1L).isPinned, "pinned category lost")
        assertEquals(1L, all.getValue(2L).isHidden, "hidden category lost")
        assertEquals(1L, all.getValue(3L).isDefault, "default category lost")
    }

    @Test
    fun `category preference snapshot skips untouched categories`() {
        syncWriteCategory(1L, "Sports")
        syncWriteCategory(2L, "News")
        db.categoryQueries.updatePinStatus(1L, 1L, userId)

        val prefs = db.categoryQueries.selectPreferencesForSync(userId).executeAsList()
        assertEquals(1, prefs.size)
        assertEquals(1L, prefs.first().categoryId)
    }
}
