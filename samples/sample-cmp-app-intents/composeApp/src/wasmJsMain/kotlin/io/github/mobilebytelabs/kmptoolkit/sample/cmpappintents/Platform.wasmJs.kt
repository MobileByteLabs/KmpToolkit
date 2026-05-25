package io.github.mobilebytelabs.kmptoolkit.sample.cmpappintents

class WasmPlatform : Platform {
    override val name: String = "Web — Kotlin/Wasm"
}

actual fun getPlatform(): Platform = WasmPlatform()
