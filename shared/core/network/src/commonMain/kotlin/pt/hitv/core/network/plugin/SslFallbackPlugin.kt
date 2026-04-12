package pt.hitv.core.network.plugin

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.plugins.HttpSend
import io.ktor.client.plugins.plugin
import io.ktor.http.URLProtocol
import io.ktor.util.AttributeKey

/**
 * Ktor plugin for HTTPS to HTTP fallback on SSL errors.
 * Replaces the OkHttp HostSelectionInterceptor's SSL fallback behavior.
 *
 * When an HTTPS request fails with an SSL-related exception, this plugin
 * automatically retries the request over plain HTTP. The fallback state
 * is kept in-memory only and resets on app restart, so HTTPS is retried
 * when the server fixes its certificate.
 *
 * This is necessary for IPTV services where many providers use expired
 * or self-signed SSL certificates.
 */
class SslFallbackPlugin private constructor() {

    @kotlin.concurrent.Volatile
    var sslFallbackActive: Boolean = false
        private set

    class Config

    companion object Plugin : HttpClientPlugin<Config, SslFallbackPlugin> {
        override val key: AttributeKey<SslFallbackPlugin> =
            AttributeKey("SslFallbackPlugin")

        override fun prepare(block: Config.() -> Unit): SslFallbackPlugin {
            Config().apply(block)
            return SslFallbackPlugin()
        }

        override fun install(plugin: SslFallbackPlugin, scope: HttpClient) {
            scope.plugin(HttpSend).intercept { request ->
                // If SSL fallback is active and the request is HTTPS, switch to HTTP
                if (plugin.sslFallbackActive && request.url.protocol == URLProtocol.HTTPS) {
                    request.url.protocol = URLProtocol.HTTP
                }

                try {
                    execute(request)
                } catch (e: Exception) {
                    // Check if this is an SSL-related error
                    val isSslError = e.isSslException()

                    if (isSslError && request.url.protocol == URLProtocol.HTTPS) {
                        // Mark fallback as active for future requests
                        plugin.sslFallbackActive = true

                        // Retry with HTTP
                        request.url.protocol = URLProtocol.HTTP
                        execute(request)
                    } else {
                        throw e
                    }
                }
            }
        }
    }
}

/**
 * Checks if an exception is SSL-related by examining the exception hierarchy
 * and message. Works across platforms (JVM SSLException, native TLS errors, etc.)
 */
internal fun Throwable.isSslException(): Boolean {
    val className = this::class.simpleName ?: ""
    val message = this.message ?: ""

    return className.contains("SSL", ignoreCase = true) ||
        className.contains("TLS", ignoreCase = true) ||
        message.contains("SSL", ignoreCase = true) ||
        message.contains("TLS", ignoreCase = true) ||
        message.contains("certificate", ignoreCase = true) ||
        message.contains("handshake", ignoreCase = true) ||
        (cause?.isSslException() == true)
}
