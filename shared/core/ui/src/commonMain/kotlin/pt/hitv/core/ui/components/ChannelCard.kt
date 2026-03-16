package pt.hitv.core.ui.components

import androidx.compose.animation.core.animateDpAsState
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
import pt.hitv.core.model.Channel
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * A card component for displaying a channel in grid layouts.
 *
 * Features:
 * - Channel logo display with Coil 3
 * - Favorite indicator
 * - Channel name with gradient background
 * - Click and long-click handling
 *
 * @param channel The channel data to display
 * @param onChannelClicked Callback for click events (click or long-click)
 * @param modifier Optional modifier for customization
 * @param favoriteLabel Accessible label for the favorite icon
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun ChannelCard(
    channel: Channel,
    onChannelClicked: (ClickType) -> Unit,
    modifier: Modifier = Modifier,
    favoriteLabel: String = "Favorite"
) {
    val themeColors = getThemeColors()

    Card(
        modifier = modifier
            .height(140.dp)
            .padding(horizontal = 6.dp, vertical = 6.dp)
            .combinedClickable(
                onClick = { onChannelClicked(ClickType.CLICK) },
                onLongClick = { onChannelClicked(ClickType.LONG_CLICK) }
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
                // Channel Logo
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
                            model = channel.streamIcon,
                            contentDescription = channel.name,
                            contentScale = ContentScale.Fit,
                            modifier = Modifier
                                .fillMaxSize()
                                .padding(8.dp)
                        )
                    }

                    // Favorite indicator
                    if (channel.isFavorite) {
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

                // Channel name with gradient background
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
                        text = channel.name.orEmpty(),
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
