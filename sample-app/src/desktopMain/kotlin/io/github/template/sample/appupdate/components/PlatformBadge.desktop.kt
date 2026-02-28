package io.github.template.sample.appupdate.components

actual fun getPlatformName(): String {
    val os = System.getProperty("os.name") ?: "Desktop"
    val arch = System.getProperty("os.arch") ?: ""
    return "$os ($arch)"
}
