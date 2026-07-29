package pt.hitv.core.data

import android.content.Context
import androidx.test.platform.app.InstrumentationRegistry
import app.cash.sqldelight.db.SqlDriver
import com.russhwolf.settings.ObservableSettings
import com.russhwolf.settings.SharedPreferencesSettings
import com.russhwolf.settings.Settings
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withTimeout
import org.koin.core.context.startKoin
import org.koin.core.context.stopKoin
import org.koin.dsl.module
import pt.hitv.core.common.AndroidContextHolder
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.di.commonModule
import pt.hitv.core.data.di.dataModule
import pt.hitv.core.data.manager.PremiumStatusProvider
import pt.hitv.core.data.manager.UngatedPremiumStatusProvider
import pt.hitv.core.data.security.CryptoManager
import pt.hitv.core.database.ChannelQueries
import pt.hitv.core.database.DatabaseDriverFactory
import pt.hitv.core.database.di.databaseModule
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.network.di.networkModule
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertSame
import kotlin.test.assertTrue

/**
 * Assembles the real dependency graph on a device and exercises the repositories through it.
 *
 * This is the layer nothing else reaches. The JVM suites test logic against desktop SQLite;
 * `SharedDataLayerDeviceTest` tests the SQL against Android's embedded SQLite; neither builds the
 * graph. Paging invalidation and the seventeen flows converted to `asFlow()` all rest on runtime
 * wiring that no compiler and no unit test can check:
 *
 *  - every repository and every paging source must observe **the same driver instance**;
 *  - the paging-source factories must actually register their listeners;
 *  - the repositories must resolve at all, with the real modules, in the real order.
 *
 * If the `SqlDriver` binding ever became a `factory`, each repository would open its own
 * connection, writes on one would never notify listeners on another, and every list in the app
 * would quietly revert to being stale — nothing throwing, nothing failing to compile. That is the
 * defect class this whole audit kept turning up, and it is the reason this file loads
 * `databaseModule`, `dataModule` and `networkModule` rather than constructing anything by hand.
 *
 * ~90% of this project is shared, so what runs here is what iOS runs — the same repositories, the
 * same paging sources, the same flows. Only the driver differs (`NativeSqliteDriver` there). It is
 * not a substitute for iOS hardware, and it says nothing about the iOS-only actuals, but it does
 * put the shared majority on a device with its wiring assembled.
 *
 * No network: only methods that read the database are called.
 */
class RepositoryGraphDeviceTest {

    private lateinit var context: Context
    private lateinit var channelQueries: ChannelQueries
    private lateinit var streamRepository: StreamRepository
    private lateinit var preferences: PreferencesHelper

    private var userId: Int = 0

    @BeforeTest
    fun setUp() {
        context = InstrumentationRegistry.getInstrumentation().targetContext
        // The Android actuals of PreferencesHelper, AppInfoProvider and PlatformDetector all read
        // a process-global context that HitvApplication normally sets. Without it the graph fails
        // with `lateinit property applicationContext has not been initialized` — which is itself
        // worth knowing: anything constructing these outside the app must seed it first.
        AndroidContextHolder.applicationContext = context.applicationContext
        runCatching { stopKoin() }

        val koin = startKoin {
            modules(
                module {
                    single<Settings> {
                        SharedPreferencesSettings(
                            context.getSharedPreferences("hitv-device-test", Context.MODE_PRIVATE)
                        )
                    }
                    single<ObservableSettings> {
                        SharedPreferencesSettings(
                            context.getSharedPreferences("hitv-device-test", Context.MODE_PRIVATE)
                        )
                    }
                    single { DatabaseDriverFactory(context) }
                    single { CryptoManager() }
                    single<PremiumStatusProvider> { UngatedPremiumStatusProvider() }
                },
                commonModule,
                databaseModule,
                networkModule,
                dataModule,
            )
        }.koin

        channelQueries = koin.get()
        streamRepository = koin.get()
        preferences = koin.get()
        userId = preferences.getUserId()
        channelQueries.deleteByUserId(userId.toLong())
    }

    @AfterTest
    fun tearDown() {
        runCatching { channelQueries.deleteByUserId(userId.toLong()) }
        runCatching { stopKoin() }
    }

    private fun seed(name: String) {
        channelQueries.insertOrIgnore(
            name = name, streamUrl = "http://example/$name", streamIcon = "",
            epgChannelId = name.lowercase(), categoryCreatorId = "1",
            isFavorite = 0L, licenseKey = null, userId = userId.toLong(),
            lastViewedTimestamp = 0L, lastUpdated = 0L, lastSeen = 0L,
            contentHash = null, syncVersion = 1L, tvArchive = 0L,
            tvArchiveDuration = 0L, catchupType = null, catchupSource = null,
        )
    }

    // ---- the wiring assumption -----------------------------------------------------------

    @Test
    fun theGraphSharesOneDriver() {
        val koin = org.koin.core.context.GlobalContext.get()
        assertSame(
            koin.get<SqlDriver>(),
            koin.get<SqlDriver>(),
            "SqlDriver is not a singleton — repositories would hold separate connections and " +
                "change notifications would never cross",
        )
    }

    @Test
    fun theRepositoriesResolveFromTheRealModules() {
        val koin = org.koin.core.context.GlobalContext.get()
        // A missing or misordered binding shows up here rather than as a crash on a user's phone.
        assertTrue(koin.getOrNull<StreamRepository>() != null, "StreamRepository did not resolve")
        assertTrue(
            koin.getOrNull<pt.hitv.core.domain.repositories.CustomGroupRepository>() != null,
            "CustomGroupRepository did not resolve",
        )
        assertTrue(
            koin.getOrNull<pt.hitv.core.domain.repositories.MovieRepository>() != null,
            "MovieRepository did not resolve",
        )
    }

    // ---- reactive repository flows through the real graph -----------------------------------

    @Test
    fun favouritesFlowReEmitsAfterAToggle() = runBlocking {
        seed("Sport TV")

        val flow = streamRepository.getFavoritesChannel()
        assertEquals(0, withTimeout(15_000) { flow.first() }.size)

        channelQueries.updateFavorite(1L, "Sport TV", userId.toLong(), "1")

        val after = withTimeout(15_000) { flow.first { it.isNotEmpty() } }
        assertEquals(listOf("Sport TV"), after.map { it.name })
    }

    @Test
    fun channelListFlowSeesASyncStyleInsert() = runBlocking {
        seed("One")
        val flow = streamRepository.getAllChannelsFlow()
        assertEquals(1, withTimeout(15_000) { flow.first() }.size)

        // What a background sync does while a list is on screen.
        seed("Two")

        val after = withTimeout(15_000) { flow.first { it.size == 2 } }
        assertEquals(setOf("One", "Two"), after.mapNotNull { it.name }.toSet())
    }

    // Paging invalidation is deliberately not asserted here: `StreamRepository` exposes
    // `getChannelsPager(): Flow<PagingData<Channel>>`, not the `PagingSource` itself, so reaching
    // it would mean collecting PagingData through a differ. The mechanism is covered instead by
    // `PagingInvalidationTest` (the listener contract and the table-name keys) and by
    // `ReactiveQueriesDeviceTest.changeNotificationsFireOnDevice` (the shipping driver really
    // notifies). What this file adds is that the graph feeding those sources is wired correctly.
}
