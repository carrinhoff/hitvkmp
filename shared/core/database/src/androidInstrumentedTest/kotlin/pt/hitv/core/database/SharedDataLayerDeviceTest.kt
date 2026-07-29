package pt.hitv.core.database

import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.Query
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Runs the shared data layer on a device against Android's own SQLite.
 *
 * ## Why this exists on top of the JVM suites
 *
 * The unit tests link `JdbcSqliteDriver`, which is a **desktop** SQLite bundled with the JVM driver
 * — typically a recent build with every feature enabled. Neither shipping platform uses that. The
 * app runs on `AndroidSqliteDriver` over Android's system SQLite, and on iOS `NativeSqliteDriver`
 * over Apple's. Both are *embedded* SQLite libraries, older and more conservative than the desktop
 * one, and SQL that the desktop build accepts can fail on them at runtime — the exact class of bug
 * that only appears once the app is on a phone.
 *
 * Roughly 90% of this project is shared code, and the data layer is shared in full. So exercising
 * it here covers the same statements iOS will execute, on the same *kind* of SQLite engine, using
 * the same SQLDelight-generated code. It does not replace a pass on real iOS hardware — the drivers
 * are not identical and the platform actuals differ — but it does move the shared half of the port
 * from "tested on desktop SQLite" to "tested on an embedded SQLite on a running device".
 *
 * Each test below corresponds to a fix made in this pass, chosen for the ones whose risk is
 * specifically *SQL dialect and engine behaviour* rather than Kotlin logic.
 */
class SharedDataLayerDeviceTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: HitvDatabase

    private val userId = 1L
    private val dbName = "hitv-device-test.db"

    @BeforeTest
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(dbName)
        // The real driver the app ships with, not an in-memory stand-in.
        driver = AndroidSqliteDriver(
            schema = HitvDatabase.Schema,
            context = context,
            name = dbName,
        )
        db = HitvDatabase(driver)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(dbName)
    }

    // ---- helpers -------------------------------------------------------------------------

    private fun channel(
        name: String,
        icon: String = "icon.png",
        category: String = "1",
        forUser: Long = userId,
        replace: Boolean = true,
    ) {
        val args = listOf(name, "http://example/$name", icon, name.lowercase(), category)
        if (replace) {
            db.channelQueries.insertOrReplace(
                name = args[0], streamUrl = args[1], streamIcon = args[2],
                epgChannelId = args[3], categoryCreatorId = args[4],
                isFavorite = 0L, licenseKey = null, userId = forUser,
                lastViewedTimestamp = 0L, lastUpdated = 1L, lastSeen = 1L,
                contentHash = null, syncVersion = 1L, tvArchive = 0L,
                tvArchiveDuration = 0L, catchupType = null, catchupSource = null,
            )
        } else {
            db.channelQueries.insertOrIgnore(
                name = args[0], streamUrl = args[1], streamIcon = args[2],
                epgChannelId = args[3], categoryCreatorId = args[4],
                isFavorite = 0L, licenseKey = null, userId = forUser,
                lastViewedTimestamp = 0L, lastUpdated = 1L, lastSeen = 1L,
                contentHash = null, syncVersion = 1L, tvArchive = 0L,
                tvArchiveDuration = 0L, catchupType = null, catchupSource = null,
            )
        }
    }

    private fun slots(vararg words: String): List<String> =
        List(6) { i -> words.getOrNull(i)?.let { "%${it.lowercase()}%" } ?: "" }

    // ---- schema ----------------------------------------------------------------------------

    @Test
    fun schemaCreatesOnDeviceSqlite() {
        // Every CREATE TABLE, INDEX, VIRTUAL TABLE and TRIGGER in the .sq files has to be accepted
        // by the embedded engine. The FTS4 virtual tables and their triggers are the risky part —
        // desktop SQLite always has FTS compiled in, embedded builds do not always agree.
        val tables = driver.executeQuery(
            null,
            "SELECT name FROM sqlite_master WHERE type='table' ORDER BY name;",
            { cursor ->
                val names = mutableListOf<String>()
                while (cursor.next().value) names += cursor.getString(0).orEmpty()
                app.cash.sqldelight.db.QueryResult.Value(names)
            },
            0,
        ).value

        listOf("Channel", "Movie", "TvShow", "CustomGroup", "CustomGroupChannel", "UserCredentials")
            .forEach { assertTrue(it in tables, "table $it missing on device; got $tables") }
        assertTrue("Movie_fts" in tables, "FTS4 virtual table missing — FTS may not be compiled in")
    }

    @Test
    fun ftsTriggersMirrorInserts() {
        // The FTS mirror is trigger-driven. If triggers silently did not fire, search would return
        // nothing while everything else looked healthy.
        db.movieQueries.insertOrIgnore(
            name = "Top Gun", streamId = "1", streamIcon = "", rating = "0", added = "0",
            categoryCreatorId = "1", containerExtension = "mp4", isFavorite = 0L,
            userId = userId, lastViewedTimestamp = 0L, lastUpdated = 0L, lastSeen = 0L,
            contentHash = null, syncVersion = 1L,
        )

        val hits = db.movieQueries.searchFts("Top*", userId, 10L, 0L).executeAsList()
        assertEquals(1, hits.size, "FTS trigger did not mirror the insert on device")
    }

    // ---- differential sync: the custom-group data loss ---------------------------------------

    @Test
    fun insertOrReplaceRenumbersChannelIdOnDevice() {
        // The behaviour behind "every sync emptied your custom groups". Confirming it here matters
        // because REPLACE-on-unique-index semantics are engine behaviour, not Kotlin.
        channel("Sport TV")
        val first = db.channelQueries.selectAllForSync(userId).executeAsList().single().channelId

        channel("Sport TV")
        val second = db.channelQueries.selectAllForSync(userId).executeAsList().single().channelId

        assertNotEquals(first, second)
    }

    @Test
    fun insertOrIgnoreKeepsChannelIdStableOnDevice() {
        // What differential sync relies on: matched rows are UPDATEd, never replaced, so the id
        // custom groups reference survives.
        channel("Sport TV", replace = false)
        val first = db.channelQueries.selectAllForSync(userId).executeAsList().single().channelId

        channel("Sport TV", replace = false)
        val rows = db.channelQueries.selectAllForSync(userId).executeAsList()

        assertEquals(1, rows.size)
        assertEquals(first, rows.single().channelId)
    }

    @Test
    fun customGroupMembershipSurvivesAnUpdateOnDevice() {
        channel("Sport TV", replace = false)
        val channelId = db.channelQueries.selectAllForSync(userId).executeAsList().single().channelId

        db.customGroupQueries.insertGroup(
            groupName = "Favourites", groupIcon = null, createdAt = 0L, updatedAt = 0L,
            sortOrder = 0L, isPinned = 0L, isHidden = 0L, isDefault = 0L,
        )
        val groupId = db.customGroupQueries.selectAllGroups().executeAsList().single().groupId
        db.customGroupQueries.addChannelToGroup(
            groupId = groupId, channelId = channelId, channelUserId = userId,
            position = 0L, addedAt = 0L,
        )

        db.channelQueries.updateById(
            name = "Sport TV", streamUrl = "http://example/new", streamIcon = "new.png",
            epgChannelId = "sport tv", categoryCreatorId = "1", isFavorite = 0L,
            licenseKey = null, lastViewedTimestamp = 0L, lastUpdated = 2L, lastSeen = 2L,
            contentHash = "h2", syncVersion = 2L, tvArchive = 0L, tvArchiveDuration = 0L,
            catchupType = null, catchupSource = null, channelId = channelId,
        )

        assertEquals(1, db.customGroupQueries.selectChannelsInGroup(groupId).executeAsList().size)
    }

    // ---- the flexible search slot guard ------------------------------------------------------

    @Test
    fun flexibleSearchIsWordOrderIndependentOnDevice() {
        channel("HD Sports", replace = false)
        val s = slots("sports", "hd")

        val found = db.channelQueries.searchByNameFlexible(
            userId = userId, w1 = s[0], w2 = s[1], w3 = s[2], w4 = s[3], w5 = s[4], w6 = s[5],
            limit = 10L, offset = 0L,
        ).executeAsList()

        assertEquals(listOf("HD Sports"), found.mapNotNull { it.name })
    }

    @Test
    fun unusedSearchSlotsAreInertOnDevice() {
        // The `:wN = '' OR ...` guard. If the engine treated the empty-string comparison
        // differently this would either drop every row or match every row.
        channel("Sport TV", replace = false)
        channel("BBC One", replace = false)
        val s = slots("sport")

        val found = db.channelQueries.searchByNameFlexible(
            userId = userId, w1 = s[0], w2 = s[1], w3 = s[2], w4 = s[3], w5 = s[4], w6 = s[5],
            limit = 10L, offset = 0L,
        ).executeAsList()

        assertEquals(listOf("Sport TV"), found.mapNotNull { it.name })
    }

    // ---- reactivity and transactions ---------------------------------------------------------

    @Test
    fun changeNotificationsFireOnDevice() {
        // Paging invalidation and every reactive repository flow depend on this. It is driver
        // behaviour, so the JVM suite proving it says nothing about the shipping drivers.
        var fired = 0
        val listener = Query.Listener { fired++ }
        driver.addListener("Channel", listener = listener)

        channel("Sport TV", replace = false)

        driver.removeListener("Channel", listener = listener)
        assertTrue(fired > 0, "no change notification on device — paged lists would never refresh")
    }

    @Test
    fun transactionsCoalesceNotificationsOnDevice() {
        db.categoryQueries.insertOrReplace(
            categoryId = 1L, categoryName = "News", userId = userId,
            isPinned = 0L, isHidden = 0L, isDefault = 0L,
        )
        var fired = 0
        val listener = Query.Listener { fired++ }
        driver.addListener("Category", listener = listener)

        db.categoryQueries.transaction {
            db.categoryQueries.clearAllDefaults(userId)
            db.categoryQueries.updateDefaultStatus(1L, 1L, userId)
        }

        driver.removeListener("Category", listener = listener)
        // One notification, so observers never render the intermediate "no default" state.
        assertEquals(1, fired)
    }

    @Test
    fun rollbackRestoresPreviousStateOnDevice() {
        db.categoryQueries.insertOrReplace(
            categoryId = 1L, categoryName = "News", userId = userId,
            isPinned = 0L, isHidden = 0L, isDefault = 1L,
        )

        runCatching {
            db.categoryQueries.transaction {
                db.categoryQueries.clearAllDefaults(userId)
                throw IllegalStateException("boom")
            }
        }

        val defaults = db.categoryQueries.selectAllByUserId(userId).executeAsList()
            .filter { it.isDefault != 0L }
        assertEquals(1, defaults.size, "rollback did not restore the default category on device")
    }

    // ---- credentials -------------------------------------------------------------------------

    @Test
    fun reloginUpdatesCredentialsOnDevice() {
        db.userCredentialsQueries.insert(
            username = "u", encryptedPassword = "enc-v1", hostname = "http://h/",
            expirationDate = "2026-01-01", epgUrl = null, allowedOutputFormats = "m3u8",
            channelPreviewEnabled = 1L,
        )
        val id = db.userCredentialsQueries.selectUserId("u", "http://h/").executeAsOne()

        // Second insert is ignored (the bug), the follow-up update is what fixes it.
        db.userCredentialsQueries.insert(
            username = "u", encryptedPassword = "enc-v2", hostname = "http://h/",
            expirationDate = "2027-06-30", epgUrl = null, allowedOutputFormats = "ts",
            channelPreviewEnabled = 1L,
        )
        db.userCredentialsQueries.updateCredentials(
            encryptedPassword = "enc-v2", expirationDate = "2027-06-30",
            allowedOutputFormats = "ts", userId = id,
        )

        val row = db.userCredentialsQueries.selectAll().executeAsList().single()
        assertEquals(id, row.userId, "re-login must not renumber the account")
        assertEquals("enc-v2", row.encryptedPassword)
        assertEquals("2027-06-30", row.expirationDate)
    }

    // ---- account teardown --------------------------------------------------------------------

    @Test
    fun epgChildRowsAreDeletableByUserIdOnDevice() {
        // The leak fix: Title/Description now filter on their own userId instead of selecting
        // through Programme, so teardown order cannot strand them.
        db.epgChannelQueries.insertOrReplace(
            channel_id = "ch1", display_name = "One", logo = null, userId = userId,
        )
        db.programmeQueries.insertProgramme(
            channel_name = "ch1", start_time = 1L, end_time = 2L, userId = userId, imageUrl = null,
        )
        val pid = db.programmeQueries.lastInsertProgrammeId().executeAsOne().MAX!!
        db.programmeQueries.insertTitle(title = "T", programme_id = pid, userId = userId)
        db.programmeQueries.insertDescription(desc = "D", programme_id = pid, userId = userId)

        // Deliberately parent-first, the order that used to strand the children.
        db.programmeQueries.deleteProgrammesByUserId(userId)
        db.programmeQueries.deleteTitlesByUserId(userId)
        db.programmeQueries.deleteDescriptionsByUserId(userId)

        val titles = driver.executeQuery(
            null, "SELECT COUNT(*) FROM Title;",
            { c -> app.cash.sqldelight.db.QueryResult.Value(if (c.next().value) c.getLong(0) else 0L) },
            0,
        ).value
        assertEquals(0L, titles, "EPG titles were stranded on device")
    }
}
