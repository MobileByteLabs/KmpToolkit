# /sync-toast - Full Instructions

> **Single source of truth** for `cmp-toast` sync contract.
> The framework `/lib-sync cmp-toast` delegates to this file.
> Update this file when the library evolves.

---

# /sync-toast — cmp-toast Sync

Verify-gated sync of `cmp-toast` into a consuming KMP app.
Gate 1: Gradle dependency. Gate 2: N/A (no Supabase). Gate 3: ToastHost placement.

---

## Module Contract (update when library changes)

```yaml
module:    cmp-toast
artifact:  io.github.mobilebytelabs:kmp-toast
version:   2.1.0
package:   com.mobilebytelabs.kmptoolkit.toast
supabase:  false
di:        false   # no DI module — state created in composable
nav:       false   # no nav destinations

config:    none — state created via rememberToastHostState()

api:
  - rememberToastHostState(): ToastHostState              # @Composable
  - ToastHost(hostState, modifier?, toast?)               # @Composable
  - toastState.showToast(message, actionLabel?, duration?, position?, style?): ToastResult
  - toastState.dismiss()
  - toastState.currentToast: StateFlow<ToastData?>

enums:
  ToastDuration: SHORT(3000ms), LONG(5000ms), INDEFINITE
  ToastPosition: TOP, CENTER, BOTTOM
  ToastStyle:    DEFAULT, SUCCESS, ERROR, WARNING, INFO
```

---

## Usage

```bash
/sync-toast           # Full sync
/sync-toast --check   # Dry run — show status, no writes
```

---

## Workflow

```
/sync-toast
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: kmp-toast:2.1.0 in libs.versions.toml               │
│  Check: used in commonMain.dependencies                      │
│  Fix:   Auto-insert correct entries                          │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                     │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema — SKIPPED (no backend)              │
└─────────────────────────┬────────────────────────────────────┘
                          │ SKIP
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 3: ToastHost Placement                                 │
│  Grep: "ToastHost(" in **/*.kt                               │
│  If found: ✅                                                │
│  If missing:                                                 │
│    → locate root App.kt or main composable                   │
│    → insert ToastHost + rememberToastHostState()             │
│    → add imports                                             │
│  Result: ✅ / ⚡                                             │
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
   → search "kmp-toast"
   → if found: verify version = 2.1.0
   → if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmp.toast"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
kmp-toast = "2.1.0"

# libs.versions.toml [libraries]
kmp-toast = { module = "io.github.mobilebytelabs:kmp-toast", version.ref = "kmp-toast" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmp.toast)
```

---

## Gate 2: Supabase Schema

**SKIPPED** — `cmp-toast` has no Supabase backend.

---

## Gate 3: ToastHost Placement

```
Grep: "ToastHost(" in **/*.kt

If found: ✅

If missing:
  → locate root App.kt composable — the one with Box or Scaffold wrapping NavHost
  → insert after NavHost (inside Box):
      val toastState = rememberToastHostState()
      // ... existing content ...
      ToastHost(hostState = toastState)
  → add imports:
      import com.mobilebytelabs.kmptoolkit.toast.ToastHost
      import com.mobilebytelabs.kmptoolkit.toast.rememberToastHostState
```

---

## --check (Dry Run)

```
GATE 1  Gradle   ✅  kmp-toast:2.1.0
GATE 2  Supabase N/A
GATE 3  Wiring   [WOULD ADD] ToastHost() in root composable
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-toast — COMPLETE                                          ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle     ✅  kmp-toast:2.1.0                         ║
║  GATE 2  Supabase   N/A  no backend                             ║
║  GATE 3  Wiring     ⚡  Added ToastHost() in App.kt             ║
╠══════════════════════════════════════════════════════════════════╣
║  Docs: docs/toast/SETUP.md                                      ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

1. **Version bump** → update `version: 2.1.0`
2. **New API** → update `api:` section
3. **New enum value** → update relevant enum in `enums:`
