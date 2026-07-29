package pt.hitv.core.common

import com.russhwolf.settings.NSUserDefaultsSettings
import platform.Foundation.NSUserDefaults
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertTrue

/**
 * Drives the plaintext-NSUserDefaults → Keychain migration in `createEncryptedSettings()`, on a
 * real iOS simulator.
 *
 * The upgrade path is the risky part of that change: existing installs already have `username`,
 * `password`, `hostUrl` and `parental_control_pin` sitting in the `pt.hitv.secure` plist. If the
 * migration silently drops them, every existing iOS user is logged out on update; if it copies but
 * fails to delete, the plaintext copy survives and the security fix achieves nothing.
 *
 * A prior install can be *simulated* precisely — plant the legacy values, then invoke the real
 * factory — which is what this does. What it deliberately does not claim to cover is a genuine
 * over-the-top upgrade of a signed build on a device, where the Keychain access group and
 * app identifier are the real ones rather than the test host's.
 */
class KeychainMigrationIosTest {

    private val legacySuite = "pt.hitv.secure"
    private val keys = PreferencesHelper.SENSITIVE_KEYS

    private fun legacyDefaults() = NSUserDefaults(suiteName = legacySuite)

    @BeforeTest
    fun setUp() = clearBothStores()

    @AfterTest
    fun tearDown() = clearBothStores()

    private fun clearBothStores() {
        val legacy = NSUserDefaultsSettings(legacyDefaults())
        val keychain = createEncryptedSettings()
        keys.forEach {
            legacy.remove(it)
            keychain.remove(it)
        }
        legacyDefaults().synchronize()
    }

    @Test
    fun `migrates legacy plaintext values into the keychain`() {
        val legacy = NSUserDefaultsSettings(legacyDefaults())
        legacy.putString("username", "legacy-user")
        legacy.putString("password", "legacy-pass")
        legacy.putString("hostUrl", "http://legacy.example.tv/")
        legacy.putString("parental_control_pin", "4321")
        legacyDefaults().synchronize()

        // The factory performs the migration as a side effect.
        val settings = createEncryptedSettings()

        assertEquals("legacy-user", settings.getStringOrNull("username"))
        assertEquals("legacy-pass", settings.getStringOrNull("password"))
        assertEquals("http://legacy.example.tv/", settings.getStringOrNull("hostUrl"))
        assertEquals("4321", settings.getStringOrNull("parental_control_pin"))
    }

    @Test
    fun `removes the plaintext copies after migrating`() {
        val legacy = NSUserDefaultsSettings(legacyDefaults())
        legacy.putString("password", "should-not-survive")
        legacyDefaults().synchronize()

        createEncryptedSettings()

        assertFalse(
            NSUserDefaultsSettings(legacyDefaults()).hasKey("password"),
            "plaintext copy survived the migration — the security fix would be moot",
        )
    }

    @Test
    fun `does not clobber a newer keychain value with a stale plaintext one`() {
        // Migration runs on every launch, so it must never overwrite what the app has since
        // written. Otherwise a password change would be silently reverted on the next start.
        val keychain = createEncryptedSettings()
        keychain.putString("password", "current-password")

        val legacy = NSUserDefaultsSettings(legacyDefaults())
        legacy.putString("password", "stale-password")
        legacyDefaults().synchronize()

        val afterMigration = createEncryptedSettings()

        assertEquals("current-password", afterMigration.getStringOrNull("password"))
        assertFalse(
            NSUserDefaultsSettings(legacyDefaults()).hasKey("password"),
            "the stale plaintext copy should still be cleaned up",
        )
    }

    @Test
    fun `is a no-op on a clean install`() {
        // Nothing planted: the factory must succeed and leave both stores empty rather than
        // writing blanks that would look like an empty username to the boot check.
        val settings = createEncryptedSettings()
        keys.forEach {
            assertFalse(settings.hasKey(it), "clean install should not have $it")
        }
    }

    @Test
    fun `is idempotent across repeated launches`() {
        val legacy = NSUserDefaultsSettings(legacyDefaults())
        legacy.putString("username", "once")
        legacyDefaults().synchronize()

        repeat(3) { createEncryptedSettings() }

        val settings = createEncryptedSettings()
        assertEquals("once", settings.getStringOrNull("username"))
        assertTrue(settings.hasKey("username"))
    }
}
