package io.github.mobilebytelabs.kmptoolkit.sample.cmpappintents

class JvmPlatform : Platform {
    override val name: String = "JVM " + System.getProperty("java.version")
}

actual fun getPlatform(): Platform = JvmPlatform()
