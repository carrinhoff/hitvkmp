package pt.hitv.core.network.di

import io.ktor.client.engine.HttpClientEngine

/**
 * Expect function for creating platform-specific HTTP client engines.
 * - Android: OkHttp engine
 * - iOS: Darwin engine
 */
expect fun createPlatformHttpEngine(): HttpClientEngine
