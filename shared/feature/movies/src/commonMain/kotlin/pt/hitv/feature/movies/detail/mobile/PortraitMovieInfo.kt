package pt.hitv.feature.movies.detail.mobile

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
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.alpha
import androidx.compose.ui.draw.blur
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.zIndex
import coil3.compose.AsyncImage
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.cast.Cast
import pt.hitv.core.model.movieInfo.Info
import pt.hitv.core.model.movieInfo.MovieData
import pt.hitv.feature.movies.detail.shared.CastSection
import pt.hitv.feature.movies.detail.shared.MovieDetailRow

@Composable
fun PortraitMovieInfo(
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

    LazyColumn(
        modifier = Modifier
            .fillMaxSize()
            .background(getThemeColors().backgroundPrimary),
        contentPadding = PaddingValues(bottom = 24.dp)
    ) {
        item {
            Box {
                movieInfo.backdropPath?.firstOrNull()?.let { backdrop ->
                    AsyncImage(
                        model = backdrop,
                        contentDescription = null,
                        contentScale = ContentScale.Crop,
                        modifier = Modifier
                            .fillMaxWidth()
                            .height(210.dp)
                            .blur(5.dp)
                            .alpha(0.2f)
                    )
                }

                IconButton(
                    onClick = onBackClick,
                    modifier = Modifier
                        .padding(14.dp)
                        .background(
                            getThemeColors().backgroundSecondary.copy(alpha = 0.85f),
                            CircleShape
                        )
                        .zIndex(1f)
                ) {
                    Icon(
                        Icons.Default.ArrowBack,
                        contentDescription = "Back",
                        tint = getThemeColors().textColor
                    )
                }
            }
        }

        item {
            Column(
                Modifier
                    .padding(horizontal = 20.dp)
                    .fillMaxWidth()
            ) {
                Row(
                    modifier = Modifier.padding(top = 10.dp)
                ) {
                    Card(
                        modifier = Modifier
                            .width(115.dp)
                            .height(160.dp),
                        shape = RoundedCornerShape(10.dp)
                    ) {
                        AsyncImage(
                            model = movieInfo.movieImage,
                            contentDescription = movieInfo.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier.fillMaxSize()
                        )
                    }

                    Spacer(Modifier.width(16.dp))

                    Column(
                        modifier = Modifier
                            .padding(top = 4.dp)
                            .weight(1f)
                    ) {
                        Text(
                            text = movieInfo.name ?: "Unknown Movie",
                            style = MaterialTheme.typography.titleLarge,
                            fontWeight = FontWeight.Bold,
                            maxLines = 4,
                            overflow = TextOverflow.Ellipsis,
                            color = getThemeColors().textColor
                        )

                        Spacer(Modifier.height(8.dp))

                        MovieDetailRow("Released", movieInfo.releasedate)
                        MovieDetailRow("Duration", movieInfo.duration)
                        MovieDetailRow("Genre", movieInfo.genre)
                    }
                }

                Spacer(Modifier.height(18.dp))

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
        }

        movieInfo.plot?.takeIf { it.isNotBlank() }?.let { description ->
            item {
                Card(
                    modifier = Modifier
                        .fillMaxWidth()
                        .padding(horizontal = 16.dp, vertical = 16.dp),
                    colors = CardDefaults.cardColors(
                        containerColor = getThemeColors().backgroundSecondary
                    )
                ) {
                    Column(modifier = Modifier.padding(16.dp)) {
                        Text(
                            text = "Plot",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold,
                            color = getThemeColors().textColor
                        )
                        Spacer(modifier = Modifier.height(7.dp))
                        Text(
                            text = description,
                            style = MaterialTheme.typography.bodyMedium,
                            color = getThemeColors().textColor.copy(alpha = 0.88f),
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
