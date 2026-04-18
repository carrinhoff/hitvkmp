package pt.hitv.feature.channels.navigation

import androidx.compose.runtime.Composable
import cafe.adriel.voyager.core.screen.Screen
import kotlinx.datetime.Clock
import org.koin.compose.koinInject
import pt.hitv.core.common.analytics.AnalyticsHelper
import pt.hitv.feature.channels.StreamViewModel
import pt.hitv.feature.channels.ui.LiveChannelsScreen
import pt.hitv.feature.player.platform.launchChannelPlayer
import pt.hitv.feature.settings.options.options.parental.ParentalSessionGuard

class ChannelsVoyagerScreen : Screen {
    override val key = "Channels"

    @Composable
    override fun Content() {
        val viewModel: StreamViewModel = koinInject()
        val analyticsHelper: AnalyticsHelper = koinInject()

        ParentalSessionGuard {
            LiveChannelsScreen(
                viewModel = viewModel,
                analyticsHelper = analyticsHelper,
                onChannelClicked = { channel, position ->
                    viewModel.saveRecentlyViewedChannel(channel, Clock.System.now().toEpochMilliseconds())
                    launchChannelPlayer(
                        url = channel.streamUrl ?: "",
                        name = channel.name ?: "",
                        logoUrl = channel.streamIcon
                    )
                },
                onNavigateToEpg = {
                    // TODO: Wire to EPG navigation
                }
            )
        }
    }
}
