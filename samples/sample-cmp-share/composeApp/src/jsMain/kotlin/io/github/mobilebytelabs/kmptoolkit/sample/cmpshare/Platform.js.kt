package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

class JsPlatform : Platform {
    override val name: String = "Web — Kotlin/JS"
}

actual fun getPlatform(): Platform = JsPlatform()
