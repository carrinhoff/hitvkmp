package pt.hitv.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.data.security.CryptoManager
import pt.hitv.core.database.ChannelQueries
import pt.hitv.core.database.EpgChannelQueries
import pt.hitv.core.database.MovieQueries
import pt.hitv.core.database.MovieInfoQueries
import pt.hitv.core.database.ProgrammeQueries
import pt.hitv.core.database.TvShowQueries
import pt.hitv.core.database.SeriesInfoQueries
import pt.hitv.core.database.CategoryQueries
import pt.hitv.core.database.CategoryVodQueries
import pt.hitv.core.database.CategoryTvShowQueries
import pt.hitv.core.database.UserCredentialsQueries
import pt.hitv.core.domain.repositories.AccountManagerRepository
import pt.hitv.core.model.UserCredentials

class AccountManagerRepositoryImpl(
    private val userCredentialsQueries: UserCredentialsQueries,
    private val channelQueries: ChannelQueries,
    private val categoryQueries: CategoryQueries,
    private val epgChannelQueries: EpgChannelQueries,
    private val programmeQueries: ProgrammeQueries,
    private val movieQueries: MovieQueries,
    private val movieInfoQueries: MovieInfoQueries,
    private val categoryVodQueries: CategoryVodQueries,
    private val tvShowQueries: TvShowQueries,
    private val seriesInfoQueries: SeriesInfoQueries,
    private val categoryTvShowQueries: CategoryTvShowQueries,
    private val cryptoManager: CryptoManager,
    private val preferencesHelper: PreferencesHelper,
) : AccountManagerRepository {

    override suspend fun saveCredentials(userCredentials: UserCredentials): Int {
        val encryptedPassword = if (userCredentials.password.isNotEmpty()) {
            cryptoManager.encryptPassword(userCredentials.password)
        } else {
            ""
        }

        val isM3uUser = userCredentials.password.isEmpty()
        val hostname = if (isM3uUser) userCredentials.hostname.trim() else normalizeHostname(userCredentials.hostname)

        userCredentialsQueries.insert(
            username = userCredentials.username,
            encryptedPassword = encryptedPassword,
            hostname = hostname,
            expirationDate = userCredentials.expirationDate,
            epgUrl = userCredentials.epgUrl,
            allowedOutputFormats = userCredentials.allowedOutputFormats?.joinToString(","),
            channelPreviewEnabled = userCredentials.channelPreviewEnabled ?: true
        )

        // Get the user ID (either newly inserted or existing)
        val existingId = userCredentialsQueries.selectUserId(userCredentials.username, hostname)
            .executeAsOneOrNull()?.userId
        return existingId?.toInt() ?: -1
    }

    private fun normalizeHostname(hostname: String): String {
        var normalized = hostname.trim()
        if (normalized.isEmpty()) return normalized

        if (normalized.contains("?") || normalized.endsWith(".m3u", ignoreCase = true) ||
            normalized.endsWith(".m3u8", ignoreCase = true)
        ) {
            return normalized
        }

        if (!normalized.endsWith("/")) {
            normalized = "$normalized/"
        }

        return normalized
    }

    override suspend fun getCredentialsByUserId(userId: Int): UserCredentials? {
        val entity = userCredentialsQueries.selectByUserId(userId.toLong()).executeAsOneOrNull()
        return entity?.let {
            val decryptedPassword = if (it.encryptedPassword.isNotEmpty()) {
                cryptoManager.decryptPassword(it.encryptedPassword)
            } else {
                ""
            }
            UserCredentials(
                userId = it.userId.toInt(),
                username = it.username,
                password = decryptedPassword,
                hostname = it.hostname,
                expirationDate = it.expirationDate,
                epgUrl = it.epgUrl,
                allowedOutputFormats = it.allowedOutputFormats?.split(","),
                channelPreviewEnabled = it.channelPreviewEnabled
            )
        }
    }

    override suspend fun getCredentialsByUsername(username: String): UserCredentials? {
        val result = userCredentialsQueries.selectByUsername(username).executeAsOneOrNull()
        return result?.let {
            val decryptedPassword = if (it.encryptedPassword.isNotEmpty()) {
                cryptoManager.decryptPassword(it.encryptedPassword)
            } else {
                ""
            }
            UserCredentials(
                userId = it.userId.toInt(),
                username = it.username,
                password = decryptedPassword,
                hostname = it.hostname,
                expirationDate = it.expirationDate,
                epgUrl = it.epgUrl,
                allowedOutputFormats = it.allowedOutputFormats?.split(","),
                channelPreviewEnabled = it.channelPreviewEnabled
            )
        }
    }

    override suspend fun deleteUserAndRelatedData(userId: Int) {
        try {
            val uid = userId.toLong()
            channelQueries.deleteByUserId(uid)
            categoryQueries.deleteByUserId(uid)
            epgChannelQueries.deleteByUserId(uid)
            programmeQueries.deleteProgrammesByUserId(uid)
            programmeQueries.deleteTitlesByUserId(uid)
            programmeQueries.deleteDescriptionsByUserId(uid)
            movieQueries.deleteByUserId(uid)
            movieInfoQueries.deleteMovieInfoByUserId(uid)
            movieInfoQueries.deleteMovieDataByUserId(uid)
            categoryVodQueries.deleteByUserId(uid)
            tvShowQueries.deleteByUserId(uid)
            seriesInfoQueries.deleteSeriesInfoByUserId(uid)
            seriesInfoQueries.deleteSeasonsByUserId(uid)
            seriesInfoQueries.deleteEpisodesByUserId(uid)
            seriesInfoQueries.deleteEpisodesInfoByUserId(uid)
            categoryTvShowQueries.deleteByUserId(uid)
            userCredentialsQueries.deleteByUserId(uid)
        } catch (e: Exception) {
            throw e
        }
    }

    override fun getAllCredentials(): Flow<List<UserCredentials>> {
        return flow {
            val list = userCredentialsQueries.selectAll().executeAsList()
            val currentUserId = preferencesHelper.getUserId()
            emit(
                list.sortedByDescending { it.userId.toInt() == currentUserId }.map {
                    UserCredentials(
                        userId = it.userId.toInt(),
                        username = it.username,
                        password = "",
                        hostname = it.hostname,
                        expirationDate = it.expirationDate,
                        allowedOutputFormats = it.allowedOutputFormats?.split(","),
                        epgUrl = it.epgUrl,
                        channelPreviewEnabled = it.channelPreviewEnabled
                    )
                }
            )
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun updateChannelPreviewEnabled(userId: Int, enabled: Boolean) {
        userCredentialsQueries.updateChannelPreviewEnabled(enabled, userId.toLong())
    }

    override suspend fun updateAccountCredentials(
        userId: Int,
        username: String,
        password: String,
        hostname: String,
        epgUrl: String?
    ) {
        val encryptedPassword = if (password.isNotEmpty()) {
            cryptoManager.encryptPassword(password)
        } else {
            val existing = userCredentialsQueries.selectByUserId(userId.toLong()).executeAsOneOrNull()
                ?: throw IllegalArgumentException("User $userId not found")
            existing.encryptedPassword
        }

        val normalizedHostname = normalizeHostname(hostname)

        try {
            userCredentialsQueries.updateAccountCredentials(
                username = username,
                encryptedPassword = encryptedPassword,
                hostname = normalizedHostname,
                epgUrl = epgUrl,
                userId = userId.toLong()
            )
        } catch (e: Exception) {
            throw IllegalStateException("An account with this username and server URL already exists", e)
        }
    }
}
