package pt.hitv.feature.series.detail.mobile

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.PaddingValues
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.pager.HorizontalPager
import androidx.compose.foundation.pager.rememberPagerState
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ScrollableTabRow
import androidx.compose.material3.Tab
import androidx.compose.material3.TabRowDefaults
import androidx.compose.material3.TabRowDefaults.tabIndicatorOffset
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.launch
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.seriesInfo.Episode
import pt.hitv.core.model.seriesInfo.Season
import pt.hitv.core.model.seriesInfo.SeriesInfo
import pt.hitv.feature.series.detail.shared.MobileEpisodeItem
import pt.hitv.feature.series.detail.shared.SeriesMetadata

@Composable
fun MobileLandscapeSeriesInfoLayout(
    seriesInfo: SeriesInfo,
    sortedSeasons: List<Season>,
    seasonsMap: Map<Season, List<Episode>>,
    onBackPressed: () -> Unit,
    onEpisodeClicked: (seasonNumber: Int, episodeIndex: Int) -> Unit,
    onTrailerClicked: (String?) -> Unit
) {
    MobileLandscapeSeriesInfo(
        seriesInfo = seriesInfo,
        sortedSeasons = sortedSeasons,
        seasonsMap = seasonsMap,
        onBackPressed = onBackPressed,
        onEpisodeClicked = onEpisodeClicked,
        onTrailerClicked = onTrailerClicked
    )
}

@Composable
fun MobilePortraitSeriesInfoLayout(
    seriesInfo: SeriesInfo,
    sortedSeasons: List<Season>,
    seasonsMap: Map<Season, List<Episode>>,
    onBackPressed: () -> Unit,
    onEpisodeClicked: (seasonNumber: Int, episodeIndex: Int) -> Unit,
    onTrailerClicked: (String?) -> Unit
) {
    MobilePortraitSeriesInfo(
        seriesInfo = seriesInfo,
        sortedSeasons = sortedSeasons,
        seasonsMap = seasonsMap,
        onBackPressed = onBackPressed,
        onEpisodeClicked = onEpisodeClicked,
        onTrailerClicked = onTrailerClicked
    )
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MobileLandscapeSeriesInfo(
    seriesInfo: SeriesInfo,
    sortedSeasons: List<Season>,
    seasonsMap: Map<Season, List<Episode>>,
    onBackPressed: () -> Unit,
    onEpisodeClicked: (seasonNumber: Int, episodeIndex: Int) -> Unit,
    onTrailerClicked: (String?) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { sortedSeasons.size })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = seriesInfo.backdropPath.firstOrNull(),
            contentDescription = "Backdrop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.4f),
                            Color.Black.copy(alpha = 0.8f),
                            getThemeColors().backgroundPrimary
                        ),
                        startY = 0f,
                        endY = 600f
                    )
                )
        )

        Row(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp)
        ) {
            Column(
                modifier = Modifier
                    .weight(1f)
                    .verticalScroll(rememberScrollState())
                    .padding(24.dp)
            ) {
                MobileSeriesHeader(
                    seriesInfo = seriesInfo,
                    isCompact = true,
                    onTrailerClicked = onTrailerClicked
                )
            }

            Column(
                modifier = Modifier
                    .weight(1f)
                    .padding(24.dp)
            ) {
                if (sortedSeasons.isNotEmpty()) {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    height = 2.dp,
                                    color = getThemeColors().primaryColor.copy(alpha = 0.7f)
                                )
                            }
                        },
                        containerColor = Color.Transparent
                    ) {
                        sortedSeasons.forEachIndexed { index, season ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = {
                                    Text(
                                        "Season ${season.seasonNumber}",
                                        fontSize = 13.sp
                                    )
                                },
                                selectedContentColor = getThemeColors().textColor,
                                unselectedContentColor = getThemeColors().textColor.copy(alpha = 0.6f)
                            )
                        }
                    }

                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.fillMaxSize()
                    ) { pageIndex ->
                        val season = sortedSeasons[pageIndex]
                        val episodes = seasonsMap[season] ?: emptyList()
                        LazyColumn(
                            contentPadding = PaddingValues(top = 16.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(episodes.size, key = { episodes[it].id ?: "" }) { index ->
                                val episode = episodes[index]
                                MobileEpisodeItem(
                                    episode = episode,
                                    isCompact = true,
                                    onClick = {
                                        onEpisodeClicked(season.seasonNumber, index)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50)
                )
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = getThemeColors().textColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@OptIn(ExperimentalFoundationApi::class, ExperimentalMaterial3Api::class)
@Composable
private fun MobilePortraitSeriesInfo(
    seriesInfo: SeriesInfo,
    sortedSeasons: List<Season>,
    seasonsMap: Map<Season, List<Episode>>,
    onBackPressed: () -> Unit,
    onEpisodeClicked: (seasonNumber: Int, episodeIndex: Int) -> Unit,
    onTrailerClicked: (String?) -> Unit
) {
    val pagerState = rememberPagerState(pageCount = { sortedSeasons.size })
    val coroutineScope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        AsyncImage(
            model = seriesInfo.backdropPath.firstOrNull(),
            contentDescription = "Backdrop",
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            Color.Black.copy(alpha = 0.3f),
                            Color.Black.copy(alpha = 0.6f),
                            getThemeColors().backgroundPrimary.copy(alpha = 0.9f),
                            getThemeColors().backgroundPrimary
                        ),
                        startY = 0f,
                        endY = 800f
                    )
                )
        )

        LazyColumn(
            modifier = Modifier
                .fillMaxSize()
                .padding(top = 48.dp),
            contentPadding = PaddingValues(16.dp),
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            item {
                MobileSeriesHeader(
                    seriesInfo = seriesInfo,
                    isCompact = false,
                    onTrailerClicked = onTrailerClicked
                )
            }

            if (sortedSeasons.isNotEmpty()) {
                item {
                    ScrollableTabRow(
                        selectedTabIndex = pagerState.currentPage,
                        edgePadding = 0.dp,
                        indicator = { tabPositions ->
                            if (pagerState.currentPage < tabPositions.size) {
                                TabRowDefaults.Indicator(
                                    Modifier.tabIndicatorOffset(tabPositions[pagerState.currentPage]),
                                    height = 2.dp,
                                    color = getThemeColors().primaryColor.copy(alpha = 0.7f)
                                )
                            }
                        },
                        containerColor = Color.Transparent
                    ) {
                        sortedSeasons.forEachIndexed { index, season ->
                            Tab(
                                selected = pagerState.currentPage == index,
                                onClick = {
                                    coroutineScope.launch { pagerState.animateScrollToPage(index) }
                                },
                                text = {
                                    Text(
                                        "Season ${season.seasonNumber}",
                                        fontSize = 14.sp
                                    )
                                },
                                selectedContentColor = getThemeColors().textColor,
                                unselectedContentColor = getThemeColors().textColor.copy(alpha = 0.6f)
                            )
                        }
                    }
                }

                item {
                    HorizontalPager(
                        state = pagerState,
                        modifier = Modifier.height(400.dp)
                    ) { pageIndex ->
                        val season = sortedSeasons[pageIndex]
                        val episodes = seasonsMap[season] ?: emptyList()
                        LazyColumn(
                            contentPadding = PaddingValues(vertical = 8.dp),
                            verticalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            items(episodes.size, key = { episodes[it].id ?: "" }) { index ->
                                val episode = episodes[index]
                                MobileEpisodeItem(
                                    episode = episode,
                                    isCompact = false,
                                    onClick = {
                                        onEpisodeClicked(season.seasonNumber, index)
                                    }
                                )
                            }
                        }
                    }
                }
            }
        }

        IconButton(
            onClick = onBackPressed,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(24.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = RoundedCornerShape(50)
                )
                .size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.ArrowBack,
                contentDescription = "Back",
                tint = getThemeColors().textColor,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}

@Composable
private fun MobileSeriesHeader(
    seriesInfo: SeriesInfo,
    isCompact: Boolean,
    onTrailerClicked: (String?) -> Unit
) {
    Column(
        modifier = Modifier.fillMaxWidth(),
        verticalArrangement = Arrangement.spacedBy(16.dp)
    ) {
        if (isCompact) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                AsyncImage(
                    model = seriesInfo.cover,
                    contentDescription = seriesInfo.name,
                    modifier = Modifier
                        .width(120.dp)
                        .height(180.dp)
                        .clip(RoundedCornerShape(8.dp)),
                    contentScale = ContentScale.Crop
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = seriesInfo.name ?: "Series Info",
                        style = MaterialTheme.typography.headlineSmall,
                        color = getThemeColors().textColor,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis
                    )

                    Spacer(modifier = Modifier.height(8.dp))

                    Text(
                        text = seriesInfo.plot ?: "No plot available.",
                        maxLines = 3,
                        style = MaterialTheme.typography.bodyMedium,
                        color = getThemeColors().textColor.copy(alpha = 0.7f),
                        overflow = TextOverflow.Ellipsis
                    )
                }
            }
        } else {
            AsyncImage(
                model = seriesInfo.cover,
                contentDescription = seriesInfo.name,
                modifier = Modifier
                    .fillMaxWidth()
                    .height(300.dp)
                    .clip(RoundedCornerShape(8.dp)),
                contentScale = ContentScale.Crop
            )

            Text(
                text = seriesInfo.name ?: "Series Info",
                style = MaterialTheme.typography.headlineMedium,
                color = getThemeColors().textColor,
                fontWeight = FontWeight.Bold
            )

            Text(
                text = seriesInfo.plot ?: "No plot available.",
                style = MaterialTheme.typography.bodyLarge,
                color = getThemeColors().textColor.copy(alpha = 0.7f)
            )
        }

        SeriesMetadata(seriesInfo = seriesInfo)

        seriesInfo.youtubeTrailer?.let { trailerUrl ->
            Button(
                onClick = { onTrailerClicked(trailerUrl) },
                modifier = Modifier.fillMaxWidth(),
                colors = ButtonDefaults.buttonColors(
                    containerColor = getThemeColors().primaryColor
                )
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = null
                )
                Spacer(modifier = Modifier.width(8.dp))
                Text("Watch Trailer", color = getThemeColors().textColor)
            }
        }
    }
}
