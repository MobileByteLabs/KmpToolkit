package io.github.mobilebytelabs.kmptoolkit.sample.clipboard

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
