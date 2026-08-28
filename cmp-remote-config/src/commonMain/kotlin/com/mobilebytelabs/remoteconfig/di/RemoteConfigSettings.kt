package com.mobilebytelabs.remoteconfig.di

/**
 * Internal settings holder. Populated by the `remoteConfig { … }` DSL
 * (see [com.mobilebytelabs.remoteconfig.remoteConfig]) and resolved as a Koin `single<RemoteConfigSettings>`.
 *
 * Not part of the public API — consumers configure via the DSL, never construct this directly.
 */
internal data class RemoteConfigSettings(
    val supabaseUrl: String,
    val supabaseKey: String,
    /**
     * Optional lazy supplier of the host app's version name (e.g. "2026.8.4"). Invoked by
     * [com.mobilebytelabs.remoteconfig.RemoteConfigEvaluator] at evaluate time (NOT at DI-build
     * time) so a platform context that initializes after Koin — such as an Android app-context
     * holder — is already ready. Its value gates a config's `min_app_version` / `max_app_version`
     * window. Null ⇒ no version gating (default).
     */
    val appVersionProvider: (() -> String?)? = null,
)
