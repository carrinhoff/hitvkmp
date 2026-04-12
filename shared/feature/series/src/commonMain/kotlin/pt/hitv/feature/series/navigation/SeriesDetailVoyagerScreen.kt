package pt.hitv.feature.series.navigation

import androidx.compose.runtime.Composable
import androidx.compose.ui.platform.LocalUriHandler
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.navigation.SeriesDetailArgs
import pt.hitv.feature.series.detail.SeriesInfoContent
import pt.hitv.feature.series.detail.SeriesInfoViewModel
import pt.hitv.feature.series.list.SeriesViewModel

class SeriesDetailVoyagerScreen(
    private val args: SeriesDetailArgs
) : Screen {
    override val key = "SeriesDetail_${args.seriesId}"

    @Composable
    override fun Content() {
        val seriesInfoViewModel: SeriesInfoViewModel = koinInject()
        val seriesViewModel: SeriesViewModel = koinInject()
        val preferencesHelper: PreferencesHelper = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        SeriesInfoContent(
            seriesId = args.seriesId,
            seriesInfoViewModel = seriesInfoViewModel,
            seriesViewModel = seriesViewModel,
            preferencesHelper = preferencesHelper,
            analyticsHelper = analyticsHelper,
            onNavigateBack = { navigator.pop() },
            onPlayEpisode = { seasonNumber, episodeIndex ->
                // TODO: Wire to series player when implemented
            },
            onPlayTrailer = { youtubeUrl ->
                try {
                    uriHandler.openUri("https://www.youtube.com/watch?v=$youtubeUrl")
                } catch (_: Exception) {}
            }
        )
    }
}
