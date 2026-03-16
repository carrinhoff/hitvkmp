package pt.hitv.core.database.di

import org.koin.dsl.module
import pt.hitv.core.database.DatabaseDriverFactory

/**
 * Android-specific Koin module providing [DatabaseDriverFactory].
 */
val databasePlatformModule = module {
    single { DatabaseDriverFactory(context = get()) }
}
