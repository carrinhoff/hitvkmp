package pt.hitv.core.ui.components

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.Spring
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.spring
import androidx.compose.animation.core.tween
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.scaleIn
import androidx.compose.animation.scaleOut
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
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.graphicsLayer
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch
import pt.hitv.core.model.Movie
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * A card component for displaying a movie in grid layouts with Material 3 motion transitions.
 *
 * Features:
 * - Movie poster display with Coil 3 AsyncImage
 * - Favorite indicator (reactive to favorites list changes)
 * - Movie title with gradient background
 * - Material 3 container transform transitions
 * - Smooth entrance animations with spring physics
 * - Press and focus states with elevation changes
 * - Click and long-click handling
 *
 * @param movie The movie data to display
 * @param isFavorite Whether this movie is marked as favorite
 * @param onMovieClicked Callback for click events (click or long-click)
 * @param modifier Optional modifier for customization
 * @param favoriteLabel Accessible label for the favorite icon
 * @param unknownMovieLabel Fallback label for movies with no name
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun MovieCard(
    movie: Movie,
    isFavorite: Boolean,
    onMovieClicked: (ClickType) -> Unit,
    modifier: Modifier = Modifier,
    favoriteLabel: String = "Favorite",
    unknownMovieLabel: String = "Unknown Movie"
) {
    var isPressed by remember { mutableStateOf(false) }
    var isVisible by remember { mutableStateOf(false) }
    val coroutineScope = rememberCoroutineScope()

    // Initial entrance animation
    LaunchedEffect(Unit) {
        delay(50)
        isVisible = true
    }

    // Material 3 motion: Enhanced animations with spring for organic feel
    val animatedScale by animateFloatAsState(
        targetValue = when {
            isPressed -> 0.92f
            else -> 1.0f
        },
        animationSpec = spring(
            dampingRatio = Spring.DampingRatioMediumBouncy,
            stiffness = Spring.StiffnessMedium
        ),
        label = "scale"
    )

    val elevation by animateDpAsState(
        targetValue = when {
            isPressed -> 2.dp
            else -> 8.dp
        },
        animationSpec = tween(durationMillis = 180),
        label = "elevation"
    )

    val themeColors = getThemeColors()

    AnimatedVisibility(
        visible = isVisible,
        enter = fadeIn(
            animationSpec = tween(durationMillis = 300)
        ) + scaleIn(
            initialScale = 0.8f,
            animationSpec = spring(
                dampingRatio = Spring.DampingRatioMediumBouncy,
                stiffness = Spring.StiffnessLow
            )
        ),
        exit = fadeOut(animationSpec = tween(durationMillis = 150)) +
            scaleOut(targetScale = 0.8f)
    ) {
        Card(
            modifier = modifier
                .height(200.dp)
                .padding(horizontal = 6.dp, vertical = 6.dp)
                .graphicsLayer {
                    scaleX = animatedScale
                    scaleY = animatedScale
                    shadowElevation = elevation.toPx()
                }
                .combinedClickable(
                    onClick = {
                        isPressed = true
                        onMovieClicked(ClickType.CLICK)
                        coroutineScope.launch {
                            delay(150)
                            isPressed = false
                        }
                    },
                    onLongClick = { onMovieClicked(ClickType.LONG_CLICK) }
                ),
            elevation = CardDefaults.cardElevation(defaultElevation = elevation),
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
                                model = movie.streamIcon,
                                contentDescription = movie.name,
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

                    // Movie name with gradient background
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
                            text = movie.name ?: unknownMovieLabel,
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
}
