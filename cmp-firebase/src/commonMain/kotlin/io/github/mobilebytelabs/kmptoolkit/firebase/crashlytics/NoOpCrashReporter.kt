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

/**
 * No-op [CrashReporter] — discards everything, keeps no [lastReport].
 *
 * Use for Compose previews, tests where crash noise is unwanted, or
 * disabled-reporting builds (privacy opt-out). Singleton — safe as a default
 * binding.
 */
object NoOpCrashReporter : CrashReporter {
    override fun recordException(throwable: Throwable, fatal: Boolean, extraKeys: Map<String, String>) = Unit
    override fun log(message: String) = Unit
    override fun setCustomKey(key: String, value: String) = Unit
    override fun setUserId(userId: String) = Unit
    override fun install() = Unit
    override val lastReport: CrashReport? = null
}
