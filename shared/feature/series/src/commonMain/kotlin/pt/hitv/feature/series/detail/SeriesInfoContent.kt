package pt.hitv.feature.series.detail

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.runtime.collectAsState
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.seriesInfo.Episode
import pt.hitv.core.model.seriesInfo.Season
import pt.hitv.core.model.seriesInfo.SeriesInfo
import pt.hitv.feature.series.detail.mobile.MobileLandscapeSeriesInfoLayout
import pt.hitv.feature.series.detail.mobile.MobilePortraitSeriesInfoLayout
import pt.hitv.feature.series.list.SeriesViewModel

/**
 * Content composable for the Series Info/Detail screen.
 *
 * Handles:
 * - Series info and season/episode data loading
 * - Favorite toggle functionality
 * - Recently viewed tracking
 * - Mobile landscape and portrait layouts
 * - Episode playback navigation
 * - Trailer navigation
 */
@Composable
fun SeriesInfoContent(
    seriesId: String,
    seriesInfoViewModel: SeriesInfoViewModel,
    seriesViewModel: SeriesViewModel,
    preferencesHelper: PreferencesHelper,
    analyticsHelper: AnalyticsHelper,
    isLandscape: Boolean = false,
    onNavigateBack: () -> Unit,
    onPlayEpisode: (seasonNumber: Int, episodeIndex: Int) -> Unit,
    onPlayTrailer: (youtubeUrl: String) -> Unit
) {
    // Observe data from unified state
    val seriesUiState by seriesViewModel.uiState.collectAsState()
    val (loadedSeriesId, seasonsMap) = seriesUiState.seasonEpisodeData
    val seriesInfo by seriesInfoViewModel.seriesInfo.collectAsState()

    // Observe favorite status
    val isFavorite by seriesInfoViewModel.isFavorite.collectAsState()

    // Check favorite status
    LaunchedEffect(seriesId) {
        seriesId.toIntOrNull()?.let { seriesInfoViewModel.checkFavoriteStatus(it) }
    }

    // Loading states
    val isDataLoading = seriesInfo == null || loadedSeriesId != seriesId
    val sortedSeasons = remember(seasonsMap) { seasonsMap.keys.sortedBy { it.seasonNumber } }

    // Trigger data loading
    LaunchedEffect(seriesId) {
        seriesInfoViewModel.loadSeriesInfo(seriesId)
        seriesViewModel.clearSeasonData()
        seriesViewModel.fetchSeasonsAndEpisodesForSeries(seriesId)
    }

    // Save Recently Viewed
    LaunchedEffect(seriesInfo) {
        val currentInfo = seriesInfo
        if (currentInfo != null) {
            val tvShow = TvShow(
                num = null,
                name = currentInfo.name,
                seriesId = seriesId.toIntOrNull() ?: 0,
                cover = currentInfo.cover,
                plot = currentInfo.plot,
                cast = currentInfo.cast,
                director = currentInfo.director,
                genre = currentInfo.genre,
                releaseDate = currentInfo.releaseDate,
                lastModified = currentInfo.lastModified,
                rating = currentInfo.rating,
                rating5based = currentInfo.rating5based,
                backdropPath = currentInfo.backdropPath,
                youtubeTrailer = currentInfo.youtubeTrailer,
                episodeRunTime = currentInfo.episodeRunTime,
                categoryId = null,
                lastViewedTimestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            )
            seriesInfoViewModel.saveRecentlyViewedSeries(tvShow)
        }
    }

    // Show loading screen while data is loading
    if (isDataLoading) {
        SeriesLoadingScreen(onBackPressed = onNavigateBack)
        return
    }

    val currentSeriesInfo = seriesInfo ?: return

    // Choose layout based on orientation
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        if (isLandscape) {
            MobileLandscapeSeriesInfoLayout(
                seriesInfo = currentSeriesInfo,
                sortedSeasons = sortedSeasons,
                seasonsMap = seasonsMap,
                onBackPressed = onNavigateBack,
                onEpisodeClicked = { seasonNumber, index ->
                    onPlayEpisode(seasonNumber, index)
                },
                onTrailerClicked = { trailerUrl ->
                    if (trailerUrl != null) {
                        onPlayTrailer(trailerUrl)
                    }
                }
            )
        } else {
            MobilePortraitSeriesInfoLayout(
                seriesInfo = currentSeriesInfo,
                sortedSeasons = sortedSeasons,
                seasonsMap = seasonsMap,
                onBackPressed = onNavigateBack,
                onEpisodeClicked = { seasonNumber, index ->
                    onPlayEpisode(seasonNumber, index)
                },
                onTrailerClicked = { trailerUrl ->
                    if (trailerUrl != null) {
                        onPlayTrailer(trailerUrl)
                    }
                }
            )
        }
    }
}

@Composable
private fun SeriesLoadingScreen(onBackPressed: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeColors().backgroundPrimary)
            .statusBarsPadding()
    ) {
        Column(
            modifier = Modifier.align(Alignment.Center),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                modifier = Modifier.size(48.dp),
                color = getThemeColors().primaryColor,
                strokeWidth = 4.dp
            )

            Text(
                text = "Loading series details...",
                style = MaterialTheme.typography.bodyLarge,
                color = getThemeColors().textColor
            )
        }

        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.5f),
                    shape = RoundedCornerShape(50)
                )
        ) {
            Icon(
                imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = getThemeColors().textColor
            )
        }
    }
}
