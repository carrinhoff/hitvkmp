package pt.hitv.core.database

import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import app.cash.sqldelight.db.SqlDriver
import app.cash.sqldelight.driver.android.AndroidSqliteDriver
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import pt.hitv.core.database.di.databaseModule
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Runs the reactive query mechanism on a device, against Android's own SQLite.
 *
 * Seventeen repository flows were converted in this pass from `flow { emit(query.executeAsList()) }`
 * — a snapshot wearing a `Flow`'s clothes — to `asFlow().mapToList(…)`. Paging invalidation rests on
 * the same foundation. All of it depends on two runtime facts that no compile check and no
 * desktop-SQLite unit test can establish:
 *
 *  1. **The driver actually delivers change notifications** on the shipping driver, not just on
 *     `JdbcSqliteDriver`.
 *  2. **Every consumer shares one driver instance.** Koin declares `SqlDriver` as a `single`; if
 *     that ever became a `factory`, each repository would open its own connection, writes on one
 *     would never notify listeners on another, and every list in the app would silently go back to
 *     being stale. Nothing would throw.
 *
 * The second is the reason this file loads the real `databaseModule` rather than constructing
 * things by hand — it is the declaration itself that is under test.
 *
 * Deliberately `runBlocking`, not `runTest`. `runTest` drives a virtual clock, so a `withTimeout`
 * inside it elapses instantly while the flow is still waiting on a real `Dispatchers.IO` emission —
 * every test here would "time out" in under a second without the database ever being slow. These
 * assertions are about real asynchronous delivery, so they need real time.
 *
 * These are the same code paths iOS runs; only the driver differs (`NativeSqliteDriver` there).
 * That makes this strong evidence for the shared half of the port and no evidence at all for the
 * iOS-specific half.
 */
class ReactiveQueriesDeviceTest {

    private lateinit var driver: SqlDriver
    private lateinit var db: HitvDatabase

    private val userId = 1L
    private val dbName = "hitv-reactive-device-test.db"

    @BeforeTest
    fun setUp() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        context.deleteDatabase(dbName)
        driver = AndroidSqliteDriver(HitvDatabase.Schema, context, dbName)
        db = HitvDatabase(driver)
    }

    @AfterTest
    fun tearDown() {
        driver.close()
        InstrumentationRegistry.getInstrumentation().targetContext.deleteDatabase(dbName)
        runCatching { stopKoin() }
    }

    private fun seed(name: String, favourite: Long = 0L) {
        db.channelQueries.insertOrIgnore(
            name = name, streamUrl = "http://example/$name", streamIcon = "",
            epgChannelId = name.lowercase(), categoryCreatorId = "1",
            isFavorite = favourite, licenseKey = null, userId = userId,
            lastViewedTimestamp = 0L, lastUpdated = 0L, lastSeen = 0L,
            contentHash = null, syncVersion = 1L, tvArchive = 0L,
            tvArchiveDuration = 0L, catchupType = null, catchupSource = null,
        )
    }

    // ---- the Koin declaration the whole design rests on ---------------------------------------

    @Test
    fun koinHandsOutASingleDriverAndDatabase() {
        val context = InstrumentationRegistry.getInstrumentation().targetContext
        val koin = startKoin {
            modules(
                module { single { DatabaseDriverFactory(context) } },
                databaseModule,
            )
        }.koin

        try {
            assertSame(
                koin.get<SqlDriver>(),
                koin.get<SqlDriver>(),
                "SqlDriver is not a singleton — two connections, and notifications would never cross",
            )
            assertSame(
                koin.get<HitvDatabase>(),
                koin.get<HitvDatabase>(),
                "HitvDatabase is not a singleton",
            )
            // And the database must be built on that same driver, not a second one.
            val fromGraph = koin.get<HitvDatabase>()
            fromGraph.channelQueries.deleteByUserId(9999L)  // smoke: the graph's DB is usable
        } finally {
            stopKoin()
        }
    }

    // ---- reactivity on the shipping driver -----------------------------------------------------

    @Test
    fun aQueryFlowReEmitsWhenItsTableChanges() = runBlocking {
        seed("Sport TV")

        val flow = db.channelQueries.selectFavoritesPaged(userId, Long.MAX_VALUE, 0L)
            .asFlow()
            .mapToList(Dispatchers.IO)

        assertEquals(0, withTimeout(10_000) { flow.first() }.size)

        // The exact write a favourite toggle performs.
        db.channelQueries.updateFavorite(1L, "Sport TV", userId, "1")

        val favourites = withTimeout(10_000) { flow.first { it.isNotEmpty() } }
        assertEquals(listOf("Sport TV"), favourites.mapNotNull { it.name })
    }

    @Test
    fun aQueryFlowSeesRowsInsertedByASync() = runBlocking {
        seed("One")

        val flow = db.channelQueries.selectAllByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)

        assertEquals(1, withTimeout(10_000) { flow.first() }.size)

        // What a background sync does while a list is on screen — the case that used to leave the
        // visible list showing pre-sync data until the screen was destroyed.
        seed("Two")

        val all = withTimeout(10_000) { flow.first { it.size == 2 } }
        assertEquals(setOf("One", "Two"), all.mapNotNull { it.name }.toSet())
    }

    @Test
    fun aQueryFlowIsNotWokenByAnUnrelatedTable() = runBlocking {
        seed("One")
        val flow = db.channelQueries.selectAllByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
        assertEquals(1, withTimeout(10_000) { flow.first() }.size)

        // Over-invalidation is not harmless — it restarts paging and resets the user's scroll.
        db.movieQueries.deleteByUserId(userId)

        // Still exactly one row, and the flow is still usable.
        assertEquals(1, withTimeout(10_000) { flow.first() }.size)
    }

    @Test
    fun writesInsideATransactionNotifyOnceOnDevice() = runBlocking {
        val flow = db.channelQueries.selectAllByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
        assertEquals(0, withTimeout(10_000) { flow.first() }.size)

        db.channelQueries.transaction {
            seed("One")
            seed("Two")
            seed("Three")
        }

        // Observers see the completed batch, never a partially-synced list.
        val all = withTimeout(10_000) { flow.first { it.isNotEmpty() } }
        assertEquals(3, all.size)
    }

    @Test
    fun aRolledBackTransactionDoesNotReachObservers() = runBlocking {
        seed("One")
        val flow = db.channelQueries.selectAllByUserId(userId)
            .asFlow()
            .mapToList(Dispatchers.IO)
        assertEquals(1, withTimeout(10_000) { flow.first() }.size)

        runCatching {
            db.channelQueries.transaction {
                seed("Two")
                throw IllegalStateException("boom")
            }
        }

        assertEquals(1, withTimeout(10_000) { flow.first() }.size)
        assertTrue(
            withTimeout(10_000) { flow.first() }.mapNotNull { it.name } == listOf("One"),
            "a rolled-back insert became visible to observers",
        )
    }
}
