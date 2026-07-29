package pt.hitv.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Pins the `INSERT OR REPLACE` semantics that make channel identity unstable, and with it the
 * reason `PRAGMA foreign_keys = ON` still cannot simply be switched on.
 *
 * `Channel.channelId` is AUTOINCREMENT, but `channel_unique` is a UNIQUE index on
 * (name, streamIcon, categoryCreatorId, userId). In SQLite, REPLACE on a unique-index conflict is a
 * DELETE followed by an INSERT — so a channel that is completely unchanged comes back with a
 * **brand-new channelId**. Anything referencing it breaks silently.
 *
 * **The Xtream sync path no longer does this.** `DifferentialChannelSync` now matches existing rows
 * and UPDATEs them in place, exactly as the original's `DAOChannel.performDifferentialChannelSync`
 * does, so channelId survives and custom groups with it — see `DifferentialChannelSyncTest`.
 *
 * These tests remain because `saveM3uData` still writes with `INSERT OR REPLACE`, so the behaviour
 * below is live for M3U playlists. It is not fixed by simply reusing the differential path there:
 * that path keys on (name, categoryCreatorId), and `saveM3uData` synthesises categoryCreatorId from
 * the *ordinal* of the category in the parsed playlist (`index + 1`), so the key shifts whenever
 * the playlist's category ordering changes. Making M3U safe means giving those categories a stable
 * identity first. Recorded rather than half-fixed.
 *
 * The foreign-key situation is downstream of the same thing. The schema declares
 * `CustomGroupChannel.channelId REFERENCES Channel(channelId) ON DELETE CASCADE`, exactly as the
 * original's `EntityCustomGroupChannel` does, and Room enables enforcement on every connection
 * while neither SQLDelight driver does — a real divergence. But enabling it while any writer still
 * uses REPLACE would make things worse, not better:
 *
 *  - **Foreign keys off (today):** the CustomGroupChannel row survives pointing at the old
 *    channelId. The join matches nothing, so the channel disappears from the group and an
 *    unreachable row is left behind.
 *  - **Foreign keys on:** the REPLACE's internal DELETE fires ON DELETE CASCADE and the membership
 *    row is *deleted outright* — the same visible loss, now unrecoverable.
 *
 * So the pragma stays off until every writer is non-destructive.
 */
class ChannelIdStabilityTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: HitvDatabase

    private val userId = 1L

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HitvDatabase.Schema.create(driver)
        db = HitvDatabase(driver)
    }

    @AfterTest
    fun tearDown() = driver.close()

    /** Writes a channel exactly the way `StreamRepositoryImpl` sync does. */
    private fun syncWriteChannel(name: String = "Sport TV") {
        db.channelQueries.insertOrReplace(
            name = name,
            streamUrl = "http://example/$name",
            streamIcon = "icon.png",
            epgChannelId = name.lowercase(),
            categoryCreatorId = "1",
            isFavorite = 0L,
            licenseKey = null,
            userId = userId,
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

    private fun channelId(): Long =
        db.channelQueries.selectAllByUserId(userId).executeAsList().single().channelId

    @Test
    fun `re-syncing an unchanged channel gives it a new channelId`() {
        syncWriteChannel()
        val first = channelId()

        syncWriteChannel() // identical data, next sync
        val second = channelId()

        // Only one row — the unique index did its job and there is no duplicate.
        assertEquals(1, db.channelQueries.selectAllByUserId(userId).executeAsList().size)
        // ...but the identity changed underneath anything referencing it.
        assertNotEquals(first, second, "INSERT OR REPLACE was expected to renumber channelId")
    }

    @Test
    fun `a custom-group membership is stranded by the next sync`() {
        syncWriteChannel()
        val originalId = channelId()

        db.customGroupQueries.insertGroup(
            groupName = "Favourites",
            groupIcon = null,
            createdAt = 0L,
            updatedAt = 0L,
            sortOrder = 0L,
            isPinned = 0L,
            isHidden = 0L,
            isDefault = 0L,
        )
        val groupId = db.customGroupQueries.selectAllGroups().executeAsList().single().groupId
        db.customGroupQueries.addChannelToGroup(
            groupId = groupId,
            channelId = originalId,
            channelUserId = userId,
            position = 0L,
            addedAt = 0L,
        )
        assertEquals(1, db.customGroupQueries.selectChannelsInGroup(groupId).executeAsList().size)

        syncWriteChannel() // a routine content sync

        // The channel still exists and the membership row still exists, but they no longer refer
        // to each other — the group renders empty. This is the user-visible bug.
        assertNotEquals(originalId, channelId())
        assertTrue(
            db.customGroupQueries.selectChannelsInGroup(groupId).executeAsList().isEmpty(),
            "expected the group to have lost its channel after a re-sync",
        )
    }

    @Test
    fun `foreign keys are off, which is what keeps the stale row alive rather than deleting it`() {
        // Pins the current state so that enabling the pragma is a deliberate, visible decision
        // rather than an incidental side effect of some future driver change.
        val fk = driver.executeQuery(null, "PRAGMA foreign_keys;", { cursor ->
            app.cash.sqldelight.db.QueryResult.Value(
                if (cursor.next().value) cursor.getLong(0) else null
            )
        }, 0).value
        assertEquals(0L, fk, "foreign key enforcement is expected to be off by default")
    }
}
