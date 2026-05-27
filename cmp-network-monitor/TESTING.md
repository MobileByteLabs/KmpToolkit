# cmp-network-monitor — Testing Strategy

> Last updated: 2026-05-27 (Phase 04 of `cmp-network-monitor-hardening` epic)

## Test sourceset map

`applyDefaultHierarchyTemplate()` is in effect, so every platform test sourceset auto-wires the moment you create files under `src/{target}Test/kotlin/...`. No `build.gradle.kts` edits required.

| Sourceset | What runs there | Toolchain on macOS dev host |
|---|---|---|
| `commonTest` | Pure-Kotlin contract + behavior tests. Inherited by EVERY platform test. | Always available |
| `jvmTest` | JVM-Desktop-specific (`JvmNetworkMonitor`, polling, HttpURLConnection HEAD). | JDK 11+ |
| `androidUnitTest` | Host JVM with Android SDK stubs. To actually exercise `ConnectivityManager`, needs Robolectric or instrumented test on device. | JDK 11+ (Robolectric NOT yet wired — see below) |
| `androidInstrumentedTest` | On-device / emulator tests. | Android Studio + emulator/device |
| `jsTest` | Auto-wired umbrella; routes to `jsBrowserTest` or `jsNodeTest` per `kotlin.js` target config. | Node.js (always present); Chrome for `jsBrowserTest`. |
| `jsBrowserTest` | **Headless Chrome** via Karma. `JsNetworkMonitor` requires `window.navigator.onLine`, so this is the ONLY runner that exercises it. | Chrome (auto-downloaded by Kotlin/JS) |
| `jsNodeTest` | **Node.js** — has NO `window` global. `JsNetworkMonitor` direct tests fail here with `ReferenceError: window is not defined`. Reserve for pure-Kotlin commonTest-style tests on JS. | Node.js |
| `wasmJsBrowserTest` | Headless browser. Same constraint as `jsBrowserTest`. | Chrome |
| `wasmJsNodeTest` | Node.js — `globalThis` works for some primitives but `WasmJsNetworkMonitor` uses `globalThis.addEventListener` which is browser-only. Prefer `wasmJsBrowserTest`. | Node.js |
| `iosX64Test` / `iosSimulatorArm64Test` | Kotlin/Native binary for iOS simulator. | Full Xcode.app + booted simulator |
| `macosArm64Test` / `macosX64Test` | Kotlin/Native binary for macOS, runs directly on host. | Full Xcode.app (links via `/usr/bin/xcrun xcodebuild`) |
| `tvosX64Test` / `tvosSimulatorArm64Test` | Kotlin/Native, tvOS simulator. | Full Xcode.app + tvOS simulator |
| `watchosSimulatorArm64Test` | Kotlin/Native, watchOS simulator. | Full Xcode.app + watchOS simulator |
| `appleTest` (parent) | Shared source visible to ALL of the above iOS/macOS/tvOS/watchOS test tasks. One file → many platforms. | Inherits per-target toolchain |
| `linuxX64Test` / `linuxArm64Test` | Kotlin/Native, Linux. | Linux toolchain (cross-compile from macOS partially supported; full link needs Linux host or Docker) |
| `mingwX64Test` | Kotlin/Native, Windows. | Windows toolchain |

## How to run

Per the v3.3.0 commit (which adds platform-specific test files for the first time):

```bash
# Commontest — runs across every platform via jvmTest as a fast proxy
./gradlew :cmp-network-monitor:jvmTest
# Phase 01 M-001 fix verified in real browser
./gradlew :cmp-network-monitor:jsBrowserTest
./gradlew :cmp-network-monitor:wasmJsBrowserTest
# Phase 04 JVM Desktop-specific
./gradlew :cmp-network-monitor:jvmTest --tests "*JvmNetworkMonitorCloseTest*"
# Phase 04 Apple-shared (needs full Xcode.app, not just CLT)
./gradlew :cmp-network-monitor:macosArm64Test --tests "*AppleNetworkMonitorCloseTest*"
./gradlew :cmp-network-monitor:iosSimulatorArm64Test --tests "*AppleNetworkMonitorCloseTest*"
# Full suite — slow, requires every toolchain on the matrix above
./gradlew :cmp-network-monitor:allTests
```

## Known blockers / environment caveats

### Sample-toolkit JS resolution (`PLAN cmp-network-monitor-hardening` Phase 04 T0)

The `:samples:sample-toolkit:composeApp` module formerly declared `js { browser() }` and `wasmJs { browser() }` targets while depending on `:cmp-toast`, `:cmp-clipboard`, `:cmp-in-app-update`, `:cmp-bubble` and other libraries that do not expose JS / WasmJs targets. The root `:kotlinNpmInstall` task aggregates NPM dependencies across every JS-targeting Gradle project, so the sample's unresolvable JS variant cascaded into a failure that blocked `:cmp-network-monitor:jsNodeTest` / `wasmJsNodeTest` / `jsBrowserTest` / `wasmJsBrowserTest` from running at all (build halted before any test pipeline was reached).

**Fix applied during Phase 04:** `samples/sample-toolkit/composeApp/build.gradle.kts` no longer declares `js { browser() }` / `wasmJs { browser() }`. The dependency list comment now accurately reflects the catalog's actual platforms (Android + iOS + Desktop). Restore those targets only when every `:cmp-*` module in `commonMain.dependencies` exposes a matching JS / WasmJs target.

### Apple-native test execution requires full Xcode.app

`macosArm64Test`, `iosSimulatorArm64Test`, etc. all link their final binary via `/usr/bin/xcrun xcodebuild`. Hosts with only Command Line Tools (`xcode-select -p` → `/Library/Developer/CommandLineTools`) will fail at `:linkDebugTestMacosArm64` with `xcrun: error: unable to find utility "xcodebuild"`. Fix: install Xcode.app from the App Store, then `sudo xcode-select -s /Applications/Xcode.app/Contents/Developer`. Test source COMPILATION still succeeds in CLT-only environments (the `.klib` is produced), so CI can validate compilation without a full Xcode install.

### Android instrumented testing requires Robolectric or a device

`androidUnitTest` runs on host JVM with `android.jar` STUBS — every Android-SDK method throws `RuntimeException: Stub!` by default. Real `ConnectivityManager` interaction needs either Robolectric (~12 MB transitive; not currently wired into `libs.versions.toml`) OR `androidInstrumentedTest` on an emulator/device. The structural fix for M-002 is validated via the pure-Kotlin pattern test `SeedGatePatternTest` in `commonTest` (which exercises the same `CompletableDeferred`-gated queueing pattern that `AndroidNetworkMonitor.kt` now uses).

### JS / WasmJs tests must use `*BrowserTest`, NOT `*NodeTest`

`JsNetworkMonitor` and `WasmJsNetworkMonitor` consume `window.navigator.onLine` / `globalThis.addEventListener` — these are browser-only. Node.js fires `ReferenceError: window is not defined` on construction. Use `:cmp-network-monitor:jsBrowserTest` / `:cmp-network-monitor:wasmJsBrowserTest` for any direct platform-monitor test.

## Phase 04 deliverables (v3.3.0)

- `src/jvmTest/.../JvmNetworkMonitorCloseTest.kt` — 3 tests: close-no-throw, close-idempotent, close-stops-polling-loop. Runs on host JVM.
- `src/appleTest/.../AppleNetworkMonitorCloseTest.kt` — 2 tests: close-no-throw, close-idempotent. Compiles cleanly; execution requires full Xcode.app per the caveat above.
- `src/jsTest/.../JsNetworkMonitorCloseRaceTest.kt` (added in Phase 01) — 4 tests, verified GREEN in `jsBrowserTest`.
- `src/wasmJsTest/.../WasmJsNetworkMonitorCloseRaceTest.kt` (added in Phase 01) — 3 tests, verified GREEN in `wasmJsBrowserTest`.
- `src/commonTest/.../SeedGatePatternTest.kt` (added in Phase 01) — 3 tests proving the M-002 fix's `CompletableDeferred` queue-not-drop pattern.
- `src/commonTest/.../NetworkMonitorExtensionsStateFlowTest.kt` (added in Phase 02) — 4 tests for the M-003 debounced StateFlow helpers.
- `src/commonTest/.../NetworkMonitorProviderVersionTest.kt` (added in Phase 03) — 5 tests for the M-004 version counter.

Test counts as of v3.3.0:
- `jvmTest`: 188 tests, 0 failures.
- `jsBrowserTest`: 4 tests, 0 failures.
- `wasmJsBrowserTest`: 3 tests, 0 failures.
- `macosArm64Test`: compiles clean (.klib produced); link requires Xcode.app.

## Out of scope for v3.3.0

- Robolectric integration for `androidUnitTest`.
- iOS/macOS/tvOS/watchOS native test EXECUTION (compilation verified).
- Linux / MinGW cross-compile from macOS.
- Compose UI tests for `cmp-network-monitor-compose` (`compose-ui-test` dep not wired).

Each of these is a future scoping decision, tracked outside this epic.
