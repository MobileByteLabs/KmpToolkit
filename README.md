# KMP Toolkit

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-clipboard)](https://central.sonatype.com/search?q=io.github.mobilebytelabs)
[![CI](https://github.com/MobileByteLabs/KmpToolkit/actions/workflows/gradle.yml/badge.svg)](https://github.com/MobileByteLabs/KmpToolkit/actions/workflows/gradle.yml)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A collection of production-ready Kotlin Multiplatform libraries — one dependency per feature, works out of the box on every platform.

## Modules

| Module | Artifact | Description | Version |
|--------|----------|-------------|:-------:|
| [cmp-clipboard](#cmp-clipboard) | `io.github.mobilebytelabs:cmp-clipboard` | Copy, paste, observe, monitor, URL detect | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-bubble](#cmp-bubble) | `io.github.mobilebytelabs:cmp-bubble` | Floating UI, bubbles, and notifications | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-toast](#cmp-toast) | `io.github.mobilebytelabs:cmp-toast` | Toast / Snackbar for Compose Multiplatform | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-open-url](#cmp-open-url) | `io.github.mobilebytelabs:cmp-open-url` | Open URLs — browser, email, maps, phone, SMS | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-deep-link](#cmp-deep-link) | `io.github.mobilebytelabs:cmp-deep-link` | Deep link handling across all KMP targets | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-in-app-update](#cmp-in-app-update) | `io.github.mobilebytelabs:cmp-in-app-update` | In-app update checking (GitHub / App Store / Play) | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-remote-config](#cmp-remote-config) | `io.github.mobilebytelabs:cmp-remote-config` | Remote config and feature flags | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-product-tickets](#cmp-product-tickets) | `io.github.mobilebytelabs:cmp-product-tickets` | In-app feedback and support tickets | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-network-monitor](#cmp-network-monitor) | `io.github.mobilebytelabs:cmp-network-monitor` | Reactive network connectivity monitoring — all 21 KMP targets | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-network-monitor-compose](#cmp-network-monitor-compose) | `io.github.mobilebytelabs:cmp-network-monitor-compose` | Compose Multiplatform extensions for network monitoring | ![](https://img.shields.io/badge/-3.2.1-brightgreen) |
| [cmp-pdf-generator](cmp-pdf-generator/README.md) | `io.github.mobilebytelabs:cmp-pdf-generator` | Cross-platform PDF generation — HTML, Markdown, DSL → File / ByteArray / URI / Share / Print | ![](https://img.shields.io/badge/-3.2.8-brightgreen) |
| [cmp-share](cmp-share/README.md) | `io.github.mobilebytelabs:cmp-share` | Cross-platform share sheet — text / URL / image / file / multi via Android Intent.ACTION_SEND, iOS UAVC, JVM clipboard, JS navigator.share | ![](https://img.shields.io/badge/-0.1.0-orange) |
| [cmp-intent-launcher](cmp-intent-launcher/README.md) | `io.github.mobilebytelabs:cmp-intent-launcher` | Typed Android-Intent builder + ActivityResult — picker contracts cross-platform; full Intent + extras on Android | ![](https://img.shields.io/badge/-0.1.0-orange) |
| [cmp-app-intents](cmp-app-intents/README.md) | `io.github.mobilebytelabs:cmp-app-intents` | Declarative App Intents DSL — SiriKit Shortcuts + Spotlight (iOS 16+); Android on-device registry (Assistant integration v0.2) | ![](https://img.shields.io/badge/-0.1.0-orange) |

## Installation

All modules are published to Maven Central. Add only what you need:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            val kmptoolkit = "3.2.1"

            implementation("io.github.mobilebytelabs:cmp-clipboard:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-bubble:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-toast:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-open-url:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-deep-link:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-in-app-update:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-remote-config:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-product-tickets:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-network-monitor:$kmptoolkit")
            // Optional: Compose extensions for network monitoring
            implementation("io.github.mobilebytelabs:cmp-network-monitor-compose:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-pdf-generator:$kmptoolkit")
            // Inter-app communication suite (v0.1 experimental)
            implementation("io.github.mobilebytelabs:cmp-share:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-intent-launcher:$kmptoolkit")
            implementation("io.github.mobilebytelabs:cmp-app-intents:$kmptoolkit")
        }
    }
}
```

Each module is completely independent — import only what your project needs.

---

## cmp-clipboard

Cross-platform clipboard utilities — copy, paste, observe, monitor, and URL detection.

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.*

val clipboard = ClipboardManager(ClipboardManagerConfig.Full)
clipboard.start()

// Read & write
clipboard.copy("Hello!")
val text = clipboard.pasteAsync()

// Observe changes
clipboard.content.collect { println("Clipboard: $it") }

// Clipboard history
clipboard.history.collect { entries -> println("${entries.size} items") }

// URL detection (Instagram, TikTok, YouTube, etc.)
clipboard.urlDetections.collect { det -> println("${det.matcher.name}: ${det.url}") }

clipboard.stop()
```

**Clipboard Monitor** — continuous background monitoring with social media URL detection:

```kotlin
val monitor = createClipboardMonitor()
SocialMediaUrlMatchers.all().forEach { monitor.addUrlMatcher(it) }
monitor.start(ClipboardMonitorConfig.SocialMediaDownloader)

monitor.urlDetections.collect { detection ->
    println("${detection.matcher.name}: ${detection.url}")
}
```

10 built-in URL matchers: Instagram, TikTok, YouTube, Twitter/X, Facebook, Snapchat, Pinterest, Reddit, LinkedIn, Threads.

---

## cmp-bubble

Cross-platform floating UI, bubbles, and notifications.

```kotlin
import com.mobilebytelabs.kmptoolkit.bubble.*

val bubble = createBubble()

bubble.show(
    title = "Download Complete",
    message = "video.mp4 saved",
    actions = listOf(
        BubbleAction("Open") { openFile() },
        BubbleAction("Share") { shareFile() }
    )
)
```

| Platform | Implementation |
|----------|---------------|
| Android 10+ | Bubbles API |
| Android <10 | Notification fallback |
| iOS / macOS | UNUserNotificationCenter banners |
| JVM | System tray |
| JS / Wasm | Browser Notification API |

---

## cmp-toast

Toast and Snackbar for Compose Multiplatform — zero setup.

```kotlin
import com.mobilebytelabs.kmptoolkit.toast.*

@Composable
fun MyScreen() {
    val toastState = rememberToastHostState()
    val scope = rememberCoroutineScope()

    Box(Modifier.fillMaxSize()) {
        Button(onClick = {
            scope.launch {
                toastState.showToast(
                    message = "Saved!",
                    duration = ToastDuration.SHORT,
                    style = ToastStyle.SUCCESS
                )
            }
        }) { Text("Save") }

        ToastHost(hostState = toastState)
    }
}
```

Durations: `SHORT` · `MEDIUM` · `LONG` · `INDEFINITE`
Styles: `DEFAULT` · `SUCCESS` · `ERROR` · `WARNING` · `INFO`
Positions: `TOP` · `CENTER` · `BOTTOM`

---

## cmp-open-url

Open any URL cross-platform — browser, email, maps, phone, SMS, or custom schemes.

```kotlin
import com.mobilebytelabs.kmptoolkit.openurl.*

UrlOpener.open("https://example.com")
UrlOpener.open("mailto:hello@example.com")
UrlOpener.open("tel:+1234567890")
UrlOpener.open("geo:37.7749,-122.4194")
UrlOpener.open("sms:+1234567890")
```

Works on Android, iOS, macOS, JVM, JS, Wasm — no platform configuration needed.

---

## cmp-deep-link

Unified deep link handling across all KMP targets. Receives, parses, and routes deep links with a type-safe DSL.

```kotlin
import com.mobilebytelabs.kmptoolkit.deeplink.*

// Receive links
DeepLinkHandler.incoming.collect { link ->
    println("${link.scheme}://${link.host}${link.path}")
}

// Type-safe route matching
val parser = deepLinkParser {
    route<ProductRoute>("/product/{id}")
    route<ProfileRoute>("/user/{username}")
}

val match: ProductRoute? = parser.parse(deepLink)
```

**Platform setup** (one-time, consumer app):

| Platform | Setup |
|----------|-------|
| Android | `ComponentActivity.handleDeepLinkIntent()` extension |
| iOS | AppDelegate `openURL` / SwiftUI `.onOpenURL` |
| macOS | AppKit URL event handler |
| JVM | `DeepLinkHandler.handleLaunchArgs(args)` |
| JS / Wasm | `DeepLinkHandler.initBrowser()` — auto-listens to `hashchange` + `popstate` |

---

## cmp-in-app-update

Check for app updates from GitHub Releases, App Store, Play Store, or a custom backend.

```kotlin
import com.mobilebytelabs.kmptoolkit.appupdate.*

val config = AppUpdateConfig.builder()
    .github(owner = "YourOrg", repo = "YourApp")
    .build()

when (val result = AppUpdate.checkForUpdate(config)) {
    is UpdateResult.Success -> {
        if (result.updateInfo.isAvailable) {
            println("New version: ${result.updateInfo.latestVersion}")
        }
    }
    is UpdateResult.Error -> println("Error: ${result.message}")
    is UpdateResult.NotSupported -> println("Not supported")
}
```

---

## cmp-remote-config

Remote configuration and feature flags for KMP apps.

```kotlin
import com.mobilebytelabs.remoteconfig.*

RemoteConfig.init(RemoteConfigConfig(url = "https://your-config-endpoint"))

val isFeatureEnabled: Boolean = RemoteConfig.getBoolean("new_feature", default = false)
val apiUrl: String = RemoteConfig.getString("api_url", default = "https://api.example.com")

// Observe changes
RemoteConfig.values.collect { config -> /* update UI */ }
```

---

## cmp-product-tickets

In-app feedback and support ticket system backed by Supabase — drop-in Compose UI included.

```kotlin
import com.mobilebytelabs.producttickets.config.ProductTicketsConfig
import com.mobilebytelabs.producttickets.di.productTicketsModule

// Initialize (once, at app start)
ProductTicketsConfig.init(
    supabaseUrl = "https://your-project.supabase.co",
    supabaseAnonKey = "your-anon-key",
    userId = currentUserId  // optional
)

// Koin DI
startKoin { modules(productTicketsModule) }
```

Navigation (Compose):

```kotlin
productTicketsDestination(
    onBackClick = { navController.popBackStack() },
    onNavigateToCreateTicket = { type -> navController.navigateToCreateTicket(type) },
    onNavigateToTicketDetail = { id -> navController.navigateToTicketDetail(id) }
)
createTicketDestination(onBackClick = { navController.popBackStack() })
ticketDetailDestination(onBackClick = { navController.popBackStack() })
```

---

## cmp-network-monitor

Reactive network connectivity monitoring for **all 21 KMP targets** — push-based on mobile, adaptive polling on desktop/native.

```kotlin
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.*

val monitor = createNetworkMonitor()

// Observe connectivity
monitor.isOnline.collect { online -> updateUI(online) }

// Rich status (Available / Unavailable / CaptivePortal)
monitor.networkStatus.collect { status ->
    when (status) {
        is NetworkStatus.Available -> println("Online via ${status.info.type}")
        is NetworkStatus.CaptivePortal -> showLoginPrompt(status.redirectUrl)
        is NetworkStatus.Unavailable -> showOfflineBanner()
    }
}

// Extensions
monitor.requireOnline()                         // throws if offline
monitor.ensureOnline()                          // suspends until online
monitor.ifOnline { api.fetchData() }            // conditional execution
monitor.retryOnReconnect(maxRetries = 3) { api.upload(payload) }
```

| Platform | API | Type |
|----------|-----|------|
| Android | ConnectivityManager + NetworkCallback | Push |
| iOS/macOS/tvOS/watchOS | NWPathMonitor | Push |
| JVM | NetworkInterface + HTTP HEAD + Adaptive Polling | Poll |
| JS / WasmJS | navigator.onLine + events | Push |
| Linux | /sys/class/net + Adaptive Polling | Poll |
| Windows (MinGW) | Winsock2 + Adaptive Polling | Poll |

See [docs/network-monitor/](docs/network-monitor/) for full setup guide.

---

## cmp-network-monitor-compose

Compose Multiplatform extensions for `cmp-network-monitor` — auto UI switching, offline banners, and CompositionLocal support.

```kotlin
import io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose.*

// Auto-switch between online/offline UI
NetworkAwareContent(
    onlineContent = { MainContent() },
    offlineContent = { OfflinePlaceholder() },
    captivePortalContent = { portal -> CaptivePortalBanner(portal.redirectUrl) },
)

// Animated offline banner
ConnectivityBanner()

// Scoped monitor with auto-cleanup
val monitor = rememberScopedNetworkMonitor()
val isOnline by monitor.collectIsOnlineAsState()
```

Supports: Android, iOS, macOS, JVM, JS, WasmJS.

---

## Platform Support

| Platform | clipboard | bubble | toast | open-url | deep-link | in-app-update | remote-config | product-tickets | network-monitor | network-monitor-compose |
|----------|:---------:|:------:|:-----:|:--------:|:---------:|:-------------:|:-------------:|:---------------:|:---------------:|:----------------------:|
| Android | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| iOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| macOS | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| JVM | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ | ✅ |
| JS | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ |
| Wasm | ✅ | ✅ | ❌ | ✅ | ✅ | ❌ | ✅ | ❌ | ✅ | ✅ |
| Linux | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| Windows | ✅ | ❌ | ❌ | ✅ | ✅ | ✅ | ✅ | ❌ | ✅ | ❌ |
| tvOS | — | — | — | — | — | — | — | — | ✅ | ❌ |
| watchOS | — | — | — | — | — | — | — | — | ✅ | ❌ |
| WasmWASI | — | — | — | — | — | — | — | — | ✅ | ❌ |

## Sample Apps

```bash
# Desktop (JVM)
./gradlew :samples:sample-clipboard:composeApp:run
./gradlew :samples:sample-open-url:composeApp:run
./gradlew :samples:sample-deep-link:composeApp:run
./gradlew :samples:sample-network-monitor:composeApp:run

# Android
./gradlew :samples:sample-clipboard:composeApp:installDebug
./gradlew :samples:sample-in-app-update:composeApp:installDebug
./gradlew :samples:sample-network-monitor:composeApp:installDebug
```

## Contributing

1. Copy `cmp-library` as a template: `cp -r cmp-library cmp-your-feature`
2. Follow `cmp-library/TEMPLATE_README.md`
3. Add the module to `settings.gradle.kts`
4. Add a sample app under `samples/`

See [CONTRIBUTING.md](CONTRIBUTING.md) for full guidelines.

## License

```
Copyright 2025 MobileByteLabs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for details.
