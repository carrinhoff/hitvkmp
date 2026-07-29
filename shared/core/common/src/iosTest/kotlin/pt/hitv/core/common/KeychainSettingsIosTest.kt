package pt.hitv.core.common

import com.russhwolf.settings.Settings
import kotlin.test.AfterTest
import kotlin.test.BeforeTest
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertFalse
import kotlin.test.assertNull
import kotlin.test.assertTrue

/**
 * Exercises the real iOS Keychain through [createEncryptedSettings] — on an actual iOS simulator.
 *
 * This is a platform actual with genuine cinterop risk that cannot be covered from `commonTest`:
 * it reaches `KeychainSettings`, which calls `SecItemAdd` / `SecItemCopyMatching` under the hood.
 * Type-checking proves the signatures line up; only running it proves the Keychain accepts the
 * calls and round-trips values.
 *
 * These credentials (`username`, `password`, `hostUrl`, `parental_control_pin`) used to live in a
 * plaintext NSUserDefaults plist on iOS, readable from any unencrypted device backup.
 *
 * Runs via `./gradlew iosSimulatorArm64Test` on a macOS runner — see the `verify-ios` job in
 * `.github/workflows/ios-testflight.yml`.
 */
class KeychainSettingsIosTest {

    private lateinit var settings: Settings

    private val probeKey = "hitv_keychain_probe"

    @BeforeTest
    fun setUp() {
        if (!keychainAvailable()) return
        settings = createEncryptedSettings()
        settings.remove(probeKey)
    }

    @AfterTest
    fun tearDown() {
        if (!keychainAvailable()) return
        settings.remove(probeKey)
    }

    @Test
    fun `writes and reads back a value`() {
        if (!keychainAvailable()) { skipBecauseNoKeychain("writes and reads back a value"); return }
        assertFalse(settings.hasKey(probeKey), "probe key should start absent")

        settings.putString(probeKey, "s3cr3t-value")

        assertTrue(settings.hasKey(probeKey), "Keychain did not accept the write")
        assertEquals("s3cr3t-value", settings.getStringOrNull(probeKey))
    }

    @Test
    fun `overwrites an existing value rather than duplicating it`() {
        if (!keychainAvailable()) { skipBecauseNoKeychain("overwrites an existing value rather than duplicating it"); return }
        // SecItemAdd fails with errSecDuplicateItem if an update path isn't taken; this catches
        // that class of bug, which would strand the user on a stale password after re-login.
        settings.putString(probeKey, "first")
        settings.putString(probeKey, "second")

        assertEquals("second", settings.getStringOrNull(probeKey))
    }

    @Test
    fun `remove actually deletes the item`() {
        if (!keychainAvailable()) { skipBecauseNoKeychain("remove actually deletes the item"); return }
        settings.putString(probeKey, "to-be-removed")
        settings.remove(probeKey)

        assertFalse(settings.hasKey(probeKey))
        assertNull(settings.getStringOrNull(probeKey))
    }

    @Test
    fun `a fresh Settings instance sees previously written values`() {
        if (!keychainAvailable()) { skipBecauseNoKeychain("a fresh Settings instance sees previously written values"); return }
        // The Keychain is process-wide, not instance-scoped: a value written before an app
        // restart must still be there afterwards, which is what keeps the user logged in.
        settings.putString(probeKey, "persisted")

        val secondInstance = createEncryptedSettings()
        assertEquals("persisted", secondInstance.getStringOrNull(probeKey))
    }

    @Test
    fun `round-trips the value shapes the app actually stores`() {
        if (!keychainAvailable()) { skipBecauseNoKeychain("round-trips the value shapes the app actually stores"); return }
        // Real credentials contain characters that have tripped naive Keychain wrappers before.
        val cases = mapOf(
            "hitv_probe_user" to "user.name+tag",
            "hitv_probe_pass" to "p@ssw0rd/with:punctuation&more",
            "hitv_probe_host" to "http://example.tv:8080/",
            "hitv_probe_pin" to "0000",
        )
        try {
            cases.forEach { (k, v) -> settings.putString(k, v) }
            cases.forEach { (k, v) ->
                assertEquals(v, settings.getStringOrNull(k), "round-trip failed for $k")
            }
        } finally {
            cases.keys.forEach { settings.remove(it) }
        }
    }
}
