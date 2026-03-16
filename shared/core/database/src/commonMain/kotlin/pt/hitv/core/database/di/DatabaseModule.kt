package pt.hitv.core.database.di

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

    single<HitvDatabase> {
        val driverFactory: DatabaseDriverFactory = get()
        val driver = driverFactory.createDriver()
        HitvDatabase(driver)
    }

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
