package pt.hitv.feature.channels.ui.components

import androidx.compose.runtime.Composable
import androidx.compose.runtime.remember
import androidx.compose.ui.Modifier
import kotlinx.coroutines.flow.MutableStateFlow
import org.koin.compose.koinInject
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.model.Channel
import pt.hitv.feature.channels.StreamViewModel

/**
 * Android actual — delegates to the existing ChannelPreviewPlayer
 * with dependencies resolved via Koin.
 */
@androidx.media3.common.util.UnstableApi
@Composable
actual fun ChannelPreviewComposable(
    channel: Channel,
    onClose: () -> Unit,
    onPreviewClicked: () -> Unit,
    modifier: Modifier
) {
    val preferencesHelper: PreferencesHelper = koinInject()
    val viewModel: StreamViewModel = koinInject()
    val isPipModeActive = remember { MutableStateFlow(false) }

    ChannelPreviewPlayer(
        channel = channel,
        onClose = onClose,
        onPreviewClicked = onPreviewClicked,
        isPipModeActive = isPipModeActive,
        preferencesHelper = preferencesHelper,
        viewModel = viewModel,
        useFixedSize = true
    )
}
