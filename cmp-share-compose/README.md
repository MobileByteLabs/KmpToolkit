# cmp-share-compose

Compose Multiplatform extensions for [`cmp-share`](../cmp-share/) — provides `rememberShareLauncher()` factory + Material 3 styled `ShareSheet()` and `ShareButton()` opinionated UX components.

## Why this module exists

Authored as part of `inter-app-comms-compose-completeness` (v0.4 → v1.0 epic). Parallel to the existing `cmp-intent-launcher-compose` precedent. Lets Compose-MP consumers drop in styled share UX without wiring imperative APIs from scratch.

## Targets (9 — Compose-MP supported)

| Target | Status |
|---|---|
| `androidLibrary` | ✓ |
| `jvm` (Desktop) | ✓ |
| `iosX64` / `iosArm64` / `iosSimulatorArm64` | ✓ |
| `macosX64` / `macosArm64` | ✓ |
| `js` (browser) | ✓ |
| `wasmJs` (browser) | ✓ |

Consumers wanting share on tvOS/watchOS/Linux/mingw use core `cmp-share`'s imperative `Share` object directly (Compose-MP doesn't support those targets).

## Coordinates

```kotlin
implementation("io.github.mobilebytelabs:cmp-share:<version>")
implementation("io.github.mobilebytelabs:cmp-share-compose:<version>")
```

## Usage

### `rememberShareLauncher()` — low-level factory

```kotlin
import com.mobilebytelabs.kmptoolkit.share.SharePayload
import com.mobilebytelabs.kmptoolkit.share.compose.rememberShareLauncher

@Composable
fun ShareScreen() {
    val share = rememberShareLauncher()
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val result = share.share(SharePayload.Url("https://github.com/MobileByteLabs/KmpToolkit"))
            // handle result
        }
    }) { Text("Share link") }
}
```

### `ShareSheet()` — Material 3 modal bottom sheet

```kotlin
import com.mobilebytelabs.kmptoolkit.share.compose.ShareSheet

@Composable
fun ShareLinkButton() {
    var showSheet by remember { mutableStateOf(false) }
    if (showSheet) {
        ShareSheet(
            payload = SharePayload.Url("https://example.com"),
            onDismiss = { showSheet = false },
            onResult = { /* handle ShareResult */ },
        )
    }
    Button(onClick = { showSheet = true }) { Text("Share") }
}
```

### `ShareButton()` — Material 3 icon button

```kotlin
import com.mobilebytelabs.kmptoolkit.share.compose.ShareButton

@Composable
fun ToolbarShare() {
    ShareButton(
        payload = SharePayload.Text("Hello from cmp-share-compose"),
        onResult = { /* handle ShareResult */ },
    )
}
```

## Dependencies

Transitively pulls:
- `org.jetbrains.compose.material3:material3`
- `org.jetbrains.compose.material:material-icons-extended` (~2 MB on Android — strippable via R8 if unused)

Consumers who want zero-Material-3-dep usage should call core `cmp-share`'s `Share.share()` directly.

## Versioning + API stability

Ships with the shared `kmptoolkit.version` (currently `3.3.2`; this module lands in the next bump). `@ExperimentalShareApi` marker is retained at first ship — marker drop is a future toolkit-release decision (separate from this module's introduction).
