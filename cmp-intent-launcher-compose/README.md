# cmp-intent-launcher-compose

Compose Multiplatform extensions for [`cmp-intent-launcher`](../cmp-intent-launcher/) — provides `@Composable rememberIntentLauncher()`.

## Why this module exists

Extracted from `cmp-intent-launcher` during `inter-app-comms-real-native-impls` Phase 1 (2026-05-28). The Compose Compiler Gradle plugin is module-level and requires `compose.runtime` on the classpath for every target — that blocked the core module from reaching tvOS/watchOS/Linux/mingw. The split:
- `cmp-intent-launcher` (core, Compose-free) — 19 KMP targets
- `cmp-intent-launcher-compose` (this module, Compose-bearing) — 9 targets

## Targets (9 — Compose-MP supported)

| Target | Status |
|---|---|
| `androidLibrary` | ✓ |
| `jvm` (Desktop) | ✓ |
| `iosX64` / `iosArm64` / `iosSimulatorArm64` | ✓ |
| `macosX64` / `macosArm64` | ✓ |
| `js` (browser) | ✓ |
| `wasmJs` (browser) | ✓ |

## Coordinates

```kotlin
implementation("io.github.mobilebytelabs:cmp-intent-launcher:<version>")
implementation("io.github.mobilebytelabs:cmp-intent-launcher-compose:<version>")
```

## Usage

```kotlin
import com.mobilebytelabs.kmptoolkit.intentlauncher.ResultContracts
import com.mobilebytelabs.kmptoolkit.intentlauncher.compose.rememberIntentLauncher

@Composable
fun PickImageScreen() {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val result = launcher.launch {
                result(ResultContracts.PickImage)
                type("image/*")
            }
            // handle result
        }
    }) { Text("Pick an image") }
}
```

## Migration from v0.2

```diff
- implementation("io.github.mobilebytelabs:cmp-intent-launcher:<v0.2>")
+ implementation("io.github.mobilebytelabs:cmp-intent-launcher:<v0.3>")
+ implementation("io.github.mobilebytelabs:cmp-intent-launcher-compose:<v0.3>")

- import com.mobilebytelabs.kmptoolkit.intentlauncher.rememberIntentLauncher
+ import com.mobilebytelabs.kmptoolkit.intentlauncher.compose.rememberIntentLauncher
```

No call-site changes needed beyond the import path. All other APIs (`IntentBuilder`, `ResultContracts`, `IntentResult`, `IntentError`, `IntentLauncher`) stay in the core module under `com.mobilebytelabs.kmptoolkit.intentlauncher.*`.

## Non-Compose alternative

For non-Compose Android: use `ComponentActivity.intentLauncher()` extension from the core module (`com.mobilebytelabs.kmptoolkit.intentlauncher.intentLauncher`).
