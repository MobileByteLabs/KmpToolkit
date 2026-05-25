package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

class WasmPlatform : Platform {
    override val name: String = "Web — Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()
