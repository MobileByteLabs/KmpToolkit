package io.github.mobilebytelabs.kmptoolkit.networkmonitor

import kotlin.js.ExperimentalWasmJsInterop

@OptIn(ExperimentalWasmJsInterop::class)
@JsFun("() => Date.now()")
private external fun jsDateNow(): Int

internal actual fun currentTimeMillis(): Long = jsDateNow().toLong()
