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

import io.github.mobilebytelabs.kmptoolkit.observe.observeLifecycle

/**
 * Injectable share entry point — the interface to depend on from a ViewModel, repository or
 * composable.
 *
 * ## Why an interface when [Share] already exists
 * [Share] is an `expect object`. An object cannot be substituted, so anything calling it directly
 * is untestable without launching a real share sheet, and cannot be swapped for an app-specific
 * decorator (analytics, consent checks, a queue). [ShareManager] is the same capability behind a
 * type you can inject, fake and wrap. [Share] remains public and unchanged for callers that want
 * the imperative form.
 *
 * ## Implementing
 * Only [capabilities] and [share] are abstract; every other member is derived from them. A test
 * double is therefore two lines:
 *
 * ```kotlin
 * class FakeShareManager(
 *     override val capabilities: ShareCapabilities = ShareCapabilities.Full,
 * ) : ShareManager {
 *     val shared = mutableListOf<SharePayload>()
 *     override suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult {
 *         shared += payload
 *         return ShareResult.Completed
 *     }
 * }
 * ```
 *
 * ## Using
 * ```kotlin
 * class ReportViewModel(private val share: ShareManager) : ViewModel() {
 *     val canExport = share.capabilities.file
 *
 *     fun exportPdf(uri: String) = viewModelScope.launch {
 *         when (val result = share.shareFile(uri, "application/pdf", message = "Latest report")) {
 *             is ShareResult.Failed -> showError(result.cause)
 *             else -> Unit
 *         }
 *     }
 * }
 * ```
 *
 * Every share method suspends: on most targets the sheet is presented and awaited, and the
 * returned [ShareResult] distinguishes completion from cancellation.
 */
public interface ShareManager {

    /** What this target can share. See [platformShareCapabilities] for the matrix. */
    public val capabilities: ShareCapabilities

    /** Share any payload. The single abstract operation — every other method routes here. */
    public suspend fun share(payload: SharePayload, options: ShareOptions = ShareOptions()): ShareResult

    /** Share plain text. */
    public suspend fun shareText(text: String, options: ShareOptions = ShareOptions()): ShareResult =
        share(SharePayload.Text(text), options)

    /** Share a link, so receivers render a preview rather than raw characters. */
    public suspend fun shareUrl(url: String, options: ShareOptions = ShareOptions()): ShareResult =
        share(SharePayload.Url(url), options)

    /**
     * Share a file by URI, optionally alongside a [message] — one chooser, both payloads.
     *
     * This is the call that used to require callers to assemble a [SharePayload.Multi] by hand.
     */
    public suspend fun shareFile(
        fileUri: String,
        mimeType: String,
        filename: String? = null,
        message: String? = null,
        options: ShareOptions = ShareOptions(),
    ): ShareResult {
        val file = SharePayload.File(fileUri, mimeType, filename)
        return share(withMessage(file, message), options)
    }

    /** Share in-memory image bytes, optionally alongside a [message]. */
    public suspend fun shareImage(
        bytes: ByteArray,
        mimeType: String = "image/png",
        filename: String? = null,
        message: String? = null,
        options: ShareOptions = ShareOptions(),
    ): ShareResult {
        val image = SharePayload.Image(bytes, mimeType, filename)
        return share(withMessage(image, message), options)
    }

    /**
     * Whether [payload] can be shared on this target — ask BEFORE rendering a share affordance.
     *
     * A [SharePayload.Multi] is supported only when bundling is supported *and* every item in it
     * is: on Windows and the web a bundle drops binary items silently, so a bundle containing one
     * is not honestly shareable there.
     */
    public fun supports(payload: SharePayload): Boolean = when (payload) {
        is SharePayload.Text -> capabilities.text
        is SharePayload.Url -> capabilities.url
        is SharePayload.Image -> capabilities.image
        is SharePayload.File -> capabilities.file
        is SharePayload.Multi -> capabilities.multi && payload.items.all { supports(it) }
    }
}

/** Bundle [message] with [payload] when there is one; otherwise share the payload alone. */
private fun withMessage(payload: SharePayload, message: String?): SharePayload = if (message.isNullOrEmpty()) {
    payload
} else {
    SharePayload.Multi(listOf(SharePayload.Text(message), payload))
}

/**
 * The one [ShareManager] — for every target.
 *
 * Stateless: all per-target behaviour lives in the [Share] engine it delegates to, so a single
 * instance is safe to share across the whole app (which is why the DI module binds it as a
 * singleton).
 */
public class ShareManagerImpl : ShareManager {

    override val capabilities: ShareCapabilities
        get() = platformShareCapabilities

    override suspend fun share(payload: SharePayload, options: ShareOptions): ShareResult =
        Share.share(payload, options).also {
            // Payload CLASS and result CLASS only — never the shared text, URL or file path, which
            // is the user's content and the whole point of the share.
            observeLifecycle(
                cmpMetadata(),
                "shared",
                mapOf("payload" to payload::class.simpleName, "result" to it::class.simpleName),
            )
        }
}
