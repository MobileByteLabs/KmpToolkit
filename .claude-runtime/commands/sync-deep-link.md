# /sync-deep-link - Full Instructions

> **Single source of truth** for `cmp-deep-link` sync contract.
> The framework `/lib-sync cmp-deep-link` delegates to this file.
> Update this file when the library version changes.

---

# /sync-deep-link — cmp-deep-link Sync

Verify-gated sync of `cmp-deep-link` into a consuming KMP app.
Gate 1: Gradle dependency. Gate 2: N/A (no Supabase). Gate 3: Platform wiring.

---

## Module Contract (update when library changes)

```yaml
module:    cmp-deep-link
artifact:  io.github.mobilebytelabs:kmp-deep-link
version:   3.2.1
package:   com.mobilebytelabs.kmptoolkit.deeplink
supabase:  false
di:        false   # no DI module — singleton object + top-level extensions
nav:       false   # no nav destinations (consumer wires their own nav)

config:    none    # zero config — auto-init via ContentProvider (Android)

api:
  # Core
  - DeepLinkHandler.incoming: SharedFlow<DeepLink>
  - DeepLinkHandler.lastReceived: StateFlow<DeepLink?>
  - DeepLinkHandler.handle(uri: String)
  - DeepLinkHandler.clear()

  # Parser DSL
  - deepLinkParser { route<T>(pattern) }: DeepLinkParser
  - DeepLinkParser.parse(link: DeepLink): T?

  # Builder
  - DeepLinkBuilder(scheme, host).build(pathPattern, pathParams, queryParams): String

  # Android extension (androidMain)
  - ComponentActivity.handleDeepLinkIntent(intent: Intent? = this.intent)

  # Desktop (jvmMain / linuxMain / mingwMain)
  - DeepLinkHandler.handleLaunchArgs(args: Array<String>)

  # Browser (jsMain / wasmJsMain)
  - DeepLinkHandler.initBrowser(mode: BrowserRoutingMode)   # HISTORY mode opt-in only

platform_wiring:
  android:
    manifest: intent-filter with <data android:scheme="...">
    kotlin:   ZERO (ContentProvider auto-inits; addOnNewIntentListener auto-wired)
  ios_swiftui:
    swift:    .deepLinkAutoHandle() on root view  (1 line via swift/DeepLinkPlugin.swift)
  ios_uikit:
    swift:    DeepLinkAppleHelper.shared.handleUrl(url:) in AppDelegate.application(_:open:)
  macos_swiftui:
    same as ios_swiftui
  browser_js_wasmjs:
    kotlin:   ZERO (HASH mode auto-inits at module load)
    opt_in:   DeepLinkHandler.initBrowser(BrowserRoutingMode.HISTORY) for SPA history routing
  jvm_desktop:
    kotlin:   DeepLinkHandler.handleLaunchArgs(args) in main()
```

---

## Usage

```bash
/sync-deep-link                  # Full sync — all gates
/sync-deep-link --check          # Dry run — show status, no writes
/sync-deep-link --wiring-only    # Gate 3 only (platform wiring)
```

---

## Workflow

```
/sync-deep-link
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: kmp-deep-link:3.2.1 in libs.versions.toml           │
│  Check: used in commonMain.dependencies                      │
│  Fix:   Auto-insert correct entries                          │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                     │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
             GATE 2: Supabase — N/A (skip)
                          │
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 3: Platform Wiring                                     │
│  Detect active targets (android / ios / jvm / browser)      │
│  Per-target: check → fix → verify                           │
│  Result: ✅ / ⚡ / ⚠️ (manual step required)                 │
└─────────────────────────┬────────────────────────────────────┘
                          │ ALL PASS
                          ▼
              ✅ SYNC COMPLETE — print state summary
```

---

## Gate 1: Gradle

### Check
```
1. Glob: gradle/libs.versions.toml
   → search "kmp-deep-link"
   → if found: verify version = 3.2.1
   → if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmp.deep.link"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
kmp-deep-link = "3.2.1"

# libs.versions.toml [libraries]
kmp-deep-link = { module = "io.github.mobilebytelabs:kmp-deep-link", version.ref = "kmp-deep-link" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmp.deep.link)
```

---

## Gate 2: Supabase — N/A

`cmp-deep-link` has no Supabase backend. Gate 2 always skips.

---

## Gate 3: Platform Wiring

Target detection: read `build.gradle.kts` of the shared KMP module for `androidTarget()`,
`iosArm64()` / `iosSimulatorArm64()`, `jvm()`, `js()`, `wasmJs()`.

Run each applicable sub-gate. Skip non-applicable targets silently.

---

### Gate 3a — Android

**Detection:** `androidTarget()` present in build.gradle.kts

**Check 1 — Manifest intent-filter:**
```
Glob: **/AndroidManifest.xml
Grep: android:scheme=
If missing: mark NEEDS_FIX
```

**Fix 1 — Manifest:**
```
Locate the <activity android:name=".MainActivity"> entry.
Add inside <activity>:
```
```xml
<intent-filter>
    <action android:name="android.intent.action.VIEW" />
    <category android:name="android.intent.category.DEFAULT" />
    <category android:name="android.intent.category.BROWSABLE" />
    <data android:scheme="TODO_YOUR_SCHEME" android:host="TODO_YOUR_HOST" />
</intent-filter>
```
```
Mark as ⚠️ MANUAL — consumer must replace TODO_YOUR_SCHEME and TODO_YOUR_HOST.
```

**Check 2 — No manual onNewIntent:**
```
Grep: onNewIntent in **/androidMain/**/*.kt
If found AND contains handleDeepLinkIntent:
  → ⚠️ STALE: manual override no longer needed — addOnNewIntentListener is auto-wired.
  → Show diff: remove onNewIntent override + handleDeepLinkIntent import.
  → AskUserQuestion: "Remove stale manual onNewIntent override? [Y/N]"
  → If Y: apply Edit to remove.
```

**Result:** ✅ intent-filter present / ⚡ added (manual scheme still needed) / ⚠️ stale override found

---

### Gate 3b — iOS / macOS SwiftUI

**Detection:** `iosArm64()` or `macosArm64()` present in build.gradle.kts

**Check — DeepLinkPlugin.swift:**
```
Glob: **/swift/DeepLinkPlugin.swift OR **/DeepLinkPlugin.swift
If missing: mark NEEDS_FIX
```

**Fix:**
```
Read: cmp-deep-link/swift/DeepLinkPlugin.swift (source of truth in the library)
Copy content to: {consumer_ios_dir}/DeepLinkPlugin.swift
  (locate iOS Xcode project dir via Glob: **/*.xcodeproj or **/iosApp/)

Then check App.swift:
Grep: ".deepLinkAutoHandle()" in **/*.swift
If missing:
  → Grep: "WindowGroup" in **/*.swift to locate App entry point
  → Show user the required 1-line addition:
      ContentView()
          .deepLinkAutoHandle()   // ← add this
  → Mark as ⚠️ MANUAL (Swift file edits require Xcode)
```

**Result:** ✅ plugin present + wired / ⚡ plugin copied (manual wiring note) / ⚠️ MANUAL

---

### Gate 3c — JVM Desktop

**Detection:** `jvm()` present in build.gradle.kts

**Check:**
```
Grep: "handleLaunchArgs" in **/jvmMain/**/*.kt OR **/desktopMain/**/*.kt
If found: ✅
If missing: mark NEEDS_FIX
```

**Fix:**
```
Locate main() function in jvmMain or desktopMain.
Insert before application() / window creation:
```
```kotlin
import com.mobilebytelabs.kmptoolkit.deeplink.handleLaunchArgs
import com.mobilebytelabs.kmptoolkit.deeplink.DeepLinkHandler

fun main(args: Array<String>) {
    DeepLinkHandler.handleLaunchArgs(args)   // ← add this line
    // ... your existing application() call
}
```

**Result:** ✅ / ⚡ FIXED

---

### Gate 3d — Browser (JS / Wasm/JS)

**Detection:** `js()` or `wasmJs()` present in build.gradle.kts

**Check:**
```
HASH mode (default) requires ZERO consumer code — auto-inits at module load.
Check for stale initBrowser() calls:

Grep: "initBrowser" in **/jsMain/**/*.kt OR **/wasmJsMain/**/*.kt
If found AND mode == HASH:
  → ⚠️ STALE: initBrowser(HASH) no longer needed — auto-init handles it.
  → Show diff: remove the call.
  → AskUserQuestion: "Remove stale initBrowser(HASH) call? [Y/N]"
If found AND mode == HISTORY:
  → ✅ intentional — HISTORY mode requires explicit opt-in.
```

**Result:** ✅ zero-config / ⚠️ stale HASH call found / ✅ HISTORY mode intentional

---

## Receive Links — Verify Consumer Code (informational)

After all gates pass, check that the consumer has at least one collector:
```
Grep: "DeepLinkHandler.incoming" OR "DeepLinkHandler.lastReceived" in **/commonMain/**/*.kt
If missing:
  → Print INFO (not a block):
      ℹ️  No DeepLinkHandler.incoming collector found.
         Add to your ViewModel or navigation host:
           DeepLinkHandler.incoming.collect { link -> /* navigate */ }
```

---

## --check (Dry Run)

Print current state without making any changes:
```
GATE 1  Gradle    ✅  kmp-deep-link:3.2.1
GATE 2  Supabase  N/A
GATE 3a Android   ✅  intent-filter present (scheme=myapp)
GATE 3b iOS       ⚠️  DeepLinkPlugin.swift missing — needs copy + Xcode wiring
GATE 3c JVM       ✅  handleLaunchArgs in main()
GATE 3d Browser   ✅  zero-config (HASH auto-init)
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-deep-link — COMPLETE                                      ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle    ✅  kmp-deep-link:3.2.1                      ║
║  GATE 2  Supabase  N/A  no backend                              ║
║  GATE 3a Android   ⚡  Added intent-filter (set your scheme!)   ║
║  GATE 3b iOS       ⚠️  Copy swift/DeepLinkPlugin.swift to Xcode ║
║             → add .deepLinkAutoHandle() to ContentView          ║
║  GATE 3c JVM       ✅  handleLaunchArgs present                 ║
║  GATE 3d Browser   ✅  zero-config (HASH auto-init)             ║
╠══════════════════════════════════════════════════════════════════╣
║  Docs: cmp-deep-link/docs/  (ANDROID.md / IOS.md / DESKTOP.md) ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

1. **Version bump** → update `version: 3.2.1` above
2. **New platform target** → add Gate 3x sub-section
3. **New API method** → update `api:` section
4. **Scheme detection** → Gate 3a check can be made smarter (read existing scheme from manifest)
