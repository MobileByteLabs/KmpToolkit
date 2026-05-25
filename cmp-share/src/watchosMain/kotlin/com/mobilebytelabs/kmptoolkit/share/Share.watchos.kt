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
 * watchOS `Share` — onUnsupported fallback.
 *
 * watchOS has no share-sheet surface. Handoff-to-iPhone via `WCSession` is possible
 * but requires paired-iPhone state — out of scope for v0.2.
 */
@ExperimentalShareApi
public actual object Share {
    public actual suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult =
        ShareResult.Failed(ShareError.UnsupportedPlatform)
}
