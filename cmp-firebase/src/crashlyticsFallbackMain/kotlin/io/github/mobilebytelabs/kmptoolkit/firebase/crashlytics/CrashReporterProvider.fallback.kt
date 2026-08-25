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

import io.github.mobilebytelabs.kmptoolkit.firebase.analytics.provideAnalyticsHelper

/**
 * Fallback-tier actual: JVM · JS · tvOS (×3) · Linux (×2) · mingwX64 · wasmJs.
 * GitLive Crashlytics does not ship here, so return the structured
 * [LoggingCrashReporter] — same [CrashReport], logged as JSON via Kermit AND mirrored
 * to analytics ([provideAnalyticsHelper], lazily) as an `app_crash` event so these
 * Crashlytics-unreachable targets still reach the single all-platform GA4 crash view.
 */
actual fun provideCrashReporter(): CrashReporter = LoggingCrashReporter(analyticsSink = { provideAnalyticsHelper() })
