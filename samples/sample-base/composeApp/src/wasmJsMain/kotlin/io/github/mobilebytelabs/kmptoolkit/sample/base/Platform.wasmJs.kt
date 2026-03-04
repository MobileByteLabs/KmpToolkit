package io.github.mobilebytelabs.kmptoolkit.sample.base

class WasmPlatform: Platform {
    override val name: String = "Web with Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()