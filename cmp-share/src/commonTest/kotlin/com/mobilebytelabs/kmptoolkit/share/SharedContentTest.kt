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

import kotlin.test.Test
import kotlin.test.assertEquals
import kotlin.test.assertNull

class SharedContentTest {
    private val send = SharedContent.ACTION_SEND

    @Test
    fun sharedLink_isText_withTrimmedSubject() {
        assertEquals(
            SharedContent.Text("https://instagram.com/reel/abc", "Reel"),
            SharedContent.parse(send, "text/plain", "  https://instagram.com/reel/abc ", " Reel ", null),
        )
    }

    @Test
    fun sharedImage_isFile() {
        assertEquals(
            SharedContent.File("content://media/1", "image/jpeg"),
            SharedContent.parse(send, "image/jpeg", null, null, "content://media/1"),
        )
    }

    @Test
    fun textMimeWithOnlyAStream_isFile() {
        assertEquals(
            SharedContent.File("content://docs/notes.txt", "text/plain"),
            SharedContent.parse(send, "text/plain", "  ", null, "content://docs/notes.txt"),
        )
    }

    @Test
    fun notAShare_isNull() {
        assertNull(SharedContent.parse("android.intent.action.MAIN", "text/plain", "x", null, null))
        assertNull(SharedContent.parse(null, null, null, null, null))
        assertNull(SharedContent.parse(send, "text/plain", "   ", null, null))
    }
}
