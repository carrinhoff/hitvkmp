package pt.hitv.core.network.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.okhttp.OkHttp
import java.security.SecureRandom
import java.security.cert.X509Certificate
import java.util.concurrent.TimeUnit
import javax.net.ssl.SSLContext
import javax.net.ssl.X509TrustManager

/**
 * Android implementation using OkHttp engine.
 *
 * Configures SSL trust-all for IPTV services (many use expired/self-signed certificates),
 * connection timeouts, and HTTP/1.1 protocol.
 */
actual fun createPlatformHttpEngine(): HttpClientEngine = OkHttp.create {
    config {
        // Short connect timeout for fast protocol fallback (HTTPS -> HTTP)
        connectTimeout(5, TimeUnit.SECONDS)
        // Longer read timeout for EPG/playlist downloads which can be slow
        readTimeout(2, TimeUnit.MINUTES)
        writeTimeout(1, TimeUnit.MINUTES)

        followRedirects(true)
        retryOnConnectionFailure(true)

        // SSL configuration for IPTV services
        // Many IPTV providers use expired or self-signed certificates
        val trustAllCerts = object : X509TrustManager {
            override fun checkClientTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun checkServerTrusted(chain: Array<X509Certificate>, authType: String) {}
            override fun getAcceptedIssuers(): Array<X509Certificate> = arrayOf()
        }

        val sslContext = SSLContext.getInstance("TLS")
        sslContext.init(null, arrayOf(trustAllCerts), SecureRandom())

        sslSocketFactory(sslContext.socketFactory, trustAllCerts)
        hostnameVerifier { _, _ -> true }
    }
}
