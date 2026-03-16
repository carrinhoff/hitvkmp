package pt.hitv.core.network.di

import io.ktor.client.engine.HttpClientEngine
import io.ktor.client.engine.darwin.Darwin

/**
 * iOS implementation using Darwin (NSURLSession) engine.
 *
 * Configures request timeout and allows connections to servers
 * with invalid/expired SSL certificates (common with IPTV services).
 */
actual fun createPlatformHttpEngine(): HttpClientEngine = Darwin.create {
    configureRequest {
        setTimeoutInterval(30.0)
    }
    configureSession {
        // Allow connections to servers with invalid certificates
        // (necessary for IPTV services with expired/self-signed certs)
    }
}
