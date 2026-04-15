package pt.hitv.feature.player.composables

import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.interaction.MutableInteractionSource
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.statusBarsPadding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.AspectRatio
import androidx.compose.material.icons.filled.Pause
import androidx.compose.material.icons.filled.PlayArrow
import androidx.compose.material.icons.filled.Replay10
import androidx.compose.material.icons.filled.Forward10
import androidx.compose.material.icons.filled.SkipNext
import androidx.compose.material.icons.filled.SkipPrevious
import androidx.compose.material.icons.filled.Timer
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.Slider
import androidx.compose.material3.SliderDefaults
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableFloatStateOf
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
import pt.hitv.core.designsystem.theme.getThemeColors
import pt.hitv.feature.player.util.SleepTimerManager

/**
 * Shared movie player overlay screen (mobile only, no TV/Cast).
 * Matches original MoviePlayerScreen layout.
 */
@Composable
fun MoviePlayerScreen(
    movieTitle: String,
    isBuffering: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    sleepTimerManager: SleepTimerManager,
    playerViewFactory: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onAspectRatioToggle: () -> Unit,
    onSleepTimerSelect: (Long) -> Unit,
    onSleepTimerCancel: () -> Unit
) {
    val themeColors = getThemeColors()
    var showControls by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerRemaining by sleepTimerManager.remainingMs.collectAsState()
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    // Auto-hide controls
    LaunchedEffect(showControls) {
        if (showControls) {
            delay(5000)
            showControls = false
        }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { showControls = !showControls }
    ) {
        // Video surface
        playerViewFactory(Modifier.fillMaxSize())

        // Buffering indicator
        if (isBuffering) {
            BufferingIndicator()
        }

        // Controls overlay
        AnimatedVisibility(
            visible = showControls,
            enter = fadeIn(),
            exit = fadeOut()
        ) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar — gradient background
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)
                            )
                        )
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = movieTitle,
                            color = Color.White,
                            fontWeight = FontWeight.Bold,
                            fontSize = 16.sp,
                            maxLines = 1,
                            overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        if (sleepTimerRemaining > 0) {
                            SleepTimerIndicator(
                                remainingMs = sleepTimerRemaining,
                                onClick = { showSleepTimerDialog = true }
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(onClick = onAspectRatioToggle, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.AspectRatio, "Aspect Ratio", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showSleepTimerDialog = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Timer, "Sleep Timer", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Center play/pause + skip controls
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(32.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    IconButton(
                        onClick = { onSeekTo((currentPositionMs - 10000).coerceAtLeast(0)) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                    }

                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier
                            .size(64.dp)
                            .background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            imageVector = if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            contentDescription = if (isPlaying) "Pause" else "Play",
                            tint = Color.White,
                            modifier = Modifier.size(40.dp)
                        )
                    }

                    IconButton(
                        onClick = { onSeekTo((currentPositionMs + 10000).coerceAtMost(durationMs)) },
                        modifier = Modifier.size(48.dp)
                    ) {
                        Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(36.dp))
                    }
                }

                // Bottom seekbar + time
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(
                            Brush.verticalGradient(
                                listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))
                            )
                        )
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val progress = if (durationMs > 0) {
                        if (isSeeking) seekPosition else (currentPositionMs.toFloat() / durationMs.toFloat())
                    } else 0f

                    Slider(
                        value = progress.coerceIn(0f, 1f),
                        onValueChange = {
                            isSeeking = true
                            seekPosition = it
                        },
                        onValueChangeFinished = {
                            isSeeking = false
                            onSeekTo((seekPosition * durationMs).toLong())
                        },
                        colors = SliderDefaults.colors(
                            thumbColor = Color.White,
                            activeTrackColor = Color.White,
                            inactiveTrackColor = Color.White.copy(alpha = 0.3f)
                        ),
                        modifier = Modifier.fillMaxWidth()
                    )

                    Row(
                        modifier = Modifier.fillMaxWidth(),
                        horizontalArrangement = Arrangement.SpaceBetween
                    ) {
                        Text(formatTime(currentPositionMs), color = Color.White, fontSize = 12.sp)
                        Text(formatTime(durationMs), color = Color.White, fontSize = 12.sp)
                    }
                }
            }
        }

        // Sleep timer dialog
        if (showSleepTimerDialog) {
            SleepTimerDialog(
                isTimerActive = sleepTimerRemaining > 0,
                remainingMs = sleepTimerRemaining,
                onDurationSelected = { onSleepTimerSelect(it); showSleepTimerDialog = false },
                onCancel = { onSleepTimerCancel(); showSleepTimerDialog = false },
                onDismiss = { showSleepTimerDialog = false }
            )
        }
    }
}

/**
 * Series player overlay — same as movie but with episode navigation.
 */
@Composable
fun SeriesPlayerScreen(
    episodeTitle: String,
    isBuffering: Boolean,
    isPlaying: Boolean,
    currentPositionMs: Long,
    durationMs: Long,
    hasNextEpisode: Boolean,
    hasPreviousEpisode: Boolean,
    sleepTimerManager: SleepTimerManager,
    playerViewFactory: @Composable (Modifier) -> Unit,
    onBack: () -> Unit,
    onPlayPause: () -> Unit,
    onSeekTo: (Long) -> Unit,
    onNextEpisode: () -> Unit,
    onPreviousEpisode: () -> Unit,
    onAspectRatioToggle: () -> Unit,
    onSleepTimerSelect: (Long) -> Unit,
    onSleepTimerCancel: () -> Unit
) {
    val themeColors = getThemeColors()
    var showControls by remember { mutableStateOf(false) }
    var showSleepTimerDialog by remember { mutableStateOf(false) }
    val sleepTimerRemaining by sleepTimerManager.remainingMs.collectAsState()
    var isSeeking by remember { mutableStateOf(false) }
    var seekPosition by remember { mutableFloatStateOf(0f) }

    LaunchedEffect(showControls) {
        if (showControls) { delay(5000); showControls = false }
    }

    Box(
        modifier = Modifier
            .fillMaxSize()
            .background(Color.Black)
            .clickable(
                indication = null,
                interactionSource = remember { MutableInteractionSource() }
            ) { showControls = !showControls }
    ) {
        playerViewFactory(Modifier.fillMaxSize())

        if (isBuffering) { BufferingIndicator() }

        AnimatedVisibility(visible = showControls, enter = fadeIn(), exit = fadeOut()) {
            Box(modifier = Modifier.fillMaxSize()) {
                // Top bar
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .background(Brush.verticalGradient(listOf(Color.Black.copy(alpha = 0.7f), Color.Transparent)))
                        .padding(horizontal = 4.dp, vertical = 4.dp)
                ) {
                    Row(
                        modifier = Modifier.fillMaxWidth().height(44.dp),
                        verticalAlignment = Alignment.CenterVertically
                    ) {
                        IconButton(onClick = onBack, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.AutoMirrored.Filled.ArrowBack, "Back", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        Text(
                            text = episodeTitle,
                            color = Color.White, fontWeight = FontWeight.Bold, fontSize = 16.sp,
                            maxLines = 1, overflow = TextOverflow.Ellipsis,
                            modifier = Modifier.weight(1f).padding(horizontal = 8.dp)
                        )
                        if (sleepTimerRemaining > 0) {
                            SleepTimerIndicator(remainingMs = sleepTimerRemaining, onClick = { showSleepTimerDialog = true })
                            Spacer(modifier = Modifier.width(4.dp))
                        }
                        IconButton(onClick = onAspectRatioToggle, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.AspectRatio, "Aspect Ratio", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                        IconButton(onClick = { showSleepTimerDialog = true }, modifier = Modifier.size(36.dp)) {
                            Icon(Icons.Default.Timer, "Sleep Timer", tint = Color.White, modifier = Modifier.size(20.dp))
                        }
                    }
                }

                // Center controls: prev + rewind + play/pause + forward + next
                Row(
                    modifier = Modifier.align(Alignment.Center),
                    horizontalArrangement = Arrangement.spacedBy(20.dp),
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    if (hasPreviousEpisode) {
                        IconButton(onClick = onPreviousEpisode, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.SkipPrevious, "Previous Episode", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                    IconButton(onClick = { onSeekTo((currentPositionMs - 10000).coerceAtLeast(0)) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Replay10, "Rewind 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    IconButton(
                        onClick = onPlayPause,
                        modifier = Modifier.size(64.dp).background(Color.White.copy(alpha = 0.2f), CircleShape)
                    ) {
                        Icon(
                            if (isPlaying) Icons.Default.Pause else Icons.Default.PlayArrow,
                            if (isPlaying) "Pause" else "Play",
                            tint = Color.White, modifier = Modifier.size(40.dp)
                        )
                    }
                    IconButton(onClick = { onSeekTo((currentPositionMs + 10000).coerceAtMost(durationMs)) }, modifier = Modifier.size(44.dp)) {
                        Icon(Icons.Default.Forward10, "Forward 10s", tint = Color.White, modifier = Modifier.size(32.dp))
                    }
                    if (hasNextEpisode) {
                        IconButton(onClick = onNextEpisode, modifier = Modifier.size(44.dp)) {
                            Icon(Icons.Default.SkipNext, "Next Episode", tint = Color.White, modifier = Modifier.size(32.dp))
                        }
                    }
                }

                // Bottom seekbar
                Column(
                    modifier = Modifier
                        .align(Alignment.BottomCenter)
                        .fillMaxWidth()
                        .background(Brush.verticalGradient(listOf(Color.Transparent, Color.Black.copy(alpha = 0.7f))))
                        .padding(horizontal = 16.dp, vertical = 8.dp)
                ) {
                    val progress = if (durationMs > 0) {
                        if (isSeeking) seekPosition else (currentPositionMs.toFloat() / durationMs.toFloat())
                    } else 0f

                    Slider(
                        value = progress.coerceIn(0f, 1f),
                        onValueChange = { isSeeking = true; seekPosition = it },
                        onValueChangeFinished = { isSeeking = false; onSeekTo((seekPosition * durationMs).toLong()) },
                        colors = SliderDefaults.colors(thumbColor = Color.White, activeTrackColor = Color.White, inactiveTrackColor = Color.White.copy(alpha = 0.3f)),
                        modifier = Modifier.fillMaxWidth()
                    )
                    Row(modifier = Modifier.fillMaxWidth(), horizontalArrangement = Arrangement.SpaceBetween) {
                        Text(formatTime(currentPositionMs), color = Color.White, fontSize = 12.sp)
                        Text(formatTime(durationMs), color = Color.White, fontSize = 12.sp)
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
                onDismiss = { showSleepTimerDialog = false }
            )
        }
    }
}

private fun formatTime(ms: Long): String {
    if (ms <= 0) return "0:00"
    val totalSeconds = ms / 1000
    val hours = totalSeconds / 3600
    val minutes = (totalSeconds % 3600) / 60
    val seconds = totalSeconds % 60
    return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
    else "$minutes:${seconds.toString().padStart(2, '0')}"
}
