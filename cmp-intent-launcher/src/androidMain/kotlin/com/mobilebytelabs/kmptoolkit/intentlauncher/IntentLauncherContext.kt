/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.intentlauncher

import android.content.Context

/**
 * Application-context holder set by [IntentLauncherInitProvider]'s `onCreate`.
 *
 * Used by [SystemIntents] — the lifecycle-bound `IntentLauncher` does not need this
 * because Composable callers provide their own `ActivityResultLauncher`.
 *
 * Pattern mirrors `cmp-share/ShareContext.kt`.
 */
internal object IntentLauncherContext {
    @Volatile
    internal lateinit var context: Context

    internal fun isInitialized(): Boolean = this::context.isInitialized
}
