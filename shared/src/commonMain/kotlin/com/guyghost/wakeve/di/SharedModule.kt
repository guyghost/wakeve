package com.guyghost.wakeve.di

/**
 * Shared DI configuration for cross-platform dependencies.
 *
 * This module provides factory functions to create:
 * - Repository implementations
 * - Use cases for business logic
 * - State machines for UI state management
 *
 * ## Note on Koin
 *
 * Koin setup is platform-specific and handled by:
 * - Android: in the Android application module
 * - iOS: in IosFactory.kt using KoinJavaComponent
 *
 * This module documents the dependencies but doesn't use Koin syntax
 * (to avoid circular build dependencies).
 *
 * ## Android Setup
 *
 * ```kotlin
 * // In composeApp/build.gradle.kts or android module
 * startKoin {
 *     modules(
 *         sharedModule,  // Your Koin module
 *         // ...
 *     )
 * }
 * ```
 *
 * ## iOS Setup
 *
 * ```swift
 * // IosFactory.kt handles Koin setup via KoinJavaComponent
 * ```
 */

/**
 * Initialize shared DI - placeholder for future setup.
 */
fun initSharedKoin() {
    // Koin initialization is done in platform-specific code.
    // This is a placeholder for any future cross-platform setup.
}
