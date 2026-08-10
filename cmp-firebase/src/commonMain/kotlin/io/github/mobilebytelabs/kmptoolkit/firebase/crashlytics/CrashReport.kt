/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase.crashlytics

import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json

/**
 * A structured, machine-readable snapshot of a crash — built on EVERY platform
 * (native Crashlytics tier and logging-fallback tier alike).
 *
 * The whole point of this model is **AI-feedability**: [toJson] emits a compact,
 * self-describing payload you can hand straight to Claude (or any LLM) with a
 * prompt like _"here's the crash, find the cause and propose a fix"_ — it carries
 * the exception class, the human message, the **full cause chain**, and the
 * **stack frames broken out to `file:line`** so the model can point at the exact
 * source location instead of guessing.
 *
 * ```kotlin
 * try { risky() } catch (t: Throwable) {
 *     FirebaseKit.crashReporter.recordException(t)
 *     val json = FirebaseKit.crashReporter.lastReport?.toJson(pretty = true)
 *     // paste `json` into Claude → "explain and fix this crash"
 * }
 * ```
 */
@Serializable
data class CrashReport(
    /** Simple class name of the thrown [Throwable], e.g. `"IllegalStateException"`. */
    val throwableClass: String,
    /** The throwable's message, if any. */
    val message: String? = null,
    /** `true` for an unrecoverable/uncaught crash, `false` for a handled/non-fatal record. */
    val fatal: Boolean = false,
    /** KMP target the crash occurred on (`"android"`, `"ios"`, `"jvm"`, …). */
    val platform: String,
    /** ISO-8601 UTC instant the report was built. */
    val timestamp: String,
    /** Thread name where available (JVM/Android); `null` on platforms without a thread model. */
    val threadName: String? = null,
    /** The `cause` chain, outermost-first — the real root cause is the last element. */
    val causeChain: List<CrashCause> = emptyList(),
    /** Parsed stack frames. `file`/`line` are populated where the platform trace exposes them. */
    val stackFrames: List<StackFrame> = emptyList(),
    /** Developer-supplied custom keys (build flavor, user tier, feature flags, …). */
    val customKeys: Map<String, String> = emptyMap(),
    /** Recent breadcrumb log lines leading up to the crash (bounded, oldest-first). */
    val breadcrumbs: List<String> = emptyList(),
    /** The original, unparsed `stackTraceToString()` — always present as a fallback for the parser. */
    val rawStackTrace: String? = null,
) {
    /** Serialize to JSON for logging, upload, or handing to an AI. */
    fun toJson(pretty: Boolean = false): String =
        (if (pretty) PrettyJson else CompactJson).encodeToString(this)

    companion object {
        internal val CompactJson = Json {
            encodeDefaults = true
            explicitNulls = false
        }
        internal val PrettyJson = Json {
            prettyPrint = true
            encodeDefaults = true
            explicitNulls = false
        }
    }
}

/** One link in a [CrashReport.causeChain]. */
@Serializable
data class CrashCause(
    val throwableClass: String,
    val message: String? = null,
)

/**
 * One parsed stack frame. On JVM/Android the trace format
 * (`at pkg.Class.method(File.kt:42)`) yields all fields; on other targets the
 * fields are best-effort and [raw] always holds the original line.
 */
@Serializable
data class StackFrame(
    val declaringClass: String? = null,
    val method: String? = null,
    val file: String? = null,
    val line: Int? = null,
    /** The original trace line, trimmed. Never null — the ground truth for the parser. */
    val raw: String,
)
