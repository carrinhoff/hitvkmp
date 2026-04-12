package pt.hitv.feature.series.ui.components

import androidx.compose.foundation.ExperimentalFoundationApi
import androidx.compose.foundation.background
import androidx.compose.foundation.combinedClickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Star
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import coil3.compose.AsyncImage
import pt.hitv.core.model.TvShow
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * Mobile-only series card for touch interactions.
 */
@OptIn(ExperimentalFoundationApi::class)
@Composable
fun SeriesCardHorizontal(
    series: TvShow,
    onSeriesClicked: (ClickType) -> Unit,
    showTopBadge: Boolean = false,
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    val isFavorite = series.isFavorite
    val gradientBrush = remember {
        Brush.verticalGradient(colors = listOf(Color(0x4D000000), Color(0xB3000000)))
    }

    Card(
        modifier = modifier
            .width(280.dp)
            .height(160.dp)
            .combinedClickable(
                onClick = { onSeriesClicked(ClickType.CLICK) },
                onLongClick = { onSeriesClicked(ClickType.LONG_CLICK) }
            ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp),
        colors = CardDefaults.cardColors(containerColor = Color.Transparent),
        shape = RoundedCornerShape(12.dp)
    ) {
        Box(modifier = Modifier.fillMaxSize()) {
            AsyncImage(
                model = series.cover,
                contentDescription = series.name,
                contentScale = ContentScale.Crop,
                modifier = Modifier.fillMaxSize()
            )

            Box(modifier = Modifier.fillMaxSize().background(gradientBrush))

            Box(modifier = Modifier.fillMaxSize().padding(12.dp)) {
                if (showTopBadge) {
                    Box(
                        modifier = Modifier.align(Alignment.TopStart)
                            .background(color = themeColors.primaryColor.copy(alpha = 0.9f), shape = RoundedCornerShape(6.dp))
                            .padding(horizontal = 8.dp, vertical = 4.dp)
                    ) { Text(text = "TOP", color = Color.White, fontSize = 11.sp, fontWeight = FontWeight.Bold) }
                }

                if (isFavorite) {
                    Box(
                        modifier = Modifier.align(Alignment.TopEnd).size(24.dp)
                            .background(color = Color(0xFFFFA000), shape = RoundedCornerShape(12.dp)),
                        contentAlignment = Alignment.Center
                    ) { Icon(imageVector = Icons.Default.Star, contentDescription = "Favorite", tint = Color.White, modifier = Modifier.size(14.dp)) }
                }

                Column(modifier = Modifier.align(Alignment.CenterStart).fillMaxWidth(0.85f)) {
                    Text(text = series.name ?: "Unknown Series", color = Color.White, fontSize = 15.sp, fontWeight = FontWeight.Bold, maxLines = 2, overflow = TextOverflow.Ellipsis, lineHeight = 18.sp)
                    val rating = series.rating?.toDoubleOrNull() ?: 0.0
                    if (rating > 0) {
                        Spacer(modifier = Modifier.height(8.dp))
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Icon(imageVector = Icons.Default.Star, contentDescription = "Rating", tint = Color(0xFFFFA000), modifier = Modifier.size(14.dp))
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(text = "${kotlin.math.round(rating * 10) / 10.0}", color = Color.White, fontSize = 13.sp, fontWeight = FontWeight.Medium)
                        }
                    }
                }
            }
        }
    }
}
