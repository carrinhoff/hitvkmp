package pt.hitv.feature.series.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import cafe.adriel.voyager.navigator.LocalNavigator
import cafe.adriel.voyager.navigator.currentOrThrow
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.core.model.enums.ClickType
import pt.hitv.core.navigation.SeriesCategoryDetailArgs
import pt.hitv.core.navigation.navigateToSeriesDetail
import pt.hitv.feature.series.detail.category.SeriesCategoryDetailScreen
import pt.hitv.feature.series.list.SeriesViewModel

class SeriesCategoryDetailVoyagerScreen(
    private val categoryId: String,
    private val categoryName: String
) : Screen {
    constructor(args: SeriesCategoryDetailArgs) : this(args.categoryId, args.categoryName)

    override val key = "SeriesCategoryDetail_$categoryId"

    @Composable
    override fun Content() {
        val viewModel: SeriesViewModel = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()
        val navigator = LocalNavigator.currentOrThrow

        SeriesCategoryDetailScreen(
            initialCategoryId = categoryId,
            initialCategoryName = categoryName,
            viewModel = viewModel,
            analyticsHelper = analyticsHelper,
            onSeriesClicked = { tvShow, position, clickType ->
                when (clickType) {
                    ClickType.CLICK -> {
                        viewModel.saveLastClickedItem(tvShow.seriesId.toString(), position)
                        viewModel.saveRecentlyViewedTvShow(tvShow, Clock.System.now().toEpochMilliseconds())
                        navigator.navigateToSeriesDetail(tvShow.seriesId.toString())
                    }
                    ClickType.LONG_CLICK -> {
                        viewModel.saveFavoriteTvShow(tvShow, isFavoritesFilterActive = false)
                    }
                }
            },
            onBackPressed = { navigator.pop() }
        )
    }
}
