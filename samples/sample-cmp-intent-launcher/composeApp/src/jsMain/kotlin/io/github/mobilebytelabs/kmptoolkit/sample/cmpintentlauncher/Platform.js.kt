package io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher

class JsPlatform : Platform {
    override val name: String = "Web — Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()
