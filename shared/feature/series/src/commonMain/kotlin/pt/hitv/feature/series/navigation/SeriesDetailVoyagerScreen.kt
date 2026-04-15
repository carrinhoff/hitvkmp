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
import pt.hitv.feature.player.platform.launchChannelPlayer

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
                // Build episode URL and launch player
                val seriesUiState = seriesViewModel.uiState.value
                val (_, seasonsMap) = seriesUiState.seasonEpisodeData
                val season = seasonsMap.keys.find { it.seasonNumber == seasonNumber }
                val episodes = if (season != null) seasonsMap[season] ?: emptyList() else emptyList()
                val episode = episodes.getOrNull(episodeIndex)
                if (episode != null) {
                    val host = preferencesHelper.getHostUrl()
                    val user = preferencesHelper.getUsername()
                    val pass = preferencesHelper.getPassword()
                    val url = "${host}series/$user/$pass/${episode.id}.${episode.containerExtension ?: "m3u8"}"
                    launchChannelPlayer(url = url, name = episode.title ?: "Episode ${episode.episodeNum}")
                }
            },
            onPlayTrailer = { youtubeUrl ->
                try {
                    uriHandler.openUri("https://www.youtube.com/watch?v=$youtubeUrl")
                } catch (_: Exception) {}
            }
        )
    }
}
