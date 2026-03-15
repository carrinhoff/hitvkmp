package pt.hitv.core.common

/**
 * Used as a wrapper for data that represents an event.
 */
open class Event<out T>(private val content: T) {

    var hasBeenHandled = false
        private set

    /**
     * Returns the content and prevents it from being used again.
     */
    fun getContentIfNotHandled(): T? {
        return if (hasBeenHandled) {
            null
        } else {
            hasBeenHandled = true
            content
        }
    }

    /**
     * Returns the content, even if it has already been handled.
     */
    fun peekContent(): T = content
}
