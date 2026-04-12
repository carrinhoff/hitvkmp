package pt.hitv.feature.series.detail.shared

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.BorderStroke
import androidx.compose.foundation.background
import androidx.compose.foundation.focusable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.aspectRatio
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.core.model.seriesInfo.Episode

@Composable
fun MobileEpisodeItem(
    episode: Episode,
    isCompact: Boolean,
    onClick: () -> Unit
) {
    val watchProgress =
        remember(episode.id, episode.info?.playbackPosition, episode.info?.durationSecs) {
            val playbackPosition = episode.info?.playbackPosition ?: 0L
            val durationSecs = episode.info?.durationSecs ?: 0.0

            if (durationSecs > 0 && playbackPosition > 0) {
                (playbackPosition.toFloat() / durationSecs.toFloat()).coerceIn(0f, 1f)
            } else {
                0f
            }
        }

    var isFocused by remember { mutableStateOf(false) }
    val scale by animateFloatAsState(
        targetValue = if (isFocused) 1.03f else 1.0f,
        animationSpec = tween(200), label = "mobile_episode_scale"
    )

    Card(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .onFocusChanged { focusState ->
                isFocused = focusState.isFocused
            }
            .graphicsLayer {
                scaleX = scale
                scaleY = scale
            }
            .focusable(),
        shape = RoundedCornerShape(8.dp),
        colors = CardDefaults.cardColors(
            containerColor = getThemeColors().backgroundSecondary
        ),
        border = if (isFocused) {
            BorderStroke(2.dp, getThemeColors().primaryColor)
        } else null
    ) {
        if (isCompact) {
            Row(
                modifier = Modifier.padding(8.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                EpisodeThumbnail(
                    episode = episode,
                    watchProgress = watchProgress,
                    width = 80.dp
                )

                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = "${episode.episodeNum}. ${episode.title}",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium,
                        maxLines = 1,
                        color = getThemeColors().textColor,
                        overflow = TextOverflow.Ellipsis
                    )

                    Text(
                        text = episode.info?.plot ?: "No description available.",
                        style = MaterialTheme.typography.bodySmall,
                        maxLines = 2,
                        color = getThemeColors().textColor.copy(alpha = 0.6f),
                        overflow = TextOverflow.Ellipsis
                    )

                    if (watchProgress > 0f) {
                        Text(
                            text = "${(watchProgress * 100).toInt()}%",
                            style = MaterialTheme.typography.bodySmall,
                            color = getThemeColors().primaryColor,
                            fontSize = 10.sp
                        )
                    }
                }
            }
        } else {
            Column(modifier = Modifier.padding(12.dp)) {
                Row(
                    horizontalArrangement = Arrangement.spacedBy(12.dp),
                    verticalAlignment = Alignment.Top
                ) {
                    EpisodeThumbnail(
                        episode = episode,
                        watchProgress = watchProgress,
                        width = 120.dp
                    )

                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            text = "${episode.episodeNum}. ${episode.title}",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.SemiBold,
                            maxLines = 2,
                            color = getThemeColors().textColor,
                            overflow = TextOverflow.Ellipsis
                        )

                        Spacer(modifier = Modifier.height(8.dp))

                        Text(
                            text = episode.info?.plot ?: "No description available.",
                            style = MaterialTheme.typography.bodyMedium,
                            maxLines = 3,
                            color = getThemeColors().textColor.copy(alpha = 0.6f),
                            overflow = TextOverflow.Ellipsis
                        )

                        if (watchProgress > 0f) {
                            Spacer(modifier = Modifier.height(8.dp))
                            Text(
                                text = "${(watchProgress * 100).toInt()}% watched",
                                style = MaterialTheme.typography.bodySmall,
                                color = getThemeColors().primaryColor,
                                fontSize = 11.sp
                            )
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun EpisodeThumbnail(
    episode: Episode,
    watchProgress: Float,
    width: Dp,
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .width(width)
            .aspectRatio(16 / 9f)
            .clip(RoundedCornerShape(4.dp))
    ) {
        AsyncImage(
            model = episode.info?.movieImage,
            contentDescription = episode.title,
            modifier = Modifier.fillMaxSize(),
            contentScale = ContentScale.Crop
        )

        // Fallback play icon if no image
        if (episode.info?.movieImage.isNullOrBlank()) {
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(Color.Black),
                contentAlignment = Alignment.Center
            ) {
                Icon(
                    imageVector = Icons.Default.PlayArrow,
                    contentDescription = "Play Icon",
                    tint = Color.White.copy(alpha = 0.7f),
                    modifier = Modifier.size(if (width > 100.dp) 32.dp else 24.dp)
                )
            }
        }

        // Progress bar overlay
        if (watchProgress > 0f) {
            Box(
                modifier = Modifier
                    .align(Alignment.BottomCenter)
                    .fillMaxWidth()
                    .height(if (width > 100.dp) 4.dp else 3.dp)
                    .background(Color.Black.copy(alpha = 0.3f))
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxHeight()
                        .fillMaxWidth(watchProgress)
                        .background(getThemeColors().primaryColor)
                )
            }
        }
    }
}
