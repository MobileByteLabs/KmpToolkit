# sample-pdf-generator

Demo app showing `cmp-pdf-generator` in 5 modes across Android, JVM Desktop, iOS, and Web.

> **Note:** This sample is scaffolded but the per-platform entry points (`androidApp/`, `iosApp/`, `desktopApp/`, `wasmJsApp/`) are not yet wired into `settings.gradle.kts`. Wire them up after first compile of the core module passes.

## Demo modes

| # | Mode | Input | Output |
|---|------|-------|--------|
| 1 | **Invoice** (`InvoiceTemplate`) | structured `InvoiceData` | ByteArray + Share |
| 2 | **Receipt** (`ReceiptTemplate`) | thermal-printer-style | Print |
| 3 | **Markdown** | TextField → MarkdownPdfAdapter | Save |
| 4 | **Composable snapshot** | Compose UI → bitmap → Image element | ByteArray |
| 5 | **DSL** | `pdf { page { … } }` | ByteArray + Save |

## Wiring (TODO)

When ready to enable, add to `kmp-toolkit/settings.gradle.kts`:

```kotlin
include(":samples:sample-pdf-generator:composeApp")
include(":samples:sample-pdf-generator:androidApp")
include(":samples:sample-pdf-generator:desktopApp")
include(":samples:sample-pdf-generator:wasmJsApp")
```

Then run:

```bash
./gradlew :samples:sample-pdf-generator:androidApp:installDebug
./gradlew :samples:sample-pdf-generator:desktopApp:run
./gradlew :samples:sample-pdf-generator:wasmJsApp:wasmJsBrowserDevelopmentRun
```

iOS sample is a separate Xcode project under `iosApp/` — open in Xcode and run.

## Status

- `composeApp/src/commonMain/kotlin/.../InvoiceFixture.kt` — sample data for invoice demo
- Other demo screens (`MarkdownDemo.kt`, `DslDemo.kt`, `ComposableSnapshotDemo.kt`) — to be implemented after core module compiles cleanly
- Sample build.gradle.kts files — copy from existing `samples/sample-clipboard/{composeApp,androidApp}/build.gradle.kts` and update artifact id + package
