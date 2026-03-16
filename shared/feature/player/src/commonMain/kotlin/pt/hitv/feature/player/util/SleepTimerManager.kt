package pt.hitv.feature.player.util

import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow

/**
 * Cross-platform sleep timer using coroutines instead of Android CountDownTimer.
 */
class SleepTimerManager(
    private val scope: CoroutineScope,
    private val onTimerFinished: () -> Unit
) {
    private val _remainingMs = MutableStateFlow(0L)
    val remainingMs: StateFlow<Long> = _remainingMs.asStateFlow()

    val isActive: Boolean get() = _remainingMs.value > 0L

    private var timerJob: Job? = null

    fun start(durationMs: Long) {
        cancel()
        _remainingMs.value = durationMs
        timerJob = scope.launch {
            val startTime = currentTimeMillis()
            val endTime = startTime + durationMs
            while (isActive) {
                delay(1000L)
                val remaining = endTime - currentTimeMillis()
                if (remaining <= 0L) {
                    _remainingMs.value = 0L
                    onTimerFinished()
                    break
                }
                _remainingMs.value = remaining
            }
        }
    }

    fun cancel() {
        timerJob?.cancel()
        timerJob = null
        _remainingMs.value = 0L
    }

    private fun currentTimeMillis(): Long = kotlinx.datetime.Clock.System.now().toEpochMilliseconds()

    companion object {
        val PRESET_DURATIONS = listOf(
            15L * 60 * 1000 to "15 minutes",
            30L * 60 * 1000 to "30 minutes",
            45L * 60 * 1000 to "45 minutes",
            60L * 60 * 1000 to "1 hour",
            120L * 60 * 1000 to "2 hours"
        )

        fun formatRemainingTime(ms: Long): String {
            val totalSeconds = ms / 1000
            val hours = totalSeconds / 3600
            val minutes = (totalSeconds % 3600) / 60
            val seconds = totalSeconds % 60
            return if (hours > 0) "$hours:${minutes.toString().padStart(2, '0')}:${seconds.toString().padStart(2, '0')}"
            else "${minutes}:${seconds.toString().padStart(2, '0')}"
        }
    }
}
