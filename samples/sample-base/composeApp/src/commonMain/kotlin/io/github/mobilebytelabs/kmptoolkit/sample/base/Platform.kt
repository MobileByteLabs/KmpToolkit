package io.github.mobilebytelabs.kmptoolkit.sample.base

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
