package io.github.mobilebytelabs.kmptoolkit.sample.cmpshare

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
