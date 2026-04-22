package pt.hitv.feature.player.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Brush
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pt.hitv.feature.player.util.SleepTimerManager

/**
 * Shared overlay for the movie and series players.
 *
 * Design shift from earlier drafts: the inner `playerViewFactory` slot is now
 * expected to render a video surface with **native platform controls** —
 * `AVPlayerViewController.showsPlaybackControls = true` on iOS,
 * `PlayerView.useController = true` on Android. That matches the original
 * hitv project, which uses ExoPlayer's native PlayerView controls for VOD.
 * Previously we drew a custom Compose seek bar + play/pause + 10s-skip on
 * top, which looked nothing like the original and missed features (AirPlay,
 * PiP, subtitles menu, native 15s skip).
 *
 * This overlay now only provides the thin top-bar chrome the original adds
 * on top of the native controls:
 *  - back button
 *  - title
 *  - aspect-ratio cycle
 *  - sleep timer button + indicator
 *  - (series only) previous/next episode buttons on the right of the top bar
 */
@Composable
fun MoviePlayerScreen(
    movieTitle: String,
    sleepTimerManager: SleepTimerManager,
    playerViewFactory: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onSleepTimerSelect: (Long) -> Unit,
    onSleepTimerCancel: () -> Unit,
) {
    VodOverlay(
        title = movieTitle,
        playerViewFactory = playerViewFactory,
        onBack = onBack,
        onAspectRatioToggle = onAspectRatioToggle,
        sleepTimerManager = sleepTimerManager,
        onSleepTimerSelect = onSleepTimerSelect,
        onSleepTimerCancel = onSleepTimerCancel,
        hasPreviousEpisode = false,
        hasNextEpisode = false,
        onPreviousEpisode = null,
        onNextEpisode = null,
    )
}

@Composable
fun SeriesPlayerScreen(
    episodeTitle: String,
    hasNextEpisode: Boolean,
    hasPreviousEpisode: Boolean,
    sleepTimerManager: SleepTimerManager,
    playerViewFactory: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onNextEpisode: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onSleepTimerSelect: (Long) -> Unit,
    onSleepTimerCancel: () -> Unit,
) {
    VodOverlay(
        title = episodeTitle,
        playerViewFactory = playerViewFactory,
        onBack = onBack,
        onAspectRatioToggle = onAspectRatioToggle,
        sleepTimerManager = sleepTimerManager,
        onSleepTimerSelect = onSleepTimerSelect,
        onSleepTimerCancel = onSleepTimerCancel,
        hasPreviousEpisode = hasPreviousEpisode,
        hasNextEpisode = hasNextEpisode,
        onPreviousEpisode = onPreviousEpisode,
        onNextEpisode = onNextEpisode,
    )
}

@Composable
private fun VodOverlay(
    title: String,
    playerViewFactory: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    sleepTimerManager: SleepTimerManager,
    onSleepTimerSelect: (Long) -> Unit,
    onSleepTimerCancel: () -> Unit,
    hasPreviousEpisode: Boolean,
    hasNextEpisode: Boolean,
    onPreviousEpisode: (() -> Unit)?,
    onNextEpisode: (() -> Unit)?,
) {
    // Top bar auto-hides after 5 s of inactivity, same cadence as original.
    var showTopBar by remember { mutableStateOf(true) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerRemaining by sleepTimerManager.remainingMs.collectAsState()

    LaunchedEffect(showTopBar) {
        if (showTopBar) {
            delay(5000)
            showTopBar = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            // Tap anywhere brings the top bar back. The native controls handle
            // their own tap-to-toggle independently (AVKit / PlayerView).
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() },
            ) { showTopBar = !showTopBar },
    ) {
        // Native video surface — this slot's implementation on each platform
        // provides the scrubber / play / pause / AirPlay / PiP / subtitles.
        playerViewFactory(Modifier.fillMaxSize())

        AnimatedVisibility(visible = showTopBar, enter = fadeIn(), exit = fadeOut()) {
            Box(
                modifier = Modifier
                    .fillMaxWidth()
                    .statusBarsPadding()
                    .background(
                        Brush.verticalGradient(
                            listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                        )
                    )
                    .padding(horizontal = 4.dp, vertical = 4.dp),
            ) {
                Row(
                    modifier = Modifier.fillMaxWidth().height(44.dp),
                    verticalAlignment = Alignment.CenterVertically,
                ) {
                    IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    Text(
                        text = title,
                        color = Color.White,
                        fontWeight = FontWeight.Bold,
                        fontSize = 16.sp,
                        maxLines = 1,
                        overflow = TextOverflow.Ellipsis,
                        modifier = Modifier.weight(1f).padding(horizontal = 8.dp),
                    )
                    if (sleepTimerRemaining > 0) {
                        SleepTimerIndicator(
                            remainingMs = sleepTimerRemaining,
                            onClick = { showSleepTimerDialog = true },
                        )
                        Spacer(modifier = Modifier.width(4.dp))
                    }
                    // Episode nav: only shown when the current playback is part of
                    // a series that has other episodes to jump to. Movies pass
                    // both flags false and skip the icons entirely.
                    if (hasPreviousEpisode && onPreviousEpisode != null) {
                        IconButton(onClick = onPreviousEpisode, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.SkipPrevious,
                                "Previous Episode",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    if (hasNextEpisode && onNextEpisode != null) {
                        IconButton(onClick = onNextEpisode, modifier = Modifier.size(36.dp)) {
                            Icon(
                                Icons.Default.SkipNext,
                                "Next Episode",
                                tint = Color.White,
                                modifier = Modifier.size(20.dp),
                            )
                        }
                    }
                    IconButton(onClick = onAspectRatioToggle, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.AspectRatio,
                            "Aspect Ratio",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                    IconButton(onClick = { showSleepTimerDialog = true }, modifier = Modifier.size(36.dp)) {
                        Icon(
                            Icons.Default.Timer,
                            "Sleep Timer",
                            tint = Color.White,
                            modifier = Modifier.size(20.dp),
                        )
                    }
                }
            }
        }

        if (showSleepTimerDialog) {
            SleepTimerDialog(
                isTimerActive = sleepTimerRemaining > 0,
                remainingMs = sleepTimerRemaining,
                onDurationSelected = { onSleepTimerSelect(it); showSleepTimerDialog = false },
                onCancel = { onSleepTimerCancel(); showSleepTimerDialog = false },
                onDismiss = { showSleepTimerDialog = false },
            )
        }
    }
}
