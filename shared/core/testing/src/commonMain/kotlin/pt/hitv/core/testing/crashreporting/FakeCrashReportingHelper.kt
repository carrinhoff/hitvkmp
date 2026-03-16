package pt.hitv.core.testing.crashreporting

import pt.hitv.core.common.crashreporting.CrashReportingHelper

/**
 * Fake implementation of [CrashReportingHelper] for testing.
 *
 * This implementation records all calls to allow verification in tests.
 *
 * Usage:
 * ```
 * val fakeCrashReporter = FakeCrashReportingHelper()
 *
 * // Inject into your ViewModel/class
 * viewModel = MyViewModel(crashReporter = fakeCrashReporter)
 *
 * // Trigger code that should report exception
 * viewModel.doSomethingThatMightFail()
 *
 * // Verify exception was recorded
 * assertThat(fakeCrashReporter.recordedExceptions).hasSize(1)
 * assertThat(fakeCrashReporter.recordedExceptions.first().first)
 *     .isInstanceOf(IOException::class.java)
 * ```
 */
class FakeCrashReportingHelper : CrashReportingHelper {

    val recordedExceptions = mutableListOf<Pair<Throwable, Map<String, String>>>()
    val customKeys = mutableMapOf<String, Any>()
    val logMessages = mutableListOf<String>()

    private var _userId: String? = null
    private var _crashCollectionEnabled: Boolean = true

    /** The current user ID that was set */
    val currentUserId: String? get() = _userId

    /** Whether crash collection is enabled */
    val crashCollectionEnabled: Boolean get() = _crashCollectionEnabled

    override fun recordException(throwable: Throwable, customKeys: Map<String, String>) {
        recordedExceptions.add(throwable to customKeys)
    }

    override fun setCustomKey(key: String, value: String) {
        customKeys[key] = value
    }

    override fun setCustomKey(key: String, value: Int) {
        customKeys[key] = value
    }

    override fun setCustomKey(key: String, value: Boolean) {
        customKeys[key] = value
    }

    override fun setCustomKey(key: String, value: Long) {
        customKeys[key] = value
    }

    override fun setCustomKey(key: String, value: Double) {
        customKeys[key] = value
    }

    override fun setCustomKey(key: String, value: Float) {
        customKeys[key] = value
    }

    override fun setUserId(userId: String?) {
        _userId = userId
    }

    override fun log(message: String) {
        logMessages.add(message)
    }

    override fun setCrashCollectionEnabled(enabled: Boolean) {
        _crashCollectionEnabled = enabled
    }

    /**
     * Clear all recorded data. Call in @BeforeTest methods.
     */
    fun clear() {
        recordedExceptions.clear()
        customKeys.clear()
        logMessages.clear()
        _userId = null
        _crashCollectionEnabled = true
    }
}
