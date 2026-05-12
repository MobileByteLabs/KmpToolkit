package com.mobilebytelabs.remoteconfig.di

/**
 * Internal settings holder. Populated by the `remoteConfig { … }` DSL
 * (see [com.mobilebytelabs.remoteconfig.remoteConfig]) and resolved as a Koin `single<RemoteConfigSettings>`.
 *
 * Not part of the public API — consumers configure via the DSL, never construct this directly.
 */
internal data class RemoteConfigSettings(val supabaseUrl: String, val supabaseKey: String)
