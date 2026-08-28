package com.mobilebytelabs.remoteconfig

import com.mobilebytelabs.remoteconfig.di.RemoteConfigSettings
import com.mobilebytelabs.remoteconfig.dispatch.ActionDispatcher
import com.mobilebytelabs.remoteconfig.dispatch.ActionHandler
import com.mobilebytelabs.remoteconfig.local.DeviceIdProvider
import com.mobilebytelabs.remoteconfig.local.RemoteConfigLocalStore
import com.mobilebytelabs.remoteconfig.model.ActionType
import com.mobilebytelabs.remoteconfig.network.RemoteConfigService
import com.mobilebytelabs.remoteconfig.ui.RemoteConfigViewModel
import org.koin.core.module.Module
import org.koin.core.module.dsl.singleOf
import org.koin.core.module.dsl.viewModelOf

/**
 * Install cmp-remote-config inside any existing Koin [Module].
 *
 * ```
 * val networkModule = module {
 *     remoteConfig {
 *         supabaseUrl = "..."
 *         supabaseKey = "..."
 *
 *         action(ActionType.PREMIUM) { _, _ -> AppNavigator.navigateTo("paywall") }
 *         action("open_downloads")    { v, _ -> AppNavigator.navigateTo("downloads?session=${v.orEmpty()}") }
 *     }
 *
 *     // … rest of your bindings …
 * }
 * ```
 *
 * Then drop [com.mobilebytelabs.remoteconfig.ui.RemoteConfigHost] anywhere in the Compose tree;
 * action CTAs route to handlers registered here.
 */
fun Module.remoteConfig(block: RemoteConfigBuilder.() -> Unit) {
    val builder = RemoteConfigBuilder().apply(block)
    val settings = builder.build()

    single { settings }
    single { RemoteConfigService(settings.supabaseUrl, settings.supabaseKey) }
    singleOf(::RemoteConfigLocalStore)
    singleOf(::DeviceIdProvider)
    single { RemoteConfigEvaluator(get(), settings.appVersionProvider) }
    viewModelOf(::RemoteConfigViewModel)

    ActionDispatcher.register(builder.handlers)
}

class RemoteConfigBuilder internal constructor() {
    var supabaseUrl: String = ""
    var supabaseKey: String = ""

    /**
     * Optional lazy supplier of the host app's version name (e.g. `{ appVersionName() }`). When set,
     * a server config with a `min_app_version` / `max_app_version` window is shown ONLY to builds
     * inside that window — the basis of a server-driven force-update gate. Lazy on purpose: it is
     * called at evaluate time, so a platform version source that initializes after Koin (an Android
     * app-context holder, an iOS bundle) is ready by then. Leave unset for no version gating.
     */
    var appVersion: (() -> String?)? = null

    internal val handlers: MutableMap<ActionType, ActionHandler> = mutableMapOf()

    /** Register a handler for a string action_type (e.g. `"open_downloads"`). */
    fun action(type: String, handler: ActionHandler) {
        handlers[ActionType(type)] = handler
    }

    /** Register a handler for a typed [ActionType] constant (preferred). */
    fun action(type: ActionType, handler: ActionHandler) {
        handlers[type] = handler
    }

    internal fun build(): RemoteConfigSettings {
        require(supabaseUrl.isNotBlank()) { "remoteConfig { supabaseUrl } is required" }
        require(supabaseKey.isNotBlank()) { "remoteConfig { supabaseKey } is required" }
        return RemoteConfigSettings(supabaseUrl, supabaseKey, appVersion)
    }
}
