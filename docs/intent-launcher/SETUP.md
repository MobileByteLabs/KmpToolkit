# cmp-intent-launcher — Integration Guide

> `io.github.mobilebytelabs:cmp-intent-launcher:3.2.11`

`cmp-intent-launcher` is **zero-configuration for Gradle** — 4 steps: add dependency, opt in,
wire the Android Activity, use the launcher in Compose.

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
cmp-intent-launcher = "3.2.11"

[libraries]
cmp-intent-launcher = { module = "io.github.mobilebytelabs:cmp-intent-launcher", version.ref = "cmp-intent-launcher" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.cmp.intent.launcher)
}
```

---

## Step 2 — Opt in to the Experimental Marker

### Per call-site (recommended)

```kotlin
@OptIn(ExperimentalIntentLauncherApi::class)
@Composable
fun MyScreen() {
    val launcher = rememberIntentLauncher()
    // ...
}
```

### Project-wide (all modules)

```kotlin
// shared/build.gradle.kts
kotlin {
    compilerOptions {
        freeCompilerArgs.addAll(
            "-opt-in=com.mobilebytelabs.kmptoolkit.intentlauncher.ExperimentalIntentLauncherApi"
        )
    }
}
```

---

## Step 3 — Use the API

Call `rememberIntentLauncher()` at Compose composition time, then `launch {}` inside a coroutine:

```kotlin
@OptIn(ExperimentalIntentLauncherApi::class)
@Composable
fun OpenFilePicker() {
    val scope   = rememberCoroutineScope()
    val launcher = rememberIntentLauncher()

    Button(onClick = {
        scope.launch {
            val result = launcher.launch {
                action = ResultContracts.GET_CONTENT
                type = "application/pdf"
            }
            when (result) {
                is IntentResult.Ok        -> println("URI: ${result.data?.uri}")
                is IntentResult.Cancelled -> { /* user dismissed */ }
                is IntentResult.Failed    -> logError(result.cause)
            }
        }
    }) {
        Text("Open PDF")
    }
}
```

### Sending an Intent with extras

```kotlin
val result = launcher.launch {
    action = "com.example.ACTION_DO_SOMETHING"
    extra("KEY_PARAM", "value")
    targetPackage("com.example.targetapp")
}
```

---

## Step 4 — Android Activity Wiring

`rememberIntentLauncher()` registers an `ActivityResultLauncher` during Compose composition.
It **must be called at the top level of a Composable** — not inside a `LaunchedEffect`,
`onClick`, or conditional block.

On Android, make sure your host Activity extends `ComponentActivity` (or `AppCompatActivity`):

```kotlin
// androidApp/src/main/kotlin/MainActivity.kt
class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContent {
            App()   // rememberIntentLauncher() called inside here — OK
        }
    }
}
```

> No additional manifest permissions are required unless your specific intent action
> requires them (e.g. `READ_MEDIA_IMAGES` for image picking on API 33+).

---

## Platform Notes

| Platform | Behaviour |
|----------|-----------|
| Android  | Full `ActivityResultLauncher` — awaitable result with `IntentData` (URI, extras) |
| iOS / macOS | `onUnsupported` → `IntentResult.Failed(IntentError.UnsupportedPlatform)`; PHPicker/NSOpenPanel planned for v0.2 |
| JVM | `Desktop.open()` / `browse()` — returns `IntentResult.Ok(null)` (no callback) |
| JS / wasmJs | `onUnsupported` → `IntentResult.Failed(IntentError.UnsupportedPlatform)` |

---

## AI-Assisted Setup

```
/sync-intent-launcher           # Verify Gradle dependency (only gate needed)
/sync-intent-launcher --check   # Dry run — show status, no writes
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
