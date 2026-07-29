package pt.hitv.core.database.di

import app.cash.sqldelight.db.SqlDriver
import org.koin.core.module.Module
import org.koin.dsl.module
import pt.hitv.core.database.DatabaseDriverFactory
import pt.hitv.core.database.HitvDatabase

/**
 * Koin module providing the HitvDatabase and all query wrappers.
 *
 * Platform-specific modules must provide [DatabaseDriverFactory].
 */
val databaseModule: Module = module {

    // The driver is exposed, not just consumed, because SQLDelight's change notifications hang off
    // it: `driver.addListener("Channel", ...)`. Paging sources need that to invalidate themselves
    // when a sync writes, which is what Room's InvalidationTracker does for free in the original.
    // Single instance — two drivers would mean two connections and notifications that never cross.
    single<SqlDriver> {
        val driverFactory: DatabaseDriverFactory = get()
        driverFactory.createDriver()
    }

    single<HitvDatabase> { HitvDatabase(get<SqlDriver>()) }

    // Provide individual query objects for injection
    single { get<HitvDatabase>().channelQueries }
    single { get<HitvDatabase>().categoryQueries }
    single { get<HitvDatabase>().categoryVodQueries }
    single { get<HitvDatabase>().categoryTvShowQueries }
    single { get<HitvDatabase>().movieQueries }
    single { get<HitvDatabase>().tvShowQueries }
    single { get<HitvDatabase>().userCredentialsQueries }
    single { get<HitvDatabase>().customGroupQueries }
    single { get<HitvDatabase>().parentalControlQueries }
    single { get<HitvDatabase>().epgChannelQueries }
    single { get<HitvDatabase>().programmeQueries }
    single { get<HitvDatabase>().movieInfoQueries }
    single { get<HitvDatabase>().seriesInfoQueries }
}
