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
 * Opt-in marker for the experimental [Share] API. Per ADR-08 — per-module marker,
 * not a shared `@ExperimentalInterAppCommsApi`. Remove the annotation when cmp-share
 * graduates to v1.0.
 *
 * Usage: `@OptIn(ExperimentalShareApi::class)` on call sites, or set
 * `-opt-in=com.mobilebytelabs.kmptoolkit.share.ExperimentalShareApi` in your build script.
 */
@RequiresOptIn(
    message = "cmp-share is experimental until v1.0; API may evolve without major-version bumps.",
    level = RequiresOptIn.Level.WARNING,
)
@Retention(AnnotationRetention.BINARY)
public annotation class ExperimentalShareApi

/**
 * Payload variants accepted by [Share.share]. See SPEC for the platform-mapping matrix.
 *
 * Plan: plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/04-cmp-share.md
 */
@ExperimentalShareApi
public sealed class SharePayload {
    public data class Text(val content: String, val mimeType: String = "text/plain") : SharePayload()

    public data class Url(val href: String) : SharePayload()

    /**
     * Image bytes with their MIME type. [filename] is optional but recommended on Android
     * (used by FileProvider) and macOS (used by NSItemProvider).
     */
    public class Image(public val bytes: ByteArray, public val mimeType: String, public val filename: String? = null) :
        SharePayload() {
        override fun equals(other: Any?): Boolean {
            if (this === other) return true
            if (other !is Image) return false
            if (!bytes.contentEquals(other.bytes)) return false
            if (mimeType != other.mimeType) return false
            if (filename != other.filename) return false
            return true
        }

        override fun hashCode(): Int {
            var result = bytes.contentHashCode()
            result = 31 * result + mimeType.hashCode()
            result = 31 * result + (filename?.hashCode() ?: 0)
            return result
        }

        override fun toString(): String = "Image(bytes=${bytes.size} bytes, mimeType=$mimeType, filename=$filename)"
    }

    public data class File(val uri: String, val mimeType: String, val filename: String? = null) : SharePayload()

    public data class Multi(val items: List<SharePayload>) : SharePayload()
}

/**
 * Options for [Share.share]. See SPEC for per-platform field semantics.
 *
 * @property chooserTitle Title for the system chooser. Android: `Intent.createChooser` title;
 *   iOS: unused. Ignored when [targetPackage] resolves to a direct-to-app share.
 * @property excludedActivities Activity/type identifiers to exclude. iOS: `UIActivityViewController.excludedActivityTypes`;
 *   Android: currently advisory (not yet applied to the chooser).
 * @property presentingController iOS-only: the `UIViewController` to present the share sheet from
 *   (cast internally). Ignored on other platforms.
 * @property targetPackage **Android-only**: when non-null, the share Intent is directed straight to this
 *   application package (`Intent.setPackage`) — e.g. `"com.whatsapp"`, `"com.instagram.android"` — so the
 *   payload opens in that app WITHOUT the system chooser. If the package is not installed / cannot handle
 *   the payload, the implementation falls back to the normal chooser rather than failing. Ignored on iOS /
 *   desktop / web (no per-app targeting on those platforms).
 */
@ExperimentalShareApi
public data class ShareOptions(
    val chooserTitle: String? = null,
    val excludedActivities: List<String> = emptyList(),
    val presentingController: Any? = null,
    val targetPackage: String? = null,
)

/**
 * Outcome of a share invocation. Never thrown — always returned.
 */
@ExperimentalShareApi
public sealed class ShareResult {
    public object Completed : ShareResult()

    public object Cancelled : ShareResult()

    public data class Failed(val cause: ShareError) : ShareResult()
}

@ExperimentalShareApi
public sealed class ShareError {
    public object UnsupportedPlatform : ShareError()

    public object NoHandler : ShareError()

    public object UserGestureMissing : ShareError()

    public data class Unknown(val message: String) : ShareError()
}

/**
 * Cross-platform share-sheet entry point.
 *
 * Per-platform behaviour matrix lives in SPEC.md + ADR-01.
 */
@ExperimentalShareApi
public expect object Share {
    public suspend fun share(payload: SharePayload, options: ShareOptions = ShareOptions()): ShareResult
}

// -----------------------------------------------------------------------------
// DSL convenience helpers — all delegate to Share.share()
// -----------------------------------------------------------------------------

@ExperimentalShareApi
public suspend fun Share.text(content: String, options: ShareOptions = ShareOptions()): ShareResult =
    share(SharePayload.Text(content), options)

@ExperimentalShareApi
public suspend fun Share.url(href: String, options: ShareOptions = ShareOptions()): ShareResult =
    share(SharePayload.Url(href), options)

@ExperimentalShareApi
public suspend fun Share.image(
    bytes: ByteArray,
    mimeType: String,
    filename: String? = null,
    options: ShareOptions = ShareOptions(),
): ShareResult = share(SharePayload.Image(bytes, mimeType, filename), options)

@ExperimentalShareApi
public suspend fun Share.file(
    uri: String,
    mimeType: String,
    filename: String? = null,
    options: ShareOptions = ShareOptions(),
): ShareResult = share(SharePayload.File(uri, mimeType, filename), options)

@ExperimentalShareApi
public suspend fun Share.multi(payloads: List<SharePayload>, options: ShareOptions = ShareOptions()): ShareResult =
    share(SharePayload.Multi(payloads), options)
