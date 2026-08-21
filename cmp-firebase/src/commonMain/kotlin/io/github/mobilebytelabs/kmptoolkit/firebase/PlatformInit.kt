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
 * Programmatically initialize the platform's native Firebase from [options].
 *
 * - **GitLive tier** (android/ios/macos/tvos/js) → `Firebase.initialize(options)`.
 * - **Measurement-Protocol tier** (jvm/watchos/linux/mingw/wasmjs) → no-op
 *   (no native Firebase SDK; analytics flows through the MP helper instead).
 *
 * A null [options] (no keys supplied for this platform) is a no-op on every
 * target — the caller has already logged the graceful-degradation warning.
 */
internal expect fun platformInitializeFirebase(options: FirebaseOptions?)
