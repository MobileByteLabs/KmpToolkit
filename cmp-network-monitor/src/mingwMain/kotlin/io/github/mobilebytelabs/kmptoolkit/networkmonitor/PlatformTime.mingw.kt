package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.alloc
import kotlinx.cinterop.memScoped
import kotlinx.cinterop.ptr
import platform.windows.FILETIME
import platform.windows.GetSystemTimeAsFileTime

@OptIn(ExperimentalForeignApi::class)
internal actual fun currentTimeMillis(): Long = memScoped {
    val ft = alloc<FILETIME>()
    GetSystemTimeAsFileTime(ft.ptr)
    val ticks = ft.dwHighDateTime.toLong().shl(32) or ft.dwLowDateTime.toLong()
    // FILETIME is 100ns intervals since 1601-01-01, convert to epoch millis
    (ticks - 116444736000000000L) / 10000L
}
