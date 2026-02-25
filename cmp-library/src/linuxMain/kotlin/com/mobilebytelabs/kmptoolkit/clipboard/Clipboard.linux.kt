package com.mobilebytelabs.kmptoolkit.clipboard

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.allocArray
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import kotlinx.cinterop.toKString
import platform.posix.FILE
import platform.posix.fgets
import platform.posix.fputs
import platform.posix.pclose
import platform.posix.popen

/**
 * Linux implementation of clipboard operations using xclip/xsel.
 *
 * This implementation uses external clipboard tools that are commonly
 * available on Linux systems:
 * - Primary: xclip (preferred)
 * - Fallback: xsel
 *
 * ## Requirements
 *
 * At least one of these tools must be installed:
 * ```bash
 * # Ubuntu/Debian
 * sudo apt install xclip
 * # or
 * sudo apt install xsel
 *
 * # Fedora/RHEL
 * sudo dnf install xclip
 * # or
 * sudo dnf install xsel
 *
 * # Arch Linux
 * sudo pacman -S xclip
 * # or
 * sudo pacman -S xsel
 * ```
 *
 * ## Wayland Support
 *
 * For Wayland, consider installing wl-clipboard:
 * ```bash
 * sudo apt install wl-clipboard
 * ```
 *
 * Note: Current implementation only supports X11 clipboard via xclip/xsel.
 */

@OptIn(ExperimentalForeignApi::class)
actual fun copyToClipboard(text: String): Boolean {
    // Try xclip first
    var process = popen("xclip -selection clipboard", "w")
    if (process != null) {
        fputs(text, process)
        pclose(process)
        return true
    }

    // Fallback to xsel
    process = popen("xsel --clipboard --input", "w")
    if (process != null) {
        fputs(text, process)
        pclose(process)
        return true
    }

    return false
}

@OptIn(ExperimentalForeignApi::class)
actual fun getFromClipboard(): String? {
    memScoped {
        // Try xclip first
        var process = popen("xclip -selection clipboard -o 2>/dev/null", "r")
        if (process != null) {
            val result = readFromProcess(process)
            pclose(process)
            if (result.isNotEmpty()) return result
        }

        // Fallback to xsel
        process = popen("xsel --clipboard --output 2>/dev/null", "r")
        if (process != null) {
            val result = readFromProcess(process)
            pclose(process)
            if (result.isNotEmpty()) return result
        }
    }
    return null
}

@OptIn(ExperimentalForeignApi::class)
private fun readFromProcess(process: kotlinx.cinterop.CPointer<FILE>): String {
    memScoped {
        val buffer = allocArray<kotlinx.cinterop.ByteVar>(4096)
        val result = StringBuilder()
        while (fgets(buffer, 4096, process) != null) {
            result.append(buffer.toKString())
        }
        return result.toString().trimEnd('\n')
    }
}

actual fun hasClipboardText(): Boolean {
    val text = getFromClipboard()
    return text != null && text.isNotEmpty()
}

actual fun clearClipboard() {
    copyToClipboard("")
}
