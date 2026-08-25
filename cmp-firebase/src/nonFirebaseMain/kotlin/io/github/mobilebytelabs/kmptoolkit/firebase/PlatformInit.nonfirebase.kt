/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package io.github.mobilebytelabs.kmptoolkit.firebase

/**
 * Measurement-Protocol / fallback tier (jvm / linux / mingw / wasmjs): no native
 * Firebase SDK ships here, so init is a no-op. Analytics on these targets flows
 * through the Measurement-Protocol helper, auto-wired from the same
 * [FirebaseConfig] by `provideAnalyticsHelper()`.
 */
internal actual fun platformInitializeFirebase(options: FirebaseOptions?) {
    // No native Firebase on this tier; MP transport is wired by provideAnalyticsHelper().
}
