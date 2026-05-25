package io.github.mobilebytelabs.kmptoolkit.sample.cmpintentlauncher

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
