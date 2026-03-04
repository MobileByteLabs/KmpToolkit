package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

class Greeting {
    private val platform = getPlatform()

    fun greet(): String = "Hello, ${platform.name}!"
}
