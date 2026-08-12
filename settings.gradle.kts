pluginManagement {
    includeBuild("build-logic")
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
// Library observability — shared hook interface + 4 default Firebase/Supabase hook impls (library-runtime-observability epic, 2026-05-30)
include(":cmp-observe")
include(":cmp-observe-koin") // Zero-config Koin companion for cmp-observe
// Firebase Analytics — 21/21 KMP targets (GitLive on 11, Measurement Protocol HTTP on 10)
include(":cmp-firebase") // Firebase — Analytics + Crashlytics (unified, in-library setup)
include(":cmp-firebase-compose") // Firebase Compose — auto screen/click/lifecycle tracking
// PDF Generator — cross-platform PDF generation (HTML / Markdown / DSL input; multi-output)
include(":cmp-pdf-generator")
// Inter-app comms suite (Phase 1 scaffolds; full impl per plan-layer/.../inter-app-comms-suite/)
include(":cmp-share") // Share — cross-platform share sheet
// Share Compose — rememberShareLauncher() + ShareSheet() + ShareButton() (v0.4)
include(":cmp-share-compose")
include(":cmp-intent-launcher") // Intent Launcher — typed Android-Intent builder + ActivityResult; iOS picker whitelist
// Intent Launcher Compose — @Composable rememberIntentLauncher() adapter (extracted v0.3) + UX (v0.4)
include(":cmp-intent-launcher-compose")
include(":cmp-app-intents") // App Intents — SiriKit Shortcuts + Spotlight (iOS 16+); Android on-device registry
// App Intents Compose — AppIntentsRegistration() + AppIntentsRegistry() (v0.4)
include(":cmp-app-intents-compose")

// Sample applications (composeApp = Compose Multiplatform, runs on Android + Desktop + Web + iOS)
include(":samples:sample-clipboard:composeApp")
include(":samples:sample-in-app-update:composeApp")
include(":samples:sample-open-url:composeApp")
include(":samples:sample-deep-link:composeApp")
include(":samples:sample-network-monitor:composeApp")
include(":samples:sample-pdf-generator:composeApp")
include(":samples:sample-inter-app-comms:composeApp")
include(":samples:sample-cmp-share:composeApp")
include(":samples:sample-cmp-intent-launcher:composeApp")
include(":samples:sample-cmp-app-intents:composeApp")
// Unified catalog showcasing every cmp-* library in one app (per-module samples kept alongside)
include(":samples:sample-toolkit:composeApp")
