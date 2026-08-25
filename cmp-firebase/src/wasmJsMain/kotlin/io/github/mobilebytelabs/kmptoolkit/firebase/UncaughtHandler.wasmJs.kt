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

/** No-op: JS/wasmJs have no reliable process-global fatal hook for a library to claim. */
internal actual fun installPlatformUncaughtHandler(onUncaught: (Throwable) -> Unit): Boolean = false
