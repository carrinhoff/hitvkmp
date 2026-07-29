package pt.hitv.core.common

import com.russhwolf.settings.KeychainSettings

/**
 * Whether a usable Keychain exists in this test process.
 *
 * ## Why this is needed
 *
 * A Kotlin/Native test binary is a bare executable, not a signed application bundle. It has no
 * bundle identifier and no keychain access group, so on a simulator the Security framework has no
 * keychain to give it: every `SecItemAdd` returns `errSecNotAvailable (-25291)`, "No keychain is
 * available."
 *
 * This is a property of the *test host*, not of the code under test. The real app is a signed
 * bundle and gets the default access group `$(AppIdentifierPrefix).pt.hitv.app`, which is why the
 * Keychain-backed preference storage works in the app while these tests cannot exercise it here.
 *
 * ## What this means for confidence
 *
 * **The Keychain path is not covered by CI.** These tests skip rather than fail, so a red build
 * still means something is genuinely broken — but a green build says nothing at all about the
 * Keychain. It is verified only by the device pass in `§7 Step 0` of `KMP_MIGRATION_AUDIT.md`:
 * install over an existing build, confirm you are still logged in and the parental PIN survived.
 *
 * Making these assertions run in CI would need an app-hosted XCTest target — a real bundle to run
 * inside. That is worth doing and is recorded as follow-up; it is not something that can be added
 * from a machine without Xcode.
 */
internal fun keychainAvailable(): Boolean = try {
    val probe = KeychainSettings(service = "pt.hitv.keychain.availability.probe")
    probe.putString("probe", "1")
    val readBack = probe.getStringOrNull("probe")
    probe.remove("probe")
    readBack == "1"
} catch (t: Throwable) {
    false
}

/**
 * Prints a single, loud line explaining why a Keychain test did nothing.
 *
 * Deliberately noisy: a skipped test that looks identical to a passing one is how a suite starts
 * lying about its coverage.
 */
internal fun skipBecauseNoKeychain(test: String): Boolean {
    println(
        "SKIPPED (no keychain in this test host): $test — " +
            "a Kotlin/Native test binary is not a signed bundle, so SecItemAdd returns " +
            "errSecNotAvailable (-25291). The Keychain path is verified by the device pass, " +
            "not by CI. See KeychainAvailability.kt."
    )
    return true
}
