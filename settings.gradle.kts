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
include(":cmp-network-monitor") // Network Monitor — reactive connectivity monitoring across all KMP targets
include(":cmp-network-monitor-compose") // Network Monitor Compose — Compose Multiplatform UI extensions
// Firebase Analytics — 21/21 KMP targets (GitLive on 11, Measurement Protocol HTTP on 10)
include(":cmp-firebase-analytics")
// PDF Generator — cross-platform PDF generation (HTML / Markdown / DSL input; multi-output)
include(":cmp-pdf-generator")
// Inter-app comms suite (Phase 1 scaffolds; full impl per plan-layer/.../inter-app-comms-suite/)
include(":cmp-share") // Share — cross-platform share sheet
include(":cmp-intent-launcher") // Intent Launcher — typed Android-Intent builder + ActivityResult; iOS picker whitelist
include(":cmp-app-intents") // App Intents — SiriKit Shortcuts + Spotlight (iOS 16+); Android on-device registry

// Sample applications
include(":samples:sample-clipboard:composeApp")
include(":samples:sample-clipboard:androidApp")
include(":samples:sample-in-app-update:composeApp")
include(":samples:sample-in-app-update:androidApp")
include(":samples:sample-open-url:composeApp")
include(":samples:sample-open-url:androidApp")
include(":samples:sample-deep-link:composeApp")
include(":samples:sample-deep-link:androidApp")
include(":samples:sample-network-monitor:composeApp")
include(":samples:sample-network-monitor:androidApp")
include(":samples:sample-pdf-generator:composeApp")
include(":samples:sample-pdf-generator:androidApp")
include(":samples:sample-inter-app-comms:composeApp")
include(":samples:sample-inter-app-comms:androidApp")
include(":samples:sample-cmp-share:composeApp")
include(":samples:sample-cmp-share:androidApp")
include(":samples:sample-cmp-intent-launcher:composeApp")
include(":samples:sample-cmp-intent-launcher:androidApp")
include(":samples:sample-cmp-app-intents:composeApp")
include(":samples:sample-cmp-app-intents:androidApp")
// Unified catalog showcasing every cmp-* library in one app (per-module samples kept alongside)
include(":samples:sample-toolkit:composeApp")
include(":samples:sample-toolkit:androidApp")
