/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.analytics

/**
 * No-op implementation of [AnalyticsHelper]. Events are silently discarded.
 *
 * Use cases:
 * - Compose previews / IDE renders
 * - Unit / UI tests where analytics noise is unwanted
 * - Disabled-analytics builds (release flavor opt-out, GDPR opt-out)
 *
 * Singleton — safe to use as the default Koin binding for tests/previews.
 */
object NoOpAnalyticsHelper : AnalyticsHelper {
    override fun logEvent(event: AnalyticsEvent) = Unit
}
