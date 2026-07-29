package pt.hitv.core.data.sync

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import pt.hitv.core.database.HitvDatabase
import pt.hitv.core.model.LiveStream
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Covers the differential channel sync against a real SQLite database.
 *
 * These mirror the original's own instrumentation tests — `DAOChannelTest`'s
 * `performDifferentialChannelSyncInsertsNewChannels` / `...DeletesStaleChannels` /
 * `...PreservesFavorites` — plus the case that motivated the port: channelId stability, which the
 * original gets for free and the port was destroying on every sync.
 *
 * `syncTimestamp` is injected rather than read from the clock so the 7-day stale window can be
 * crossed deterministically.
 */
class DifferentialChannelSyncTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: HitvDatabase
    private lateinit var sync: DifferentialChannelSync

    private val userId = 1
    private val uid = 1L
    private val mainUrl = "http://example/live/"
    private val t0 = 1_000_000L

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HitvDatabase.Schema.create(driver)
        db = HitvDatabase(driver)
        sync = DifferentialChannelSync(db.channelQueries)
    }

    @AfterTest
    fun tearDown() = driver.close()

    private fun liveStream(
        name: String,
        streamId: Int = 1,
        categoryId: Int = 1,
        streamIcon: String = "icon.png",
        tvArchive: Int = 0,
    ) = LiveStream(
        num = streamId,
        name = name,
        streamType = "live",
        streamId = streamId,
        streamIcon = streamIcon,
        epgChannelId = name.lowercase(),
        added = 0,
        categoryId = categoryId,
        customSid = "",
        tvArchive = tvArchive,
        directSource = "",
        tvArchiveDuration = 0,
    )

    private fun channels() = db.channelQueries.selectAllForSync(uid).executeAsList()

    // ---- the original's cases -----------------------------------------------------------

    @Test
    fun `inserts new channels`() {
        val result = sync.sync(
            listOf(liveStream("CH1", streamId = 1), liveStream("CH2", streamId = 2, categoryId = 2)),
            userId, mainUrl, t0,
        )

        assertEquals(2, result.inserted)
        assertEquals(0, result.updated)
        assertEquals(0, result.deleted)
        assertEquals(2, channels().size)
    }

    @Test
    fun `deletes stale channels`() {
        sync.sync(listOf(liveStream("OldCH")), userId, mainUrl, t0)
        assertEquals(1, channels().size)

        // Eight days later, the provider no longer lists it.
        val result = sync.sync(emptyList(), userId, mainUrl, t0 + 8L * 24 * 60 * 60 * 1000)

        assertEquals(1, result.deleted)
        assertTrue(channels().isEmpty())
    }

    @Test
    fun `a channel missing for less than the retention window survives`() {
        sync.sync(listOf(liveStream("Flaky")), userId, mainUrl, t0)

        // A provider hiccup six days in must not drop the channel — nor its favourite flag.
        val result = sync.sync(emptyList(), userId, mainUrl, t0 + 6L * 24 * 60 * 60 * 1000)

        assertEquals(0, result.deleted)
        assertEquals(1, channels().size)
    }

    @Test
    fun `preserves favorites across an update`() {
        sync.sync(listOf(liveStream("Sports TV", streamIcon = "icon1")), userId, mainUrl, t0)
        val id = channels().single().channelId
        db.channelQueries.updateFavorite(1L, "Sports TV", uid, "1")

        // Same channel, new artwork — the API knows nothing about favourites.
        val result = sync.sync(
            listOf(liveStream("Sports TV", streamIcon = "icon1_new")), userId, mainUrl, t0 + 1000,
        )

        assertEquals(1, result.updated)
        val after = channels().single()
        assertEquals(1L, after.isFavorite, "favourite flag was expected to survive the sync")
        assertEquals("icon1_new", after.streamIcon)
        assertEquals(id, after.channelId)
    }

    @Test
    fun `preserves watch history across an update`() {
        sync.sync(listOf(liveStream("Sports TV", streamIcon = "a")), userId, mainUrl, t0)
        db.channelQueries.updateLastViewedTimestamp(555L, "Sports TV", uid, "1")

        sync.sync(listOf(liveStream("Sports TV", streamIcon = "b")), userId, mainUrl, t0 + 1000)

        assertEquals(555L, channels().single().lastViewedTimestamp)
    }

    // ---- the regression this port was written for ---------------------------------------

    @Test
    fun `channelId is stable across syncs - the regression`() {
        sync.sync(listOf(liveStream("Sport TV")), userId, mainUrl, t0)
        val first = channels().single().channelId

        sync.sync(listOf(liveStream("Sport TV")), userId, mainUrl, t0 + 1000)

        assertEquals(first, channels().single().channelId, "INSERT OR REPLACE renumbering is back")
    }

    @Test
    fun `custom group memberships survive a sync`() {
        sync.sync(listOf(liveStream("Sport TV")), userId, mainUrl, t0)
        val channelId = channels().single().channelId

        db.customGroupQueries.insertGroup(
            groupName = "Favourites", groupIcon = null, createdAt = 0L, updatedAt = 0L,
            sortOrder = 0L, isPinned = 0L, isHidden = 0L, isDefault = 0L,
        )
        val groupId = db.customGroupQueries.selectAllGroups().executeAsList().single().groupId
        db.customGroupQueries.addChannelToGroup(
            groupId = groupId, channelId = channelId, channelUserId = uid, position = 0L, addedAt = 0L,
        )

        sync.sync(listOf(liveStream("Sport TV")), userId, mainUrl, t0 + 1000)

        assertEquals(
            1,
            db.customGroupQueries.selectChannelsInGroup(groupId).executeAsList().size,
            "the group lost its channel — channelId was not preserved",
        )
    }

    // ---- change detection ----------------------------------------------------------------

    @Test
    fun `an unchanged channel is not rewritten`() {
        sync.sync(listOf(liveStream("Sport TV")), userId, mainUrl, t0)
        val before = channels().single()

        val result = sync.sync(listOf(liveStream("Sport TV")), userId, mainUrl, t0 + 1000)

        assertEquals(0, result.updated)
        assertEquals(1, result.unchanged)
        val after = channels().single()
        // syncVersion and lastUpdated stay put; only lastSeen advances, via markAsSeen.
        assertEquals(before.syncVersion, after.syncVersion)
        assertEquals(before.lastUpdated, after.lastUpdated)
        assertEquals(t0 + 1000, after.lastSeen)
    }

    @Test
    fun `a changed channel bumps syncVersion`() {
        sync.sync(listOf(liveStream("Sport TV", streamIcon = "a")), userId, mainUrl, t0)
        assertEquals(1L, channels().single().syncVersion)

        sync.sync(listOf(liveStream("Sport TV", streamIcon = "b")), userId, mainUrl, t0 + 1000)

        assertEquals(2L, channels().single().syncVersion)
    }

    @Test
    fun `catch-up availability changes are picked up`() {
        // updateById originally omitted the catchup columns, so a channel that gained catch-up
        // would never show it once sync stopped using INSERT OR REPLACE.
        sync.sync(listOf(liveStream("Sport TV", tvArchive = 0)), userId, mainUrl, t0)
        assertEquals(0L, channels().single().tvArchive)

        sync.sync(listOf(liveStream("Sport TV", tvArchive = 1)), userId, mainUrl, t0 + 1000)

        assertEquals(1L, channels().single().tvArchive)
    }

    @Test
    fun `a renamed channel is treated as new and the old one ages out`() {
        // Faithful to the original: the match key is name + category, so a rename is an insert.
        sync.sync(listOf(liveStream("Old Name")), userId, mainUrl, t0)

        val result = sync.sync(
            listOf(liveStream("New Name")), userId, mainUrl, t0 + 8L * 24 * 60 * 60 * 1000,
        )

        assertEquals(1, result.inserted)
        assertEquals(1, result.deleted)
        assertEquals(listOf("New Name"), channels().map { it.name })
    }

    @Test
    fun `other users are untouched`() {
        sync.sync(listOf(liveStream("Shared")), userId, mainUrl, t0)
        sync.sync(listOf(liveStream("Shared")), 2, mainUrl, t0)

        // Syncing user 1 with an empty list far in the future must not reap user 2's channel.
        sync.sync(emptyList(), userId, mainUrl, t0 + 8L * 24 * 60 * 60 * 1000)

        assertTrue(db.channelQueries.selectAllForSync(uid).executeAsList().isEmpty())
        assertEquals(1, db.channelQueries.selectAllForSync(2L).executeAsList().size)
    }

    @Test
    fun `handles more channels than the mark-seen batch size`() {
        // SQLite's variable limit is why markAsSeen is chunked; this crosses the boundary.
        val many = (1..(MARK_SEEN_BATCH + 25)).map { liveStream("CH$it", streamId = it) }
        sync.sync(many, userId, mainUrl, t0)

        val result = sync.sync(many, userId, mainUrl, t0 + 1000)

        assertEquals(many.size, result.unchanged)
        assertEquals(many.size, channels().size)
        assertTrue(channels().all { it.lastSeen == t0 + 1000 })
    }
}
