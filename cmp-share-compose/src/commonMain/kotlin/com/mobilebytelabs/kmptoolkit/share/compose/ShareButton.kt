/*
 * Copyright 2026 MobileByteLabs
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     https://www.apache.org/licenses/LICENSE-2.0
 */
package com.mobilebytelabs.kmptoolkit.share.compose

import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Share
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.ui.Modifier
import com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi
import com.mobilebytelabs.kmptoolkit.share.Share
import com.mobilebytelabs.kmptoolkit.share.ShareOptions
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.ShareResult
import kotlinx.coroutines.launch

/**
 * Material 3 [IconButton] that invokes [Share.share] with the given [payload] on click.
 * Default icon is [Icons.Filled.Share].
 *
 * Example:
 * ```kotlin
 * ShareButton(payload = SharePayload.Url("https://example.com"))
 * ```
 *
 * For richer UX with payload preview + dismiss control, use [ShareSheet] instead.
 *
 * @param payload the [SharePayload] to dispatch on click
 * @param modifier optional [Modifier]
 * @param options optional [ShareOptions] forwarded to [Share.share]
 * @param onResult called with the [ShareResult] after the share dispatch completes
 * @param contentDescription accessibility label (default "Share")
 * @param content slot for the icon — defaults to [Icons.Filled.Share]
 */
@Composable
@ExperimentalShareApi
public fun ShareButton(
    payload: SharePayload,
    modifier: Modifier = Modifier,
    options: ShareOptions = ShareOptions(),
    onResult: (ShareResult) -> Unit = {},
    contentDescription: String = "Share",
    content: @Composable () -> Unit = { Icon(Icons.Filled.Share, contentDescription = contentDescription) },
) {
    val scope = rememberCoroutineScope()
    IconButton(
        onClick = { scope.launch { onResult(Share.share(payload, options)) } },
        modifier = modifier,
        content = { content() },
    )
}
