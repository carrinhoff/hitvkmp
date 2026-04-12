package pt.hitv.feature.movies.detail.mobile

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.Movie
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.cast.Cast
import pt.hitv.core.model.movieInfo.Info
import pt.hitv.core.model.movieInfo.MovieData
import pt.hitv.feature.movies.detail.shared.CastSection
import pt.hitv.feature.movies.detail.shared.MovieMetadata

@Composable
fun LandscapeMovieInfo(
    movieInfo: Info,
    movieData: MovieData,
    castList: List<Cast>,
    savedPosition: Long? = null,
    onBackClick: () -> Unit,
    onPlayClick: () -> Unit,
    onTrailerClick: () -> Unit
) {
    val hasProgress = savedPosition != null && savedPosition > 0
    val buttonText = if (hasProgress) "Continue" else "Play"

    Box(modifier = Modifier.fillMaxSize()) {
        movieInfo.backdropPath?.firstOrNull()?.let { backdrop ->
            AsyncImage(
                model = backdrop,
                contentDescription = null,
                contentScale = ContentScale.Crop,
                modifier = Modifier
                    .fillMaxSize()
                    .blur(7.dp)
                    .alpha(0.45f)
            )
        }

        Box(
            Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            getThemeColors().backgroundPrimary.copy(alpha = 0.95f),
                            getThemeColors().backgroundPrimary.copy(alpha = 0.70f),
                            getThemeColors().backgroundPrimary.copy(alpha = 0.97f)
                        )
                    )
                )
        )

        Row(modifier = Modifier.fillMaxSize()) {
            Column(
                modifier = Modifier
                    .width(260.dp)
                    .fillMaxHeight()
                    .padding(16.dp),
                horizontalAlignment = Alignment.CenterHorizontally
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.Start
                ) {
                    IconButton(
                        onClick = onBackClick,
                        modifier = Modifier
                            .background(
                                getThemeColors().backgroundSecondary.copy(alpha = 0.85f),
                                CircleShape
                            )
                    ) {
                        Icon(
                            Icons.Default.ArrowBack,
                            contentDescription = "Back",
                            tint = getThemeColors().textColor
                        )
                    }
                }

                Spacer(modifier = Modifier.height(10.dp))

                Card(
                    modifier = Modifier
                        .width(170.dp)
                        .height(245.dp),
                    shape = RoundedCornerShape(12.dp)
                ) {
                    AsyncImage(
                        model = movieInfo.movieImage,
                        contentDescription = movieInfo.name,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier.fillMaxSize()
                    )
                }
            }

            LazyColumn(
                modifier = Modifier.weight(1f),
                verticalArrangement = Arrangement.spacedBy(16.dp)
            ) {
                item {
                    Text(
                        text = movieInfo.name ?: "Unknown Movie",
                        style = MaterialTheme.typography.headlineLarge,
                        fontWeight = FontWeight.Bold,
                        color = getThemeColors().textColor
                    )

                    Spacer(modifier = Modifier.height(16.dp))

                    MovieMetadata(movieInfo = movieInfo, isCompact = false)

                    Spacer(modifier = Modifier.height(18.dp))

                    Row(
                        Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.spacedBy(12.dp)
                    ) {
                        Button(
                            onClick = onPlayClick,
                            modifier = Modifier.weight(1f),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = getThemeColors().primaryColor
                            )
                        ) {
                            Icon(Icons.Default.PlayArrow, contentDescription = null)
                            Spacer(modifier = Modifier.width(6.dp))
                            Text(buttonText, color = getThemeColors().textColor)
                        }

                        if (!movieInfo.youtubeTrailer.isNullOrBlank()) {
                            OutlinedButton(
                                onClick = onTrailerClick,
                                modifier = Modifier.weight(1f)
                            ) {
                                Icon(Icons.Default.Movie, contentDescription = null)
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Trailer")
                            }
                        }
                    }
                }

                movieInfo.plot?.takeIf { it.isNotBlank() }?.let { description ->
                    item {
                        Card(
                            colors = CardDefaults.cardColors(
                                containerColor = getThemeColors().backgroundSecondary
                            )
                        ) {
                            Column(modifier = Modifier.padding(16.dp)) {
                                Text(
                                    text = "Plot",
                                    style = MaterialTheme.typography.titleLarge,
                                    fontWeight = FontWeight.Bold,
                                    color = getThemeColors().textColor
                                )
                                Spacer(modifier = Modifier.height(8.dp))
                                Text(
                                    text = description,
                                    style = MaterialTheme.typography.bodyMedium,
                                    color = getThemeColors().textColor.copy(alpha = 0.85f),
                                    lineHeight = 19.sp
                                )
                            }
                        }
                    }
                }

                if (castList.isNotEmpty()) {
                    item {
                        CastSection(castList = castList)
                    }
                }
            }
        }
    }
}
