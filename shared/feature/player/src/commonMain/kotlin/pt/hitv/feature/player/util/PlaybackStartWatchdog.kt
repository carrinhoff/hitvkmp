package pt.hitv.feature.player.util

import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.launch

/**
 * Fires once if playback never produces a first frame within [timeoutMs].
 *
 * Ported from the original's `hitv/feature/player/.../util/PlaybackStartWatchdog.kt`, which
 * exists because — quoting its own KDoc — "ExoPlayer can sit in `STATE_BUFFERING` indefinitely
 * without ever calling `onPlayerError` — a panel that accepts the connection and then sends
 * nothing produces exactly that."
 *
 * Coroutine-based instead of `Handler`/`Looper` so it works in `commonMain`. Same 25 s default,
 * same [arm] / [cancel] semantics (arm on prepare, cancel on first-frame/ready and on teardown),
 * and the same "0 disables it entirely" escape hatch.
 *
 * ## Deliberate divergence from the original — read before changing
 *
 * In the original the timeout callback is **analytics-only**: it reports a
 * `PlaybackFailureSource.NEVER_STARTED` event and leaves the UI alone
 * (`MoviePlayerActivity.kt:740-755`). That is sufficient on Android because ExoPlayer's
 * `onPlayerError` fires independently of the timeline and is what actually surfaces the error
 * to the user.
 *
 * That safety net does not exist on iOS. All four iOS hosts detect `AVPlayerItemStatusFailed`
 * *inside* `addPeriodicTimeObserverForInterval`, and that block is driven by the item's
 * timeline advancing. When an item fails during load — dead stream, expired token, 401 from
 * the panel — the timeline never advances and playback never starts, so the failure branch may
 * never run at all and the user is left on an infinite buffering spinner with no error and no
 * retry.
 *
 * So here the callback is wired to surface a user-visible error, not just log one. Analytics is
 * NoOp in this port anyway, so a faithful analytics-only port would do nothing whatsoever.
 * See KMP_MIGRATION_AUDIT.md §6.
 *
 * Not thread-safe by design — [arm] and [cancel] are called from player callbacks, which are
 * delivered on the main thread on both platforms.
 *
 * @param scope the scope the countdown runs in; cancelling it cancels a pending deadline.
 * @param timeoutMs how long to wait. 0 disables the watchdog entirely.
 * @param onTimeout invoked when the deadline passes without [cancel].
 */
class PlaybackStartWatchdog(
    private val scope: CoroutineScope,
    private val timeoutMs: Long = DEFAULT_TIMEOUT_MS,
    private val onTimeout: () -> Unit,
) {

    private var job: Job? = null

    /** True once [cancel] has been called for a successful start, so we only fire once. */
    private var started = false

    /** (Re)starts the countdown. Any previously armed deadline is discarded. */
    fun arm() {
        cancel()
        if (timeoutMs <= 0L) return // 0 disables the watchdog entirely.
        started = false
        job = scope.launch {
            delay(timeoutMs)
            if (!started) onTimeout()
        }
    }

    /**
     * Stops a pending deadline. Safe to call when nothing is armed, and to call repeatedly.
     * Call this the moment playback is confirmed running, and again on teardown.
     */
    fun cancel() {
        started = true
        job?.cancel()
        job = null
    }

    companion object {
        /** Generous enough that a slow-but-working provider isn't reported as broken. */
        const val DEFAULT_TIMEOUT_MS = 25_000L
    }
}
