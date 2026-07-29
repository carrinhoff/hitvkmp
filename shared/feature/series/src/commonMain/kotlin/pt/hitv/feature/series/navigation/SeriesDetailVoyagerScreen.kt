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
import pt.hitv.feature.player.platform.launchSeriesPlayer
import pt.hitv.core.common.util.YouTubeUrl

class SeriesDetailVoyagerScreen(
    private val seriesId: String
) : Screen {
    constructor(args: SeriesDetailArgs) : this(args.seriesId)

    override val key = "SeriesDetail_$seriesId"

    @Composable
    override fun Content() {
        val seriesInfoViewModel: SeriesInfoViewModel = koinInject()
        val seriesViewModel: SeriesViewModel = koinInject()
        val preferencesHelper: PreferencesHelper = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow
        val uriHandler = LocalUriHandler.current

        SeriesInfoContent(
            seriesId = seriesId,
            seriesInfoViewModel = seriesInfoViewModel,
            seriesViewModel = seriesViewModel,
            preferencesHelper = preferencesHelper,
            analyticsHelper = analyticsHelper,
            onNavigateBack = { navigator.pop() },
            onPlayEpisode = { seasonNumber, episodeIndex ->
                // Route to the SERIES player, not the channel player. It resolves the
                // episode itself from (seriesId, season, index) via SeriesPlayerViewModel,
                // which is what gives us per-episode resume, progress saving and
                // prev/next — all of which are lost when an episode is handed to the
                // live-TV player as a bare URL.
                launchSeriesPlayer(
                    seriesId = seriesId,
                    seasonNumber = seasonNumber,
                    episodeIndex = episodeIndex
                )
            },
            onPlayTrailer = { rawTrailer ->
                YouTubeUrl.watchUrlOrNull(rawTrailer)?.let { url ->
                    try {
                        uriHandler.openUri(url)
                    } catch (_: Exception) {}
                }
            }
        )
    }
}
