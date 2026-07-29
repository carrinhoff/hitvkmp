package pt.hitv.core.sync

import kotlinx.datetime.Clock
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.Resources
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.domain.repositories.TvShowRepository

/**
 * Preference keys for "last successful run" timestamps per background task.
 * Read by the sync settings screen to show "last ran X minutes ago" and infer
 * the next scheduled run (lastRun + interval).
 */
object SyncTimestampKeys {
    const val LAST_SYNC_EPG_MS = "last_sync_epg_ms"
    const val LAST_SYNC_CONTENT_MS = "last_sync_content_ms"
}

/**
 * Per-stage completion flags for the post-login content sync. Each stage flips
 * its flag immediately after a successful fetch so that an interruption
 * (iOS suspension on screen lock, network error, user backgrounding the app
 * past its background-task window) leaves a partial-but-resumable state — the
 * next [SyncManagerImpl.performFullSync] skips already-completed stages instead
 * of redownloading channels just because series failed.
 *
 * Cleared when [SyncManagerImpl.performFullSync] completes all three stages successfully, by
 * [pt.hitv.core.sync.SyncStateManager.forceFullRefresh] (Refresh Data), and implicitly on logout
 * via the credentials wipe.
 *
 * Clearing on success is essential, not tidiness: without it every later run found all three flags
 * set and skipped every stage, so periodic and background content sync became a permanent no-op
 * after the first success — while still reporting success, because skipping everything returns
 * `isSuccess = true`.
 */
object SyncStageKeys {
    const val CHANNELS_DONE = "sync_stage_channels_done"
    const val MOVIES_DONE = "sync_stage_movies_done"
    const val SERIES_DONE = "sync_stage_series_done"
}

/**
 * Shared sync logic that delegates to repositories for actual data operations.
 *
 * This class contains the common orchestration logic for syncing data.
 * Platform-specific scheduling is handled by [SyncScheduler] implementations.
 */
class SyncManagerImpl(
    private val syncScheduler: SyncScheduler,
    private val streamRepository: StreamRepository,
    private val movieRepository: MovieRepository,
    private val tvShowRepository: TvShowRepository,
    private val preferencesHelper: PreferencesHelper
) : SyncManager {

    override suspend fun syncChannels(userId: Int): SyncResult {
        return try {
            val result = streamRepository.fetchChannelsData()
            when (result) {
                is Resources.Success -> SyncResult(isSuccess = true, inserted = result.data.size)
                is Resources.Error -> SyncResult(isSuccess = false, errorMessage = result.message)
                is Resources.Loading -> SyncResult(isSuccess = true)
            }
        } catch (e: Exception) {
            SyncResult(isSuccess = false, errorMessage = e.message)
        }
    }

    override suspend fun syncMovies(userId: Int): SyncResult {
        return try {
            val result = movieRepository.fetchMoviesData()
            when (result) {
                is Resources.Success -> SyncResult(isSuccess = true, inserted = result.data.size)
                is Resources.Error -> SyncResult(isSuccess = false, errorMessage = result.message)
                is Resources.Loading -> SyncResult(isSuccess = true)
            }
        } catch (e: Exception) {
            SyncResult(isSuccess = false, errorMessage = e.message)
        }
    }

    override suspend fun syncSeries(userId: Int): SyncResult {
        return try {
            val result = tvShowRepository.fetchTvShowsData()
            when (result) {
                is Resources.Success -> SyncResult(isSuccess = true, inserted = result.data.size)
                is Resources.Error -> SyncResult(isSuccess = false, errorMessage = result.message)
                is Resources.Loading -> SyncResult(isSuccess = true)
            }
        } catch (e: Exception) {
            SyncResult(isSuccess = false, errorMessage = e.message)
        }
    }

    override suspend fun syncEpg(userId: Int): SyncResult {
        return try {
            val result = streamRepository.fetchEPG(
                epgUrlOverride = null,
                onChannelProgress = { _, _ -> },
                onProgrammeProgress = { _, _ -> }
            )
            when (result) {
                is Resources.Success -> {
                    // NOTE: do NOT insert here. `StreamRepositoryImpl.fetchEPG` already calls
                    // insertEpgDB on both of its branches (the M3U/override path and the Xtream
                    // streaming path) before returning Success. The redundant second insert that
                    // used to live here wrote the entire programme set twice — doubling the DB
                    // work and holding the parsed graph alive across it, which is exactly the
                    // memory profile that gets a BGTask jetsam-killed on iOS.
                    preferencesHelper.setStoredLongTag(
                        SyncTimestampKeys.LAST_SYNC_EPG_MS,
                        Clock.System.now().toEpochMilliseconds()
                    )
                    SyncResult(isSuccess = true)
                }
                is Resources.Error -> SyncResult(isSuccess = false, errorMessage = result.message)
                is Resources.Loading -> SyncResult(isSuccess = true)
            }
        } catch (e: Exception) {
            SyncResult(isSuccess = false, errorMessage = e.message)
        }
    }

    /**
     * Performs a full data sync (channels, movies, series) with progress callbacks.
     *
     * Each stage reports progress as a percentage (0-100), a stage name, and a message.
     * If any stage fails, the sync stops and returns the failing result.
     *
     * Resumable: each stage persists a [SyncStageKeys] flag the moment it succeeds, so an
     * interrupted sync (iOS suspension, network drop) resumes from the failed stage on the next
     * call rather than redoing completed work. Once all three stages succeed the flags are
     * cleared, so the next scheduled run is a real sync rather than a no-op.
     */
    suspend fun performFullSync(
        userId: Int,
        onProgress: suspend (totalPercent: Int, stage: String, message: String) -> Unit
    ): SyncResult {
        val channelsDone = preferencesHelper.getStoredBoolean(SyncStageKeys.CHANNELS_DONE)
        val moviesDone = preferencesHelper.getStoredBoolean(SyncStageKeys.MOVIES_DONE)
        val seriesDone = preferencesHelper.getStoredBoolean(SyncStageKeys.SERIES_DONE)

        // Stage 1: Channels (0-33%)
        if (!channelsDone) {
            onProgress(0, "Channels", "Fetching channels...")
            val channelResult = syncChannels(userId)
            if (!channelResult.isSuccess) return channelResult
            preferencesHelper.setStoredBoolean(SyncStageKeys.CHANNELS_DONE, true)
            onProgress(33, "Channels", "Channels synced: ${channelResult.inserted} items")
        } else {
            onProgress(33, "Channels", "Channels already synced")
        }

        // Stage 2: Movies (33-66%)
        if (!moviesDone) {
            onProgress(33, "Movies", "Fetching movies...")
            val movieResult = syncMovies(userId)
            if (!movieResult.isSuccess) return movieResult
            preferencesHelper.setStoredBoolean(SyncStageKeys.MOVIES_DONE, true)
            onProgress(66, "Movies", "Movies synced: ${movieResult.inserted} items")
        } else {
            onProgress(66, "Movies", "Movies already synced")
        }

        // Stage 3: Series (66-100%)
        if (!seriesDone) {
            onProgress(66, "Series", "Fetching series...")
            val seriesResult = syncSeries(userId)
            if (!seriesResult.isSuccess) return seriesResult
            preferencesHelper.setStoredBoolean(SyncStageKeys.SERIES_DONE, true)
            onProgress(100, "Series", "Series synced: ${seriesResult.inserted} items")
        } else {
            onProgress(100, "Series", "Series already synced")
        }

        // All three stages succeeded, so the resume flags have done their job — clear them.
        //
        // These flags exist so an INTERRUPTED sync can resume without redownloading completed
        // stages. They were never cleared on success, only by "Refresh Data" or logout, so every
        // subsequent run found all three set and skipped everything: after the first successful
        // sync, periodic and background content sync were a **permanent no-op**. The user's
        // library silently stopped updating, and the sync UI still reported success because
        // skipping every stage returns isSuccess = true.
        //
        // Clearing here keeps the intended semantics exactly: interrupted → flags survive → next
        // attempt resumes; completed → flags cleared → next scheduled run does real work.
        preferencesHelper.setStoredBoolean(SyncStageKeys.CHANNELS_DONE, false)
        preferencesHelper.setStoredBoolean(SyncStageKeys.MOVIES_DONE, false)
        preferencesHelper.setStoredBoolean(SyncStageKeys.SERIES_DONE, false)

        // Stamp the content-sync timestamp so the Settings screen can show "last ran X ago".
        // EPG has its own stamp because EPG sync runs on a separate cadence.
        preferencesHelper.setStoredLongTag(
            SyncTimestampKeys.LAST_SYNC_CONTENT_MS,
            Clock.System.now().toEpochMilliseconds()
        )

        return SyncResult(isSuccess = true)
    }

    override fun schedulePeriodicSync(intervalHours: Int) {
        syncScheduler.schedulePeriodicSync(intervalHours)
    }

    override fun cancelPeriodicSync() {
        syncScheduler.cancelPeriodicSync()
    }
}
