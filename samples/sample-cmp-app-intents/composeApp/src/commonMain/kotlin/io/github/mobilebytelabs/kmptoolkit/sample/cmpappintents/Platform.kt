package io.github.mobilebytelabs.kmptoolkit.sample.cmpappintents

interface Platform {
    val name: String
}

expect fun getPlatform(): Platform
