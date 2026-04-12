package pt.hitv.core.data.manager

import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.flow
import kotlinx.coroutines.flow.flowOn
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.IO
import kotlinx.datetime.Clock
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.database.ParentalControlQueries
import pt.hitv.core.data.mapper.toParentalControl
import pt.hitv.core.domain.manager.ParentalControlManager
import pt.hitv.core.model.ParentalControl

/**
 * Implementation of [ParentalControlManager] that handles parental control operations.
 * Provides centralized access to PIN validation and category protection
 * with session management to reduce PIN entry annoyance.
 */
class ParentalControlManagerImpl(
    private val preferencesHelper: PreferencesHelper,
    private val parentalControlQueries: ParentalControlQueries,
    private val premiumStatusProvider: PremiumStatusProvider
) : ParentalControlManager {

    private val sessionTimeout = 30 * 60 * 1000L // 30 minutes

    companion object {
        const val DEFAULT_SESSION_TIMEOUT_MINUTES = 30
        const val SESSION_TIMEOUT_KEY = "parental_control_session_timeout"
        const val LAST_UNLOCK_TIME_KEY = "parental_control_last_unlock_time"
        const val SESSION_TIMEOUT_UNTIL_APP_CLOSES = -1
        const val SESSION_TIMEOUT_ALWAYS_ASK = -2
    }

    private fun hasPremiumSubscription(): Boolean {
        return premiumStatusProvider.hasPremiumSubscription()
    }

    private fun getLastUnlockTime(): Long {
        return preferencesHelper.getStoredLongTag(LAST_UNLOCK_TIME_KEY).let {
            if (it < 0) 0L else it
        }
    }

    private fun setLastUnlockTime(time: Long) {
        preferencesHelper.setStoredLongTag(LAST_UNLOCK_TIME_KEY, time)
    }

    override fun isParentalControlEnabled(): Boolean {
        if (!hasPremiumSubscription()) return false
        return preferencesHelper.isParentalControlEnabled() && preferencesHelper.hasParentalControlPin()
    }

    override fun isSessionAuthenticated(): Boolean {
        if (!isParentalControlEnabled()) return true
        val timeoutMinutes = preferencesHelper.getStoredIntTag(SESSION_TIMEOUT_KEY)
        if (timeoutMinutes == SESSION_TIMEOUT_ALWAYS_ASK) return false
        val lastUnlock = getLastUnlockTime()
        if (lastUnlock == 0L) return false
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val timeout = getSessionTimeout()
        return (currentTime - lastUnlock) < timeout
    }

    private fun getSessionTimeout(): Long {
        val minutes = preferencesHelper.getStoredIntTag(SESSION_TIMEOUT_KEY)
        return when {
            minutes == SESSION_TIMEOUT_UNTIL_APP_CLOSES -> Long.MAX_VALUE
            minutes > 0 -> minutes * 60 * 1000L
            else -> sessionTimeout
        }
    }

    override fun setSessionTimeout(minutes: Int) {
        preferencesHelper.setStoredIntTag(SESSION_TIMEOUT_KEY, minutes)
    }

    override fun getSessionTimeoutMinutes(): Int {
        val minutes = preferencesHelper.getStoredIntTag(SESSION_TIMEOUT_KEY)
        return when {
            minutes == SESSION_TIMEOUT_ALWAYS_ASK -> -2
            minutes == SESSION_TIMEOUT_UNTIL_APP_CLOSES -> -1
            minutes > 0 -> minutes
            else -> DEFAULT_SESSION_TIMEOUT_MINUTES
        }
    }

    override fun validatePin(pin: String): Boolean {
        if (!isParentalControlEnabled()) return true
        val storedPin = preferencesHelper.getParentalControlPin()
        val isValid = pin == storedPin
        if (isValid) {
            setLastUnlockTime(Clock.System.now().toEpochMilliseconds())
        }
        return isValid
    }

    override fun endSession() {
        setLastUnlockTime(0L)
    }

    override fun clearSessionOnAppStart() {
        val timeoutMinutes = preferencesHelper.getStoredIntTag(SESSION_TIMEOUT_KEY)
        if (timeoutMinutes == SESSION_TIMEOUT_UNTIL_APP_CLOSES) {
            endSession()
        }
    }

    override fun getRemainingSessionTime(): Long {
        if (!isParentalControlEnabled()) return 0
        val timeoutMinutes = preferencesHelper.getStoredIntTag(SESSION_TIMEOUT_KEY)
        if (timeoutMinutes == SESSION_TIMEOUT_ALWAYS_ASK) return -2
        val lastUnlock = getLastUnlockTime()
        if (lastUnlock == 0L) return 0
        if (timeoutMinutes == SESSION_TIMEOUT_UNTIL_APP_CLOSES) return -1
        val currentTime = Clock.System.now().toEpochMilliseconds()
        val timeout = getSessionTimeout()
        val elapsed = currentTime - lastUnlock
        if (elapsed >= timeout) return 0
        val remaining = timeout - elapsed
        return (remaining / 60000).coerceAtLeast(1)
    }

    override fun setPin(pin: String) {
        preferencesHelper.setParentalControlPin(pin)
        if (pin.isNotEmpty()) {
            preferencesHelper.setParentalControlEnabled(true)
        }
    }

    override fun clearPin() {
        preferencesHelper.clearParentalControlPin()
        preferencesHelper.setParentalControlEnabled(false)
        endSession()
    }

    override suspend fun isCategoryProtected(categoryId: Int, userId: Int): Boolean {
        if (!isParentalControlEnabled()) return false
        return parentalControlQueries.isCategoryProtected(categoryId.toLong(), userId.toLong())
            .executeAsOneOrNull()?.let { it != 0L } ?: false
    }

    override suspend fun getProtectedCategoryIds(userId: Int): List<String> {
        if (!isParentalControlEnabled()) return emptyList()
        if (isSessionAuthenticated()) return emptyList()
        return parentalControlQueries.selectProtectedCategoryIds(userId.toLong())
            .executeAsList()
            .map { it.toString() }
    }

    override suspend fun setCategoryProtection(categoryId: Int, categoryName: String, userId: Int, isProtected: Boolean) {
        val existing = parentalControlQueries.selectByCategory(categoryId.toLong(), userId.toLong())
            .executeAsOneOrNull()
        if (existing != null) {
            parentalControlQueries.updateProtectionStatus(if (isProtected) 1L else 0L, categoryId.toLong(), userId.toLong())
        } else {
            parentalControlQueries.insertOrReplace(
                categoryId = categoryId.toLong(),
                categoryName = categoryName,
                userId = userId.toLong(),
                isProtected = if (isProtected) 1L else 0L,
                createdAt = Clock.System.now().toEpochMilliseconds()
            )
        }
    }

    override fun getAllParentalControls(userId: Int): Flow<List<ParentalControl>> {
        return flow {
            val controls = parentalControlQueries.selectAllByUserId(userId.toLong())
                .executeAsList()
                .map { it.toParentalControl() }
            emit(controls)
        }.flowOn(Dispatchers.IO)
    }

    override fun getParentalControlByCategory(categoryId: Int, userId: Int): Flow<ParentalControl?> {
        return flow {
            emit(
                parentalControlQueries.selectByCategory(categoryId.toLong(), userId.toLong())
                    .executeAsOneOrNull()?.toParentalControl()
            )
        }.flowOn(Dispatchers.IO)
    }

    override fun getProtectedCategoriesCount(userId: Int): Flow<Int> {
        return flow {
            emit(parentalControlQueries.countProtected(userId.toLong()).executeAsOne().toInt())
        }.flowOn(Dispatchers.IO)
    }

    override suspend fun removeProtection(categoryId: Int, userId: Int) {
        parentalControlQueries.updateProtectionStatus(0L, categoryId.toLong(), userId.toLong())
    }

    override suspend fun deleteAllParentalControls(userId: Int) {
        parentalControlQueries.deleteAllByUserId(userId.toLong())
    }

    override suspend fun requiresPinForCategory(categoryId: Long, userId: Int): Boolean {
        if (isSessionAuthenticated()) return false
        return isParentalControlEnabled() && isCategoryProtected(categoryId.toInt(), userId)
    }
}
