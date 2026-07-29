package pt.hitv.core.data.manager

import pt.hitv.core.domain.manager.ParentalControlManager
import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNotEquals
import kotlin.test.assertTrue

/**
 * Guards the session-timeout sentinels.
 *
 * The parental-control timeout picker stored `0` for "Never (always ask)". The session logic only
 * recognises `-2`, and `0` is not `> 0` either, so `getSessionTimeout()` fell through to its
 * `else` branch and returned the **30-minute default**. A parent selecting the strictest option
 * silently got the standard one — and it looked correct in the UI, because the picker showed the
 * selection it had stored.
 *
 * The sentinels now live on the [ParentalControlManager] interface so the picker and the session
 * logic read the same constants. These tests pin the values and, more importantly, the properties
 * that made the original bug possible.
 */
class SessionTimeoutSentinelTest {

    @Test
    fun `sentinels have their expected values`() {
        assertEquals(-2, ParentalControlManager.SESSION_TIMEOUT_ALWAYS_ASK)
        assertEquals(-1, ParentalControlManager.SESSION_TIMEOUT_UNTIL_APP_CLOSES)
        assertEquals(30, ParentalControlManager.DEFAULT_SESSION_TIMEOUT_MINUTES)
    }

    @Test
    fun `the impl aliases the domain constants rather than redeclaring them`() {
        assertEquals(
            ParentalControlManager.SESSION_TIMEOUT_ALWAYS_ASK,
            ParentalControlManagerImpl.SESSION_TIMEOUT_ALWAYS_ASK,
        )
        assertEquals(
            ParentalControlManager.SESSION_TIMEOUT_UNTIL_APP_CLOSES,
            ParentalControlManagerImpl.SESSION_TIMEOUT_UNTIL_APP_CLOSES,
        )
        assertEquals(
            ParentalControlManager.DEFAULT_SESSION_TIMEOUT_MINUTES,
            ParentalControlManagerImpl.DEFAULT_SESSION_TIMEOUT_MINUTES,
        )
    }

    @Test
    fun `zero is not a valid sentinel - the exact shape of the original bug`() {
        // If someone reintroduces 0 as "always ask", this fails.
        assertNotEquals(0, ParentalControlManager.SESSION_TIMEOUT_ALWAYS_ASK)
        assertNotEquals(0, ParentalControlManager.SESSION_TIMEOUT_UNTIL_APP_CLOSES)
    }

    @Test
    fun `sentinels are negative so they can never collide with a real minute count`() {
        // getSessionTimeout() branches on `minutes > 0` for real durations; any sentinel that was
        // positive would be interpreted as a duration instead.
        assertTrue(ParentalControlManager.SESSION_TIMEOUT_ALWAYS_ASK < 0)
        assertTrue(ParentalControlManager.SESSION_TIMEOUT_UNTIL_APP_CLOSES < 0)
    }

    @Test
    fun `the two sentinels are distinct`() {
        assertNotEquals(
            ParentalControlManager.SESSION_TIMEOUT_ALWAYS_ASK,
            ParentalControlManager.SESSION_TIMEOUT_UNTIL_APP_CLOSES,
        )
    }
}
