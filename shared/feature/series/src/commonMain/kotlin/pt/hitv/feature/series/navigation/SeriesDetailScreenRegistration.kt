package pt.hitv.feature.series.navigation

import pt.hitv.core.navigation.HitvScreen
import pt.hitv.core.navigation.ScreenRegistry
import pt.hitv.core.navigation.SeriesDetailArgs

fun registerSeriesDetailScreen() {
    ScreenRegistry.register(HitvScreen.SERIES_DETAIL) { args ->
        SeriesDetailVoyagerScreen(args as SeriesDetailArgs)
    }
}
