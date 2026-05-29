# cmp-app-intents-compose

Compose Multiplatform extensions for [`cmp-app-intents`](../cmp-app-intents/) — `AppIntentsRegistration()` Composable for lifecycle-bound registration + `AppIntentsRegistry()` Material 3 dev/debug invocation UI.

## Why this module exists

Authored as part of `inter-app-comms-compose-completeness` (v0.4 → v1.0 epic) per S0.B PIVOT-aware Phase 4 design. Parallel to `cmp-share-compose` + existing `cmp-intent-launcher-compose`. Closes the gap where v0.3-alpha shipped `cmp-app-intents` core but no Compose adapter for Composable-friendly registration + debug UX.

## Targets (9 — Compose-MP supported)

Standard 9-target shape: `androidLibrary`, `jvm`, `iosX64`/`iosArm64`/`iosSimulatorArm64`, `macosX64`/`macosArm64`, `js`, `wasmJs`.

## Coordinates

```kotlin
implementation("io.github.mobilebytelabs:cmp-app-intents:<version>")
implementation("io.github.mobilebytelabs:cmp-app-intents-compose:<version>")
```

## Usage

### `AppIntentsRegistration(config)` — lifecycle-bound registration

```kotlin
import com.mobilebytelabs.kmptoolkit.appintents.appIntents
import com.mobilebytelabs.kmptoolkit.appintents.compose.AppIntentsRegistration

@Composable
fun MyApp() {
    val config = remember {
        appIntents {
            intent("openHome") {
                title = "Open Home"
                description = "Navigate to the home screen"
                perform { _ -> AppIntentResult.Done }
            }
        }
    }
    AppIntentsRegistration(config)
    // ... rest of your app
}
```

### `AppIntentsRegistry()` — Material 3 debug UI

```kotlin
import com.mobilebytelabs.kmptoolkit.appintents.compose.AppIntentsRegistry

@Composable
fun DebugScreen() {
    AppIntentsRegistration(config)
    AppIntentsRegistry(
        modifier = Modifier.fillMaxSize(),
        onInvokeResult = { id, result -> println("$id → $result") },
    )
}
```

## Dependencies

Transitively pulls Material 3 + materialIconsExtended (same as cmp-share-compose).

## Versioning + API stability

Ships with the shared `kmptoolkit.version` (currently `3.3.2`; this module lands in the next bump). `@ExperimentalAppIntentsApi` marker is retained at first ship — marker drop is a future toolkit-release decision.
