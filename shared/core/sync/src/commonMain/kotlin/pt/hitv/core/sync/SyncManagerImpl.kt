package pt.hitv.core.sync

import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.Resources
import pt.hitv.core.domain.repositories.MovieRepository
import pt.hitv.core.domain.repositories.StreamRepository
import pt.hitv.core.domain.repositories.TvShowRepository

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
                    streamRepository.insertEpgDB(
                        epgList = result.data,
                        onChannelProgress = { _, _ -> },
                        onProgrammeProgress = { _, _ -> }
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
     */
    suspend fun performFullSync(
        userId: Int,
        onProgress: suspend (totalPercent: Int, stage: String, message: String) -> Unit
    ): SyncResult {
        // Stage 1: Channels (0-33%)
        onProgress(0, "Channels", "Fetching channels...")
        val channelResult = syncChannels(userId)
        if (!channelResult.isSuccess) return channelResult
        onProgress(33, "Channels", "Channels synced: ${channelResult.inserted} items")

        // Stage 2: Movies (33-66%)
        onProgress(33, "Movies", "Fetching movies...")
        val movieResult = syncMovies(userId)
        if (!movieResult.isSuccess) return movieResult
        onProgress(66, "Movies", "Movies synced: ${movieResult.inserted} items")

        // Stage 3: Series (66-100%)
        onProgress(66, "Series", "Fetching series...")
        val seriesResult = syncSeries(userId)
        if (!seriesResult.isSuccess) return seriesResult
        onProgress(100, "Series", "Series synced: ${seriesResult.inserted} items")

        return SyncResult(isSuccess = true)
    }

    override fun schedulePeriodicSync(intervalHours: Int) {
        syncScheduler.schedulePeriodicSync(intervalHours)
    }

    override fun cancelPeriodicSync() {
        syncScheduler.cancelPeriodicSync()
    }
}
