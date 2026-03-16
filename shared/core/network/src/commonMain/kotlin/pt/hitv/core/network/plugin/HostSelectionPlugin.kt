package pt.hitv.core.network.plugin

import io.ktor.client.HttpClient
import io.ktor.client.plugins.HttpClientPlugin
import io.ktor.client.request.HttpRequestPipeline
import io.ktor.http.URLBuilder
import io.ktor.http.Url
import io.ktor.util.AttributeKey
import pt.hitv.core.common.PreferencesHelper

/**
 * Ktor plugin that dynamically rewrites request URLs based on the host URL
 * stored in PreferencesHelper. Replaces the OkHttp HostSelectionInterceptor.
 *
 * Requests using absolute URLs (e.g., TMDB API calls) are left unchanged.
 * Only requests to the base host are rewritten with the preferred host/scheme/port.
 */
class HostSelectionPlugin private constructor(
    val preferencesHelper: PreferencesHelper
) {

    class Config {
        lateinit var preferencesHelper: PreferencesHelper
    }

    companion object Plugin : HttpClientPlugin<Config, HostSelectionPlugin> {
        override val key: AttributeKey<HostSelectionPlugin> =
            AttributeKey("HostSelectionPlugin")

        override fun prepare(block: Config.() -> Unit): HostSelectionPlugin {
            val config = Config().apply(block)
            return HostSelectionPlugin(config.preferencesHelper)
        }

        override fun install(plugin: HostSelectionPlugin, scope: HttpClient) {
            scope.requestPipeline.intercept(HttpRequestPipeline.Before) {
                val hostUrl = plugin.preferencesHelper.getHostUrl()
                if (hostUrl.isBlank()) return@intercept

                val preferredUrl = try {
                    Url(hostUrl)
                } catch (_: Exception) {
                    return@intercept
                }

                // Only rewrite if the request URL looks like a relative path
                // (uses localhost or the default placeholder).
                // Absolute URLs to external services (e.g., TMDB) are left as-is.
                val currentUrl = context.url.build()
                val isRelativeRequest = currentUrl.host == "localhost" ||
                    currentUrl.host.isEmpty()

                if (isRelativeRequest) {
                    context.url.apply {
                        protocol = preferredUrl.protocol
                        host = preferredUrl.host
                        port = preferredUrl.port
                    }
                }
            }
        }
    }
}
