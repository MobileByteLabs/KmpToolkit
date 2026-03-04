package io.github.mobilebytelabs.kmptoolkit.sample.base

class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}