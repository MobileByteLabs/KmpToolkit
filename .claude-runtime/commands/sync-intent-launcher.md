# /sync-intent-launcher - Full Instructions

> **Single source of truth** for `cmp-intent-launcher` sync contract.
> The framework `/lib-sync cmp-intent-launcher` delegates to this file.
> Update this file when the library version changes.

---

# /sync-intent-launcher — cmp-intent-launcher Sync

Verify-gated sync of `cmp-intent-launcher` into a consuming KMP app.
Gate 1: Gradle dependency. Gate 2: N/A. Gate 3: N/A (zero-config Gradle; Activity wiring
is Compose-layer responsibility documented in SETUP.md Step 4).

---

## Module Contract (update when library changes)

```yaml
module:    cmp-intent-launcher
artifact:  io.github.mobilebytelabs:cmp-intent-launcher
version:   3.2.11
package:   com.mobilebytelabs.kmptoolkit.intentlauncher
supabase:  false
di:        false   # no DI module — rememberIntentLauncher() Compose helper
nav:       false   # no nav destinations
config:    none    # zero Gradle configuration; Activity wiring = developer responsibility

api:
  - IntentLauncher.launch(block: IntentBuilder.() -> Unit): IntentResult
  - rememberIntentLauncher(): IntentLauncher
  - IntentResult.Ok(data: IntentData?)
  - IntentResult.Cancelled
  - IntentResult.Failed(cause: IntentError)
  - ResultContracts.PICK / GET_CONTENT / OPEN_DOCUMENT / CREATE_DOCUMENT / SEND
```

---

## Usage

```bash
/sync-intent-launcher           # Full sync
/sync-intent-launcher --check   # Dry run — show status, no writes
```

---

## Workflow

```
/sync-intent-launcher
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: cmp-intent-launcher:3.2.11 in libs.versions.toml    │
│  Check: used in commonMain.dependencies                      │
│  Fix:   Auto-insert correct entries                          │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                     │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
         ✅ SYNC COMPLETE (Gate 2 + Gate 3 not applicable)
```

---

## Gate 1: Gradle

### Check
```
1. Glob: gradle/libs.versions.toml
   → search "cmp-intent-launcher"
   → if found: verify version = 3.2.11
   → if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "cmp.intent.launcher"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
cmp-intent-launcher = "3.2.11"

# libs.versions.toml [libraries]
cmp-intent-launcher = { module = "io.github.mobilebytelabs:cmp-intent-launcher", version.ref = "cmp-intent-launcher" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.cmp.intent.launcher)
```

---

## Gate 2 + Gate 3: Not Applicable

`cmp-intent-launcher` is zero-config at the Gradle level:
- No Supabase backend
- No DI module (Compose `rememberIntentLauncher()` helper, no Koin wiring needed)
- No nav destinations

Android Activity wiring requirement: `rememberIntentLauncher()` must be called at Compose
composition time inside a `ComponentActivity`. This is a developer responsibility — see
`docs/intent-launcher/SETUP.md` Step 4. Not auto-verified by this gate.

After Gate 1 passes, sync is complete.

---

## --check (Dry Run)

```
GATE 1  Gradle   ✅  cmp-intent-launcher:3.2.11
GATE 2  Supabase N/A
GATE 3  Wiring   N/A
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-intent-launcher — COMPLETE                                ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle     ✅  cmp-intent-launcher:3.2.11              ║
║  GATE 2  Supabase   N/A  no backend                             ║
║  GATE 3  Wiring     N/A  Compose wiring (see SETUP.md Step 4)   ║
╠══════════════════════════════════════════════════════════════════╣
║  Docs: docs/intent-launcher/SETUP.md                            ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

1. **Version bump** → update `version: 3.2.11` above
2. **New API method** → update `api:` section
3. **PHPicker / NSOpenPanel (v0.2)** → add a Gate 3 section for iOS/macOS wiring when
   real implementations land; update platform support table in `docs/intent-launcher/README.md`
