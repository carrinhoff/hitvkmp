package pt.hitv.core.data.repository

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.map
import app.cash.sqldelight.coroutines.asFlow
import app.cash.sqldelight.coroutines.mapToList
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.coroutines.flow.flowOn
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.url.ServerUrlNormalizer
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

        // Two genuinely different shapes, so two normalizers — see ServerUrlNormalizer. An M3U
        // playlist URL is fetched verbatim, so its path, query and trailing slash must survive
        // untouched (appending a slash can 404 a valid URL); it still needs whitespace stripped and
        // a scheme prepended. An Xtream base is concatenated with "player_api.php" and so needs
        // exactly one trailing slash. The M3U branch used a bare `trim()`, which left a pasted URL
        // with interior whitespace broken and a scheme-less one unusable.
        val isM3uUser = userCredentials.password.isEmpty()
        val hostname = if (isM3uUser) {
            ServerUrlNormalizer.normalizePlaylistUrl(userCredentials.hostname)
        } else {
            normalizeHostname(userCredentials.hostname)
        }

        // Insert-then-read-back-the-id, atomic as in the original's `insertOrGetUserId`
        // (`@Transaction`). Unwrapped, a concurrent write between the two statements can hand back
        // a different account's id — and the caller stores that as the signed-in user.
        // Encryption stays outside: it is platform work, not database work.
        return userCredentialsQueries.transactionWithResult {
            userCredentialsQueries.insert(
                username = userCredentials.username,
                encryptedPassword = encryptedPassword,
                hostname = hostname,
                expirationDate = userCredentials.expirationDate,
                epgUrl = userCredentials.epgUrl,
                allowedOutputFormats = userCredentials.allowedOutputFormats?.joinToString(","),
                channelPreviewEnabled = if (userCredentials.channelPreviewEnabled != false) 1L else 0L
            )

            // Get the user ID (either newly inserted or existing)
            val existingId = userCredentialsQueries.selectUserId(userCredentials.username, hostname)
                .executeAsOneOrNull()

            // `insert` is INSERT OR IGNORE against a UNIQUE(username, hostname), so re-logging into
            // an account the app already knows was a **no-op**: the newly entered password, the
            // refreshed expiry date and the server's allowed output formats were all discarded, and
            // the app carried on using whatever it stored the first time. Change your password at
            // the provider, re-enter it here, and the app stays broken; renew a subscription and it
            // keeps showing the old expiry.
            //
            // The original handles this explicitly — `DAOUserCredentials.insertOrGetUserId` checks
            // for the -1 rowid that signals an ignored insert and calls `updateCredentials` on the
            // existing row. SQLDelight does not hand back the rowid here, so the update is applied
            // unconditionally: for a row that was just inserted it rewrites the same three values
            // and is a no-op, and it is inside this transaction either way, so observers still see
            // a single change.
            //
            // Deliberately only these three columns, matching the original: `epgUrl` and
            // `channelPreviewEnabled` are user settings rather than provider facts, and a re-login
            // must not reset them.
            if (existingId != null) {
                userCredentialsQueries.updateCredentials(
                    encryptedPassword = encryptedPassword,
                    expirationDate = userCredentials.expirationDate,
                    allowedOutputFormats = userCredentials.allowedOutputFormats?.joinToString(","),
                    userId = existingId,
                )
            }

            existingId?.toInt() ?: -1
        }
    }

    /**
     * Delegates to the ported [ServerUrlNormalizer] rather than the partial reimplementation that
     * used to live here.
     *
     * The old version trimmed only the ends of the string and appended a trailing slash. It did not
     * prepend a scheme, so a host typed as `myserver.com:8080` was stored without `http://` and
     * failed every request; it did not strip interior whitespace, the single most common cause of
     * IPTV login failures; it did not collapse repeated trailing slashes; and it did not strip a
     * pasted `/player_api.php?...` endpoint back to the base. All four are on the login screen,
     * which is the first thing a new user touches.
     */
    private fun normalizeHostname(hostname: String): String =
        ServerUrlNormalizer.normalize(hostname)

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
                channelPreviewEnabled = it.channelPreviewEnabled != 0L
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
                channelPreviewEnabled = it.channelPreviewEnabled != 0L
            )
        }
    }

    /**
     * Removes an account and every row belonging to it.
     *
     * Wrapped in a single transaction. The original spreads the work over four `@Transaction` DAO
     * methods (`DAOChannel`/`DAOEpg`/`DAOMovie`/`DAOTvShow`.`deleteUserAndRelatedData`) plus the
     * credentials delete; SQLDelight has no DAO layer to hang those off, so one outer transaction
     * expresses the same intent. It is also strictly safer: the port previously ran eighteen
     * statements unwrapped, so a failure partway left the account half-deleted — credentials still
     * present, content gone, or the reverse — with no way to retry cleanly.
     *
     * EPG children are deleted before their parents, matching `DAOEpg.deleteUserAndRelatedData`
     * (descriptions → titles → programmes → channels). The port had inverted this, deleting
     * programmes first. That mattered because `deleteTitlesByUserId`/`deleteDescriptionsByUserId`
     * used to select through Programme — see the note in `Programme.sq`. Those queries now filter
     * on their own `userId` and so are order-independent, but the faithful order is kept: it is
     * what the original does, and it stays correct if the predicates ever change back.
     */
    override suspend fun deleteUserAndRelatedData(userId: Int) {
        try {
            val uid = userId.toLong()
            userCredentialsQueries.transaction {
                channelQueries.deleteByUserId(uid)
                categoryQueries.deleteByUserId(uid)

                programmeQueries.deleteDescriptionsByUserId(uid)
                programmeQueries.deleteTitlesByUserId(uid)
                programmeQueries.deleteProgrammesByUserId(uid)
                epgChannelQueries.deleteByUserId(uid)

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
            }
        } catch (e: Exception) {
            throw e
        }
    }

    override fun getAllCredentials(): Flow<List<UserCredentials>> {
        // Reactive: this emitted once, so the Switch Account screen never refreshed. Adding,
        // editing or deleting an account left the list showing what it held when it opened —
        // including the account the user had just removed.
        return userCredentialsQueries.selectAll()
            .asFlow()
            .mapToList(Dispatchers.IO)
            .map { list ->
                val currentUserId = preferencesHelper.getUserId()
                list.sortedByDescending { it.userId.toInt() == currentUserId }.map {
                    UserCredentials(
                        userId = it.userId.toInt(),
                        username = it.username,
                        // Never expose decrypted passwords to the UI layer, as in the original.
                        password = "",
                        hostname = it.hostname,
                        expirationDate = it.expirationDate,
                        allowedOutputFormats = it.allowedOutputFormats?.split(","),
                        epgUrl = it.epgUrl,
                        channelPreviewEnabled = it.channelPreviewEnabled != 0L
                    )
                }
            }
    }

    override suspend fun updateChannelPreviewEnabled(userId: Int, enabled: Boolean) {
        userCredentialsQueries.updateChannelPreviewEnabled(if (enabled) 1L else 0L, userId.toLong())
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
