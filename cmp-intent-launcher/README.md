# cmp-intent-launcher

Typed Android-Intent builder with cross-platform `ActivityResult` contracts for
Kotlin Multiplatform.

> **Status:** Experimental. All public APIs marked `@ExperimentalIntentLauncherApi`.
> Ships alongside the other `cmp-*` modules at the shared `kmptoolkit.version`.

## Features

- **Typed Intent DSL**: `intent { action(...); data(...); type(...); extra(...); result(...) }`
- **ActivityResult contracts**: `PickImage`, `PickDocument`, `PickMultipleImages`, `PickContact`, `Custom<R>`
- **Compose-scoped launcher**: `rememberIntentLauncher()` — lifecycle-bound to enclosing Composable
- **Android escape hatch**: `ComponentActivity.intentLauncher()` for non-Compose callers
- **Lifecycle-free `SystemIntents`** (v0.4+): `openAppSettings()` + `createDocument(name, mime)` callable from commonMain with no Composable / Activity wiring (Android Context auto-injected via ContentProvider; SAF round-trip via invisible proxy Activity)
- **Cross-platform fallback**: arbitrary intents route through `onUnsupported { }` lambda on non-Android

## `SystemIntents` — lifecycle-free entry points (v0.4+)

For operations that don't need an `ActivityResult` round-trip (settings deep-link)
or that should round-trip through a library-managed proxy (`createDocument` SAF),
use the `SystemIntents` expect object — callable from commonMain / DI graphs / non-UI
code:

```kotlin
import com.mobilebytelabs.kmptoolkit.intentlauncher.SystemIntents

suspend fun openSettings() {
    when (val result = SystemIntents.openAppSettings()) {
        is IntentResult.Ok -> { /* settings opened */ }
        is IntentResult.Failed -> { /* UnsupportedPlatform on JS/wasmJs/tvOS/watchOS */ }
        IntentResult.Cancelled -> { /* not used by openAppSettings */ }
    }
}

suspend fun saveReport() {
    val result = SystemIntents.createDocument(
        suggestedName = "report.pdf",
        mimeType = "application/pdf",
    )
    if (result is IntentResult.Ok) {
        val uri = result.data?.uri // platform-native URI to write to
    }
}
```

Per-platform behaviour:

| Method | Android | iOS | macOS | JVM | Linux | Windows | JS / wasmJs | tvOS / watchOS |
|---|---|---|---|---|---|---|---|---|
| `openAppSettings` | ✅ ACTION_APPLICATION_DETAILS_SETTINGS | ✅ openSettingsURLString | ✅ x-apple.systempreferences | ✅ OS-aware shell | ✅ gnome-control-center / kcmshell5 / xdg-open | ✅ ms-settings:appsfeatures | ❌ Unsupported | ❌ Unsupported |
| `createDocument` | ✅ Proxy Activity + SAF | ✅ UIDocumentPicker (ExportToService) | ✅ NSSavePanel | ✅ JFileChooser | ✅ zenity --save | ❌ Unsupported (Win32 needs cinterop) | ✅ showSaveFilePicker (Chromium only) | ❌ Unsupported |

**Android consumer note**: `IntentLauncherInitProvider` + `CreateDocumentProxyActivity`
are bundled in the library AndroidManifest via manifest-merger — no consumer-side
declaration needed.

## Platform support

| Platform | Behaviour | v0.1 status |
|----------|-----------|------------|
| Android | Full Intent surface via `Intent` + `androidx.activity.result.ActivityResultContracts` | ✅ Complete |
| JVM (Desktop) | AWT `FileDialog (LOAD)` for picker contracts; `withContext(Dispatchers.IO)` | ✅ Complete |
| JS | Hidden `<input type=file>` element + `URL.createObjectURL()` for picker contracts | ✅ Complete |
| iOS | Routes picker contracts through `onUnsupported` callback | ⚠️ PHPicker / UIDocumentPicker delegate wiring — v0.2 polish |
| macOS | Routes picker contracts through `onUnsupported` callback | ⚠️ NSOpenPanel wiring — v0.2 polish |
| wasmJs | Routes picker contracts through `onUnsupported` callback | ⚠️ DOM `<input>` via `@JsFun` — v0.2 polish |

> **Not targeted:** tvOS, watchOS, Linux native, mingwX64, wasmWasi.
> Per cmp-toolkit Tier-3 exclusion policy.

## Install

```kotlin
// build.gradle.kts
dependencies {
    val kmptoolkit = "3.2.13" // or latest
    implementation("io.github.mobilebytelabs:cmp-intent-launcher:$kmptoolkit")
}
```

## Quick start

### Pick an image (cross-platform)

```kotlin
@OptIn(ExperimentalIntentLauncherApi::class)
@Composable
fun PickImageButton(onPicked: (String) -> Unit) {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()

    Button(onClick = {
        scope.launch {
            val result = launcher.launch {
                result(ResultContracts.PickImage)
                type("image/*")
            }
            when (result) {
                is IntentResult.Ok -> result.data?.uri?.let(onPicked)
                IntentResult.Cancelled -> { /* user dismissed */ }
                is IntentResult.Failed -> showToast("Pick failed: ${result.cause}")
            }
        }
    }) { Text("Pick image") }
}
```

### Pick a PDF

```kotlin
@OptIn(ExperimentalIntentLauncherApi::class)
@Composable
fun PickPdfButton(onPicked: (String) -> Unit) {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val result = launcher.launch {
                action("android.intent.action.OPEN_DOCUMENT")
                type("application/pdf")
                result(ResultContracts.PickDocument)
            }
            (result as? IntentResult.Ok)?.data?.uri?.let(onPicked)
        }
    }) { Text("Attach PDF") }
}
```

### Custom Android action with extras

```kotlin
@OptIn(ExperimentalIntentLauncherApi::class)
@Composable
fun VendorBarcodeScanButton(onScanned: (String) -> Unit) {
    val launcher = rememberIntentLauncher()
    val scope = rememberCoroutineScope()
    Button(onClick = {
        scope.launch {
            val result = launcher.launch {
                action("com.vendor.SCAN")
                packageName("com.vendor.barcodescanner")
                extra("scan_mode", "QR_CODE")
                onUnsupported {
                    IntentResult.Failed(IntentError.UnsupportedPlatform)
                }
            }
            (result as? IntentResult.Ok)?.data?.uri?.let(onScanned)
        }
    }) { Text("Scan barcode") }
}
```

### Non-Compose Android (escape hatch)

```kotlin
@OptIn(ExperimentalIntentLauncherApi::class)
class LegacyFragment : Fragment() {
    private lateinit var launcher: IntentLauncher
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        launcher = (requireActivity() as ComponentActivity).intentLauncher()
    }
    fun pickAttachment() {
        viewLifecycleOwner.lifecycleScope.launch {
            launcher.launch { result(ResultContracts.PickDocument) }
        }
    }
}
```

## ⚠️ JS / wasmJs user-gesture requirement

The hidden `<input type=file>` element approach REQUIRES `.launch()` to be invoked
from within a user-gesture handler (Composable `onClick`, key press, touch). Browsers
reject programmatic `.click()` outside a user-activation context.

```kotlin
// ✅ CORRECT
Button(onClick = { scope.launch { launcher.launch { result(ResultContracts.PickImage) } } })

// ❌ WRONG — returns IntentResult.Failed(UserGestureMissing)
LaunchedEffect(Unit) { launcher.launch { result(ResultContracts.PickImage) } }
```

## Coexists with cmp-open-url

| Use case | Module |
|---|---|
| "Open this URL" (`https://`, `mailto:`, `tel:`, `geo:`) | `cmp-open-url` — `openUrl()` / `openWithApp(url, AppHint)` |
| "Open the file picker, await result" | `cmp-intent-launcher` — `launcher.launch { result(ResultContracts.PickDocument) }` |
| "Send custom Android Intent with extras" | `cmp-intent-launcher` — full Intent builder |

cmp-open-url handles URL opening (no result); cmp-intent-launcher handles richer Intent
shapes including await-result. See ADR-02 in idea-layer.

## See also

- SPEC: [idea-layer/modules/cmp-intent-launcher/SPEC.md](../../../idea-layer/modules/cmp-intent-launcher/SPEC.md)
- API: [idea-layer/modules/cmp-intent-launcher/API.md](../../../idea-layer/modules/cmp-intent-launcher/API.md)
- ADRs: [idea-layer/modules/cmp-intent-launcher/adrs/](../../../idea-layer/modules/cmp-intent-launcher/adrs/)
- Sibling modules: [cmp-share](../cmp-share/), [cmp-app-intents](../cmp-app-intents/), [cmp-open-url](../cmp-open-url/)
