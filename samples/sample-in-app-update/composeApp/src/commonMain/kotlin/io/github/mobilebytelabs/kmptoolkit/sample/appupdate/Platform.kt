package io.github.mobilebytelabs.kmptoolkit.sample.appupdate

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
