# Changelog

All notable changes to this project will be documented in this file.

The format is based on [Keep a Changelog](https://keepachangelog.com/en/1.0.0/),
and this project adheres to [Semantic Versioning](https://semver.org/spec/v2.0.0.html).

---

## [Unreleased]

### Changed — **BREAKING** `cmp-observe` is now dependency-free; Firebase hooks moved to `cmp-observe-firebase`

`cmp-observe` shipped the hook interface AND three Firebase hook implementations in one artifact.
The Firebase SDKs support 4 targets, which capped `cmp-observe` at 15 — so no `cmp-*` module could
depend on it from `commonMain` without capping its own 21-target matrix. That is why library
self-reporting sat at 1 of 23 modules for four months.

`cmp-observe` is now pure stdlib on all **21 targets** and every `cmp-*` module reports itself.

**Migration** — if you register any Firebase hook, add one dependency:

```kotlin
// build.gradle.kts
implementation("io.github.mobilebytelabs:cmp-observe-firebase:<version>")
```

Imports are unchanged (`io.github.mobilebytelabs.kmptoolkit.observe.hooks.*`); the classes moved
artifact, not package. Nothing to change if you only use `LibraryObservation` + your own hooks.

Affected: `FirebaseAnalyticsHealthHook`, `FirebaseCrashlyticsAttributionHook`,
`FirebasePerformanceHook`.

### Fixed — samples compile again: lifecycle moved to the androidx KMP coordinates

`org.jetbrains.androidx.lifecycle:*` became an **empty relocation shim** at `2.11.0-beta01` (its
common metadata holds only `root_package/0_.knm`), and Compose 1.12.0's constraints force-upgraded
the declared `2.9.6` to exactly that shim. `androidx.lifecycle.viewmodel.compose.viewModel` therefore
stopped resolving in `commonMain` — `sample-clipboard` had been failing to compile, unnoticed because
`pr-check` filters `module-pattern: 'cmp-'` and never builds the samples.

androidx unified its KMP publication at 2.11.0, so the catalog now points at `androidx.lifecycle:*`
(which ships real `-metadata` jars) at `2.11.0`. No published `cmp-*` module depends on lifecycle;
this affects the samples only.

### Changed — `cmp-library` 16 → 21 targets, `cmp-observe-koin` 15 → 20

`cmp-library` (the template every new module is copied from) had a `watchOS Targets` section header
with nothing under it, so it shipped 16 of the standard 21 and each copy started five short.
`cmp-observe-koin` gains the same five; `wasmWasi` stays blocked by koin-core. See
[TARGET_MATRIX.md](TARGET_MATRIX.md) §5.

### Added — a README for every module

All 27 `cmp-*` modules now ship a `README.md` (13 were missing one), each documenting the real public
API and referencing `TARGET_MATRIX.md` as the target SoT. A stale duplicate of the module template's
README that had been copied into `cmp-open-url` is removed.

### Added — every `cmp-*` module now reports its own lifecycle

All 15 headless modules call `observeInit` at initialisation and `observeLifecycle` on their primary
operations, so a consumer registering one hook sees init, failure and operation events from the whole
toolkit. Events carry operation **shape**, never content: a clipboard copy reports success but not
the text, a toast reports duration and style but not the message, a PDF reports page count and result
class but not the document. A new CI gate (`.github/scripts/assert-observability-wired.sh`) fails any
module that generates `CmpMetadata` but never reports.


### Added — `cmp-app-review`: one commonMain call, the right review route on every target

New module. Call it from shared code; it resolves per platform with no `expect`/`actual` and no
branching in consumer code.

```kotlin
// once at startup, in commonMain
AppReview.configure(
    StoreListing(appStoreId = "1234567890", webUrl = "https://example.com/app"),
)

// anywhere
AppReview.requestReview()
```

| Target | Route |
|---|---|
| Android | Play In-App Review → `market://details?id=…` |
| iOS / macOS | `SKStoreReviewController` → store review composer |
| tvOS / watchOS | store listing (watchOS hands it to the paired iPhone) |
| Windows | `ms-windows-store://review/` → web listing |
| JVM · Linux · JS · wasmJs | web listing in the browser |
| wasmWasi | `NoStoreConfigured` |

**Where no API exists, the fallback is real, not a no-op.** Supply the stores you publish to via
`StoreListing` and the listing opens through `cmp-open-url`. `playStorePackage` is optional — Android
reads the running app's package.

**`NativeFlowRequested` promises only that the ask reached the OS.** Neither Play nor StoreKit reports
whether a prompt rendered or what the user did, and both silently no-op once their quota is spent.
Never gate a reward on it. `openStoreListing()` is the route that always navigates.

**21 targets**, matching `cmp-open-url`. Ships `FakeAppReviewManager` in the main artifact, a
parameterless `appReviewModule` Koin binding, and an `AppReviewCapabilities` descriptor so a caller can
branch before asking.

Notes:
- **Binds like every other platform manager** — `single<AppReviewManager> { AppReviewManagerImpl() }`.
  No Activity, no configuration, no arguments, so it drops straight into an existing platform module
  next to `UrlLauncherImpl()` / `ShareManagerImpl()`. The Android Activity that Play's flow needs is
  tracked by the module itself.
- **Store ids are configured once, not injected.** `AppReview.configure(StoreListing(...))` at startup;
  every injected manager resolves the listing at CALL time, so the DI graph may legitimately be built
  before configuration runs. Capturing it in the constructor would leave a fork that registers the
  module early with a silently empty listing.
- **First cmp-to-cmp dependency in the toolkit** — `api(project(":cmp-open-url"))`, because the
  fallback *is* URL opening. The `UrlLauncher` is injectable, so a test need not open anything.
- Android needs no setup: a `ContentProvider` captures the application context and an
  `ActivityLifecycleCallbacks` tracks the Activity Play's flow launches into (the two patterns
  already used by `cmp-share`/`cmp-intent-launcher` and `cmp-deep-link`). The Activity is held weakly
  and cleared on pause.
- Play In-App Review only works in Play-installed builds; sideloaded and debug builds fail and fall
  through to `market://`.

### Changed — ⚠️ BREAKING: cmp-product-tickets split into headless core + `-compose`

The same split E2 applied to `cmp-remote-config`, for the same reason. `cmp-product-tickets` is now
**headless at 15 KMP targets** (was 7); the screens, navigation graph and `ProductTicketsViewModel`
moved to **`cmp-product-tickets-compose`**.

**Migration — add one dependency; nothing else changes.** Packages and class names are identical, and
`productTicketsModule` keeps its exact name and effect:

```kotlin
implementation(libs.cmp.product.tickets)          // unchanged — models, repository, Supabase service
implementation(libs.cmp.product.tickets.compose)  // ADD THIS for the screens / nav / productTicketsModule
```

Verified as a relocation: the union of both BCV baselines keeps **all 32 previous public symbols** and
adds one (`productTicketsDataModule`, below).

**Why.** 7 of the module's 13 files needed no renderer, but material3, navigation-compose and
koin-compose-viewmodel in `commonMain` pinned the library to the 7 Compose-Multiplatform targets, so
filing a ticket from a server, CLI or background worker was impossible. The headless core now builds
for android, jvm, js, wasmJs, iosArm64, iosSimulatorArm64, macosArm64, **linuxX64, mingwX64, tvOS ×3
and watchOS ×3**. As in E2, a Compose-only source set inside one module is not an option — the Compose
compiler plugin applies to every compilation and fails wherever the runtime is absent.

**`productTicketsDataModule` (new, in the core).** The old `productTicketsModule` registered the
service and repository *and* the ViewModel. Rather than move it wholesale — which would have forced
`ProductTicketsService`, `ProductTicketsServiceImpl` and the Ktor client from `internal` to public —
the headless bindings now live in `productTicketsDataModule`, and `productTicketsModule` (in the
Compose artifact) is `module { includes(productTicketsDataModule); viewModelOf(...) }`. Installing
`productTicketsModule` gives the identical graph it always did; a headless host can install
`productTicketsDataModule` alone.

**Targets deliberately absent, measured:** `iosX64`/`macosX64` (Compose 1.12.0 publishes no artifact),
`linuxArm64`/`watchosArm32`/`watchosDeviceArm64` (`postgrest-kt` 3.2.6 publishes none for those three),
`wasmWasi` (koin-core has no wasmWasi artifact).

**Fixed in passing — a runtime bug the split exposed.** `cmp-product-tickets-compose` initially lacked
the `kotlinx-serialization` plugin. The `@Serializable` navigation routes still *compiled* (the
annotation alone is inert without the plugin) but generated no serializers, so type-safe
`navigate(CreateTicketRoute(...))` and `toRoute<TicketDetailRoute>()` would have failed at **runtime**.
Caught by the BCV baseline diff — the generated `$$serializer` / `$Companion` entries vanished — not by
the build. The plugin is now applied, with that reasoning recorded next to it.

### Added — cmp-observe: a recording hook double and a way to isolate hook tests

- **`FakeLibraryObservationHook`** — shipped in the main artifact under `testing/`, like
  `FakeNetworkMonitor` and `FakeAnalyticsHelper`. Records each callback as a structured
  `Event` (`InitStart` / `InitComplete` / `InitFailure` / `Lifecycle` / `Close`), so `meta`, the
  lifecycle `payload` and the failure `throwable` are all assertable:

  ```kotlin
  val hook = FakeLibraryObservationHook()
  LibraryObservation.register(hook)
  myLibrary.initialize()

  assertEquals("cmp-share", hook.initCompletions.single().meta.name)
  assertEquals("whatsapp", hook.payloadOf("share_invoked")?.get("target"))
  ```

  Pass `failWith` to model a hook that throws — the other half of the documented contract — and
  assert your own code still runs: `FakeLibraryObservationHook(failWith = RuntimeException("boom"))`.
  It counts reaches in `failedCallCount`, which a hand-rolled throwing stub cannot tell you.

- **`resetLibraryObservation()`** — unregisters every hook, for `@BeforeTest` / `@AfterTest`.
  `LibraryObservation` is a process-wide `object` and its `reset()` has always been **`internal`**:
  it isolated this library's own suite and was unreachable from any consumer, so a consumer's tests
  leaked registrations into each other and could not assert event counts at all. This exposes exactly
  that one capability, as a function in `testing/` rather than by making production `reset()` public.

Both are strictly additive — the BCV baseline gains 91 lines and removes nothing.

Dogfooded: this module's own `LibraryObservationTest` now uses both, dropping a 17-line hand-rolled
recorder and a five-override anonymous throwing hook. That surfaced a real gap — the old recorder
flattened every callback to `"lifecycle:<module>:<event>"`, so the test named *"notifyLifecycle
propagates event name + payload"* never asserted the payload, and `notifyInitFailure` could only check
the throwable's message rather than its identity. Both now assert what their names claim.

### Changed — ⚠️ BREAKING: cmp-remote-config split into headless core + `-compose`

`cmp-remote-config` is now **headless and builds for 15 KMP targets** (was 7). Its Compose surface
moved to a new artifact, **`cmp-remote-config-compose`**.

**Migration — add one dependency; nothing else changes.** Every package, class and function keeps its
exact name and fully-qualified package, so there are no import rewrites:

```kotlin
implementation(libs.cmp.remote.config)          // unchanged — evaluator, flags, model, network
implementation(libs.cmp.remote.config.compose)  // ADD THIS if you render any remote-config UI
```

You need the new artifact if you use `RemoteConfigHost`, `RemoteConfigBanner`, `RemoteConfigDialog`,
`RemoteConfigBottomSheet`, `RemoteConfigFullScreen`, `RemoteConfigViewModel`, `RemoteConfigState`,
the dynamic UI renderer, or the `Module.remoteConfig { }` Koin DSL. If you only evaluate flags, keep
the single core dependency and gain the new targets for free.

Verified as a pure relocation: the union of both modules' BCV baselines has the **same 45 public
symbols** as the old single baseline — nothing was removed from the toolkit, only moved.

**Why.** 12 of the module's 20 files needed no renderer, yet material3, navigation-compose and Coil
in `commonMain` pinned the whole library to the 7 Compose-Multiplatform targets — so a server or CLI
could not evaluate a flag at all. The headless core now builds for android, jvm, js, wasmJs,
iosArm64, iosSimulatorArm64, macosArm64, **linuxX64, mingwX64, tvOS ×3 and watchOS ×3**.

This could not be done inside one module with a Compose-only source set, which was tried first: the
Compose compiler plugin applies to *every* compilation and fails with "The Compose Compiler requires
the Compose Runtime to be on the class path" on any target lacking the runtime. That is the
structural reason this toolkit ships `X` / `X-compose` pairs.

**Targets deliberately absent, measured rather than assumed:** `iosX64`/`macosX64` (Compose 1.12.0
publishes no artifact for either), `linuxArm64`/`watchosArm32`/`watchosDeviceArm64` (`postgrest-kt`
3.2.6 publishes no artifact for exactly those three), `wasmWasi` (koin-core has no wasmWasi artifact).

**Also in this change:**
- `ActionContext`'s constructor is now **public** (was `internal`). Additive — it is the parameter
  type of the public `ActionHandler` interface, `ActionDispatcher` now lives in the Compose module
  where `internal` is not visible, and it lets you construct one to unit-test your own handler.
- `RemoteConfigLocalStore` / `DeviceIdProvider` keep their `Settings` parameter and its default, but
  the default is now supplied per platform. On linuxX64 / mingwX64 — where multiplatform-settings
  declares no `Settings()` factory because the OS has no preference store — it is an in-memory store:
  impression counts and cooldowns are correct within a process and do not survive a restart. Pass
  your own `Settings` for persistence. Constructor signatures are unchanged.
- `ActionDispatcher` gains its first tests (4), covering handler routing, registration replacement,
  and unknown action types being ignored rather than thrown.

### Changed — API graduation: all four opt-in markers are now stable

`cmp-share`, `cmp-app-intents`, `cmp-intent-launcher` and **`cmp-pdf-generator`** no longer require
an opt-in. Every `@Experimental*Api` annotation has been removed from the public surface.

- **Nothing to do, but you can delete things.** Any `@OptIn(Experimental…Api::class)` and any
  `-opt-in=…Experimental…Api` compiler flag is now redundant. Both still compile — see below — so
  this is not a breaking change, and there is no rush.
- **The marker classes are RETAINED as deprecated no-ops**, not deleted. Each keeps
  `@Retention(BINARY)` and drops `@RequiresOptIn`, so source written during the experimental era
  keeps compiling (with a deprecation warning pointing at the now-redundant annotation). They are
  scheduled for removal in the next major version. `cmp-pdf-generator`'s marker additionally keeps its
  `@Target` list, because its experimental surface permitted `TYPEALIAS` — a target the default set
  omits.
- **Zero binary change.** Annotations are not part of the Binary Compatibility Validator's dumped API,
  so `apiCheck` passes on all four modules with no baseline movement whatsoever. A published consumer
  cannot break on this.
- Each module gains a `GraduatedApiCompatTest` pinning both halves of the promise — that the stable
  surface is reachable with no opt-in, and that the legacy `@OptIn` form still compiles.

This closes the condition recorded in the v0.4 Phase 11 notes below, which deferred the marker
decision "pending Windows CI verification … before locking BCV baselines + dropping markers". Both
now hold: BCV baselines are committed for every publishable module, and the Windows Native CI job
executes `mingwX64Test` (previously the Windows paths were only ever compile- and link-verified).
One correction to that note's premise: the Windows file-dialog surface shipped via PowerShell over
`_popen`, not Win32 `GetSaveFileNameW` cinterop, so what CI now verifies is the PowerShell route.

### Changed — cmp-firebase: GitLive `3.0.0-alpha02` + wasmJs on the native tier

- **GitLive bumped `3.0.0-alpha01` → `3.0.0-alpha02`**, Firebase BoM `34.17.0` → `34.18.0`.
  Kotlin (`2.4.0`), Compose MP (`1.11.1`), serialization and BCV are unchanged, so the
  SwiftPM iOS-linking model introduced in alpha01 is untouched.
- **⚠️ `wasmJs` moved from the Measurement-Protocol tier to the native Firebase tier.**
  Upstream [PR #832](https://github.com/GitLiveApp/firebase-kotlin-sdk/pull/832) gives wasmJs
  full parity with the JS target, so `wasmJsMain` now depends on `firebaseMain` and runs the
  real Firebase JS SDK. Tier counts are now firebaseMain **11** / nonFirebaseMain **4**.
  - **Action required for wasmJs apps:** a wasmJs app that previously supplied only
    `measurementProtocol` must now also supply **`FirebaseConfig.web`** (`applicationId`,
    `apiKey`, `projectId`, `authDomain`) — wasmJs reads the *same* `web` entry as `js`.
    Skipping it means native init is silently skipped and analytics NoOps; there is no
    exception at config time.
  - **Crashlytics is unchanged** — upstream did not add `wasmjs` to `firebase-crashlytics`,
    so wasmJs keeps the `LoggingCrashReporter` fallback for crash reporting.
- **Fixed** `FirebaseConfig.optionsForPlatform()` never routed `"wasmjs"`, so the tier move
  would have compiled cleanly and then failed at runtime on an uninitialized default app.
  Now `"js", "wasmjs" -> web`, with a regression test.
- Regenerated `kotlin-js-store/wasm/yarn.lock` (+680 lines of `@firebase/*` npm packages
  pulled in by the wasmJs binding).
- Documentation corrected across `cmp-firebase/{README,DEVELOPMENT}.md` and
  `docs/firebase/*`, including three pre-existing errors: iOS setup instructed consumers to
  use a `Podfile` that GitLive 3.x no longer honors; a claim that alpha01 "dropped
  `iosX64`/`macosX64`" (it did not); and a tier table that placed **JVM** under
  `firebaseMain` and counted watchOS/wasmWasi as shipping targets (total is 15, not 21).

### Fixed — cmp-share: binary-compatibility baseline was 5 releases stale

- Regenerated `cmp-share/api/jvm/cmp-share.api`. `ShareOptions.targetPackage`
  ([#161](https://github.com/MobileByteLabs/KmpToolkit/pull/161), 2026-08-27) added a fourth
  constructor parameter and **shipped in v3.5.15 → v3.5.20 without the BCV dump being
  regenerated**, leaving `apiCheck` red on `development` the whole time.
- The signature change is a genuine binary break (the 3-arg constructor is replaced, not
  overloaded), but `ShareOptions` is annotated `@ExperimentalShareApi`, so the break is
  sanctioned by that API tier. No source change was made — only the baseline now records
  what already shipped.

### Added — Documentation infrastructure (`kmp-toolkit-docs-mkdocs-cookbook`)

- **Documentation site** at https://mobilebytelabs.github.io/KmpToolkit/ —
  mkdocs-material-powered, auto-published on push to `development` via
  `.github/workflows/docs-publish.yml`. Includes per-module landing pages
  for all 21 cmp-* modules + getting-started guide + cookbook with 12
  task-oriented recipes across 4 topic areas (inter-app comms, network
  monitor, observability, storage).
- **Dokka 2.0 API reference** bundled as `-javadoc.jar` in every Maven
  Central artifact via the new `io.github.mobilebytelabs.kmptoolkit.dokka`
  convention plugin (`build-logic/convention/`) — IntelliJ / Android Studio
  hover-jump-to-symbol now shows full KDoc out of the box.
- Cookbook recipe template at `docs/_partials/cookbook-recipe-template.md`
  for community contributions.
- `scripts/audit-kdoc-coverage.sh` to enumerate public symbols without
  KDoc — runs locally; not yet a hard gate (deferred to a follow-up
  `kmp-toolkit-kdoc-backfill` plan once the 286 missing-symbol backlog is
  worked through).

### Added — Inter-App Comms v0.4 (closes ADR-09 + Compose adapter modules + opinionated UX)

Plan: [`inter-app-comms-compose-completeness`](../../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-compose-completeness/PLAN.md) — 12-sub-plan epic on v0.3-alpha foundation.

**Phase 0 — Spikes** (`SPIKE_FINDINGS_V0_4.md`):
- S1.A Compose-MP target audit → UNCHANGED (CMP roadmap still at 9 targets per JetBrains)
- S1.B Win32 OPENFILENAMEW + IShellLinkW marshal → PROVISIONAL PASS
- S1.C tvOS Swift dispatch cinterop → PROVISIONAL PASS
- S1.D Gradle codegen approach → MANIFEST-JSON picked (zero KSP dep)
- S1.E Kover threshold calibration → per-module table (75-85%)

**Phases 2-5 — ADR-09 closures:**
- ADR-09 #1: tvOS Swift bridge probing wired (Swift class detected via ObjC runtime; full dispatch deferred to real-device CI)
- ADR-09 #2: watchOS `WCSession.transferFile` real impl for Image/File/Multi binary payloads
- ADR-09 #3: mingw CF_DIB binary clipboard via Win32 cinterop (PROVISIONAL — needs Windows CI runtime verify)
- ADR-09 #4: JVM OS-detect with ProcessBuilder subprocess dispatch (macOS open / Linux xdg-open / Windows cmd start)
- ADR-09 #7/#8: cmp-intent-launcher Linux PickContact WONTFIX KDoc + mingw `GetOpenFileNameW` real impl
- ADR-09 #11: cmp-app-intents watchOS/tvOS manifest writes + Swift bridge handoff + Linux XDG/.desktop + mingw APPDATA manifest
- ADR-09 #12: `generateShortcutsXml` + `generateSwiftIntents` Gradle codegen tasks (MANIFEST-JSON approach)

**Phases 6-7 — NEW Compose adapter modules:**
- `cmp-share-compose` — `@Composable rememberShareLauncher()` + Material 3 `ShareSheet()` + `ShareButton()`
- `cmp-app-intents-compose` — `@Composable AppIntentsRegistration(config)` (DisposableEffect lifecycle) + `AppIntentsRegistry()` Material 3 dev/debug LazyColumn UI

**Phase 8 — cmp-intent-launcher-compose UX expansion:**
- `@Composable IntentPickerDialog(contract, onResult, onDismiss)` — Material 3 AlertDialog scaffold
- `@Composable IntentPickerSheet(...)` — Material 3 ModalBottomSheet variant
- Material 3 + materialIconsExtended deps added

**Phase 9 — ABI + coverage infrastructure:**
- `binaryCompatibilityValidator` plugin applied to all 6 modules (3 core + 3 compose); baselines deferred to first Windows-CI green run
- `kover` plugin applied to all 6 modules with per-S1.E thresholds (cinterop-heavy: 75-80%; pure-Kotlin compose: 85%)
- Smoke commonTest cases authored for new compose modules

**Phase 10 — Sample app:**
- `samples/sample-inter-app-comms/composeApp` adds dependencies on 3 compose adapter modules
- New "Compose UX" tab demonstrates all opinionated Composables (ShareSheet, ShareButton, IntentPickerDialog, IntentPickerSheet, AppIntentsRegistration, AppIntentsRegistry)
- `iosApp/iosApp/AppIntents/CmpAppIntentBridge.swift` committed for end-to-end demo + `Generated/` dir scaffolded
- **NOTE**: Linux/mingw native binary targets NOT added to sample composeApp (Compose Multiplatform doesn't support those targets per S1.A — would fail build); a future non-Compose `sample-inter-app-comms-natives` would cover them

**Phase 11 — Docs (shared-version-aware):**
- NEW `docs/inter-app-comms/CAPABILITY_MATRIX.md` — canonical 3-module × per-target × per-API support matrix
- ADR-09 audit log updated per row (closed / WONTFIX / provisional)
- `@ExperimentalShareApi` / `@ExperimentalIntentLauncherApi` / `@ExperimentalAppIntentsApi` markers **RETAINED** in this toolkit release — marker drop is a future toolkit-release decision pending Windows CI verification of Win32 cinterop runtime correctness (S1.B PROVISIONAL needs runtime evidence before locking BCV baselines + dropping markers). All 6 IPC modules continue to ship with the shared `kmptoolkit.version` (currently `3.3.2`; next bump to `3.4.0` lands this v0.4 epic).

**Phase 13 — cmp-intent-launcher `SystemIntents` lifecycle-free entry points** (post-Phase 11 follow-up):
- `SystemIntents.openAppSettings(): IntentResult` — Android `ACTION_APPLICATION_DETAILS_SETTINGS`, iOS `UIApplicationOpenSettingsURLString`, macOS `NSWorkspace.open("x-apple.systempreferences:")`, JVM OS-aware shell dispatch, Linux gnome-control-center→kcmshell5→xdg-open chain, Windows `start ms-settings:appsfeatures`. JS/wasmJs/tvOS/watchOS → `Failed(UnsupportedPlatform)`.
- `SystemIntents.createDocument(suggestedName, mimeType): IntentResult` — Android invisible proxy Activity wraps `ActivityResultContracts.CreateDocument` (lifecycle-free), iOS `UIDocumentPickerViewController(URLs:, inMode: ExportToService)` with `SystemIntentsDelegatePin`, macOS `NSSavePanel.runModal()`, JVM `JFileChooser` on EDT via `Dispatchers.IO`, JS/wasmJs `window.showSaveFilePicker()` via `js()`/`@JsFun` with state-discriminator callback, Linux `zenity --file-selection --save`. mingw/tvOS/watchOS → `Failed(UnsupportedPlatform)` (Win32 `GetSaveFileNameW` cinterop is a future task).
- New auto-init infrastructure: `IntentLauncherInitProvider` ContentProvider + `IntentLauncherContext` (mirrors `cmp-share/ShareInitProvider` pattern). `CreateDocumentProxyActivity` declared in library manifest with translucent theme + `excludeFromRecents`.
- Tests: `SystemIntentsContractTest` (commonTest), `SystemIntentsJvmTest` (headless JVM), `SystemIntentsAndroidUnitTest` (no-init-provider failure-shape).
- Unblocks pure-commonMain `IntentManager` consumers (kmp-project-template `IntentManagerImpl`) — no per-target source set needed for `openAppSettings` / `createDocument`.

### Added — Desktop JVM expansion for cmp-product-tickets + cmp-remote-config

- **`cmp-product-tickets`** now ships a `jvm()` target — Desktop Compose apps can consume the full ProductTickets DSL + UI + Supabase integration. Pure commonMain module (no platform-specific code), so the entire change was adding `jvm()` to the targets list. Transitive deps (Ktor, Supabase, Koin, kotlinx) were already JVM-ready.
- **`cmp-remote-config`** same treatment — `jvm()` added, DynamicUiRenderer + RemoteConfigService + UiNode model all compile cleanly on JVM. Coil-compose, multiplatform-settings, supabase-postgrest all multiplatform-ready.
- Motivation: the unified `samples/sample-toolkit` catalog app (also added in this release) needs every catalog library to support Desktop JVM. Previously these two were the only commonMain-only-but-JVM-missing libraries, blocking the desktop demo.
- No API changes. No expect/actual added. No platform-specific source set introduced.

### Added — Unified `samples/sample-toolkit` catalog app

- New `samples/sample-toolkit/{composeApp,androidApp}` showcases every `cmp-*` library in a single navigable Compose Multiplatform app. Home screen groups libraries by category (UI / Comms / Network / Lifecycle / Data / Backend); tap a card to drill into that library's demo screen.
- Built with `androidx.navigation:navigation-compose-multiplatform` v2.9.2 (already in the version catalog).
- 15 demo screens covering: toast, bubble, clipboard, share, intent-launcher, app-intents, open-url, deep-link, network-monitor, in-app-update, pdf-generator, remote-config, firebase-analytics, product-tickets.
- Per-module samples (`sample-clipboard`, `sample-cmp-share`, `sample-inter-app-comms`, etc.) remain alongside — sample-toolkit is the catalog, the per-module samples remain the focused references.

### Added — v0.2 Platform Parity (cmp-share + cmp-app-intents)

**Toolkit version bumped 3.2.11 → 3.3.0.** Two suite modules now ship 19 KMP targets each — matching the target matrix of `cmp-deep-link` / `cmp-open-url` / `cmp-clipboard`.

- **`cmp-share`** adds `tvosX64/Arm64/SimulatorArm64`, `watchosX64/Arm32/Arm64/SimulatorArm64/DeviceArm64`, `linuxX64/Arm64`, `mingwX64` (11 new targets, 19 total). Real implementations:
  - **Linux**: URL/file share via `xdg-open` subprocess (`xdg-utils` dependency). Single-quote-escaped to prevent shell injection.
  - **mingw (Windows)**: URL/file share via `cmd /c start`. Double-quote-escaped.
  - **tvOS**: `UnsupportedPlatform` fallback — Kotlin/Native bindings (as of Kotlin 2.3.10) don't expose `UIPasteboard` for tvOS even though the Objective-C API exists.
  - **watchOS**: `UnsupportedPlatform` fallback — no share-sheet surface.
- **`cmp-app-intents`** adds same 11 targets (19 total). Tier-3 platforms get registry-only behavior — `AppIntents.register()` stores config in `AppIntentsRuntime`; `invokeForTesting()` works for dev/test on every platform. Swift bridge `CmpAppIntentBridge.swift` updated with `#if canImport(CoreSpotlight)` guards so the same file compiles for iOS, macOS, tvOS, and watchOS — Spotlight indexing silently skips on tvOS/watchOS (no CoreSpotlight on those platforms).
- **`cmp-intent-launcher`** stays at 9 targets. **Constraint discovered during Phase 10.A**: the Compose Compiler Gradle plugin is module-level (not source-set-level) and requires `compose.runtime` on every target's classpath. The `composeMain` intermediate source-set workaround fails because the compiler plugin runs before source-set resolution. Future v0.3 path: split into `cmp-intent-launcher-core` (non-Compose, 19 targets) + `cmp-intent-launcher` (current API, depends on -core). Documented in `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/10-platform-parity-v0-2.md`.
- **wasmWasi** remains excluded — server-side WASM has no DOM, no clipboard, no UI gesture surface (genuine technical impossibility).

### Added — cmp-intent-launcher iOS picker support
- **iOS picker contracts now route to native UIKit pickers.** `ResultContracts.PickImage`
  + `PickMultipleImages` → `PHPickerViewController` (iOS 14+); `PickDocument` →
  `UIDocumentPickerViewController`; `PickContact` → `CNContactPickerViewController`.
  Suspend-coroutine bridges resume exactly once from the delegate callback (success /
  cancel / error); a `DelegatePin` singleton holds strong refs while presentations are
  in flight to defend against ARC dropping the Kotlin/Native delegate shadow.
- Arbitrary actions still fall back to `onUnsupported` or `IntentResult.Failed(UnsupportedPlatform)`.
- Plan: `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md` §G-9.6 follow-up.
- **No public API changes** — caller code that already used `ResultContracts.PickImage`
  with an `onUnsupported` fallback now receives real picker results on iOS; the fallback
  is preserved for non-picker actions.

### Added — cmp-app-intents iOS Spotlight indexing
- **Searchable intents are now indexed into iOS Spotlight + Siri Suggestions.** The Swift
  bridge `CmpAppIntentBridge.bootstrap(callbackResolver:)` reads the manifest written by
  Kotlin's `AppIntents.register(config)`, pushes every `searchable: true` entry into
  `CSSearchableIndex.defaultSearchableIndex()` with title + contentDescription, and
  exposes `handleContinue(_:)` to route Spotlight taps back into the Kotlin DSL via
  `CmpAppIntentsCallback.shared.handler`.
- Activity prefix `com.mobilebytelabs.kmptoolkit.appintents.<id>` is the user-activity
  type to declare in your Info.plist `NSUserActivityTypes` array.
- Real Kotlin↔Swift callback wiring replaces the placeholder `print()` from v0.1.
  The bridge uses KVC + `NSSelectorFromString("invoke::")` so it stays framework-alias
  agnostic (works whether your Kotlin/Native framework is published as `kmptoolkit`,
  `shared`, or any custom name).
- macOS 10.13+ also benefits — `CoreSpotlight` is available there.
- Plan: `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/09-per-module-samples.md` §Resolved deferred items.
- Migration: existing consumers should replace `CmpAppIntentBridge.shared.loadManifest()`
  at app launch with `CmpAppIntentBridge.shared.bootstrap { CmpAppIntentsCallback.shared }`.

### Changed — build configuration
- **`org.gradle.jvmargs` bumped to `-Xmx4096M`** (was 2048M). The lower heap was OOMing
  wasmJs compilation when 3+ new sample modules with Compose were added to the workspace.
  Local developer machines and CI both benefit; no downstream changes required.
- **`compose.desktop.packaging.checkJdkVendor=false`** added to `gradle.properties` to
  permit `createDistributable` runs on Homebrew JDKs (per JetBrains/compose-multiplatform#3107).
  CI release pipelines are unaffected — they use vendor-pinned Corretto/Temurin where
  the flag is a no-op.

### Added — cmp-pdf-generator (new module)
- **New module `cmp-pdf-generator`** — cross-platform PDF generation library.
  Coordinates: `io.github.mobilebytelabs:cmp-pdf-generator` (ships at the shared
  `kmptoolkit.version`).
- **Input modes:** HTML string, Markdown (via `MarkdownPdfAdapter`), DSL (`pdf { … }`).
  Composable snapshot + image-to-PDF deferred to a future release.
- **Output destinations:** File, ByteArray, platform URI, Share, Print, Save.
- **Page config:** A3/A4/A5/B5/LETTER/LEGAL/TABLOID/STATEMENT + custom size +
  portrait/landscape + per-edge margins + optional per-page header/footer/page-numbers.
- **Branding:** injectable `PdfBranding(logo, poweredByText, theme, dateFormatter, watermark)`.
  De-branded `HtmlTemplateGenerator` base class.
- **Pre-built templates:** `InvoiceTemplate`, `ReportTemplate`, `ReceiptTemplate`,
  `StatementTemplate`, `LetterTemplate`.
- **Error model:** sealed `PdfError` hierarchy + cancellation + `Flow<PdfProgressEvent>`.
- **Platforms:** Android, iOS (14+), macOS (11+), JVM, JS (browser+nodejs),
  wasmJs (HTML route only — DSL/byte route deferred pending pdf-lib wasmJs interop).
  Not targeted: tvOS, watchOS, Linux native, mingwX64, wasmWasi — `kotlinx-html`
  and `org.intellij.markdown` don't publish artifacts there.
- **Marker:** all public symbols `@ExperimentalPdfGeneratorApi` until v1.0.
- **PLAN:** `plan-layer/project-plans/mbs/kmp-toolkit/active/cmp-pdf-generator/`
  (epic, 12 sub-plans, 266 tasks).

### Fixed — cmp-open-url
- **iOS / macOS `AppHint` parity (G6 fix)** — `openWithApp(url, AppHint.{EMAIL/PHONE/SMS/MAPS})`
  on Apple platforms now rewrites the URL to the scheme-appropriate form (`mailto:`, `tel:`,
  `sms:`, `maps:`/`geo:`) BEFORE calling `UIApplication.openURL` / `NSWorkspace.openURL`.
  Previously the hint was silently ignored — `AppHint.EMAIL + bare HTTPS` would open Safari
  instead of Mail.app. Incompatible (url, hint) combinations now return
  `OpenUrlResult.Error(message)` with a clear hint about the required URL scheme.
  - `AppHint.Custom(packageName)` is now documented as **Android-only**; on iOS / macOS /
    JVM / JS / wasmJs it silently falls back to `AppHint.DEFAULT` behaviour (no behaviour
    change vs the previous silent fallback — only KDoc clarification).
  - Pure-logic `transformUrl()` helper added in commonMain (internal); 20+ unit-test cases
    cover the rewrite matrix.
  - Plan: `plan-layer/project-plans/mbs/kmp-toolkit/active/inter-app-comms-suite/03-open-url-g6-fix.md`
  - **No public API changes** — BCV baseline unaffected; consumer code requires no migration.

### Breaking — cmp-remote-config (next release: 4.0.0)
- **Removed dependency on `cmp-product-tickets`.** `cmp-remote-config` is now standalone.
  No transitive Maven pull, no shared config object, no cross-module imports.
- **Public API reduced to a single Koin DSL extension**: `fun Module.remoteConfig(block)`.
  Drops the previous public `val remoteConfigModule` + `object RemoteConfigConfig`.
- **`RemoteConfigConfig` (public mutable singleton with `ifEmpty { ProductTicketsConfig.x }` fallbacks) deleted.**
  Replaced by an internal `RemoteConfigSettings` data class populated via the DSL builder.
- **Dropped `productType` parameter.** Per-project Supabase model (mirrors cmp-product-tickets v3.0.0 — each
  consumer app has its own Supabase project, so the `product_type` filter column is meaningless on the client).
  Removed from: `RemoteConfigService` ctor, `getActiveConfigs()` filter, `get_device_impressions` /
  `record_config_impression` / `dismiss_config` RPC parameters (`p_product_type`), `RemoteConfig` data class
  (`@SerialName("product_type")` field), `RemoteConfigBuilder` DSL, `RemoteConfigSettings`.
  - Server follow-up: the `product_remote_config.product_type` column and `p_product_type` RPC arguments can be
    dropped in a separate migration; until then they remain ignored (`ignoreUnknownKeys = true` on the JSON deserializer).
- **`ActionType` changed from `enum class` to `@JvmInline value class`.** Built-in constants
  (NONE/URL/DEEPLINK/STORE/DISMISS) preserved as companion vals; consumer apps can extend with their own:
  `object RemoteActions { val OPEN_DOWNLOADS = ActionType("open_downloads") }`. New built-in: `PREMIUM`.
  `ActionType.from(value)` removed — construct directly: `ActionType("my_type")`.
- **New extensible action dispatcher.** Register handlers in the DSL via
  `action("type") { value, ctx -> … }`. Handlers fire when `RemoteConfigHost` is called without an
  explicit `onAction` parameter; the explicit `onAction` escape hatch is preserved.

#### Migration
```kotlin
// Before
ProductTicketsConfig.init(supabaseUrl, supabaseAnonKey, boardType = "your_app")
startKoin { modules(productTicketsModule, remoteConfigModule) }

// After (one block inside any existing Koin module)
val networkModule = module {
    remoteConfig {
        supabaseUrl = "https://YOUR_PROJECT.supabase.co"
        supabaseKey = "YOUR_ANON_KEY"
        action(ActionType.PREMIUM) { _, _ -> AppNavigator.navigateTo("paywall") }
        action("open_downloads")    { v, _ -> AppNavigator.navigateTo("downloads/${v.orEmpty()}") }
    }
    // … your other bindings …
}
startKoin { modules(networkModule, /* other modules */) }   // remoteConfigModule no longer exists
```


### Added
- **New Module: kmp-open-url** (`io.github.mobilebytelabs:kmp-open-url:3.2.1`)
  - Cross-platform URL opening for all 14 KMP targets
  - `openUrl(url)` — open with default platform handler, never throws
  - `openInBrowser(url)` — force system browser, bypasses app-association rules
  - `openWithApp(url, AppHint)` — open with preferred app category, returns `OpenUrlResult`
  - `canOpen(url)` — check URL handleability without opening
  - `AppHint` sealed class: DEFAULT, BROWSER, EMAIL, MAPS, PHONE, SMS, Custom(packageName)
  - `OpenUrlResult` sealed class: Success, NoHandler, Error(message)
  - Android: `Intent.ACTION_VIEW` + ContentProvider auto-init (zero setup)
  - iOS/macOS: `UIApplication.openURL` / `NSWorkspace.openURL` via shared `appleMain`
  - tvOS/watchOS: graceful `NoHandler` — no crash
  - JVM Desktop: `Desktop.getDesktop().browse()` + `xdg-open` headless fallback
  - JS Browser: `window.open(url, "_blank")`
  - wasmJs: `window.open` via `@JsFun` interop
  - Linux Native: `xdg-open` via `platform.posix.system`
  - Windows Native: `ShellExecuteW` via Win32 API
  - wasmWasi: deliberate no-op (no display concept)

- **New Module: kmp-clipboard** (`io.github.mobilebytelabs:kmp-clipboard:0.1.0`)
  - Extracted clipboard functionality into standalone module
  - ClipboardObserver for monitoring clipboard changes with app foreground detection
  - Platform-specific implementations: Android (ClipboardManager + ProcessLifecycleOwner), iOS (UIPasteboard), macOS (NSPasteboard), JVM (AWT FlavorListener), JS (navigator.clipboard), Linux (xclip/xsel), Windows (Win32 API)
  - All 20+ platform targets supported

- **New Module: kmp-toast** (`io.github.mobilebytelabs:kmp-toast:0.1.0`)
  - Pure Compose Multiplatform toast/snackbar notifications
  - Duration options: SHORT (2s), MEDIUM (3.5s), LONG (5s), INDEFINITE
  - Position options: TOP, CENTER, BOTTOM
  - Style options: DEFAULT, SUCCESS, ERROR, WARNING, INFO
  - Action button support and swipe-to-dismiss

- **New Module: kmp-in-app-update** (`io.github.mobilebytelabs:kmp-in-app-update:0.5.0`)
  - Extracted from cmp-library into dedicated module
  - Check for app updates with GitHub Releases, Supabase, or custom backends
  - Platform-specific implementations for Play Store, App Store, Mac App Store
  - Update types: NONE, FLEXIBLE, IMMEDIATE

- **Template Module: cmp-library** (`io.github.mobilebytelabs:kmp-template:1.0.0-template`)
  - Converted to template/reference module for creating new libraries
  - Includes TEMPLATE_README.md with step-by-step instructions
  - Sample expect/actual pattern with Greeting.kt and Platform.kt

- **Sample Apps**
  - `sample-clipboard`: Clipboard + Observer + Toast integration demo
  - `sample-in-app-update`: In-App Update demo with GitHub Releases

### Changed
- **Modular Architecture**: KMP Toolkit now uses a modular architecture
  - Each feature is a standalone library module
  - Consumers can import only the features they need
  - GitHub Actions auto-discovers and publishes all cmp-* modules

### Documentation
- Added `MODULE_STRUCTURE.md` documenting the modular architecture pattern
- Updated `README.md` with new installation instructions for individual modules
- Added `TEMPLATE_README.md` for creating new library modules

---

## kmp-in-app-update

### [0.5.0] - 2026-03-04

#### Added
- Initial release as standalone module (extracted from cmp-library)
- Cross-platform in-app update checking:
  - `AppUpdate.checkForUpdate(config)` - Check for available updates
  - `AppUpdateConfig.builder()` - Configure update sources
  - `GitHubResolver` - Check updates via GitHub Releases
  - `SupabaseResolver` - Check updates via Supabase backend

#### Platform Support
| Platform | Native Store | Custom Resolver |
|----------|:------------:|:---------------:|
| Android | ✅ Play Store | ✅ |
| iOS | ✅ App Store | ✅ |
| macOS | ✅ Mac App Store | ✅ |
| JVM | ❌ | ✅ |
| Linux | ❌ | ✅ |
| Windows | ❌ | ✅ |
| JS | ❌ | ❌ |
| Wasm | ❌ | ❌ |

---

## kmp-toast

### [0.1.0] - 2026-03-04

#### Added
- Initial release as pure Compose Multiplatform module
- Toast/Snackbar notifications:
  - `ToastHost` - Container composable for toasts
  - `rememberToastHostState()` - State management
  - `showToast()` - Display toast with customization

#### Features
- **Duration**: SHORT (2s), MEDIUM (3.5s), LONG (5s), INDEFINITE
- **Position**: TOP, CENTER, BOTTOM
- **Style**: DEFAULT, SUCCESS, ERROR, WARNING, INFO
- Action button support
- Swipe-to-dismiss gesture

#### Platform Support
Works on all Compose Multiplatform targets (Android, iOS, macOS, JVM, Web)

---

## kmp-clipboard

### [0.1.0] - 2026-03-04

#### Added
- Initial release as standalone module
- Cross-platform clipboard utilities:
  - `copyToClipboard(text: String): Boolean` - Copy text to clipboard
  - `getFromClipboard(): String?` - Read text from clipboard
  - `hasClipboardText(): Boolean` - Check if clipboard has text
  - `clearClipboard()` - Clear clipboard contents

#### Platform Support
| Platform | Copy | Read | Notes |
|----------|:----:|:----:|-------|
| Android | ✅ | ✅ | Auto-initialized via ContentProvider |
| iOS | ✅ | ✅ | Uses UIPasteboard |
| macOS | ✅ | ✅ | Uses NSPasteboard |
| JVM | ✅ | ✅ | Uses AWT Toolkit |
| Linux | ✅ | ✅ | Requires xclip or xsel |
| Windows | ✅ | ✅ | Uses Win32 API |
| JS | ✅ | ❌ | Async API, write-only for sync |
| Wasm JS | ❌ | ❌ | Complex JS interop required |
| Wasm WASI | ❌ | ❌ | No clipboard in WASI runtime |
| tvOS | ❌ | ❌ | No pasteboard support |
| watchOS | ❌ | ❌ | No pasteboard support |

---

## kmp-template (cmp-library)

### [1.0.0-template] - 2026-03-04

#### Changed
- Converted from production library to template/reference module
- All production code moved to dedicated modules:
  - Clipboard → `kmp-clipboard`
  - App Update → `kmp-in-app-update`
  - Toast → `kmp-toast`

#### Purpose
- Reference implementation for creating new KMP library modules
- Sample expect/actual pattern with `Greeting.kt` and `Platform.kt`
- Step-by-step instructions in `TEMPLATE_README.md`

### [0.5.0] - Previous

- App Update functionality (now in kmp-in-app-update)

### [0.4.0] - Previous

- Initial release with Clipboard and App Update features combined

---

## Migration Guide

### From kmp-toolkit 0.4.x/0.5.x to Modular Libraries

The monolithic `kmp-toolkit` has been split into focused, independent modules. Choose only the modules you need:

```kotlin
// build.gradle.kts
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Before (all features bundled)
            // implementation("io.github.mobilebytelabs:kmp-toolkit:0.5.0")

            // After (import only what you need)
            implementation("io.github.mobilebytelabs:kmp-clipboard:0.1.0")      // Clipboard utilities
            implementation("io.github.mobilebytelabs:kmp-toast:0.1.0")          // Toast/Snackbar UI
            implementation("io.github.mobilebytelabs:kmp-in-app-update:0.5.0")  // App update checking
        }
    }
}
```

### API Changes

| Feature | Old Import | New Import |
|---------|------------|------------|
| Clipboard | `com.mobilebytelabs.kmptoolkit.*` | `com.mobilebytelabs.kmptoolkit.clipboard.*` |
| Toast | N/A (new) | `com.mobilebytelabs.kmptoolkit.toast.*` |
| App Update | `com.mobilebytelabs.kmptoolkit.appupdate.*` | `com.mobilebytelabs.kmptoolkit.appupdate.*` (unchanged) |

---

[Unreleased]: https://github.com/MobileByteLabs/KmpToolkit/compare/v0.5.0...HEAD
[kmp-in-app-update-0.5.0]: https://github.com/MobileByteLabs/KmpToolkit/releases/tag/in-app-update-v0.5.0
[kmp-toast-0.1.0]: https://github.com/MobileByteLabs/KmpToolkit/releases/tag/toast-v0.1.0
[kmp-clipboard-0.1.0]: https://github.com/MobileByteLabs/KmpToolkit/releases/tag/clipboard-v0.1.0
