package pt.hitv.feature.player

/**
 * Which VOD containers AVFoundation can actually open.
 *
 * ## Why this is needed on iOS and not on Android
 *
 * Xtream serves a movie or episode in whatever container the provider stored it in, and the URL
 * carries that container verbatim: `…/movie/user/pass/12345.mkv`. ExoPlayer has extractors for
 * Matroska, AVI, FLV and more, so the original — Android-only — plays those without anyone having
 * to think about it. AVFoundation has no such extractors; handed an `.mkv` it simply fails.
 *
 * That is a platform limit, not something a URL can be rewritten around: the file on the server
 * really is an MKV. What *is* fixable is the failure mode. Without this check the user taps a
 * movie, gets a black screen, and waits out the 25-second `PlaybackStartWatchdog` before seeing
 * "The movie did not start playing. The stream may be unavailable." — which is both slow and
 * misleading, because the stream is perfectly available and simply cannot be decoded here.
 *
 * ## Deliberately a deny-list
 *
 * Only containers known *not* to work are listed. An unrecognised or absent extension is allowed
 * through so the player can try: guessing wrong in that direction merely reproduces the old
 * behaviour, whereas an over-eager allow-list would block content that plays fine.
 */
object AvFoundationSupport {

    /**
     * Containers AVFoundation cannot open, and that IPTV providers actually serve.
     *
     * `ts` is here for a subtler reason than the rest: a raw MPEG-TS file cannot be played
     * progressively, even though HLS — which AVFoundation does support — is delivered as `.ts`
     * segments behind an `.m3u8` manifest. It is the manifest that matters, so `.m3u8` is fine and
     * a direct `.ts` URL is not.
     */
    private val UNSUPPORTED = setOf(
        "mkv", "avi", "flv", "wmv", "divx", "vob", "rmvb", "rm",
        "mpg", "mpeg", "ogv", "webm", "ts", "m2ts", "mts", "asf",
    )

    /**
     * The container [url] declares, lowercased, or null when it declares none.
     *
     * Query and fragment are stripped first — providers append tokens, and `…/12345.mkv?t=abc`
     * must still be recognised as MKV.
     */
    fun containerOf(url: String): String? {
        val path = url.trim().substringBefore('?').substringBefore('#')
        val lastSlash = path.lastIndexOf('/')
        val lastDot = path.lastIndexOf('.')
        if (lastDot <= lastSlash || lastDot == path.lastIndex) return null
        return path.substring(lastDot + 1).lowercase()
    }

    /** True when [url] declares a container AVFoundation is known to be unable to open. */
    fun isUnsupportedByAvFoundation(url: String): Boolean =
        containerOf(url) in UNSUPPORTED

    /**
     * A message for a channel whose stream is ClearKey-encrypted, or null when it is not.
     *
     * The same shape of gap as the container deny-list. ExoPlayer supports ClearKey, and
     * `LiveMediaSourceFactory` wires a `DefaultDrmSessionManager` for any channel carrying a
     * `licenseKey`. AVFoundation implements FairPlay only — there is no ClearKey path — so the
     * `licenseKey` threaded into the iOS player is accepted and then never used.
     *
     * The stream therefore arrives encrypted, `AVPlayerItem` fails, the retry ladder spends three
     * attempts on it, and the user eventually sees a generic "Playback error". Naming the cause
     * costs nothing and is the difference between "this app is broken" and "this channel needs a
     * DRM iOS does not support".
     */
    fun drmUnsupportedMessage(licenseKey: String?): String? =
        if (licenseKey.isNullOrBlank()) {
            null
        } else {
            "This channel is DRM-protected with ClearKey, which iOS cannot decrypt. " +
                "It plays on Android only."
        }

    /**
     * A message naming the actual problem, for the player's error surface.
     *
     * Returns null when the container is fine or unknown, so callers can use it as a pre-flight
     * check: a non-null result means do not bother loading the item.
     */
    fun unsupportedContainerMessage(url: String): String? {
        val container = containerOf(url) ?: return null
        if (container !in UNSUPPORTED) return null
        return "This title is in .$container format, which iOS cannot play. " +
            "Try another version if your provider offers one."
    }
}
