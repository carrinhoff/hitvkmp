package pt.hitv.feature.series.ui.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowForward
import androidx.compose.material3.Icon
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import pt.hitv.core.model.TvShow
import pt.hitv.core.ui.components.SeeAllCard
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * MOBILE-ONLY category section for displaying series in horizontal rows.
 */
@Composable
fun CategorySeriesSection(
    categoryTitle: String,
    series: List<TvShow>,
    favoriteIds: Set<Int> = emptySet(),
    onSeriesClicked: (TvShow, Int, ClickType) -> Unit,
    onViewAllClicked: () -> Unit,
    showTopBadge: Boolean = false,
    totalCount: Int = series.size,
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()

    Column(modifier = modifier.fillMaxWidth().padding(vertical = 8.dp)) {
        Row(
            modifier = Modifier.fillMaxWidth().padding(horizontal = 16.dp, vertical = 8.dp).clickable(onClick = onViewAllClicked),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(text = categoryTitle.uppercase(), color = themeColors.primaryColor, fontSize = 16.sp, fontWeight = FontWeight.Bold, maxLines = 1, overflow = TextOverflow.Ellipsis, modifier = Modifier.weight(1f))
            Icon(imageVector = Icons.AutoMirrored.Filled.ArrowForward, contentDescription = "View All", tint = themeColors.primaryColor, modifier = Modifier.padding(start = 8.dp))
        }

        LazyRow(contentPadding = PaddingValues(horizontal = 16.dp), horizontalArrangement = Arrangement.spacedBy(12.dp)) {
            items(count = series.size, key = { index -> series[index].seriesId.toString() }, contentType = { "series_card" }) { index ->
                val tvShow = series[index]
                val stableCallback = remember(tvShow.seriesId) { { clickType: ClickType -> onSeriesClicked(tvShow, index, clickType) } }
                val seriesWithFavorite: TvShow = remember(tvShow.seriesId, favoriteIds) { tvShow.copy(isFavorite = favoriteIds.contains(tvShow.seriesId)) }
                SeriesCardHorizontal(series = seriesWithFavorite, onSeriesClicked = stableCallback, showTopBadge = showTopBadge)
            }
            item(key = "see_all_card", contentType = "see_all") { SeeAllCard(totalCount = totalCount, onClicked = onViewAllClicked) }
        }
    }
}
