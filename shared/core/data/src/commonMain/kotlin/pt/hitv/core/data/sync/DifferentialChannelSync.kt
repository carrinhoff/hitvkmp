package pt.hitv.core.data.sync

import kotlinx.datetime.Clock
import pt.hitv.core.database.ChannelQueries
import pt.hitv.core.model.LiveStream

/**
 * Result of a differential channel sync, mirroring the original's `DifferentialSyncResult`.
 */
data class DifferentialSyncResult(
    val inserted: Int = 0,
    val updated: Int = 0,
    val unchanged: Int = 0,
    val deleted: Int = 0,
)

/**
 * How long a channel survives after the provider stops returning it, matching the original's
 * inlined `syncTimestamp - (7 * 24 * 60 * 60 * 1000L)` in `performDifferentialChannelSync`.
 */
internal const val STALE_CHANNEL_RETENTION_MS = 7L * 24 * 60 * 60 * 1000

/**
 * Ids per `markAsSeen` statement. SQLite's default `SQLITE_MAX_VARIABLE_NUMBER` is 999, and a real
 * playlist has tens of thousands of channels, so the single `IN (...)` the original issues would
 * fail outright here.
 */
internal const val MARK_SEEN_BATCH = 500

/**
 * Differential channel sync, ported from `DAOChannel.performDifferentialChannelSync`.
 *
 * The port previously wrote every synced channel with `INSERT OR REPLACE`. Because `channel_unique`
 * is a UNIQUE index on (name, streamIcon, categoryCreatorId, userId) and `channelId` is
 * AUTOINCREMENT, a REPLACE on conflict is a DELETE plus an INSERT — so an entirely unchanged
 * channel came back with a **new channelId after every sync**. Anything referencing that id broke
 * silently: `CustomGroupChannel` rows kept pointing at the old id, so every custom group quietly
 * emptied itself on the next sync. `ChannelIdStabilityTest` pins both halves of that.
 *
 * Matching on (name, categoryCreatorId) and UPDATE-ing in place keeps `channelId` stable, which is
 * what the original relies on. `contentHash` — a column the port already had but always wrote as
 * `null` — decides whether a row needs writing at all, so an unchanged sync now issues no
 * per-channel writes instead of 50k deletes and inserts.
 *
 * Lives next to the queries rather than inside `StreamRepositoryImpl` for the same reason the
 * original puts it on `DAOChannel`: it is pure database work, and keeping it separable is what
 * makes it testable against a real SQLite database without standing up the whole repository.
 *
 * Two deliberate deviations from the original, both platform-driven:
 *  - `markAsSeen` is issued in chunks of [MARK_SEEN_BATCH]; the original passes every seen id to a
 *    single `IN (:channelIds)`, which a real playlist blows past SQLite's variable limit with.
 *  - The 7-day cutoff is a named constant rather than an inline expression.
 *
 * Callers are expected to wrap this in a transaction — `fetchChannelsData` already does.
 */
internal class DifferentialChannelSync(
    private val channelQueries: ChannelQueries,
) {

    fun sync(
        liveStreams: List<LiveStream>,
        userId: Int,
        mainUrl: String,
        syncTimestamp: Long = Clock.System.now().toEpochMilliseconds(),
    ): DifferentialSyncResult {
        val uid = userId.toLong()

        val existingByKey = channelQueries.selectAllForSync(uid)
            .executeAsList()
            .associateBy { naturalChannelKey(it.name, it.categoryCreatorId) }

        val seenIds = ArrayList<Long>(existingByKey.size)
        var inserted = 0
        var updated = 0
        var unchanged = 0

        liveStreams.forEach { liveStream ->
            val name = liveStream.name
            val streamUrl = mainUrl + liveStream.streamId
            val streamIcon = liveStream.streamIcon
            val epgChannelId = liveStream.epgChannelId.trim().lowercase()
            val categoryCreatorId = liveStream.categoryId.toString()
            val tvArchive = liveStream.tvArchive.toLong()
            val tvArchiveDuration = liveStream.tvArchiveDuration.toLong()

            val hash = channelContentHash(
                name = name,
                streamUrl = streamUrl,
                streamIcon = streamIcon,
                epgChannelId = epgChannelId,
                categoryCreatorId = categoryCreatorId,
                tvArchive = tvArchive,
                tvArchiveDuration = tvArchiveDuration,
            )

            val existing = existingByKey[naturalChannelKey(name, categoryCreatorId)]

            if (existing != null) {
                seenIds += existing.channelId
                if (existing.contentHash == hash) {
                    // Untouched channel: left entirely alone, only lastSeen moves (below).
                    unchanged++
                } else {
                    channelQueries.updateById(
                        name = name,
                        streamUrl = streamUrl,
                        streamIcon = streamIcon,
                        epgChannelId = epgChannelId,
                        categoryCreatorId = categoryCreatorId,
                        // User-owned columns are carried across, exactly as the original's
                        // `fromWithExistingSync` does.
                        isFavorite = existing.isFavorite,
                        licenseKey = existing.licenseKey,
                        lastViewedTimestamp = existing.lastViewedTimestamp,
                        lastUpdated = syncTimestamp,
                        lastSeen = syncTimestamp,
                        contentHash = hash,
                        syncVersion = existing.syncVersion + 1,
                        tvArchive = tvArchive,
                        tvArchiveDuration = tvArchiveDuration,
                        catchupType = liveStream.catchupType,
                        catchupSource = liveStream.catchupSource,
                        channelId = existing.channelId,
                    )
                    updated++
                }
            } else {
                channelQueries.insertOrIgnore(
                    name = name,
                    streamUrl = streamUrl,
                    streamIcon = streamIcon,
                    epgChannelId = epgChannelId,
                    categoryCreatorId = categoryCreatorId,
                    isFavorite = 0L,
                    licenseKey = null,
                    userId = uid,
                    lastViewedTimestamp = 0L,
                    lastUpdated = syncTimestamp,
                    lastSeen = syncTimestamp,
                    contentHash = hash,
                    syncVersion = 1L,
                    tvArchive = tvArchive,
                    tvArchiveDuration = tvArchiveDuration,
                    catchupType = liveStream.catchupType,
                    catchupSource = liveStream.catchupSource,
                )
                inserted++
            }
        }

        seenIds.chunked(MARK_SEEN_BATCH).forEach { batch ->
            channelQueries.markAsSeen(syncTimestamp, uid, batch)
        }

        // Channels the provider has not returned for a week are dropped, as in the original. Every
        // row written above carries lastSeen = syncTimestamp, so this can only remove genuinely
        // absent channels — never the ones just synced.
        val cutoff = syncTimestamp - STALE_CHANNEL_RETENTION_MS
        val staleCount = channelQueries.countStale(uid, cutoff).executeAsOne().toInt()
        channelQueries.deleteStale(uid, cutoff)

        return DifferentialSyncResult(
            inserted = inserted,
            updated = updated,
            unchanged = unchanged,
            deleted = staleCount,
        )
    }

    /**
     * The key the original matches existing channels on: `"${name}_${categoryCreatorId}"`.
     *
     * Deliberately NOT the same columns as the `channel_unique` index (which also includes
     * streamIcon) — a provider changing a channel's logo must update the existing row rather than
     * be treated as a new channel, otherwise the old row ages out and its custom-group
     * memberships, favourite flag and watch history go with it.
     */
    private fun naturalChannelKey(name: String?, categoryCreatorId: String?): String =
        "${name.orEmpty()}_${categoryCreatorId.orEmpty()}"

    /**
     * Mirrors `EntityChannel.generateContentHash`: the fields whose change means the row must be
     * rewritten. `String.hashCode` is specified by the language rather than the platform, so this
     * yields identical values on Android and iOS — important, since a mismatch would make every
     * channel look changed on one platform and rewrite the whole table on every sync.
     */
    private fun channelContentHash(
        name: String,
        streamUrl: String,
        streamIcon: String,
        epgChannelId: String,
        categoryCreatorId: String,
        tvArchive: Long,
        tvArchiveDuration: Long,
    ): String =
        "$name$streamUrl$streamIcon$epgChannelId$categoryCreatorId$tvArchive$tvArchiveDuration"
            .hashCode()
            .toString()
}
