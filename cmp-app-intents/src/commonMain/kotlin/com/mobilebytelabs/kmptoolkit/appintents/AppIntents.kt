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

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * Opt-in marker for the experimental cmp-app-intents API.
 *
 * Per ADR-08 — per-module marker; not a shared `@ExperimentalInterAppCommsApi`.
 */
@RequiresOptIn(
    message = "cmp-app-intents is experimental until v1.0; API may evolve without major-version bumps.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalAppIntentsApi

// -----------------------------------------------------------------------------
// Parameter type model
// -----------------------------------------------------------------------------

@ExperimentalAppIntentsApi
public sealed class ParamType {
    public object Text : ParamType()
    public object Integer : ParamType()
    public object Number : ParamType()
    public object Bool : ParamType()
    public data class Entity(val entityName: String) : ParamType()
}

// -----------------------------------------------------------------------------
// Intent result
// -----------------------------------------------------------------------------

@ExperimentalAppIntentsApi
public sealed class AppIntentResult {
    public data class Dialog(val message: String) : AppIntentResult()
    public data class Snippet(val markdown: String) : AppIntentResult()
    public object Done : AppIntentResult()
    public data class Failed(val message: String) : AppIntentResult()
}

// -----------------------------------------------------------------------------
// Builders
// -----------------------------------------------------------------------------

@ExperimentalAppIntentsApi
public class AppIntentBuilder internal constructor(internal val id: String) {
    public var title: String = ""
    public var description: String = ""

    /**
     * Optional Android 2026 Built-in Intent identifier (e.g., `actions.intent.OPEN_APP_FEATURE`,
     * `actions.intent.GET_THING`). When null, `AssistantBii.resolveBii(def)` picks a default
     * based on intent shape (`GET_THING` for searchable, `OPEN_APP_FEATURE` otherwise).
     * Consumed by Phase 4 generateShortcutsXml Gradle task to emit `res/xml/cmp_app_intents_shortcuts.xml`
     * capability blocks per [SPIKE_FINDINGS_V0_3.md] S0.B PIVOT verdict.
     */
    public var bii: String? = null
    internal val parameters: MutableList<ParamDef> = mutableListOf()
    internal var perform: (suspend (Map<String, Any>) -> AppIntentResult)? = null
    internal var searchable: Boolean = false
    internal var searchableCategory: String? = null

    public fun parameter(name: String, type: ParamType, isRequired: Boolean = true) {
        parameters.add(ParamDef(name = name, type = type, isRequired = isRequired))
    }

    public fun perform(block: suspend (Map<String, Any>) -> AppIntentResult) {
        perform = block
    }

    public fun searchable(category: String? = null) {
        searchable = true
        searchableCategory = category
    }
}

@ExperimentalAppIntentsApi
public class AppIntentsBuilder internal constructor() {
    internal val intents: MutableList<AppIntentDef> = mutableListOf()

    public fun intent(id: String, block: AppIntentBuilder.() -> Unit) {
        val builder = AppIntentBuilder(id).apply(block)
        intents.add(
            AppIntentDef(
                id = id,
                title = builder.title,
                description = builder.description,
                bii = builder.bii,
                parameters = builder.parameters.toList(),
                searchable = builder.searchable,
                searchableCategory = builder.searchableCategory,
                perform = builder.perform ?: { _ -> AppIntentResult.Failed("Intent '$id' has no perform block") },
            ),
        )
    }
}

@ExperimentalAppIntentsApi
public fun appIntents(block: AppIntentsBuilder.() -> Unit): AppIntentsConfig =
    AppIntentsConfig(AppIntentsBuilder().apply(block).intents.toList())

// -----------------------------------------------------------------------------
// Config + def types
// -----------------------------------------------------------------------------

@ExperimentalAppIntentsApi
public class AppIntentsConfig internal constructor(public val intents: List<AppIntentDef>)

@ExperimentalAppIntentsApi
public class AppIntentDef internal constructor(
    public val id: String,
    public val title: String,
    public val description: String,
    /** 2026 BII identifier (e.g., actions.intent.OPEN_APP_FEATURE); null → AssistantBii picks default. */
    public val bii: String?,
    public val parameters: List<ParamDef>,
    public val searchable: Boolean,
    public val searchableCategory: String?,
    internal val perform: suspend (Map<String, Any>) -> AppIntentResult,
)

@ExperimentalAppIntentsApi
public data class ParamDef(val name: String, val type: ParamType, val isRequired: Boolean)

// -----------------------------------------------------------------------------
// Runtime registry (internal) — holds perform lambdas keyed by intent id;
// invoked from per-platform callback receivers
// -----------------------------------------------------------------------------

@ExperimentalAppIntentsApi
internal object AppIntentsRuntime {
    private var registered: AppIntentsConfig? = null
    private val handlers: MutableMap<String, suspend (Map<String, Any>) -> AppIntentResult> = mutableMapOf()

    fun register(config: AppIntentsConfig) {
        registered = config
        handlers.clear()
        for (def in config.intents) {
            handlers[def.id] = def.perform
        }
    }

    fun current(): AppIntentsConfig? = registered

    suspend fun invoke(id: String, params: Map<String, Any>): AppIntentResult? = handlers[id]?.invoke(params)
}

// -----------------------------------------------------------------------------
// Manifest serialization — JSON written by register() on iOS (read by Swift bridge);
// also used as the canonical golden-snapshot format for cross-platform testing.
// -----------------------------------------------------------------------------

@Serializable
internal data class ManifestEntry(
    val id: String,
    val title: String,
    val description: String,
    val parameters: List<ManifestParam>,
    val searchable: Boolean,
    val searchableCategory: String? = null,
)

@Serializable
internal data class ManifestParam(
    val name: String,
    val type: String, // "Text" / "Integer" / "Number" / "Bool" / "Entity:Name"
    val isRequired: Boolean,
)

@ExperimentalAppIntentsApi
internal fun AppIntentsConfig.serializeManifest(): String {
    val entries = intents.map { def ->
        ManifestEntry(
            id = def.id,
            title = def.title,
            description = def.description,
            parameters = def.parameters.map { p ->
                val typeStr = when (val t = p.type) {
                    ParamType.Text -> "Text"
                    ParamType.Integer -> "Integer"
                    ParamType.Number -> "Number"
                    ParamType.Bool -> "Bool"
                    is ParamType.Entity -> "Entity:${t.entityName}"
                }
                ManifestParam(name = p.name, type = typeStr, isRequired = p.isRequired)
            },
            searchable = def.searchable,
            searchableCategory = def.searchableCategory,
        )
    }
    return Json.encodeToString(entries)
}

// -----------------------------------------------------------------------------
// Public registration entry point
// -----------------------------------------------------------------------------

@ExperimentalAppIntentsApi
public expect object AppIntents {
    public fun register(config: AppIntentsConfig)
    public suspend fun invokeForTesting(id: String, params: Map<String, Any> = emptyMap()): AppIntentResult?
}
