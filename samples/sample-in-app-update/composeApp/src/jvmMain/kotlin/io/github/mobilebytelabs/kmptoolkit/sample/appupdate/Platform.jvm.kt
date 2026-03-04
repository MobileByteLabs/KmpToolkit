package io.github.mobilebytelabs.kmptoolkit.sample.appupdate

class JVMPlatform : Platform {
    override val name: String = "JVM ${System.getProperty("java.version")}"
}

actual fun getPlatform(): Platform = JVMPlatform()
