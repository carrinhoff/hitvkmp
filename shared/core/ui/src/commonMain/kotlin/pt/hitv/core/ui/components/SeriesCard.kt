package pt.hitv.core.ui.components

import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * A card component for displaying a TV series in grid layouts.
 *
 * Features:
 * - Series cover/poster display with Coil 3
 * - Favorite indicator (reactive to favorites list changes)
 * - Series title with gradient background
 * - Click and long-click handling
 *
 * @param tvShow The TV show data to display
 * @param isFavorite Whether this series is marked as favorite
 * @param onSeriesClicked Callback for click events (click or long-click)
 * @param modifier Optional modifier for customization
 * @param favoriteLabel Accessible label for the favorite icon
 * @param unknownSeriesLabel Fallback label for series with no name
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesCard(
    tvShow: TvShow,
    isFavorite: Boolean,
    onSeriesClicked: (ClickType) -> Unit,
    modifier: Modifier = Modifier,
    favoriteLabel: String = "Favorite",
    unknownSeriesLabel: String = "Unknown Series"
) {
    val themeColors = getThemeColors()

    Card(
        modifier = modifier
            .height(200.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = { onSeriesClicked(ClickType.CLICK) },
                onLongClick = { onSeriesClicked(ClickType.LONG_CLICK) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 8.dp),
        colors = CardDefaults.cardColors(
            containerColor = themeColors.backgroundSecondary
        ),
        shape = RoundedCornerShape(16.dp)
    ) {
        Box(
            modifier = Modifier
                .fillMaxSize()
                .background(
                    Brush.verticalGradient(
                        colors = listOf(
                            themeColors.backgroundSecondary,
                            themeColors.backgroundSecondary.copy(alpha = 0.9f)
                        )
                    )
                )
        ) {
            Column(
                modifier = Modifier.fillMaxSize()
            ) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .weight(1f),
                    contentAlignment = Alignment.Center
                ) {
                    Card(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(8.dp),
                        shape = RoundedCornerShape(12.dp),
                        colors = CardDefaults.cardColors(
                            containerColor = themeColors.textColor.copy(alpha = 0.1f)
                        )
                    ) {
                        AsyncImage(
                            model = tvShow.cover,
                            contentDescription = tvShow.name,
                            contentScale = ContentScale.Crop,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(4.dp)
                        )
                    }

                    if (isFavorite) {
                        Box(
                            modifier = Modifier
                                .align(Alignment.TopEnd)
                                .padding(12.dp)
                                .size(28.dp)
                                .background(
                                    themeColors.primaryColor,
                                    CircleShape
                                ),
                            contentAlignment = Alignment.Center
                        ) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = favoriteLabel,
                                tint = themeColors.textColor,
                                modifier = Modifier.size(16.dp)
                            )
                        }
                    }
                }

                // Series name with gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .height(32.dp)
                        .background(
                            Brush.horizontalGradient(
                                colors = listOf(
                                    themeColors.primaryColor,
                                    themeColors.backgroundSecondary
                                )
                            ),
                            RoundedCornerShape(bottomStart = 16.dp, bottomEnd = 16.dp)
                        ),
                    contentAlignment = Alignment.Center
                ) {
                    Text(
                        text = tvShow.name ?: unknownSeriesLabel,
                        color = themeColors.textColor,
                        fontSize = 13.sp,
                        fontWeight = FontWeight.SemiBold,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        textAlign = TextAlign.Center,
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(horizontal = 8.dp)
                    )
                }
            }
        }
    }
}
