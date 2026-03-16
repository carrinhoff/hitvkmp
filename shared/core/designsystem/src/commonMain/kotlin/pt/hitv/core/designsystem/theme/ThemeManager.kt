package pt.hitv.core.designsystem.theme

import androidx.compose.ui.graphics.Color
import pt.hitv.core.common.PreferencesHelper
import pt.hitv.core.common.constants.BillingConstants

open class ThemeManager(
    private val preferencesHelper: PreferencesHelper
) {

    companion object {
        private const val THEME_PREFERENCE_KEY = "selected_theme"
        private const val PREMIUM_THEMES_PURCHASED_KEY = "premium_themes_purchased"
    }

    // Premium status provider - set by app module
    private var premiumThemeStatusProvider: PremiumThemeStatusProvider? = null

    fun setPremiumThemeStatusProvider(provider: PremiumThemeStatusProvider) {
        this.premiumThemeStatusProvider = provider
    }

    // Theme change listeners
    private val themeChangeListeners = mutableListOf<(AppTheme) -> Unit>()

    fun addThemeChangeListener(listener: (AppTheme) -> Unit) {
        themeChangeListeners.add(listener)
    }

    fun removeThemeChangeListener(listener: (AppTheme) -> Unit) {
        themeChangeListeners.remove(listener)
    }

    private fun notifyThemeChanged(theme: AppTheme) {
        themeChangeListeners.forEach { it(theme) }
    }

    enum class AppTheme(
        val themeName: String,
        val displayName: String,
        val isPremium: Boolean,
        val primaryColor: Color,
        val backgroundPrimary: Color,
        val backgroundSecondary: Color,
        val cardColor: Color,
        val textColor: Color,
        val textSecondary: Color,
        val success: Color,
        val danger: Color,
        val warning: Color
    ) {
        DEFAULT(
            themeName = "default",
            displayName = "Prime Video",
            isPremium = false,
            primaryColor = Color(0xFF00D4FF),
            backgroundPrimary = Color(0xFF0A1A2A),
            backgroundSecondary = Color(0xFF1A2A3A),
            cardColor = Color(0xFF243447),
            textColor = Color.White,
            textSecondary = Color(0xFFB0B0B0),
            success = Color(0xFF10B981),
            danger = Color(0xFFEF4444),
            warning = Color(0xFFFFA500)
        ),

        NETFLIX_RED(
            themeName = "netflix_red",
            displayName = "Netflix Style",
            isPremium = true,
            primaryColor = Color(0xFFE50914),
            backgroundPrimary = Color(0xFF141414),
            backgroundSecondary = Color(0xFF333333),
            cardColor = Color(0xFF2D2D2D),
            textColor = Color.White,
            textSecondary = Color(0xFFB3B3B3),
            success = Color(0xFF46D369),
            danger = Color(0xFFE50914),
            warning = Color(0xFFF5C518)
        ),

        AMAZON_PRIME(
            themeName = "amazon_prime",
            displayName = "Classic Orange",
            isPremium = true,
            primaryColor = Color(0xFFF34213),
            backgroundPrimary = Color(0xFF1A1D21),
            backgroundSecondary = Color(0xFF2C313A),
            cardColor = Color(0xFF2C313A),
            textColor = Color.White,
            textSecondary = Color(0xFFAAAAAA),
            success = Color(0xFF10B981),
            danger = Color(0xFFEF4444),
            warning = Color(0xFFF59E0B)
        ),

        GAMING_GREEN(
            themeName = "gaming_green",
            displayName = "Gaming Matrix",
            isPremium = true,
            primaryColor = Color(0xFF00FF88),
            backgroundPrimary = Color(0xFF0A0F0A),
            backgroundSecondary = Color(0xFF1A2F1A),
            cardColor = Color(0xFF0F1F0F),
            textColor = Color.White,
            textSecondary = Color(0xFF88CC88),
            success = Color(0xFF00FF88),
            danger = Color(0xFFFF4444),
            warning = Color(0xFFFFCC00)
        ),

        CINEMA_GOLD(
            themeName = "cinema_gold",
            displayName = "Cinema Luxury",
            isPremium = true,
            primaryColor = Color(0xFFFFC107),
            backgroundPrimary = Color(0xFF1A1611),
            backgroundSecondary = Color(0xFF2A2420),
            cardColor = Color(0xFF2F2A1F),
            textColor = Color.White,
            textSecondary = Color(0xFFCCBB99),
            success = Color(0xFF10B981),
            danger = Color(0xFFEF4444),
            warning = Color(0xFFFFC107)
        ),

        MIDNIGHT_OLED(
            themeName = "midnight_oled",
            displayName = "Midnight OLED",
            isPremium = true,
            primaryColor = Color(0xFF00CCFF),
            backgroundPrimary = Color(0xFF000000),
            backgroundSecondary = Color(0xFF0D0D0D),
            cardColor = Color(0xFF111111),
            textColor = Color.White,
            textSecondary = Color(0xFF888888),
            success = Color(0xFF00E676),
            danger = Color(0xFFFF1744),
            warning = Color(0xFFFFAB00)
        )
    }

    fun getCurrentTheme(): AppTheme {
        val savedTheme = preferencesHelper.getStoredTag(THEME_PREFERENCE_KEY)
        return AppTheme.values().find { it.themeName == savedTheme } ?: AppTheme.DEFAULT
    }

    fun setTheme(theme: AppTheme): Boolean {
        if (theme.isPremium && !hasPremiumThemes()) {
            return false
        }

        preferencesHelper.setStoredTag(THEME_PREFERENCE_KEY, theme.themeName)
        notifyThemeChanged(theme)
        return true
    }

    fun hasPremiumThemes(): Boolean {
        return if (BillingConstants.USE_FAKE_BILLING) {
            preferencesHelper.getStoredBoolean(PREMIUM_THEMES_PURCHASED_KEY)
        } else {
            premiumThemeStatusProvider?.hasPremiumThemes()
                ?: preferencesHelper.getStoredBoolean(PREMIUM_THEMES_PURCHASED_KEY)
        }
    }

    fun unlockPremiumThemes() {
        preferencesHelper.setStoredBoolean(PREMIUM_THEMES_PURCHASED_KEY, true)
    }

    fun getAvailableThemes(): List<AppTheme> {
        return if (hasPremiumThemes()) {
            AppTheme.values().toList()
        } else {
            listOf(AppTheme.DEFAULT)
        }
    }

    fun getPremiumThemes(): List<AppTheme> {
        return AppTheme.values().filter { it.isPremium }
    }

    fun unlockPremiumThemesForTesting() {
        // Commented out - users must purchase premium themes
        // preferencesHelper.setStoredBoolean(PREMIUM_THEMES_PURCHASED_KEY, true)
    }

    fun resetPremiumThemesForProduction() {
        preferencesHelper.setStoredBoolean(PREMIUM_THEMES_PURCHASED_KEY, false)
    }

    fun resetAllPremiumDataForTesting() {
        preferencesHelper.setStoredBoolean(PREMIUM_THEMES_PURCHASED_KEY, false)
        preferencesHelper.setStoredTag(THEME_PREFERENCE_KEY, AppTheme.DEFAULT.themeName)
    }
}
