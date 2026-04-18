package pt.hitv.feature.player.composables

import androidx.compose.foundation.background
import androidx.compose.foundation.gestures.detectTapGestures
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.BoxScope
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
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
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.input.pointer.pointerInput
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import kotlinx.coroutines.delay
import pt.hitv.feature.player.PlaybackState
import pt.hitv.feature.player.PlayerAspectMode
import pt.hitv.feature.player.PlayerHost

/**
 * Overlay-only series player screen — identical chrome to [MoviePlayerScreen]
 * plus SkipPrevious / SkipNext icons in the top-right row (when [hasPrevious] /
 * [hasNext] respectively). The native bottom controller underneath handles
 * play/pause/scrubbing; ExoPlayer's playlist and AVPlayer's queue provide native
 * prev/next buttons, and the overlay icons call [onPrevious] / [onNext] which
 * route to `PlayerHost.seekToPrevious()/Next()`.
 *
 * Matches `MobileSeriesPlayerContent` from the original hitv project
 * (`feature/player/.../series/SeriesPlayerScreen.kt`) — subtitles icon is
 * deferred as with the movie player.
 */
@Composable
fun SeriesPlayerScreen(
    host: PlayerHost,
    title: String,
    aspectMode: PlayerAspectMode,
    hasPrevious: Boolean,
    hasNext: Boolean,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleAspect: () -> Unit,
    onSleepTimerClick: () -> Unit,
    sleepTimerRemainingMs: Long,
    modifier: Modifier = Modifier
) {
    val controller = host.controller
    val playbackState by controller.playbackState.collectAsState()
    val error by controller.error.collectAsState()

    var showControls by remember { mutableStateOf(true) }

    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5_000L)
            showControls = false
        }
    }

    Box(
        modifier = modifier
            .fillMaxSize()
            .background(Color.Black)
            .pointerInput(Unit) {
                detectTapGestures(onTap = { showControls = !showControls })
            }
    ) {
        host.Surface(Modifier.fillMaxSize())

        if (playbackState == PlaybackState.Buffering) {
            CircularProgressIndicator(
                modifier = Modifier.align(Alignment.Center).size(64.dp),
                color = MaterialTheme.colorScheme.primary,
                strokeWidth = 4.dp
            )
        }

        error?.let { msg ->
            Text(
                text = msg,
                color = MaterialTheme.colorScheme.error,
                fontSize = 18.sp,
                modifier = Modifier.align(Alignment.Center)
            )
        }

        if (showControls) {
            SeriesPlayerOverlay(
                title = title,
                aspectMode = aspectMode,
                hasPrevious = hasPrevious,
                hasNext = hasNext,
                sleepTimerRemainingMs = sleepTimerRemainingMs,
                onBack = onBack,
                onPrevious = onPrevious,
                onNext = onNext,
                onToggleAspect = onToggleAspect,
                onSleepTimerClick = onSleepTimerClick
            )
        }
    }
}

@Composable
private fun BoxScope.SeriesPlayerOverlay(
    title: String,
    aspectMode: PlayerAspectMode,
    hasPrevious: Boolean,
    hasNext: Boolean,
    sleepTimerRemainingMs: Long,
    onBack: () -> Unit,
    onPrevious: () -> Unit,
    onNext: () -> Unit,
    onToggleAspect: () -> Unit,
    onSleepTimerClick: () -> Unit
) {
    // Title — same shape as MoviePlayerScreen.
    Text(
        text = title,
        color = MaterialTheme.colorScheme.onSurface,
        fontSize = 20.sp,
        fontWeight = FontWeight.Normal,
        maxLines = 1,
        overflow = TextOverflow.Ellipsis,
        textAlign = TextAlign.Center,
        modifier = Modifier
            .align(Alignment.TopCenter)
            .padding(top = 16.dp, start = 56.dp, end = 160.dp)
            .background(
                MaterialTheme.colorScheme.surface.copy(alpha = 0.5f),
                RoundedCornerShape(4.dp)
            )
            .padding(8.dp)
    )

    // Back button — top-left.
    IconButton(
        onClick = onBack,
        modifier = Modifier
            .align(Alignment.TopStart)
            .padding(start = 20.dp, top = 20.dp)
    ) {
        Icon(
            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
            contentDescription = "Back",
            tint = MaterialTheme.colorScheme.onSurface
        )
    }

    // Top-right: SkipPrevious (if any) + SkipNext (if any) + AspectRatio +
    // Overflow. Icons are 48.dp buttons with 24.dp inner icons.
    Row(
        modifier = Modifier
            .align(Alignment.TopEnd)
            .padding(top = 10.dp, end = 10.dp)
    ) {
        if (hasPrevious) {
            IconButton(
                onClick = onPrevious,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipPrevious,
                    contentDescription = "Previous Episode",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        if (hasNext) {
            IconButton(
                onClick = onNext,
                modifier = Modifier.size(48.dp)
            ) {
                Icon(
                    imageVector = Icons.Default.SkipNext,
                    contentDescription = "Next Episode",
                    tint = MaterialTheme.colorScheme.onSurface,
                    modifier = Modifier.size(24.dp)
                )
            }
        }

        IconButton(
            onClick = onToggleAspect,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.AspectRatio,
                contentDescription = "Aspect Ratio: ${aspectMode.name}",
                tint = MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }

        IconButton(
            onClick = onSleepTimerClick,
            modifier = Modifier.size(48.dp)
        ) {
            Icon(
                imageVector = Icons.Default.Timer,
                contentDescription = "Sleep Timer",
                tint = if (sleepTimerRemainingMs > 0) MaterialTheme.colorScheme.primary
                       else MaterialTheme.colorScheme.onSurface,
                modifier = Modifier.size(24.dp)
            )
        }
    }
}
