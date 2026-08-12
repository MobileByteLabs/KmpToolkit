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

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.kmpPlatform
import kotlin.time.Clock

/**
 * Build a rich, AI-feedable [CrashReport] from any [Throwable].
 *
 * Uses only common Kotlin APIs (`::class.simpleName`, `cause`,
 * `stackTraceToString()`, `kotlin.time.Clock`) so it produces the same shape on
 * every KMP target — no expect/actual required for the payload itself.
 *
 * @param platform    the running target; defaults to [kmpPlatform]
 * @param fatal       whether this was an uncaught/unrecoverable crash
 * @param threadName  thread name if the caller can supply one (JVM/Android)
 * @param customKeys  developer context to attach (flavor, user tier, flags, …)
 * @param breadcrumbs recent log lines leading up to the crash
 */
fun Throwable.toCrashReport(
    platform: String = kmpPlatform,
    fatal: Boolean = false,
    threadName: String? = null,
    customKeys: Map<String, String> = emptyMap(),
    breadcrumbs: List<String> = emptyList(),
): CrashReport {
    val raw = stackTraceToString()
    return CrashReport(
        throwableClass = this::class.simpleName ?: "Throwable",
        message = message,
        fatal = fatal,
        platform = platform,
        timestamp = Clock.System.now().toString(),
        threadName = threadName,
        causeChain = buildCauseChain(this),
        stackFrames = parseStackFrames(raw),
        customKeys = customKeys,
        breadcrumbs = breadcrumbs,
        rawStackTrace = raw,
    )
}

/** Walk `cause` outermost-first, guarding against cyclic cause references. */
internal fun buildCauseChain(root: Throwable): List<CrashCause> {
    val chain = mutableListOf<CrashCause>()
    val seen = mutableSetOf<Throwable>()
    var current: Throwable? = root.cause
    while (current != null && seen.add(current)) {
        chain += CrashCause(
            throwableClass = current::class.simpleName ?: "Throwable",
            message = current.message,
        )
        current = current.cause
    }
    return chain
}

// Matches a JVM/Android trace line: `at pkg.Outer$Inner.method(File.kt:42)`
// Groups: 1=fully-qualified class, 2=method, 3=file (optional), 4=line (optional)
private val FRAME_REGEX =
    Regex("""at\s+(.+)\.([^.(]+)\((?:([^:)]+)(?::(\d+))?)?\)""")

/**
 * Parse a `stackTraceToString()` blob into structured [StackFrame]s. JVM/Android
 * frames resolve to `class`/`method`/`file`/`line`; on other targets we keep the
 * [StackFrame.raw] line and fill whatever the regex can recover.
 */
internal fun parseStackFrames(rawTrace: String): List<StackFrame> = rawTrace.lineSequence()
    .map { it.trim() }
    .filter { it.startsWith("at ") }
    .map { line ->
        val m = FRAME_REGEX.find(line)
        if (m != null) {
            val file = m.groupValues[3].takeIf { it.isNotBlank() }
            val line0 = m.groupValues[4].toIntOrNull()
            StackFrame(
                declaringClass = m.groupValues[1].takeIf { it.isNotBlank() },
                method = m.groupValues[2].takeIf { it.isNotBlank() },
                file = file,
                line = line0,
                raw = line,
            )
        } else {
            StackFrame(raw = line)
        }
    }
    .toList()
