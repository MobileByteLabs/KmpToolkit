# cmp-firebase Gradle Plugin

Gradle plugin that applies the build-side setup [`cmp-firebase`](../cmp-firebase/README.md) needs
in a **Kotlin Multiplatform** app, so Firebase on iOS works without hand-configuring the parts that
are easy to get silently wrong.

[![Gradle Plugin Portal](https://img.shields.io/gradle-plugin-portal/v/io.github.mobilebytelabs.firebase)](https://plugins.gradle.org/plugin/io.github.mobilebytelabs.firebase)
[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/cmp-firebase-gradle-plugin)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-firebase-gradle-plugin)
[![Kotlin](https://img.shields.io/badge/Kotlin-2.4.20-blue.svg?logo=kotlin)](https://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-green.svg)](https://www.apache.org/licenses/LICENSE-2.0)

## Why this plugin exists

A published Maven artifact's `build.gradle.kts` runs only when building *that artifact* — never in
the consumer's build. So `cmp-firebase` has no way to check how your app configures its Apple
frameworks, and one particular misconfiguration is invisible until runtime:

> **Firebase's SwiftPM products are static libraries.** If your shared KMP framework is *dynamic*
> (the Gradle default), the app compiles, links, and then **crashes at launch**. The crash points
> at Firebase internals, not at the framework setting that caused it.

A Gradle plugin *does* run in the consumer's build, which is the only place that check can happen.
That is the whole reason this module exists.

## Installation

```kotlin
// settings.gradle.kts — mavenCentral() must be in pluginManagement (most KMP projects have it)
pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
```

```kotlin
// shared/build.gradle.kts — your KMP module
plugins {
    kotlin("multiplatform")
    id("io.github.mobilebytelabs.firebase") version "<version>"
}
```

That single line replaces the dependency declaration **and** the build setup. Use the same version
as the `cmp-firebase` library — they are released together and the plugin adds the matching
library version for you.

## What it does

### 1. Adds the `cmp-firebase` dependency

At the plugin's **own** version, into `commonMain`, so the library and the build contract enforced
for it can never drift apart:

```kotlin
// applied automatically — you do not write this
commonMain.dependencies {
    implementation("io.github.mobilebytelabs:cmp-firebase:<same version as the plugin>")
}
```

If your project already declares `cmp-firebase`, the plugin logs and steps aside — a hand-pinned
version is never duplicated or overridden.

Opt out entirely when the dependency comes from a platform/BOM:

```kotlin
cmpFirebase {
    addDependency = false
}
```

### 2. Forces `isStatic = true` on every Apple framework

```
> Configure project :shared
cmp-firebase: forcing isStatic=true on iosArm64 framework 'Shared' —
Firebase's SwiftPM products are static libraries and a dynamic framework crashes at runtime.
```

Applied through `configureEach`, so frameworks declared *after* the plugin — the normal case, since
plugins go at the top of the file and targets below — are covered too. It logs only when it
actually changes something, rather than mutating your build silently.

### 3. Enforces the Kotlin version floor

Requires **Kotlin 2.4.20+**, failing at *configuration* time with an explanation:

```
cmp-firebase requires Kotlin 2.4.20 or newer — this build uses 2.1.0.

GitLive Firebase 3.x links the native firebase-ios-sdk via SwiftPM, and the transitive
resolution that carries it across the Maven boundary is a Kotlin 2.4 feature. On 2.1.0 no
SwiftPM package is generated, nothing resolves the native SDK, and the Apple build fails at
link time with:

    ld: framework 'FirebaseCore' not found

Fix: upgrade Kotlin to 2.4.20+, or provision the Firebase Apple frameworks yourself
(e.g. CocoaPods), which cmp-firebase does not support.
```

Without this you get the raw linker error, which names `FirebaseCore` and gives no hint that the
real cause is your Kotlin version.

## Configuration

| Property | Type | Default | Description |
|---|---|---|---|
| `addDependency` | `Boolean` | `true` | Whether the plugin adds `cmp-firebase` to `commonMain` at its own version. Set `false` when the dependency is declared by hand or supplied by a platform/BOM. |

```kotlin
cmpFirebase {
    addDependency = false
}
```

## Requirements

| | |
|---|---|
| Kotlin | **2.4.20+** (enforced — see above) |
| Gradle | 8.0+ |
| Kotlin plugin | `org.jetbrains.kotlin.multiplatform` (the plugin no-ops without it) |
| Apple deployment target | iOS 15.0 / macOS 10.15 / tvOS 15.0 — the `firebase-ios-sdk` 12.x floor |

Plugin ordering does not matter: both checks hang off `plugins.withId`, so applying this before or
after the Kotlin plugin works.

## What it does *not* do

Deliberately, because these live in your Xcode project rather than your Gradle build:

- **The `embedAndSignAppleFrameworkForXcode` run-script build phase.** Every KMP app with an iOS
  target needs this, Firebase or not.
- **The Xcode deployment target.** Set it to iOS 15.0+ yourself.
- **`GoogleService-Info.plist` / `FirebaseApp.configure()`.** Both are optional anyway — see
  [`cmp-firebase`'s programmatic init](../cmp-firebase/README.md#one-commonmain-init-no-native-config-files),
  which configures every platform from one `commonMain` call.

## Full setup guide

The plugin covers the build side. For Firebase keys, per-platform setup, analytics and crash
reporting APIs, see:

- **[cmp-firebase README](../cmp-firebase/README.md)** — module overview and API
- **[docs/firebase/SETUP.md](../docs/firebase/SETUP.md)** — step-by-step per-platform setup
- **[KmpToolkit wiki](https://github.com/MobileByteLabs/KmpToolkit/wiki)** — the whole toolkit

## Troubleshooting

**`ld: framework 'FirebaseCore' not found`** — Kotlin is below 2.4.20 (the plugin now fails earlier
with a clearer message), or the Apple SwiftPM resolution has not run. Confirm your Kotlin version
and that the `embedAndSignAppleFrameworkForXcode` build phase exists in Xcode.

**App crashes at launch on iOS, inside Firebase** — the shared framework is dynamic. Applying this
plugin fixes it; if you set `isStatic` yourself, make sure it is `true` for every Apple target.

**`Plugin [id: 'io.github.mobilebytelabs.firebase'] was not found`** — add `mavenCentral()` to
`pluginManagement.repositories` in `settings.gradle.kts`, or use the Gradle Plugin Portal.

## License

```
Copyright 2026 MobileByteLabs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    https://www.apache.org/licenses/LICENSE-2.0
```
