package pt.hitv.core.database

import app.cash.sqldelight.driver.jdbc.sqlite.JdbcSqliteDriver
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals

/**
 * Covers `selectAllGroupsWithChannelCount`, which replaced "list the groups, then run one count
 * query per group" in `CustomGroupRepositoryImpl.getAllCustomGroups`.
 *
 * Two things needed fixing there and both are load-bearing here:
 *
 *  - The old pairing was an N+1, one query per group on a screen that lists all of them.
 *  - More importantly it could not be made reactive. SQLDelight keys its change notifications on
 *    the tables a query reads, and `selectAllGroups` reads only `CustomGroup` — so adding or
 *    removing a channel changed the count with nothing to announce it. Reading both tables in one
 *    query means membership changes notify too.
 *
 * The count itself is the kind of SQL that is quietly wrong rather than broken: with a LEFT JOIN,
 * `COUNT(*)` counts the synthesised all-null row and reports **1** for an empty group. The query
 * uses `COUNT(gc.id)`, which ignores nulls and reports 0. The empty-group case below is the whole
 * reason this file exists.
 */
class CustomGroupChannelCountTest {

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

    private fun group(name: String, sortOrder: Long = 0L): Long {
        db.customGroupQueries.insertGroup(
            groupName = name, groupIcon = null, createdAt = 0L, updatedAt = 0L,
            sortOrder = sortOrder, isPinned = 0L, isHidden = 0L, isDefault = 0L,
        )
        return db.customGroupQueries.lastInsertGroupId().executeAsOne().MAX!!
    }

    private fun addChannel(groupId: Long, channelId: Long) {
        db.customGroupQueries.addChannelToGroup(
            groupId = groupId, channelId = channelId, channelUserId = 1L,
            position = 0L, addedAt = 0L,
        )
    }

    private fun counts(): Map<String, Long> =
        db.customGroupQueries.selectAllGroupsWithChannelCount()
            .executeAsList()
            .associate { it.groupName to it.channelCount }

    @Test
    fun `an empty group counts zero, not one`() {
        group("Empty")

        // COUNT(*) would say 1 here — the LEFT JOIN's null row.
        assertEquals(mapOf("Empty" to 0L), counts())
    }

    @Test
    fun `counts match the number of channels in each group`() {
        val a = group("A", sortOrder = 0L)
        val b = group("B", sortOrder = 1L)
        group("C", sortOrder = 2L)

        addChannel(a, 1L)
        addChannel(a, 2L)
        addChannel(a, 3L)
        addChannel(b, 4L)

        assertEquals(mapOf("A" to 3L, "B" to 1L, "C" to 0L), counts())
    }

    @Test
    fun `every group is listed even when none has channels`() {
        // GROUP BY on the joined table must not drop rows.
        group("A", sortOrder = 0L)
        group("B", sortOrder = 1L)

        assertEquals(2, db.customGroupQueries.selectAllGroupsWithChannelCount().executeAsList().size)
    }

    @Test
    fun `ordering matches the query it replaced`() {
        group("Zeta", sortOrder = 0L)
        group("Alpha", sortOrder = 1L)
        group("Beta", sortOrder = 0L)

        // sortOrder ASC, then groupName ASC — same as selectAllGroups.
        assertEquals(
            listOf("Beta", "Zeta", "Alpha"),
            db.customGroupQueries.selectAllGroupsWithChannelCount().executeAsList().map { it.groupName },
        )
    }

    @Test
    fun `removing a channel lowers the count`() {
        val a = group("A")
        addChannel(a, 1L)
        addChannel(a, 2L)
        assertEquals(2L, counts().getValue("A"))

        db.customGroupQueries.removeChannelFromGroupById(a, 1L)

        assertEquals(1L, counts().getValue("A"))
    }

    @Test
    fun `the group fields still round-trip`() {
        db.customGroupQueries.insertGroup(
            groupName = "Pinned", groupIcon = "icon.png", createdAt = 11L, updatedAt = 22L,
            sortOrder = 5L, isPinned = 1L, isHidden = 0L, isDefault = 1L,
        )

        val row = db.customGroupQueries.selectAllGroupsWithChannelCount().executeAsList().single()

        assertEquals("Pinned", row.groupName)
        assertEquals("icon.png", row.groupIcon)
        assertEquals(11L, row.createdAt)
        assertEquals(22L, row.updatedAt)
        assertEquals(5L, row.sortOrder)
        assertEquals(1L, row.isPinned)
        assertEquals(0L, row.isHidden)
        assertEquals(1L, row.isDefault)
    }
}
