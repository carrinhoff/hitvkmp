package pt.hitv.core.ui.components

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.focus.FocusRequester
import androidx.compose.ui.focus.focusRequester
import androidx.compose.ui.focus.onFocusChanged
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.key.onPreviewKeyEvent
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.tv.material3.Border
import androidx.tv.material3.Card
import androidx.tv.material3.CardDefaults
import androidx.tv.material3.ExperimentalTvMaterial3Api
import androidx.tv.material3.Glow
import coil3.compose.AsyncImage
import kotlinx.coroutines.delay
import pt.hitv.core.model.Movie
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * TV Material Movie Card Component (Android-only).
 *
 * A horizontal movie card optimized for Android TV using androidx.tv.material3.Card.
 * Features:
 * - Built-in focus scale animation
 * - Built-in focus border and glow effects
 * - D-pad navigation support
 * - Movie poster background with gradient overlay
 * - "TOP" badge and favorite star indicators
 * - Rating display
 *
 * @param movie The movie data to display
 * @param isFavorite Whether this movie is marked as favorite
 * @param onMovieClicked Callback for click events (click or long-click)
 * @param showTopBadge Whether to show the "TOP" badge
 * @param shouldRequestFocus Whether this card should request focus
 * @param onFocused Callback when this card receives focus
 * @param onFocusRestored Callback when focus is successfully restored
 * @param modifier Optional modifier
 * @param favoriteLabel Accessible label for favorite icon
 * @param unknownMovieLabel Fallback label for unknown movies
 */
@OptIn(ExperimentalTvMaterial3Api::class)
@Composable
fun TvMaterialMovieCard(
    movie: Movie,
    isFavorite: Boolean,
    onMovieClicked: (ClickType) -> Unit,
    showTopBadge: Boolean = false,
    shouldRequestFocus: Boolean = false,
    onFocused: (() -> Unit)? = null,
    onFocusRestored: (() -> Unit)? = null,
    modifier: Modifier = Modifier,
    favoriteLabel: String = "Favorite",
    unknownMovieLabel: String = "Unknown Movie"
) {
    val themeColors = getThemeColors()
    val focusRequester = remember { FocusRequester() }

    // Focus restoration
    LaunchedEffect(shouldRequestFocus) {
        if (shouldRequestFocus) {
            try {
                delay(200)
                focusRequester.requestFocus()
                delay(300)
                onFocusRestored?.invoke()
                onFocused?.invoke()
            } catch (e: Exception) {
                if (e !is kotlinx.coroutines.CancellationException) {
                    // Focus restoration failed
                }
            }
        }
    }

    Card(
        onClick = { onMovieClicked(ClickType.CLICK) },
        onLongClick = { onMovieClicked(ClickType.LONG_CLICK) },
        modifier = modifier
            .width(280.dp)
            .height(160.dp)
            .focusRequester(focusRequester)
            .onFocusChanged { focusState ->
                if (focusState.isFocused) {
                    onFocused?.invoke()
                }
            }
            .onPreviewKeyEvent { false },
        shape = CardDefaults.shape(shape = RoundedCornerShape(12.dp)),
        scale = CardDefaults.scale(
            scale = 1.0f,
            focusedScale = 1.02f,
            pressedScale = 0.95f
        ),
        border = CardDefaults.border(
            focusedBorder = Border(
                border = androidx.compose.foundation.BorderStroke(
                    1.dp,
                    themeColors.primaryColor
                ),
                shape = RoundedCornerShape(12.dp)
            )
        ),
        glow = CardDefaults.glow(
            focusedGlow = Glow.None,
            pressedGlow = Glow.None
        ),
        colors = CardDefaults.colors(
            containerColor = Color.Transparent,
            focusedContainerColor = Color.Transparent
        )
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            // Movie poster as background
            AsyncImage(
                model = movie.streamIcon,
                contentDescription = movie.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            // Dark gradient overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .background(
                        Brush.verticalGradient(
                            colors = listOf(
                                Color.Black.copy(alpha = 0.3f),
                                Color.Black.copy(alpha = 0.8f)
                            )
                        )
                    )
            )

            // Content overlay
            Box(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(12.dp)
            ) {
                // TOP badge (top-left)
                if (showTopBadge) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopStart)
                            .background(
                                color = themeColors.primaryColor.copy(alpha = 0.9f),
                                shape = RoundedCornerShape(6.dp)
                            )
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) {
                        Text(
                            text = "TOP",
                            color = Color.White,
                            fontSize = 11.sp,
                            fontWeight = FontWeight.Bold
                        )
                    }
                }

                // Favorite star (top-right)
                if (isFavorite) {
                    Box(
                        modifier = Modifier
                            .align(Alignment.TopEnd)
                            .size(24.dp)
                            .background(
                                color = Color(0xFFFFA000),
                                shape = RoundedCornerShape(12.dp)
                            ),
                        contentAlignment = Alignment.Center
                    ) {
                        Icon(
                            imageVector = Icons.Default.Star,
                            contentDescription = favoriteLabel,
                            tint = Color.White,
                            modifier = Modifier.size(14.dp)
                        )
                    }
                }

                // Movie title and rating
                Column(
                    modifier = Modifier
                        .align(Alignment.CenterStart)
                        .fillMaxWidth(0.85f)
                ) {
                    Text(
                        text = movie.name ?: unknownMovieLabel,
                        color = Color.White,
                        fontSize = 15.sp,
                        fontWeight = FontWeight.Bold,
                        maxLines = 2,
                        overflow = TextOverflow.Ellipsis,
                        lineHeight = 18.sp
                    )

                    val rating = movie.rating?.toDoubleOrNull() ?: 0.0
                    if (rating > 0) {
                        Spacer(modifier = Modifier.height(8.dp))

                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(
                                imageVector = Icons.Default.Star,
                                contentDescription = "Rating",
                                tint = Color(0xFFFFA000),
                                modifier = Modifier.size(14.dp)
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = String.format("%.1f", rating),
                                color = Color.White,
                                fontSize = 13.sp,
                                fontWeight = FontWeight.Medium
                            )
                        }
                    }
                }
            }
        }
    }
}
