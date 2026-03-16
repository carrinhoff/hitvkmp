package pt.hitv.core.network.datasource

import io.ktor.client.call.body
import io.ktor.client.statement.HttpResponse
import io.ktor.http.isSuccess
import pt.hitv.core.common.Resources

/**
 * Base class for remote data sources providing common error handling for Ktor responses.
 * Replaces the Retrofit-based BaseGetResponse.
 */
abstract class BaseRemoteDataSource {

    /**
     * Executes a network call and wraps the result in [Resources].
     * Handles HTTP errors and exceptions uniformly.
     */
    protected suspend fun <T> getResult(
        endpoint: String = "unknown",
        call: suspend () -> T
    ): Resources<T> {
        return try {
            val result = call()
            Resources.Success(result)
        } catch (e: io.ktor.client.plugins.ClientRequestException) {
            // 4xx errors
            val errorMsg = when (e.response.status.value) {
                401 -> "Unauthorized: Check credentials."
                403 -> "Forbidden: Access denied."
                404 -> "Not Found: Endpoint not found."
                else -> "HTTP Error: ${e.response.status.value} ${e.response.status.description}"
            }
            Resources.Error(errorMsg)
        } catch (e: io.ktor.client.plugins.ServerResponseException) {
            // 5xx errors
            Resources.Error("Internal Server Error: ${e.response.status.value}")
        } catch (e: io.ktor.client.plugins.ResponseException) {
            // Other HTTP errors
            Resources.Error("Network call failed: ${e.response.status.value} ${e.response.status.description}")
        } catch (e: io.ktor.utils.io.errors.IOException) {
            // Network I/O errors (connection refused, timeout, DNS, etc.)
            val errorMsg = when {
                e.message?.contains("resolve", ignoreCase = true) == true ||
                    e.message?.contains("UnknownHost", ignoreCase = true) == true ->
                    "Network Error: Cannot resolve host. Check connection or URL."

                e.message?.contains("timeout", ignoreCase = true) == true ||
                    e.message?.contains("timed out", ignoreCase = true) == true ->
                    "Network Error: Connection timed out."

                e.message?.contains("SSL", ignoreCase = true) == true ||
                    e.message?.contains("handshake", ignoreCase = true) == true ->
                    "Network Error: SSL handshake failed. Check certificate or network configuration."

                e.message?.contains("refused", ignoreCase = true) == true ->
                    "Network Error: Connection refused."

                else -> "Network Error: ${e.message ?: e.toString()}"
            }
            Resources.Error(errorMsg)
        } catch (e: Exception) {
            val errorMsg = when {
                e.message?.contains("SSL", ignoreCase = true) == true ||
                    e.message?.contains("handshake", ignoreCase = true) == true ->
                    "Network Error: SSL handshake failed. Check certificate or network configuration."

                e.message?.contains("timeout", ignoreCase = true) == true ->
                    "Network Error: Connection timed out."

                e.message?.contains("resolve", ignoreCase = true) == true ->
                    "Network Error: Cannot resolve host. Check connection or URL."

                else -> "Network Error: ${e.message ?: e.toString()}"
            }
            Resources.Error(errorMsg)
        }
    }
}
