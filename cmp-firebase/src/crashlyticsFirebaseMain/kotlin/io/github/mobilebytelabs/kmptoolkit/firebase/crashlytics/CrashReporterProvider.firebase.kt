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
 * Native-tier actual: Android · iOS (×3) · macOS (×2).
 * Returns a [FirebaseCrashReporter] backed by GitLive's `Firebase.crashlytics`, with
 * crashes also mirrored to analytics ([provideAnalyticsHelper], lazily) as an
 * `app_crash` event so native crashes join the single all-platform GA4 crash view
 * alongside the richer native Crashlytics report.
 */
actual fun provideCrashReporter(): CrashReporter = FirebaseCrashReporter(analyticsSink = { provideAnalyticsHelper() })
