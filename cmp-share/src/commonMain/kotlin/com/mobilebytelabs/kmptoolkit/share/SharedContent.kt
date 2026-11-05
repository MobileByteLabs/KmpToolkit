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
 * Content another app shared INTO this one — the receiving half of `cmp-share`, which otherwise only
 * shares out. On Android, read it from the launch / `onNewIntent` Intent with `ShareReceiver.fromIntent`.
 * Other platforms have no in-process equivalent (iOS receives through an app-level Share Extension).
 */
public sealed interface SharedContent {
    /** Shared text — usually a link (a downloader app's "share to" flow) or a note. */
    public data class Text(val text: String, val subject: String? = null) : SharedContent

    /** A shared file (an image, a video, a document) by its content URI. */
    public data class File(val uri: String, val mimeType: String? = null) : SharedContent

    public companion object {
        /** `Intent.ACTION_SEND`. */
        public const val ACTION_SEND: String = "android.intent.action.SEND"

        /**
         * The platform-free rule behind `ShareReceiver.fromIntent`: an `ACTION_SEND` with a non-text payload
         * stream is a [File]; otherwise non-blank text is [Text]. Anything else — another action, nothing usable —
         * is null, so a normal launch is never mistaken for a share.
         */
        public fun parse(
            action: String?,
            mimeType: String?,
            text: String?,
            subject: String?,
            streamUri: String?,
        ): SharedContent? {
            if (action != ACTION_SEND) return null
            val isText = mimeType?.startsWith("text/") == true
            if (!streamUri.isNullOrBlank() && !isText) return File(streamUri, mimeType)
            val body = text?.trim()?.takeIf(String::isNotEmpty)
            return when {
                body != null -> Text(body, subject?.trim()?.takeIf(String::isNotEmpty))
                !streamUri.isNullOrBlank() -> File(streamUri, mimeType)
                else -> null
            }
        }
    }
}
