package pt.hitv.core.testing

import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.UnconfinedTestDispatcher
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.setMain

/**
 * KMP-compatible test dispatcher setup that replaces the Android JUnit Rule pattern.
 *
 * Usage in commonTest:
 * ```
 * class MyViewModelTest {
 *     private val testDispatcherRule = MainDispatcherSetup()
 *
 *     @BeforeTest
 *     fun setup() {
 *         testDispatcherRule.setup()
 *     }
 *
 *     @AfterTest
 *     fun tearDown() {
 *         testDispatcherRule.tearDown()
 *     }
 *
 *     @Test
 *     fun myTest() = runTest {
 *         // Test code using Main dispatcher
 *     }
 * }
 * ```
 *
 * @param testDispatcher The test dispatcher to use. Defaults to [UnconfinedTestDispatcher]
 *                       which executes coroutines eagerly. Use `StandardTestDispatcher()`
 *                       if you need more control over coroutine execution timing.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherSetup(
    val testDispatcher: TestDispatcher = UnconfinedTestDispatcher()
) {
    fun setup() {
        Dispatchers.setMain(testDispatcher)
    }

    fun tearDown() {
        Dispatchers.resetMain()
    }
}
