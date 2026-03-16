package pt.hitv.core.billing.di

import org.koin.core.module.Module

/**
 * Platform-specific Koin module for billing.
 * Each platform provides the actual BillingManager binding.
 */
expect val billingPlatformModule: Module
