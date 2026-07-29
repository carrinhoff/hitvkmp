package pt.hitv.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertTrue

/**
 * Exercises the word-order-independent channel search against a real SQLite database.
 *
 * The Kotlin half of this fix is pinned by `SearchUtilsTest`; the risk that test cannot reach is
 * the SQL itself — the `(:wN = '' OR LOWER(name) LIKE :wN)` guard that stands in for the dynamic
 * WHERE clause Room's @RawQuery gives the original. If that guard is wrong the query still runs
 * and still returns rows, just the wrong ones, so it needs a database to catch.
 *
 * Two separate defects are covered:
 *
 *  1. **Word order.** The port searched with one collapsed `%word1%word2%` pattern, which requires
 *     the words in the order typed. "sports hd" then missed "HD Sports". The original ANDs one
 *     `LOWER(name) LIKE '%word%'` per word, so any order matches.
 *
 *  2. **The custom-group picker searched userId 0.** `searchAllChannelsList` and
 *     `AllChannelsSearchPagingSource` called the single-user query with a hardcoded `0L` under a
 *     comment claiming it searched every account. `UserCredentials.userId` is INTEGER PRIMARY KEY
 *     AUTOINCREMENT, so SQLite issues 1 for the first account and never 0 — the picker's search
 *     returned nothing on every install. The original omits the userId predicate entirely.
 */
class FlexibleChannelSearchTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: HitvDatabase

    private val userId = 1L
    private val otherUserId = 2L

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HitvDatabase.Schema.create(driver)
        db = HitvDatabase(driver)
    }

    @AfterTest
    fun tearDown() = driver.close()

    private fun channel(name: String, forUser: Long = userId) {
        db.channelQueries.insertOrReplace(
            name = name,
            streamUrl = "http://example/$name",
            streamIcon = "",
            epgChannelId = name.lowercase(),
            categoryCreatorId = "1",
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

    /** Mirrors `SearchUtils.flexibleLikeSlots` — kept local so core:database stays dependency-free. */
    private fun slots(vararg words: String): List<String> =
        List(6) { i -> words.getOrNull(i)?.let { "%${it.lowercase()}%" } ?: "" }

    private fun search(vararg words: String, forUser: Long = userId): List<String> {
        val s = slots(*words)
        return db.channelQueries.searchByNameFlexible(
            userId = forUser,
            w1 = s[0], w2 = s[1], w3 = s[2], w4 = s[3], w5 = s[4], w6 = s[5],
            limit = 100L, offset = 0L,
        ).executeAsList().mapNotNull { it.name }
    }

    private fun searchAll(vararg words: String): List<String> {
        val s = slots(*words)
        return db.channelQueries.searchAllByNameFlexible(
            w1 = s[0], w2 = s[1], w3 = s[2], w4 = s[3], w5 = s[4], w6 = s[5],
            limit = 100L, offset = 0L,
        ).executeAsList().mapNotNull { it.name }
    }

    // ---- the word-order regression ------------------------------------------------------

    @Test
    fun `both word orders find the channel - the regression`() {
        channel("HD Sports")
        // The old `%sports%hd%` pattern matched neither ordering here, since the name has "HD"
        // first. Faithful behaviour: word order is irrelevant.
        assertEquals(listOf("HD Sports"), search("sports", "hd"))
        assertEquals(listOf("HD Sports"), search("hd", "sports"))
    }

    @Test
    fun `the old collapsed pattern really did miss it`() {
        channel("HD Sports")
        // Proves the bug rather than asserting it in prose: this is verbatim what the three call
        // sites built — `"%" + words.joinToString("%") + "%"` against the single-pattern query —
        // and it finds nothing, while the flexible query above finds the channel either way round.
        val collapsed = db.channelQueries
            .searchByName(userId, "%sports%hd%", 100L, 0L)
            .executeAsList()
        assertTrue(collapsed.isEmpty(), "expected the ordered pattern to miss, got $collapsed")

        // It only ever worked when the user happened to type the name's own word order.
        assertEquals(
            listOf("HD Sports"),
            db.channelQueries.searchByName(userId, "%hd%sports%", 100L, 0L)
                .executeAsList().mapNotNull { it.name },
        )
    }

    @Test
    fun `all words must still be present`() {
        channel("Sport TV 1")
        channel("Sport TV 2 HD")
        // AND semantics, not OR — adding a word narrows.
        assertEquals(listOf("Sport TV 2 HD"), search("sport", "hd"))
        assertEquals(listOf("Sport TV 1", "Sport TV 2 HD"), search("sport"))
    }

    @Test
    fun `a word matching nothing yields no rows`() {
        channel("Sport TV 1")
        assertTrue(search("sport", "brasil").isEmpty())
    }

    // ---- the empty-slot guard -----------------------------------------------------------

    @Test
    fun `unused slots do not filter anything out`() {
        channel("Sport TV 1")
        channel("BBC One")
        // One real word, five empty slots: the empty slots must be inert. If the `= ''` guard
        // were wrong these would either drop every row or match every row.
        assertEquals(listOf("Sport TV 1"), search("sport"))
    }

    @Test
    fun `an all-empty slot set returns every channel for the user`() {
        channel("Sport TV 1")
        channel("BBC One")
        assertEquals(listOf("BBC One", "Sport TV 1"), search())
    }

    @Test
    fun `search is case-insensitive on the column`() {
        channel("SPORT TV")
        assertEquals(listOf("SPORT TV"), search("sport", "tv"))
    }

    @Test
    fun `results stay ordered by name`() {
        channel("Zeta HD")
        channel("Alpha HD")
        channel("Mid HD")
        assertEquals(listOf("Alpha HD", "Mid HD", "Zeta HD"), search("hd"))
    }

    @Test
    fun `all six slots can be used at once`() {
        channel("A B C D E F extra")
        channel("A B C D E")
        assertEquals(listOf("A B C D E F extra"), search("a", "b", "c", "d", "e", "f"))
    }

    // ---- user scoping -------------------------------------------------------------------

    @Test
    fun `the single-user query stays scoped to its user`() {
        channel("Sport TV", forUser = userId)
        channel("Sport TV Other", forUser = otherUserId)
        assertEquals(listOf("Sport TV"), search("sport", forUser = userId))
        assertEquals(listOf("Sport TV Other"), search("sport", forUser = otherUserId))
    }

    @Test
    fun `the all-users query spans accounts - the custom-group picker regression`() {
        channel("Sport TV", forUser = userId)
        channel("Sport TV Other", forUser = otherUserId)
        assertEquals(listOf("Sport TV", "Sport TV Other"), searchAll("sport"))
    }

    @Test
    fun `no channel is ever stored under userId 0, which is what the old picker searched`() {
        channel("Sport TV", forUser = userId)
        channel("Sport TV Other", forUser = otherUserId)
        // The exact call the picker used to make. Empty on every install — the bug.
        assertTrue(search("sport", forUser = 0L).isEmpty())
    }
}
