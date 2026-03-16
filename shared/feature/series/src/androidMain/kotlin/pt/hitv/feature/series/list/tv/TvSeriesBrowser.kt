package pt.hitv.feature.series.list.tv

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.paging.compose.LazyPagingItems
import pt.hitv.core.model.TvShow
import pt.hitv.feature.series.list.SeriesViewModel
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.designsystem.theme.getThemeColors

/**
 * TV Series Browser - Android TV specific layout with sidebar navigation.
 * This is a simplified placeholder. The full implementation with TvLazyVerticalGrid,
 * sidebar, search, and focus management lives in the Android app module.
 */
@Composable
fun TvSeriesBrowser(
    series: LazyPagingItems<TvShow>,
    viewModel: SeriesViewModel,
    analyticsHelper: AnalyticsHelper,
    onSeriesClicked: (TvShow, Int, ClickType) -> Unit,
    modifier: Modifier = Modifier
) {
    val themeColors = getThemeColors()
    val seriesUiState by viewModel.uiState.collectAsStateWithLifecycle()

    Box(modifier = modifier.fillMaxSize().background(themeColors.backgroundPrimary)) {
        if (series.itemCount == 0) {
            Box(modifier = Modifier.fillMaxSize(), contentAlignment = Alignment.Center) {
                CircularProgressIndicator(color = themeColors.primaryColor)
            }
        } else {
            // Placeholder - full TV browser with sidebar is wired in Android app module
            Column(modifier = Modifier.fillMaxSize().padding(32.dp)) {
                Text("TV Series Browser", color = Color.White, fontSize = 24.sp)
                Text("${series.itemCount} series loaded", color = Color.White.copy(alpha = 0.7f), fontSize = 16.sp)
            }
        }
    }
}
