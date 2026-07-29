package pt.hitv.core.database

import app.cash.sqldelight.Query
import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Pins the two things a transaction buys a clear-then-set mutation, both of which the port had lost.
 *
 * The original marks these `@Transaction` on the DAO — `setDefaultCategory`,
 * `deleteCustomGroupWithChannels`, `reorderChannelsInGroup`, `replaceChannelsInGroup`. The port ran
 * the same statements bare.
 *
 *  1. **Atomicity.** `setDefaultCategory` clears every default and then sets one. A failure between
 *     the two leaves *no* default at all, silently discarding a setting the user chose.
 *
 *  2. **One notification instead of two.** This only started to matter once the repository flows
 *     went reactive. SQLDelight defers change notifications until a transaction commits; without
 *     one, `clearAllDefaults()` publishes on its own, so every observer sees an intermediate state
 *     with nothing selected before the real value lands — a visible flicker in Manage Categories,
 *     and a spurious extra recomposition everywhere else.
 *
 * The listener-count assertions below are the direct evidence for (2): the same two writes notify
 * twice unwrapped and once wrapped.
 */
class TransactionNotificationTest {

    private lateinit var driver: JdbcSqliteDriver
    private lateinit var db: HitvDatabase

    private val userId = 1L

    @BeforeTest
    fun setUp() {
        driver = JdbcSqliteDriver(JdbcSqliteDriver.IN_MEMORY)
        HitvDatabase.Schema.create(driver)
        db = HitvDatabase(driver)
        category(1L, "News")
        category(2L, "Sport")
    }

    @AfterTest
    fun tearDown() = driver.close()

    private fun category(id: Long, name: String, isDefault: Long = 0L) {
        db.categoryQueries.insertOrReplace(
            categoryId = id, categoryName = name, userId = userId,
            isPinned = 0L, isHidden = 0L, isDefault = isDefault,
        )
    }

    private fun defaults(): List<String> =
        db.categoryQueries.selectAllByUserId(userId).executeAsList()
            .filter { it.isDefault != 0L }
            .map { it.categoryName }

    /** Counts notifications on the Category table for the duration of [block]. */
    private fun countingNotifications(block: () -> Unit): Int {
        var fires = 0
        val listener = Query.Listener { fires++ }
        driver.addListener("Category", listener = listener)
        try {
            block()
        } finally {
            driver.removeListener("Category", listener = listener)
        }
        return fires
    }

    @Test
    fun `unwrapped clear-then-set notifies twice - the flicker`() {
        val fires = countingNotifications {
            db.categoryQueries.clearAllDefaults(userId)
            db.categoryQueries.updateDefaultStatus(1L, 2L, userId)
        }

        // Two notifications means observers rendered the middle state: nothing selected.
        assertEquals(2, fires)
    }

    @Test
    fun `a transaction publishes the change once`() {
        val fires = countingNotifications {
            db.categoryQueries.transaction {
                db.categoryQueries.clearAllDefaults(userId)
                db.categoryQueries.updateDefaultStatus(1L, 2L, userId)
            }
        }

        assertEquals(1, fires, "the intermediate no-default state was published")
        assertEquals(listOf("Sport"), defaults())
    }

    @Test
    fun `a rollback leaves the previous default intact`() {
        category(1L, "News", isDefault = 1L)
        assertEquals(listOf("News"), defaults())

        runCatching {
            db.categoryQueries.transaction {
                db.categoryQueries.clearAllDefaults(userId)
                throw IllegalStateException("boom, midway")
            }
        }

        // Unwrapped, this is where the user's choice silently disappears.
        assertEquals(listOf("News"), defaults())
    }

    @Test
    fun `a rollback publishes nothing at all`() {
        val fires = countingNotifications {
            runCatching {
                db.categoryQueries.transaction {
                    db.categoryQueries.clearAllDefaults(userId)
                    throw IllegalStateException("boom, midway")
                }
            }
        }

        assertEquals(0, fires, "a rolled-back write must not notify")
    }

    @Test
    fun `bulk group membership writes notify once, not once per row`() {
        // The addChannelsToGroup path: adding a few hundred channels used to be a few hundred
        // separate commits, each waking every observer.
        var fires = 0
        val listener = Query.Listener { fires++ }
        driver.addListener("CustomGroupChannel", listener = listener)

        db.customGroupQueries.transaction {
            repeat(50) { i ->
                db.customGroupQueries.addChannelToGroup(
                    groupId = 1L, channelId = i.toLong(), channelUserId = userId,
                    position = i.toLong(), addedAt = 0L,
                )
            }
        }
        driver.removeListener("CustomGroupChannel", listener = listener)

        assertEquals(1, fires)
    }
}
