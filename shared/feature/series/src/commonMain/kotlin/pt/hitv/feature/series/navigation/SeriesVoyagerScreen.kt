package pt.hitv.feature.series.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import org.koin.compose.koinInject
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.navigation.navigateToSeriesDetail
import pt.hitv.core.navigation.navigateToSeriesCategoryDetail
import pt.hitv.feature.series.list.SeriesScreen
import pt.hitv.feature.series.list.SeriesViewModel

class SeriesVoyagerScreen : Screen {
    override val key = "Series"

    @Composable
    override fun Content() {
        val viewModel: SeriesViewModel = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow

        SeriesScreen(
            viewModel = viewModel,
            analyticsHelper = analyticsHelper,
            onSeriesClicked = { tvShow, _, clickType ->
                when (clickType) {
                    ClickType.CLICK -> navigator.navigateToSeriesDetail(tvShow.seriesId.toString())
                    ClickType.LONG_CLICK -> viewModel.saveFavoriteTvShow(tvShow, false)
                }
            },
            onNavigateToCategory = { categoryId, categoryName ->
                navigator.navigateToSeriesCategoryDetail(categoryId, categoryName)
            },
            onManageCategoriesClick = {
                // TODO: Wire to manage categories navigation
            }
        )
    }
}
