/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.appintents

import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle

/**
 * Injectable entry point for app-intent registration — the type to depend on from a ViewModel or
 * app-startup component.
 *
 * ## Why an interface when [AppIntents] already exists
 * [AppIntents] is an `expect object`, so code calling it directly cannot be substituted: no fake in
 * tests, no decorator that filters intents by feature flag or entitlement. [AppIntentsManager] is
 * the same capability behind an injectable type, and it adds [capabilities] so a caller can tell
 * whether registration will actually reach an assistant. [AppIntents] stays public and unchanged.
 *
 * ## Using
 * ```kotlin
 * class AppStartup(private val intents: AppIntentsManager) {
 *     fun onCreate() {
 *         intents.register(
 *             appIntents {
 *                 intent("add_task") {
 *                     title = "Add task"
 *                     parameter("text", ParamType.Text)
 *                     perform { params -> repo.add(params["text"] as String); AppIntentResult.Done }
 *                 }
 *             },
 *         )
 *
 *         // Only promise "Hey Siri" where it can actually work.
 *         if (intents.capabilities.osIntegration) showVoiceOnboarding()
 *     }
 * }
 * ```
 */
public interface AppIntentsManager {

    /** How far registration reaches here. See [platformAppIntentsCapabilities]. */
    public val capabilities: AppIntentsCapabilities

    /** Register [config], replacing any previous registration. The one abstract operation. */
    public fun register(config: AppIntentsConfig)

    /** Build and register in one step. */
    public fun register(block: AppIntentsBuilder.() -> Unit): AppIntentsConfig {
        val config = appIntents(block)
        register(config)
        return config
    }

    /** The currently registered config, or `null` before the first [register]. */
    public fun current(): AppIntentsConfig? = AppIntentsRuntime.current()

    /**
     * Perform a registered intent directly.
     *
     * Returns `null` when no intent with [id] is registered — distinct from an intent that ran and
     * returned [AppIntentResult.Failed].
     */
    public suspend fun invoke(id: String, params: Map<String, Any> = emptyMap()): AppIntentResult? =
        AppIntentsRuntime.invoke(id, params)

    /** Whether registering here reaches the OS, rather than staying in-process. */
    public fun reachesOs(): Boolean = capabilities.osIntegration
}

/**
 * The one [AppIntentsManager] — for every target.
 *
 * Stateless: the registry and all per-target publishing live in [AppIntents], so a single instance
 * is safe to share (which is why the DI module binds it as a singleton).
 */
public class AppIntentsManagerImpl : AppIntentsManager {

    override val capabilities: AppIntentsCapabilities
        get() = platformAppIntentsCapabilities

    override fun register(config: AppIntentsConfig) {
        AppIntents.register(config)
        // Reports the COUNT and the reach tier, never the intent ids or phrases: those are app
        // vocabulary a consumer may treat as unreleased product surface.
        observeLifecycle(
            cmpMetadata(),
            "intents_registered",
            mapOf("count" to config.intents.size, "reach" to capabilities.assistantReach),
        )
    }
}
