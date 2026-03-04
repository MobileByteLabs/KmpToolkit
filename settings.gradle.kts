pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "template-library"
include(":cmp-library")
include(":cmp-clipboard")

// Sample applications
include(":samples:sample-clipboard:composeApp")
