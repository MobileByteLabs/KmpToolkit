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

import android.content.Context

/**
 * Application-context holder set by [ShareInitProvider]'s `onCreate`.
 *
 * Avoids requiring consumer apps to manually init; the ContentProvider auto-runs
 * before Application.onCreate via the standard Android boot order.
 *
 * Pattern mirrors `cmp-open-url/OpenUrlInitProvider.kt`.
 */
internal object ShareContext {
    @Volatile
    internal lateinit var context: Context

    internal fun isInitialized(): Boolean = this::context.isInitialized
}
