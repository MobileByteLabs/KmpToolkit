package io.github.mobilebytelabs.kmptoolkit.networkmonitor.di

import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitor
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.NetworkMonitorConfig
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.createNetworkMonitor

/**
 * Koin-compatible factory functions for [NetworkMonitor] dependency injection.
 *
 * Since `cmp-network-monitor` has zero dependencies beyond kotlinx-coroutines,
 * we provide factory functions rather than a Koin module to avoid a Koin dependency.
 *
 * ## Koin usage:
 *
 * ```kotlin
 * // In your app's Koin modules:
 * import org.koin.dsl.module
 * import io.github.mobilebytelabs.kmptoolkit.networkmonitor.di.provideNetworkMonitor
 *
 * val appModule = module {
 *     single<NetworkMonitor> { provideNetworkMonitor(getOrNull()) }
 * }
 * ```
 *
 * ## Hilt usage (Android):
 *
 * ```kotlin
 * @Module
 * @InstallIn(SingletonComponent::class)
 * object NetworkModule {
 *     @Provides @Singleton
 *     fun provideMonitor(): NetworkMonitor = provideNetworkMonitor()
 * }
 * ```
 *
 * ## Manual DI:
 *
 * ```kotlin
 * val monitor = provideNetworkMonitor()
 * ```
 */
fun provideNetworkMonitor(config: NetworkMonitorConfig? = null): NetworkMonitor =
    createNetworkMonitor(config ?: NetworkMonitorConfig())
