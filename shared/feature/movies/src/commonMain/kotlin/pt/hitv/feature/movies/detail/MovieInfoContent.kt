package pt.hitv.feature.movies.detail

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Error
import androidx.compose.material.icons.filled.Info
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import kotlinx.coroutines.delay
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.Resources
import pt.hitv.core.model.Movie
import pt.hitv.core.model.cast.Cast
import pt.hitv.core.model.movieInfo.Info
import pt.hitv.core.model.movieInfo.MovieData

/**
 * Main entry point for the movie detail screen.
 *
 * Handles dual-fetch data loading (cache + network), error/loading states,
 * recently viewed tracking, and animations.
 */
@Composable
fun MovieInfoContent(
    streamId: String?,
    viewModel: MovieInfoViewModel,
    preferencesHelper: PreferencesHelper,
    isLandscape: Boolean = false,
    onNavigateBack: () -> Unit,
    onPlayMovie: (movieUrl: String, movieTitle: String) -> Unit,
    onPlayTrailer: (youtubeUrl: String) -> Unit
) {
    var isLoading by remember { mutableStateOf(true) }
    var isContentVisible by remember { mutableStateOf(false) }
    var movieInfo by remember { mutableStateOf<Info?>(null) }
    var movieData by remember { mutableStateOf<MovieData?>(null) }
    var castList by remember { mutableStateOf<List<Cast>>(emptyList()) }
    var hasError by remember { mutableStateOf(false) }
    var savedPosition by remember { mutableStateOf<Long?>(null) }
    var refreshPositionTrigger by remember { mutableIntStateOf(0) }

    val uiState by viewModel.uiState.collectAsStateWithLifecycle()
    val movieInfoCached = uiState.cachedMovieInfo
    val movieInfoNetwork = uiState.networkMovieInfo
    val movieCast = uiState.movieCast
    val isFavorite = uiState.isFavorite

    fun isValidMovieInfo(info: Info?): Boolean {
        if (info == null) return false
        return !info.name.isNullOrBlank() && !info.movieImage.isNullOrBlank()
    }

    fun playMovie(data: MovieData, info: Info) {
        val host = preferencesHelper.getHostUrl()
        val user = preferencesHelper.getUsername()
        val pass = preferencesHelper.getPassword()
        if (host.isBlank() || user.isBlank() || pass.isBlank()) return

        val url = "$host/movie/$user/$pass/${data.streamId}.${data.containerExtension}"
        val title = info.name ?: "Movie"
        onPlayMovie(url, title)
    }

    // Initial data loading
    LaunchedEffect(streamId) {
        if (streamId.isNullOrEmpty()) {
            onNavigateBack()
            return@LaunchedEffect
        }
        viewModel.getLocalMovieInfo(streamId)
        viewModel.checkFavoriteStatus(streamId)
    }

    // Save to "Recently Viewed"
    LaunchedEffect(movieInfo, movieData) {
        if (movieInfo != null && movieData != null) {
            val movie = Movie(
                movieId = movieData!!.streamId.toLong(),
                name = movieInfo!!.name ?: "",
                streamId = movieData!!.streamId.toString(),
                streamIcon = movieInfo!!.movieImage,
                added = "",
                categoryId = null,
                containerExtension = movieData!!.containerExtension,
                lastViewedTimestamp = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()
            )
            try {
                viewModel.saveRecentlyViewed(movie)
            } catch (_: Exception) {}
        }
    }

    // Handle cached data
    LaunchedEffect(movieInfoCached) {
        when (val cached = movieInfoCached) {
            is Resources.Success -> {
                cached.data?.let { cachedData ->
                    val info = cachedData.info
                    if (isValidMovieInfo(info)) {
                        movieInfo = info
                        movieData = cachedData.movieData
                        isLoading = false
                        movieInfo?.tmdbId?.takeIf { it.isNotBlank() }?.let { tmdbId ->
                            viewModel.getCast(tmdbId)
                        }
                    } else {
                        viewModel.getMovieInfo(streamId ?: "")
                    }
                } ?: run {
                    viewModel.getMovieInfo(streamId ?: "")
                }
            }
            is Resources.Error -> {
                viewModel.getMovieInfo(streamId ?: "")
            }
            is Resources.Loading -> {
                isLoading = true
            }
        }
    }

    // Handle network data
    LaunchedEffect(movieInfoNetwork) {
        movieInfoNetwork?.let { network ->
            when (network) {
                is Resources.Success -> {
                    network.data?.let { response ->
                        movieInfo = response.info
                        movieData = response.movieData
                        isLoading = false
                        viewModel.insertMovieInfo(response)
                        movieInfo?.tmdbId?.takeIf { it.isNotBlank() }?.let { tmdbId ->
                            viewModel.getCast(tmdbId)
                        }
                    } ?: run {
                        hasError = true
                        isLoading = false
                    }
                }
                is Resources.Error -> {
                    hasError = true
                    isLoading = false
                }
                is Resources.Loading -> {
                    isLoading = true
                }
            }
        }
    }

    // Handle cast data
    LaunchedEffect(movieCast) {
        movieCast?.let { cast ->
            when (cast) {
                is Resources.Success -> {
                    castList = cast.data?.cast?.filter {
                        !it.profilePath.isNullOrBlank()
                    } ?: emptyList()
                }
                else -> {}
            }
        }
    }

    // Load saved playback position
    LaunchedEffect(movieData, refreshPositionTrigger) {
        movieData?.let { data ->
            try {
                savedPosition = viewModel.getPlaybackPosition(data.streamId)
            } catch (_: Exception) {}
        }
    }

    // Show content with entrance animation
    LaunchedEffect(movieInfo, movieData) {
        if (movieInfo != null && movieData != null && !isLoading) {
            delay(50)
            isContentVisible = true
        }
    }

    // Render UI
    Box(modifier = Modifier.fillMaxSize().statusBarsPadding()) {
        when {
            isLoading -> LoadingScreen()
            hasError -> ErrorScreen(onBackClick = onNavigateBack)
            movieInfo != null && movieData != null && isValidMovieInfo(movieInfo) -> {
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(tween(300)),
                    exit = fadeOut(tween(200))
                ) {
                    MovieInfoScreen(
                        movieInfo = movieInfo!!,
                        movieData = movieData!!,
                        castList = castList,
                        savedPosition = savedPosition,
                        isFavorite = isFavorite,
                        isLandscape = isLandscape,
                        onBackClick = onNavigateBack,
                        onPlayClick = { playMovie(movieData!!, movieInfo!!) },
                        onFavoriteClick = {
                            val movie = Movie(
                                movieId = movieData!!.streamId.toLong(),
                                name = movieInfo!!.name ?: "",
                                streamId = movieData!!.streamId.toString(),
                                streamIcon = movieInfo!!.movieImage,
                                added = "",
                                categoryId = null,
                                containerExtension = movieData!!.containerExtension
                            )
                            viewModel.toggleFavorite(movie)
                        },
                        onTrailerClick = { movieInfo!!.youtubeTrailer?.let { onPlayTrailer(it) } }
                    )
                }
            }
            movieData != null -> {
                val data = movieData!!
                val info = movieInfo
                AnimatedVisibility(
                    visible = isContentVisible,
                    enter = fadeIn(tween(400)),
                    exit = fadeOut(tween(200))
                ) {
                    MinimalMoviePlaybackScreen(
                        movieData = data,
                        movieInfo = info,
                        onBackClick = onNavigateBack,
                        onPlayClick = {
                            playMovie(data, info ?: Info(durationSecs = "0", name = data.name))
                        }
                    )
                }
            }
            else -> ErrorScreen(onBackClick = onNavigateBack)
        }
    }
}

@Composable
private fun LoadingScreen() {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp)
        ) {
            CircularProgressIndicator(
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp,
                modifier = Modifier.size(48.dp)
            )
            Text(
                text = "Loading movie details...",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground
            )
        }
    }
}

@Composable
private fun ErrorScreen(onBackClick: () -> Unit) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background),
        contentAlignment = Alignment.Center
    ) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(16.dp),
            modifier = Modifier.padding(32.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Error,
                contentDescription = "Error",
                tint = MaterialTheme.colorScheme.error,
                modifier = Modifier.size(64.dp)
            )
            Text(
                text = "Failed to load movie details",
                style = MaterialTheme.typography.headlineSmall,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )
            Button(
                onClick = onBackClick,
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.AutoMirrored.Filled.ArrowBack, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Go Back")
            }
        }
    }
}

@Composable
private fun MinimalMoviePlaybackScreen(
    movieData: MovieData,
    movieInfo: Info?,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit
) {
    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
    ) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center
        ) {
            Icon(
                imageVector = Icons.Default.Info,
                contentDescription = "Limited info",
                tint = MaterialTheme.colorScheme.primary,
                modifier = Modifier.size(64.dp)
            )

            Spacer(modifier = Modifier.height(24.dp))

            Text(
                text = movieData.name,
                style = MaterialTheme.typography.headlineMedium,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.onBackground,
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(12.dp))

            Text(
                text = "Limited information available",
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onBackground.copy(alpha = 0.7f),
                textAlign = TextAlign.Center
            )

            Spacer(modifier = Modifier.height(32.dp))

            Button(
                onClick = onPlayClick,
                modifier = Modifier
                    .fillMaxWidth(0.6f)
                    .height(56.dp),
                colors = ButtonDefaults.buttonColors(
                    containerColor = MaterialTheme.colorScheme.primary
                )
            ) {
                Icon(Icons.Default.PlayArrow, contentDescription = null)
                Spacer(modifier = Modifier.width(8.dp))
                Text("Play Movie", fontSize = 18.sp)
            }

            Spacer(modifier = Modifier.height(16.dp))

            movieInfo?.let { info ->
                Card(
                    modifier = Modifier
                        .fillMaxWidth(0.7f)
                        .padding(vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = MaterialTheme.colorScheme.surface
                    )
                ) {
                    Column(
                        modifier = Modifier.padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Text(
                            text = "Technical Information",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold,
                            color = MaterialTheme.colorScheme.onSurface
                        )
                        info.duration?.takeIf { it.isNotBlank() }?.let { duration ->
                            Text(
                                text = "Duration: $duration",
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                            )
                        }
                        Text(
                            text = "Format: ${movieData.containerExtension.uppercase()}",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.8f)
                        )
                    }
                }
            }
        }

        IconButton(
            onClick = onBackClick,
            modifier = Modifier
                .align(Alignment.TopStart)
                .padding(16.dp)
                .background(
                    MaterialTheme.colorScheme.surface.copy(alpha = 0.9f),
                    CircleShape
                )
        ) {
            Icon(
                Icons.AutoMirrored.Filled.ArrowBack,
                contentDescription = "Back",
                tint = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
