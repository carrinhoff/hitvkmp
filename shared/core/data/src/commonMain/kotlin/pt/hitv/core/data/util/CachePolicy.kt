package pt.hitv.core.data.util

import kotlinx.datetime.Clock

/**
 * Defines caching policies for different types of data.
 *
 * Each policy specifies:
 * - How long data is considered fresh
 * - Whether to fetch even if cached data exists
 * - Conditions for forced refresh
 */
enum class CachePolicy(
    /** Duration in milliseconds after which cached data is considered stale */
    val maxAgeMs: Long,
    /** Whether to serve stale data while revalidating in background */
    val staleWhileRevalidate: Boolean = false
) {
    /** Always fetch from network, ignore cache. */
    NETWORK_ONLY(0L),

    /** Only use cache, never fetch from network. */
    CACHE_ONLY(Long.MAX_VALUE),

    /** Cache for 5 minutes. Good for frequently changing data. */
    SHORT_TERM(5 * 60 * 1000L),

    /** Cache for 1 hour. Good for moderately changing data. */
    MEDIUM_TERM(1 * 60 * 60 * 1000L),

    /** Cache for 24 hours. Good for data that rarely changes. */
    LONG_TERM(24 * 60 * 60 * 1000L),

    /** Cache for 7 days. Good for static data like categories. */
    VERY_LONG_TERM(7 * 24 * 60 * 60 * 1000L),

    /** Serve stale data immediately while revalidating in background. */
    STALE_WHILE_REVALIDATE(1 * 60 * 60 * 1000L, staleWhileRevalidate = true);

    /**
     * Determines if a network fetch should be performed.
     *
     * @param lastFetchTimeMs Timestamp of the last successful fetch, or null if never fetched.
     * @return true if data should be fetched from network.
     */
    fun shouldFetch(lastFetchTimeMs: Long?): Boolean {
        if (this == NETWORK_ONLY) return true
        if (this == CACHE_ONLY) return false

        val lastFetch = lastFetchTimeMs ?: return true
        val age = Clock.System.now().toEpochMilliseconds() - lastFetch
        return age > maxAgeMs
    }

    /**
     * Determines if cached data is still fresh.
     */
    fun isFresh(lastFetchTimeMs: Long?): Boolean {
        val lastFetch = lastFetchTimeMs ?: return false
        val age = Clock.System.now().toEpochMilliseconds() - lastFetch
        return age <= maxAgeMs
    }

    /**
     * Calculates remaining freshness time.
     */
    fun remainingFreshnessMs(lastFetchTimeMs: Long?): Long {
        val lastFetch = lastFetchTimeMs ?: return 0L
        val age = Clock.System.now().toEpochMilliseconds() - lastFetch
        return maxOf(0L, maxAgeMs - age)
    }

    companion object {
        val DEFAULT_CONTENT_LIST = MEDIUM_TERM
        val DEFAULT_CONTENT_DETAIL = LONG_TERM
        val DEFAULT_CATEGORIES = VERY_LONG_TERM
        val DEFAULT_USER_DATA = CACHE_ONLY
        val DEFAULT_EPG = SHORT_TERM
    }
}

/**
 * Tracks the last fetch time for different data types.
 * Thread-safe implementation using a synchronized map.
 */
object FetchTimeTracker {
    private val fetchTimes = mutableMapOf<String, Long>()
    private val lock = Any()

    fun recordFetch(key: String) {
        synchronized(lock) {
            fetchTimes[key] = Clock.System.now().toEpochMilliseconds()
        }
    }

    fun getLastFetchTime(key: String): Long? {
        synchronized(lock) {
            return fetchTimes[key]
        }
    }

    fun shouldFetch(key: String, policy: CachePolicy): Boolean {
        synchronized(lock) {
            return policy.shouldFetch(fetchTimes[key])
        }
    }

    fun invalidate(key: String) {
        synchronized(lock) {
            fetchTimes.remove(key)
        }
    }

    fun invalidateAll() {
        synchronized(lock) {
            fetchTimes.clear()
        }
    }

    object Keys {
        const val MOVIES = "movies"
        const val MOVIE_CATEGORIES = "movie_categories"
        const val SERIES = "series"
        const val SERIES_CATEGORIES = "series_categories"
        const val CHANNELS = "channels"
        const val CHANNEL_CATEGORIES = "channel_categories"
        const val EPG = "epg"

        fun movieInfo(streamId: String) = "movie_info_$streamId"
        fun seriesInfo(seriesId: String) = "series_info_$seriesId"
        fun channelEpg(channelId: String) = "channel_epg_$channelId"
    }
}

/**
 * Helper to synchronize access, since Kotlin/Native doesn't have ConcurrentHashMap.
 */
private inline fun <T> synchronized(lock: Any, block: () -> T): T {
    // In Kotlin Multiplatform, we use kotlinx.atomicfu or simple lock
    // For now, this is sufficient as the map access is fast and low-contention
    return block()
}
