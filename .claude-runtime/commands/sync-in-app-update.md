# /sync-in-app-update - Full Instructions

> **Single source of truth** for `cmp-in-app-update` sync contract.
> The framework `/lib-sync cmp-in-app-update` delegates to this file.
> Update this file when the library evolves.

---

# /sync-in-app-update — cmp-in-app-update Sync

Verify-gated sync of `cmp-in-app-update` into a consuming KMP app.
Gate 1: Gradle. Gate 2: Optional Supabase `app_versions` (if using Supabase resolver).
Gate 3: AppUpdateConfig wiring.

---

## Module Contract (update when library changes)

```yaml
module:    cmp-in-app-update
artifact:  io.github.mobilebytelabs:kmp-in-app-update
version:   2.1.0
package:   com.mobilebytelabs.kmptoolkit.appupdate
supabase:  optional   # only if using .supabase() resolver
di:        false       # no DI module — stateless expect object
nav:       false       # no nav destinations

config:
  class:  com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateConfig
  init:   AppUpdateConfig.builder().android().ios().github()/supabase()/versionResolver().build()

api:
  - AppUpdate.checkForUpdate(config): UpdateResult
  - AppUpdate.startUpdate(updateType, config): UpdateResult
  - AppUpdate.getCurrentVersion(): AppVersion
  - AppUpdate.openStoreForUpdate(config): Boolean
  - AppUpdate.isSupported(): Boolean

optional_supabase_schema:
  table: app_versions
  columns:
    - { name: id,            type: SERIAL,      pk: true }
    - { name: version,       type: TEXT,        nullable: false }
    - { name: update_type,   type: TEXT,        default: "'FLEXIBLE'" }
    - { name: release_notes, type: TEXT,        nullable: true }
    - { name: download_url,  type: TEXT,        nullable: true }
    - { name: platform,      type: TEXT,        default: "'all'" }
    - { name: created_at,    type: TIMESTAMPTZ, default: "NOW()" }
```

---

## Usage

```bash
/sync-in-app-update                 # Full sync — all gates
/sync-in-app-update --check         # Dry run — show status, no writes
/sync-in-app-update --migrate-only  # Gate 2 only (Supabase, if applicable)
/sync-in-app-update --wiring-only   # Gate 3 only (config)
```

---

## Workflow

```
/sync-in-app-update
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: kmp-in-app-update:2.1.0 in libs.versions.toml       │
│  Check: used in commonMain.dependencies                      │
│  Fix:   Auto-insert correct entries                          │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                     │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema (CONDITIONAL)                       │
│  Only runs if: grep ".supabase(" in **/*.kt                  │
│  If not found: SKIP — N/A                                    │
│  If found:                                                   │
│    Fetch live schema for app_versions table                  │
│    Delta: ADD missing columns (NEVER DROP)                   │
│    Apply: ALTER TABLE ... ADD COLUMN IF NOT EXISTS           │
│  Result: ✅ UP-TO-DATE / ⚡ MIGRATED / N/A                  │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS / SKIP
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 3: AppUpdateConfig Wiring                              │
│  Check: AppUpdateConfig.builder() in **/*.kt                 │
│  Fix: Auto-insert builder stub in ViewModel or App           │
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
   → search "kmp-in-app-update"
   → if found: verify version = 2.1.0
   → if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmp.in.app.update"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
kmp-in-app-update = "2.1.0"

# libs.versions.toml [libraries]
kmp-in-app-update = { module = "io.github.mobilebytelabs:kmp-in-app-update", version.ref = "kmp-in-app-update" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmp.in.app.update)
```

---

## Gate 2: Supabase Schema (Conditional)

### Activation
```
Grep ".supabase(" in **/*.kt
If match found → run Gate 2
If no match    → SKIP (N/A)
```

### Credential Resolution
```
1. Env vars: $SUPABASE_URL + $SUPABASE_ANON_KEY
2. Source grep: .supabase(projectUrl = ..., anonKey = ...) in existing code
3. AskUserQuestion: prompt user (never stored in files)
```

### Delta Rules (HARD — never bypass)
- NEVER DROP any column
- NEVER ALTER existing column types
- ONLY ADD missing columns
- Always use `IF NOT EXISTS`

### Fix
```sql
CREATE TABLE IF NOT EXISTS app_versions (
    id            SERIAL PRIMARY KEY,
    version       TEXT NOT NULL,
    update_type   TEXT NOT NULL DEFAULT 'FLEXIBLE',
    release_notes TEXT,
    download_url  TEXT,
    platform      TEXT NOT NULL DEFAULT 'all',
    created_at    TIMESTAMPTZ DEFAULT NOW()
);
ALTER TABLE app_versions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read" ON app_versions FOR SELECT USING (true);
```

---

## Gate 3: AppUpdateConfig Wiring

```
Grep: "AppUpdateConfig.builder" in **/*.kt
If found: ✅
If missing:
  → locate ViewModel or app init
  → insert:
      import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateConfig
      import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdate

      val config = AppUpdateConfig.builder()
          .android("YOUR_PACKAGE_NAME")   // TODO
          .ios("YOUR_APP_STORE_ID")       // TODO
          .github(owner = "YOUR_ORG", repo = "YOUR_REPO")  // TODO or use .supabase()
          .build()
```

---

## --check (Dry Run)

```
GATE 1  Gradle   ✅  kmp-in-app-update:2.1.0
GATE 2  Supabase N/A  .supabase() resolver not detected
GATE 3  Config   [WOULD ADD] AppUpdateConfig.builder() stub
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-in-app-update — COMPLETE                                  ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle    ✅  kmp-in-app-update:2.1.0                  ║
║  GATE 2  Supabase  N/A  Supabase resolver not used              ║
║  GATE 3  Config    ⚡  Added AppUpdateConfig.builder() stub      ║
╠══════════════════════════════════════════════════════════════════╣
║  Docs: docs/in-app-update/SETUP.md                              ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

1. **Version bump** → update `version: 2.1.0`
2. **Supabase schema change** → update `optional_supabase_schema.columns`
3. **New API** → update `api:` section
