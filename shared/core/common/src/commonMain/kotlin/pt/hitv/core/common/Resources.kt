package pt.hitv.core.common

/**
 * A sealed class representing the state of a data resource (loading, success, or error).
 * This provides better type safety and allows for exhaustive 'when' checks.
 */
sealed class Resources<out T> {
    /**
     * Represents a successful state with data.
     * @param data The successfully loaded data.
     */
    data class Success<out T>(val data: T) : Resources<T>()

    /**
     * Represents an error state.
     * @param message A message describing the error.
     * @param data Optional data that might be available even in case of error (e.g., cached data).
     */
    data class Error<out T>(val message: String, val data: T? = null) : Resources<T>()

    /**
     * Represents a loading state.
     * @param data Optional data that might be available while loading (e.g., previous data).
     */
    data class Loading<out T>(val data: T? = null) : Resources<T>()
}

/**
 * Maps the data inside a [Resources] wrapper using the provided [transform] function.
 * Preserves the resource state (Loading/Success/Error) while transforming the data type.
 */
fun <T, R> Resources<T>.mapData(transform: (T) -> R): Resources<R> {
    return when (this) {
        is Resources.Success -> Resources.Success(transform(data))
        is Resources.Error -> Resources.Error(message, data?.let(transform))
        is Resources.Loading -> Resources.Loading(data?.let(transform))
    }
}
