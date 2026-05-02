package io.github.mobilebytelabs.kmptoolkit.networkmonitor

internal actual fun currentTimeMillis(): Long = kotlin.js.Date.now().toLong()
