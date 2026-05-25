package io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher

class WasmPlatform : Platform {
    override val name: String = "Web — Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()
