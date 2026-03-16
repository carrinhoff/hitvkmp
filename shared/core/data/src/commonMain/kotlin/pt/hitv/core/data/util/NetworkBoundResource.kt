package pt.hitv.core.data.util

import kotlinx.coroutines.flow.*
import pt.hitv.core.common.Resources

/**
 * A generic function that implements the offline-first data fetching pattern.
 *
 * This follows the NiA (Now in Android) pattern where:
 * 1. First emit cached data (if available) with Loading state
 * 2. Fetch fresh data from network
 * 3. Save network data to cache
 * 4. Emit final Success or Error state
 *
 * @param query A function that returns a Flow of cached data from the database.
 * @param fetch A suspend function that fetches fresh data from the network.
 * @param saveFetchResult A suspend function that saves fetched data to the database.
 * @param shouldFetch A function that determines whether to fetch from network.
 * @param onFetchFailed Optional callback when network fetch fails.
 */
inline fun <ResultType, RequestType> networkBoundResource(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> Resources<RequestType>,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType?) -> Boolean = { it == null },
    crossinline onFetchFailed: (String?) -> Unit = {}
): Flow<Resources<ResultType>> = flow {
    // Step 1: Get cached data first
    val cachedData = query().firstOrNull()

    // Step 2: Emit loading state with cached data (if available)
    emit(Resources.Loading(cachedData))

    // Step 3: Determine if we should fetch from network
    if (shouldFetch(cachedData)) {
        // Step 4: Fetch from network
        when (val response = fetch()) {
            is Resources.Success -> {
                // Step 5: Save to database
                response.data?.let { saveFetchResult(it) }

                // Step 6: Emit fresh data from database (single source of truth)
                emitAll(
                    query().map { Resources.Success(it) }
                )
            }

            is Resources.Error -> {
                onFetchFailed(response.message)
                // Emit error with cached data as fallback
                emit(Resources.Error(response.message ?: "Unknown error", cachedData))
            }

            is Resources.Loading -> {
                // Should not happen, but handle gracefully
                emit(Resources.Loading(cachedData))
            }
        }
    } else {
        // No fetch needed, just emit cached data as success
        emitAll(
            query().map { Resources.Success(it) }
        )
    }
}

/**
 * A version that supports nullable result types, commonly used for single-item queries.
 */
inline fun <ResultType, RequestType> networkBoundResourceWithMapper(
    crossinline query: () -> Flow<ResultType?>,
    crossinline fetch: suspend () -> Resources<RequestType>,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType?) -> Boolean = { it == null },
    crossinline onFetchFailed: (String?) -> Unit = {}
): Flow<Resources<ResultType?>> = flow {
    val cachedData = query().firstOrNull()
    emit(Resources.Loading(cachedData))

    if (shouldFetch(cachedData)) {
        when (val response = fetch()) {
            is Resources.Success -> {
                response.data?.let { saveFetchResult(it) }
                emitAll(query().map { Resources.Success(it) })
            }

            is Resources.Error -> {
                onFetchFailed(response.message)
                emit(Resources.Error(response.message ?: "Unknown error", cachedData))
            }

            is Resources.Loading -> {
                emit(Resources.Loading(cachedData))
            }
        }
    } else {
        emitAll(query().map { Resources.Success(it) })
    }
}

/**
 * Cache-first approach that emits cached data immediately, then updates when network data arrives.
 *
 * Unlike [networkBoundResource], this immediately emits Success with cached data (if available),
 * making the UI responsive while still fetching fresh data in the background.
 */
inline fun <ResultType, RequestType> cacheFirstThenNetwork(
    crossinline query: () -> Flow<ResultType>,
    crossinline fetch: suspend () -> Resources<RequestType>,
    crossinline saveFetchResult: suspend (RequestType) -> Unit,
    crossinline shouldFetch: (ResultType?) -> Boolean = { true },
    crossinline onFetchFailed: (String?) -> Unit = {}
): Flow<Resources<ResultType>> = flow {
    // Emit cached data immediately
    val cachedData = query().firstOrNull()

    if (cachedData != null) {
        emit(Resources.Success(cachedData))
    } else {
        emit(Resources.Loading(null))
    }

    // Fetch from network if needed
    if (shouldFetch(cachedData)) {
        when (val response = fetch()) {
            is Resources.Success -> {
                response.data?.let { saveFetchResult(it) }
                // Emit updated data from database
                emitAll(query().map { Resources.Success(it) })
            }

            is Resources.Error -> {
                onFetchFailed(response.message)
                // Keep showing cached data, but also emit error
                if (cachedData != null) {
                    emit(Resources.Error(response.message ?: "Network error", cachedData))
                } else {
                    emit(Resources.Error(response.message ?: "Network error", null))
                }
            }

            is Resources.Loading -> Unit // Ignore
        }
    }
}
