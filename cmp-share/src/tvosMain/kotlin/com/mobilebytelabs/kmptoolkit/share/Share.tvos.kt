/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share

/**
 * tvOS `Share` — `UnsupportedPlatform` fallback.
 *
 * tvOS has no `UIActivityViewController` (genuine platform limitation) and Kotlin/Native's
 * tvOS bindings (as of Kotlin 2.3.10) do NOT expose `UIPasteboard` — even though it
 * exists at the Objective-C level on tvOS 9+. A custom Swift adapter to expose
 * UIPasteboard to K/N is a v0.3 candidate; for v0.2 every payload returns
 * `Failed(UnsupportedPlatform)`.
 *
 * v0.2 sub-plan 10.C — constraint documented.
 */
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult =
        ShareResult.Failed(ShareError.UnsupportedPlatform)
}
