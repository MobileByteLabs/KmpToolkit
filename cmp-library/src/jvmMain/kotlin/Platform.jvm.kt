package io.github.template

actual fun getPlatformName(): String = "JVM ${System.getProperty("java.version")}"
