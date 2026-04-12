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

// Library modules
include(":cmp-library") // Template/reference module
include(":cmp-clipboard") // Clipboard utilities
include(":cmp-toast") // Toast/Snackbar UI
include(":cmp-in-app-update") // In-App Update checking
include(":cmp-user-tickets") // Feature Request/Bug Report/Contact Support
include(":cmp-remote-config") // Remote Config

// Sample applications
include(":samples:sample-clipboard:composeApp")
include(":samples:sample-in-app-update:composeApp")
