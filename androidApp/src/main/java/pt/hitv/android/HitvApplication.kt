package pt.hitv.android

import android.app.Application
import org.koin.android.ext.koin.androidContext
import org.koin.android.ext.koin.androidLogger
import org.koin.core.context.startKoin
import pt.hitv.android.di.appModule
import pt.hitv.core.common.AndroidContextHolder
import pt.hitv.feature.channels.navigation.registerChannelsScreens
import pt.hitv.feature.movies.navigation.registerMoviesScreens
import pt.hitv.feature.series.navigation.registerSeriesScreens
import pt.hitv.feature.premium.navigation.registerPremiumScreens
import pt.hitv.feature.auth.navigation.registerAuthScreens
import pt.hitv.feature.settings.navigation.registerSettingsScreens

class HitvApplication : Application() {
    override fun onCreate() {
        super.onCreate()

        // Initialize Android context for encrypted preferences
        AndroidContextHolder.applicationContext = this

        // Register all Voyager screen factories before Koin/navigation
        registerAuthScreens()
        registerChannelsScreens()
        registerMoviesScreens()
        registerSeriesScreens()
        registerPremiumScreens()
        registerSettingsScreens()

        startKoin {
            androidLogger()
            androidContext(this@HitvApplication)
            modules(appModule())
        }
    }
}
