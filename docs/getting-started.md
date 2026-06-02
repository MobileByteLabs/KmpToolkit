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

All modules share a single `kmptoolkit.version` — pin them all to the same
release to keep transitive interop predictable.

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
- Open the `-javadoc.jar` for any module in IntelliJ / Android Studio for full
  KDoc API reference (it auto-mounts when you add the dependency).
