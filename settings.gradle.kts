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
include(":cmp-product-tickets") // Product Tickets — Feature Request/Bug Report/Contact Support
include(":cmp-remote-config") // Remote Config
include(":cmp-bubble") // Floating UI, Bubbles, Notifications
include(":cmp-open-url") // Open URL — cross-platform URL/scheme handler (browser, email, maps, phone, SMS)
include(":cmp-deep-link") // Deep Link — unified deep link handling across all KMP targets

// Sample applications
include(":samples:sample-clipboard:composeApp")
include(":samples:sample-in-app-update:composeApp")
include(":samples:sample-open-url:composeApp")
include(":samples:sample-deep-link:composeApp")
