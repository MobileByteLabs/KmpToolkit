package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ptr
import kotlinx.cinterop.reinterpret
import kotlinx.cinterop.set
import kotlinx.cinterop.toKString
import kotlinx.cinterop.usePinned
import platform.windows.CF_TEXT
import platform.windows.CloseClipboard
import platform.windows.EmptyClipboard
import platform.windows.GMEM_MOVEABLE
import platform.windows.GetClipboardData
import platform.windows.GlobalAlloc
import platform.windows.GlobalLock
import platform.windows.GlobalUnlock
import platform.windows.IsClipboardFormatAvailable
import platform.windows.OpenClipboard
import platform.windows.SetClipboardData

/**
 * Windows implementation of clipboard operations using Win32 API.
 *
 * This implementation provides full clipboard support on Windows
 * using the native Win32 clipboard functions.
 *
 * ## Thread Safety
 *
 * Windows clipboard operations are thread-safe - the system handles
 * concurrent access through OpenClipboard/CloseClipboard.
 */

@OptIn(ExperimentalForeignApi::class)
actual fun copyToClipboard(text: String): Boolean {
    if (OpenClipboard(null) == 0) return false

    return try {
        EmptyClipboard()

        val bytes = text.encodeToByteArray()
        val size = (bytes.size + 1).toULong()
        val hMem = GlobalAlloc(GMEM_MOVEABLE, size)

        if (hMem == null) {
            CloseClipboard()
            return false
        }

        val locked = GlobalLock(hMem)
        if (locked != null) {
            bytes.usePinned { pinned ->
                val dest = locked.reinterpret<ByteVar>()
                for (i in bytes.indices) {
                    dest[i] = pinned.get()[i]
                }
                dest[bytes.size] = 0 // Null terminator
            }
            GlobalUnlock(hMem)
        }

        SetClipboardData(CF_TEXT.toUInt(), hMem)
        CloseClipboard()
        true
    } catch (e: Exception) {
        CloseClipboard()
        false
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun getFromClipboard(): String? {
    if (OpenClipboard(null) == 0) return null

    return try {
        val hData = GetClipboardData(CF_TEXT.toUInt())
        if (hData == null) {
            CloseClipboard()
            return null
        }

        val text = GlobalLock(hData)?.let { ptr ->
            val cPointer = ptr.reinterpret<ByteVar>()
            cPointer.toKString()
        }

        GlobalUnlock(hData)
        CloseClipboard()
        text
    } catch (e: Exception) {
        CloseClipboard()
        null
    }
}

@OptIn(ExperimentalForeignApi::class)
actual fun hasClipboardText(): Boolean = IsClipboardFormatAvailable(CF_TEXT.toUInt()) != 0

@OptIn(ExperimentalForeignApi::class)
actual fun clearClipboard() {
    if (OpenClipboard(null) != 0) {
        EmptyClipboard()
        CloseClipboard()
    }
}
