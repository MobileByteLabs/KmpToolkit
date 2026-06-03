---
title: "Getting started"
description: "Install kmp-toolkit modules from Maven Central, wire DI with Koin, and ship your first cross-platform feature."
---

# Getting started

## Install

Add Maven Central to your Gradle setup, then add the module(s) you want.

```kotlin
// build.gradle.kts (per consumer module)
repositories {
    mavenCentral()
}

dependencies {
    implementation("io.github.mobilebytelabs:cmp-share:3.5.1")
    implementation("io.github.mobilebytelabs:cmp-share-compose:3.5.1")
    // ...other cmp-* modules as needed
}
```

!!! tip "Pin all modules to the same version"
    All modules share a single `kmptoolkit.version`. Mixing versions
    across `cmp-*` artifacts works for compatible minor releases but is
    not guaranteed across majors — pin them all together to keep
    transitive interop predictable.

## Koin wiring (optional — for modules that ship a Koin companion)

```kotlin
import com.mobilebytelabs.kmptoolkit.observe.koin.observeKoinModule
import com.mobilebytelabs.kmptoolkit.observe.koin.FirebaseCrashlyticsAttributionHook
import com.mobilebytelabs.kmptoolkit.observe.koin.FirebaseAnalyticsHealthHook

startKoin {
    modules(
        observeKoinModule(
            hooks = listOf(
                FirebaseCrashlyticsAttributionHook(),
                FirebaseAnalyticsHealthHook(),
            ),
        ),
    )
}
```

## First-screen example

See [Cookbook → Share text from a button](cookbook/inter-app-comms/share-text.md)
for a minimal share button you can copy into a Compose screen.

## What's next

- Browse [Modules](modules/cmp-share.md) for per-module READMEs + DEVELOPMENT.md excerpts.
- Browse the [Cookbook](cookbook/inter-app-comms/index.md) for task-oriented recipes.

!!! info "API reference"
    Open the `-javadoc.jar` for any module in IntelliJ / Android Studio —
    full KDoc surfaces automatically when the dependency is added. No
    separate API site to bookmark.
