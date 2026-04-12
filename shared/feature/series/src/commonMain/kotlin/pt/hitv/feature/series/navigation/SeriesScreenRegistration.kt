package pt.hitv.feature.series.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry
import pt.hitv.core.navigation.SeriesCategoryDetailArgs

fun registerSeriesScreens() {
    ScreenRegistry.register(HitvScreen.SERIES) { SeriesVoyagerScreen() }
    registerSeriesDetailScreen()
    ScreenRegistry.register(HitvScreen.SERIES_CATEGORY) { args ->
        SeriesCategoryDetailVoyagerScreen(args as SeriesCategoryDetailArgs)
    }
}
