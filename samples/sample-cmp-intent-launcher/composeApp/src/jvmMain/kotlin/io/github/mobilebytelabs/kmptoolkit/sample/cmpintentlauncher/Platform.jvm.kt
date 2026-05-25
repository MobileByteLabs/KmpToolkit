package io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher

class JvmPlatform : Platform {
    override val name: String = "JVM " + System.getProperty("java.version")
}

actual fun getPlatform(): Platform = JvmPlatform()
