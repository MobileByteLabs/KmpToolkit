# /sync-share - Full Instructions

> **Single source of truth** for `cmp-share` sync contract.
> The framework `/lib-sync cmp-share` delegates to this file.
> Update this file when the library version changes.

---

# /sync-share — cmp-share Sync

Verify-gated sync of `cmp-share` into a consuming KMP app.
Gate 1: Gradle dependency. Gate 2: N/A. Gate 3: N/A (zero-config module).

---

## Module Contract (update when library changes)

```yaml
module:    cmp-share
artifact:  io.github.mobilebytelabs:cmp-share
version:   3.2.11
package:   com.mobilebytelabs.kmptoolkit.share
supabase:  false
di:        false   # no DI module — expect object Share + extension functions
nav:       false   # no nav destinations
config:    none    # zero configuration required

api:
  - Share.share(payload: SharePayload, options: ShareOptions): ShareResult
  - Share.text(content: String, options: ShareOptions): ShareResult
  - Share.url(href: String, options: ShareOptions): ShareResult
  - Share.image(bytes: ByteArray, mimeType: String, filename: String?): ShareResult
  - Share.file(bytes: ByteArray, mimeType: String, filename: String): ShareResult
  - Share.multi(payloads: List<SharePayload>, options: ShareOptions): ShareResult
```

---

## Usage

```bash
/sync-share           # Full sync
/sync-share --check   # Dry run — show status, no writes
```

---

## Workflow

```
/sync-share
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: cmp-share:3.2.11 in libs.versions.toml              │
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
   → search "cmp-share"
   → if found: verify version = 3.2.11
   → if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "cmp.share"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
cmp-share = "3.2.11"

# libs.versions.toml [libraries]
cmp-share = { module = "io.github.mobilebytelabs:cmp-share", version.ref = "cmp-share" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.cmp.share)
```

---

## Gate 2 + Gate 3: Not Applicable

`cmp-share` is zero-config:
- No Supabase backend
- No DI module (expect object + extension functions, no wiring needed)
- No nav destinations

After Gate 1 passes, sync is complete.

---

## --check (Dry Run)

```
GATE 1  Gradle   ✅  cmp-share:3.2.11
GATE 2  Supabase N/A
GATE 3  Wiring   N/A
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-share — COMPLETE                                          ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle     ✅  cmp-share:3.2.11                        ║
║  GATE 2  Supabase   N/A  no backend                             ║
║  GATE 3  Wiring     N/A  zero-config module                     ║
╠══════════════════════════════════════════════════════════════════╣
║  Docs: docs/share/SETUP.md                                      ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

1. **Version bump** → update `version: 3.2.11` above
2. **New API method** → update `api:` section
3. **New platform gate** → add a Gate 2 / Gate 3 section (see `sync-network-monitor.md`
   as reference for zero-config modules; `sync-product-tickets.md` for full 3-gate modules)
