# /sync-clipboard - Full Instructions

> **Single source of truth** for `cmp-clipboard` sync contract.
> The framework `/lib-sync cmp-clipboard` delegates to this file.
> Update this file when the library version changes.

---

# /sync-clipboard — cmp-clipboard Sync

Verify-gated sync of `cmp-clipboard` into a consuming KMP app.
Gate 1: Gradle dependency. Gate 2: N/A. Gate 3: N/A (zero-config module).

---

## Module Contract (update when library changes)

```yaml
module:    cmp-clipboard
artifact:  io.github.mobilebytelabs:kmp-clipboard
version:   2.1.0
package:   com.mobilebytelabs.kmptoolkit.clipboard
supabase:  false
di:        false   # no DI module — top-level functions
nav:       false   # no nav destinations
config:    none    # zero configuration required

api:
  - copyToClipboard(text: String): Boolean
  - getFromClipboard(): String?
  - hasClipboardText(): Boolean
  - clearClipboard()
  - createClipboardObserver(): ClipboardObserver
  - rememberClipboardObserver(): ClipboardObserver  # Compose
```

---

## Usage

```bash
/sync-clipboard           # Full sync
/sync-clipboard --check   # Dry run — show status, no writes
```

---

## Workflow

```
/sync-clipboard
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: kmp-clipboard:2.1.0 in libs.versions.toml           │
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
   → search "kmp-clipboard"
   → if found: verify version = 2.1.0
   → if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmp.clipboard"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
kmp-clipboard = "2.1.0"

# libs.versions.toml [libraries]
kmp-clipboard = { module = "io.github.mobilebytelabs:kmp-clipboard", version.ref = "kmp-clipboard" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmp.clipboard)
```

---

## Gate 2 + Gate 3: Not Applicable

`cmp-clipboard` is zero-config:
- No Supabase backend
- No DI module (top-level functions, no wiring needed)
- No nav destinations

After Gate 1 passes, sync is complete.

---

## --check (Dry Run)

```
GATE 1  Gradle   ✅  kmp-clipboard:2.1.0
GATE 2  Supabase N/A
GATE 3  Wiring   N/A
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-clipboard — COMPLETE                                      ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle     ✅  kmp-clipboard:2.1.0                     ║
║  GATE 2  Supabase   N/A  no backend                             ║
║  GATE 3  Wiring     N/A  zero-config module                     ║
╠══════════════════════════════════════════════════════════════════╣
║  Docs: docs/clipboard/SETUP.md                                  ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

1. **Version bump** → update `version: 2.1.0` above
2. **New API method** → update `api:` section
