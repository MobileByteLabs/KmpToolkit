# KMP Toolkit

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-clipboard?label=Maven%20Central&color=blue)](https://central.sonatype.com/search?q=g%3Aio.github.mobilebytelabs)
[![CI](https://github.com/MobileByteLabs/KmpToolkit/actions/workflows/gradle.yml/badge.svg)](https://github.com/MobileByteLabs/KmpToolkit/actions/workflows/gradle.yml)
[![Kotlin](https://img.shields.io/badge/kotlin-2.3.10-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A collection of production-ready **Kotlin Multiplatform** libraries — one dependency per feature, works out of the box on every platform.

📖 **Full usage docs:** **[Docs site](https://mobilebytelabs.github.io/KmpToolkit/)** · **[Wiki](https://github.com/MobileByteLabs/KmpToolkit/wiki)** · 🚀 **[Releases](https://github.com/MobileByteLabs/KmpToolkit/releases)** · 📦 **[Maven Central](https://central.sonatype.com/search?q=g%3Aio.github.mobilebytelabs)**

> The [Docs site](https://mobilebytelabs.github.io/KmpToolkit/) ships an API reference for every module via Dokka HTML bundled inside each Maven Central `-javadoc.jar` artifact — IntelliJ / Android Studio surface it automatically in hover popups.

## Modules

All modules ship together at the unified `kmptoolkit.version`. Click a module to open its Wiki page (install + usage + per-platform behaviour).

| Module | Artifact | Description | Latest |
|--------|----------|-------------|:------:|
| [cmp-clipboard](https://github.com/MobileByteLabs/KmpToolkit/wiki/Clipboard) | `io.github.mobilebytelabs:cmp-clipboard` | Copy, paste, observe, monitor, URL detect | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-clipboard?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-clipboard) |
| [cmp-bubble](https://github.com/MobileByteLabs/KmpToolkit/wiki/Bubble) | `io.github.mobilebytelabs:cmp-bubble` | Floating UI, bubbles, and notifications | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-bubble?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-bubble) |
| [cmp-toast](https://github.com/MobileByteLabs/KmpToolkit/wiki/Toast) | `io.github.mobilebytelabs:cmp-toast` | Toast / Snackbar for Compose Multiplatform | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-toast?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-toast) |
| [cmp-open-url](https://github.com/MobileByteLabs/KmpToolkit/wiki/Open-URL) | `io.github.mobilebytelabs:cmp-open-url` | Open URLs — browser, email, maps, phone, SMS | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-open-url?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-open-url) |
| [cmp-deep-link](https://github.com/MobileByteLabs/KmpToolkit/wiki/Deep-Link) | `io.github.mobilebytelabs:cmp-deep-link` | Deep link handling across all KMP targets | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-deep-link?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-deep-link) |
| [cmp-in-app-update](https://github.com/MobileByteLabs/KmpToolkit/wiki/App-Update) | `io.github.mobilebytelabs:cmp-in-app-update` | In-app update checking (GitHub / App Store / Play) | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-in-app-update?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-in-app-update) |
| [cmp-remote-config](https://github.com/MobileByteLabs/KmpToolkit/wiki/Remote-Config) | `io.github.mobilebytelabs:cmp-remote-config` | Remote config and feature flags | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-remote-config?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-remote-config) |
| [cmp-product-tickets](https://github.com/MobileByteLabs/KmpToolkit/wiki/Product-Tickets) | `io.github.mobilebytelabs:cmp-product-tickets` | In-app feedback and support tickets | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-product-tickets?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-product-tickets) |
| [cmp-network-monitor](https://github.com/MobileByteLabs/KmpToolkit/wiki/Network-Monitor) | `io.github.mobilebytelabs:cmp-network-monitor` | Reactive network connectivity — all 21 KMP targets | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-network-monitor?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-network-monitor) |
| [cmp-network-monitor-compose](https://github.com/MobileByteLabs/KmpToolkit/wiki/Network-Monitor-Compose) | `io.github.mobilebytelabs:cmp-network-monitor-compose` | Compose extensions for network monitoring | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-network-monitor-compose?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-network-monitor-compose) |
| [cmp-pdf-generator](https://github.com/MobileByteLabs/KmpToolkit/wiki/PDF-Generator) | `io.github.mobilebytelabs:cmp-pdf-generator` | Cross-platform PDF generation (HTML / Markdown / DSL → File / Bytes / URI / Share / Print) | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-pdf-generator?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-pdf-generator) |
| [cmp-firebase-analytics](https://github.com/MobileByteLabs/KmpToolkit/wiki/Firebase-Analytics) | `io.github.mobilebytelabs:cmp-firebase-analytics` | Firebase Analytics + performance tracking | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-firebase-analytics?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-firebase-analytics) |
| [cmp-share](https://github.com/MobileByteLabs/KmpToolkit/wiki/Share) | `io.github.mobilebytelabs:cmp-share` | Cross-platform share sheet — text / URL / image / file / multi | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-share?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-share) |
| [cmp-share-compose](https://github.com/MobileByteLabs/KmpToolkit/wiki/Share-Compose) | `io.github.mobilebytelabs:cmp-share-compose` | `@Composable rememberShareLauncher()` + Material 3 ShareSheet / ShareButton | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-share-compose?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-share-compose) |
| [cmp-intent-launcher](https://github.com/MobileByteLabs/KmpToolkit/wiki/Intent-Launcher) | `io.github.mobilebytelabs:cmp-intent-launcher` | Typed Android-Intent builder + ActivityResult contracts + lifecycle-free SystemIntents | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-intent-launcher?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-intent-launcher) |
| [cmp-intent-launcher-compose](https://github.com/MobileByteLabs/KmpToolkit/wiki/Intent-Launcher-Compose) | `io.github.mobilebytelabs:cmp-intent-launcher-compose` | `@Composable rememberIntentLauncher()` + IntentPickerDialog / Sheet | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-intent-launcher-compose?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-intent-launcher-compose) |
| [cmp-app-intents](https://github.com/MobileByteLabs/KmpToolkit/wiki/App-Intents) | `io.github.mobilebytelabs:cmp-app-intents` | Declarative App Intents DSL — SiriKit Shortcuts + Android Assistant BIIs | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-app-intents?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-app-intents) |
| [cmp-app-intents-compose](https://github.com/MobileByteLabs/KmpToolkit/wiki/App-Intents-Compose) | `io.github.mobilebytelabs:cmp-app-intents-compose` | `@Composable AppIntentsRegistration` + Material 3 AppIntentsRegistry | [![](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-app-intents-compose?label=%20)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-app-intents-compose) |

Each module is completely independent — add only what your project needs.

## Installation

All modules ship together at the unified `kmptoolkit` version. Declare it once in your version catalog, then add only the libraries you need:

```toml
# gradle/libs.versions.toml
[versions]
kmptoolkit = "LATEST"  # see Maven Central badge in the Modules table above

[libraries]
cmp-clipboard       = { module = "io.github.mobilebytelabs:cmp-clipboard",       version.ref = "kmptoolkit" }
cmp-bubble          = { module = "io.github.mobilebytelabs:cmp-bubble",          version.ref = "kmptoolkit" }
cmp-toast           = { module = "io.github.mobilebytelabs:cmp-toast",           version.ref = "kmptoolkit" }
cmp-open-url        = { module = "io.github.mobilebytelabs:cmp-open-url",        version.ref = "kmptoolkit" }
cmp-deep-link       = { module = "io.github.mobilebytelabs:cmp-deep-link",       version.ref = "kmptoolkit" }
cmp-in-app-update   = { module = "io.github.mobilebytelabs:cmp-in-app-update",   version.ref = "kmptoolkit" }
cmp-remote-config   = { module = "io.github.mobilebytelabs:cmp-remote-config",   version.ref = "kmptoolkit" }
cmp-product-tickets = { module = "io.github.mobilebytelabs:cmp-product-tickets", version.ref = "kmptoolkit" }
cmp-network-monitor = { module = "io.github.mobilebytelabs:cmp-network-monitor", version.ref = "kmptoolkit" }
cmp-pdf-generator   = { module = "io.github.mobilebytelabs:cmp-pdf-generator",   version.ref = "kmptoolkit" }
cmp-firebase-analytics = { module = "io.github.mobilebytelabs:cmp-firebase-analytics", version.ref = "kmptoolkit" }
cmp-share           = { module = "io.github.mobilebytelabs:cmp-share",           version.ref = "kmptoolkit" }
cmp-intent-launcher = { module = "io.github.mobilebytelabs:cmp-intent-launcher", version.ref = "kmptoolkit" }
cmp-app-intents     = { module = "io.github.mobilebytelabs:cmp-app-intents",     version.ref = "kmptoolkit" }
# Compose adapter modules (optional)
cmp-share-compose            = { module = "io.github.mobilebytelabs:cmp-share-compose",            version.ref = "kmptoolkit" }
cmp-intent-launcher-compose  = { module = "io.github.mobilebytelabs:cmp-intent-launcher-compose",  version.ref = "kmptoolkit" }
cmp-app-intents-compose      = { module = "io.github.mobilebytelabs:cmp-app-intents-compose",      version.ref = "kmptoolkit" }
cmp-network-monitor-compose  = { module = "io.github.mobilebytelabs:cmp-network-monitor-compose",  version.ref = "kmptoolkit" }
```

Then reference any library from `commonMain` via the standard `implementation(libs.cmp.<name>)` pattern.

> 💡 Replace `LATEST` with the version shown in the [Maven Central badge](https://central.sonatype.com/search?q=g%3Aio.github.mobilebytelabs) at the top of this README — single source of truth, single line to bump when a new release ships.

## Platform Support

The toolkit targets every platform Kotlin Multiplatform supports. Per-module coverage varies — see each module's Wiki page for the authoritative matrix. Quick reference:

| Platform | Targets | Modules with full coverage |
|---|---|---|
| **Android** | `androidTarget` | All |
| **iOS** | `iosX64`, `iosArm64`, `iosSimulatorArm64` | All |
| **macOS** | `macosX64`, `macosArm64` | All except *bubble*, *product-tickets* |
| **JVM Desktop** | `jvm` | All |
| **JS** | `js` (browser + node) | All except *bubble*, *toast*, *in-app-update*, *product-tickets* |
| **wasmJs** | `wasmJs` (browser + node) | All except *bubble*, *toast*, *in-app-update*, *product-tickets* |
| **Linux** | `linuxX64`, `linuxArm64` | *clipboard*, *open-url*, *deep-link*, *in-app-update*, *remote-config*, *network-monitor*, *share*, *intent-launcher*, *app-intents* |
| **Windows** | `mingwX64` | Same as Linux |
| **tvOS / watchOS** | `tvosX64/Arm64/SimulatorArm64`, `watchosX64/Arm32/Arm64/SimulatorArm64/DeviceArm64` | *network-monitor*, *share* (limited), *app-intents* (manifest-only) |

📊 **Full per-module × per-target matrix:** [Wiki › Platform Matrix](https://github.com/MobileByteLabs/KmpToolkit/wiki/Platform-Matrix) · [docs/inter-app-comms/CAPABILITY_MATRIX.md](docs/inter-app-comms/CAPABILITY_MATRIX.md)

## Inter-App Comms Suite

Three modules ship together as the **inter-app communication** suite — all share the same Compose-MP adapter pattern (core + `-compose` adapter):

- **[cmp-share](https://github.com/MobileByteLabs/KmpToolkit/wiki/Share)** + **[cmp-share-compose](https://github.com/MobileByteLabs/KmpToolkit/wiki/Share-Compose)** — share sheets via OS-native dispatch
- **[cmp-intent-launcher](https://github.com/MobileByteLabs/KmpToolkit/wiki/Intent-Launcher)** + **[cmp-intent-launcher-compose](https://github.com/MobileByteLabs/KmpToolkit/wiki/Intent-Launcher-Compose)** — typed `Intent` builder + lifecycle-free `SystemIntents` (settings + save dialog) + Composable launchers
- **[cmp-app-intents](https://github.com/MobileByteLabs/KmpToolkit/wiki/App-Intents)** + **[cmp-app-intents-compose](https://github.com/MobileByteLabs/KmpToolkit/wiki/App-Intents-Compose)** — declarative App Intents DSL (SiriKit + Assistant BIIs)

Architecture docs: [docs/inter-app-comms/CAPABILITY_MATRIX.md](docs/inter-app-comms/CAPABILITY_MATRIX.md) · [ADR-09 (platform-impl-exits)](cmp-app-intents/adrs/ADR-09-platform-impl-exits.md)

## Sample App

[**`sample-toolkit`**](samples/sample-toolkit/) is the unified catalog app — every published `cmp-*` module wired in one Compose Multiplatform project. Pick a module from the catalog UI to launch its dedicated showcase. Same app, every platform:

```bash
# Desktop (JVM)
./gradlew :samples:sample-toolkit:composeApp:run

# Android
./gradlew :samples:sample-toolkit:androidApp:installDebug

# Web (browser — JS + wasmJs)
./gradlew :samples:sample-toolkit:composeApp:jsBrowserDevelopmentRun
./gradlew :samples:sample-toolkit:composeApp:wasmJsBrowserDevelopmentRun

# iOS — open samples/sample-toolkit/iosApp in Xcode
```

Per-module standalone samples (one per library) live alongside `sample-toolkit` under [`samples/`](samples/) — useful if you want to study a single module in isolation.

> **First-run note (web targets):** `jsBrowserDevelopmentRun` / `wasmJsBrowserDevelopmentRun` downloads ~50MB of Skiko WASM assets on a cold cache. Subsequent runs are fast. If you hit `OutOfMemoryError: GC overhead limit exceeded`, ensure `org.gradle.jvmargs=-Xmx4096M` in `gradle.properties` (the repo default).

## Contributing

New module template lives at [`cmp-library/`](cmp-library/) — copy + rename + add to `settings.gradle.kts` + ship a sample under `samples/`. Full walkthrough: [Wiki › Adding New Features](https://github.com/MobileByteLabs/KmpToolkit/wiki/Adding-New-Features) · [CONTRIBUTING.md](CONTRIBUTING.md).

## License

```
Copyright 2026 MobileByteLabs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0
```

See [LICENSE](LICENSE) for details.
