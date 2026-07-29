package pt.hitv.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotNull

/**
 * Pins what happens when a user logs in again to an account the app already knows.
 *
 * `insert` is `INSERT OR IGNORE` against `UNIQUE(username, hostname)`, so on its own a re-login is a
 * no-op — the newly entered password, the refreshed expiry and the server's allowed output formats
 * are all silently discarded. Change your password at the provider and re-enter it, and the app
 * carries on authenticating with the old one; renew a subscription and it keeps showing the old
 * expiry date.
 *
 * The original handles this explicitly: `DAOUserCredentials.insertOrGetUserId` detects the -1 rowid
 * that signals an ignored insert and calls `updateCredentials` on the existing row. The port had
 * `updateCredentials` in the schema and no call site.
 *
 * These tests cover the SQL contract `AccountManagerRepositoryImpl.saveCredentials` now relies on:
 * that the insert really is ignored (so the bug is demonstrated, not asserted), that the follow-up
 * update refreshes exactly the right three columns, and that it leaves the user's own settings and
 * every other account alone.
 */
class CredentialsReloginTest {

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

    private fun insert(
        username: String = "user1",
        password: String = "enc-v1",
        hostname: String = "http://host/",
        expiry: String? = "2026-01-01",
        epgUrl: String? = "http://epg/",
        formats: String? = "m3u8",
        preview: Long = 1L,
    ) = db.userCredentialsQueries.insert(
        username = username,
        encryptedPassword = password,
        hostname = hostname,
        expirationDate = expiry,
        epgUrl = epgUrl,
        allowedOutputFormats = formats,
        channelPreviewEnabled = preview,
    )

    private fun row(username: String = "user1", hostname: String = "http://host/") =
        db.userCredentialsQueries.selectAll().executeAsList()
            .single { it.username == username && it.hostname == hostname }

    @Test
    fun `a second insert for the same account is ignored - the bug`() {
        insert(password = "enc-v1", expiry = "2026-01-01", formats = "m3u8")
        insert(password = "enc-v2", expiry = "2027-06-30", formats = "m3u8,ts")

        // Only one account, and it still holds the *original* values. This is exactly what a
        // re-login did before the fix.
        assertEquals(1, db.userCredentialsQueries.selectAll().executeAsList().size)
        val r = row()
        assertEquals("enc-v1", r.encryptedPassword)
        assertEquals("2026-01-01", r.expirationDate)
        assertEquals("m3u8", r.allowedOutputFormats)
    }

    @Test
    fun `the follow-up update refreshes password expiry and formats`() {
        insert(password = "enc-v1", expiry = "2026-01-01", formats = "m3u8")
        val userId = db.userCredentialsQueries.selectUserId("user1", "http://host/").executeAsOne()

        db.userCredentialsQueries.updateCredentials(
            encryptedPassword = "enc-v2",
            expirationDate = "2027-06-30",
            allowedOutputFormats = "m3u8,ts",
            userId = userId,
        )

        val r = row()
        assertEquals("enc-v2", r.encryptedPassword)
        assertEquals("2027-06-30", r.expirationDate)
        assertEquals("m3u8,ts", r.allowedOutputFormats)
    }

    @Test
    fun `the update leaves the user's own settings alone`() {
        // epgUrl and channelPreviewEnabled are the user's choices, not provider facts. The original
        // updates neither, and a re-login must not reset them.
        insert(epgUrl = "http://my-own-epg/", preview = 0L)
        val userId = db.userCredentialsQueries.selectUserId("user1", "http://host/").executeAsOne()

        db.userCredentialsQueries.updateCredentials(
            encryptedPassword = "enc-v2",
            expirationDate = "2027-06-30",
            allowedOutputFormats = "ts",
            userId = userId,
        )

        val r = row()
        assertEquals("http://my-own-epg/", r.epgUrl)
        assertEquals(0L, r.channelPreviewEnabled)
    }

    @Test
    fun `the account keeps its userId across a re-login`() {
        // Everything the user owns is keyed on userId — channels, favourites, watch history. A
        // re-login that renumbered the account would orphan all of it.
        insert()
        val before = db.userCredentialsQueries.selectUserId("user1", "http://host/").executeAsOne()

        insert(password = "enc-v2")

        assertEquals(before, db.userCredentialsQueries.selectUserId("user1", "http://host/").executeAsOne())
    }

    @Test
    fun `the same username on a different host is a separate account`() {
        // The unique index is (username, hostname), so one provider login reused on another server
        // must not collide.
        insert(hostname = "http://host-a/")
        insert(hostname = "http://host-b/")

        assertEquals(2, db.userCredentialsQueries.selectAll().executeAsList().size)
    }

    @Test
    fun `updating one account does not touch another`() {
        insert(username = "user1", password = "a-v1")
        insert(username = "user2", password = "b-v1")
        val id1 = db.userCredentialsQueries.selectUserId("user1", "http://host/").executeAsOne()

        db.userCredentialsQueries.updateCredentials(
            encryptedPassword = "a-v2",
            expirationDate = "2027-01-01",
            allowedOutputFormats = "ts",
            userId = id1,
        )

        assertEquals("a-v2", row("user1").encryptedPassword)
        assertEquals("b-v1", row("user2").encryptedPassword)
    }

    @Test
    fun `selectUserId finds the row the update targets`() {
        // saveCredentials looks the id up by (username, hostname) straight after the insert; if
        // that ever returned null the update would be skipped and the bug would return silently.
        insert()
        assertNotNull(db.userCredentialsQueries.selectUserId("user1", "http://host/").executeAsOneOrNull())
    }
}
