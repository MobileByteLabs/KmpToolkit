# /sync-remote-config - Full Instructions

> **Single source of truth** for `cmp-remote-config` sync contract.
> The framework `/lib-sync cmp-remote-config` delegates to this file.
> Update this file when the library evolves (new columns, version, nav API).

---

# /sync-remote-config — cmp-remote-config Sync

Verify-gated sync of `cmp-remote-config` into a consuming KMP app.
Gate 0: Prerequisite (cmp-user-tickets). Gate 1: Gradle. Gate 2: Supabase schema delta.
Gate 3a: Koin DI. Gate 3b: RemoteConfigHost placement.

---

## Module Contract (update when library changes)

```yaml
module:         cmp-remote-config
artifact:       io.github.mobilebytelabs:kmptoolkit-remote-config
version:        2.1.0
package:        com.mobilebytelabs.remoteconfig
supabase_table: product_remote_config
supabase_rpcs:
  - get_device_impressions(p_device_id TEXT, p_product_type TEXT)
  - record_config_impression(p_config_id UUID, p_device_id TEXT, p_product_type TEXT)
  - dismiss_config(p_config_id UUID, p_device_id TEXT, p_product_type TEXT)

prerequisite:   cmp-user-tickets (FeatureRequestConfig.init() must be present)

config:
  uses: FeatureRequestConfig.supabaseUrl + supabaseAnonKey + productType
  note: no separate config class — piggybacks on user-tickets config

di:
  module:  remoteConfigModule
  import:  com.mobilebytelabs.remoteconfig.di.remoteConfigModule

ui:
  composable: RemoteConfigHost(onAction: (ActionType, String?) -> Unit)
  import:     com.mobilebytelabs.remoteconfig.ui.RemoteConfigHost
  note:       embedded in root composable — NOT a nav destination

# EXPECTED SCHEMA — source of truth for Gate 2 delta engine
# When adding a new column: append here + bump version above
schema:
  table: product_remote_config
  columns:
    - { name: id,                     type: UUID,        nullable: false, default: "gen_random_uuid()" }
    - { name: product_type,           type: TEXT,        nullable: false }
    - { name: platform,               type: TEXT,        nullable: false, default: "'all'" }
    - { name: min_app_version,        type: TEXT,        nullable: true }
    - { name: max_app_version,        type: TEXT,        nullable: true }
    - { name: title,                  type: TEXT,        nullable: false }
    - { name: description,            type: TEXT,        nullable: true }
    - { name: image_url,              type: TEXT,        nullable: true }
    - { name: display_type,           type: TEXT,        nullable: false, default: "'dialog'" }
    - { name: priority,               type: INT,         nullable: false, default: "0" }
    - { name: is_dismissible,         type: BOOLEAN,     nullable: false, default: "true" }
    - { name: action_text,            type: TEXT,        nullable: true }
    - { name: action_type,            type: TEXT,        nullable: false, default: "'none'" }
    - { name: action_value,           type: TEXT,        nullable: true }
    - { name: secondary_action_text,  type: TEXT,        nullable: true }
    - { name: secondary_action_type,  type: TEXT,        nullable: false, default: "'dismiss'" }
    - { name: secondary_action_value, type: TEXT,        nullable: true }
    - { name: max_impressions,        type: INT,         nullable: false, default: "1" }
    - { name: cooldown_hours,         type: INT,         nullable: false, default: "24" }
    - { name: start_at,               type: TIMESTAMPTZ, nullable: true }
    - { name: end_at,                 type: TIMESTAMPTZ, nullable: true }
    - { name: is_enabled,             type: BOOLEAN,     nullable: false, default: "true" }
    - { name: accent_color,           type: TEXT,        nullable: true }
    - { name: icon_emoji,             type: TEXT,        nullable: true }
    - { name: content_json,           type: TEXT,        nullable: true }
    - { name: created_at,             type: TIMESTAMPTZ, nullable: true, default: "NOW()" }
    - { name: updated_at,             type: TIMESTAMPTZ, nullable: true, default: "NOW()" }

device_impressions_schema:
  table: device_impressions
  columns:
    - { name: id,           type: UUID,        nullable: false, default: "gen_random_uuid()" }
    - { name: config_id,    type: UUID,        nullable: false }
    - { name: device_id,    type: TEXT,        nullable: false }
    - { name: product_type, type: TEXT,        nullable: false }
    - { name: impressions,  type: INT,         nullable: false, default: "0" }
    - { name: dismissed,    type: BOOLEAN,     nullable: false, default: "false" }
    - { name: created_at,   type: TIMESTAMPTZ, nullable: true,  default: "NOW()" }
    - { name: updated_at,   type: TIMESTAMPTZ, nullable: true,  default: "NOW()" }
```

---

## Usage

```bash
/sync-remote-config                 # Full sync — all gates
/sync-remote-config --check         # Dry run — show status, no writes
/sync-remote-config --migrate-only  # Gate 2 only (Supabase schema)
/sync-remote-config --wiring-only   # Gates 3a+3b only
```

---

## Workflow

```
/sync-remote-config
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 0: Prerequisite                                        │
│  Grep: "FeatureRequestConfig.init" in **/*.kt               │
│  If missing: BLOCK — "Configure cmp-user-tickets first"      │
│  Result: ✅ PASS / ❌ BLOCKED                                │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: kmptoolkit-remote-config:2.1.0 in libs.versions.toml│
│  Check: used in commonMain.dependencies                      │
│  Fix:   Auto-insert correct entries                          │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                     │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema Sync                                │
│  Fetch:  Live schema for product_remote_config table        │
│  Compare: schema.columns above vs live columns               │
│  Delta:  Columns in schema but missing from live → ADD       │
│  Preserve: Extra live columns (team-added) → never touched   │
│  Apply:  ALTER TABLE ... ADD COLUMN IF NOT EXISTS            │
│  Also:   Check device_impressions table + 3 RPCs exist       │
│  Result: ✅ UP-TO-DATE / ⚡ MIGRATED / ❌ BLOCKED            │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 3a: Koin DI Module                                     │
│  Grep: "remoteConfigModule" in **/*.kt                       │
│  If found: ✅                                                │
│  If missing: append to startKoin { modules(...) }            │
│             add import: com.mobilebytelabs.remoteconfig.di.remoteConfigModule
├──────────────────────────────────────────────────────────────┤
│  GATE 3b: RemoteConfigHost Composable                        │
│  Grep: "RemoteConfigHost" in **/*.kt                         │
│  If found: ✅                                                │
│  If missing: insert in root App.kt composable Box            │
│             add import: com.mobilebytelabs.remoteconfig.ui.RemoteConfigHost
└─────────────────────────┬────────────────────────────────────┘
                          │ ALL PASS
                          ▼
              ✅ SYNC COMPLETE — print state summary
```

---

## Gate 0: Prerequisite

```
Grep: "FeatureRequestConfig.init" in **/*.kt

If NOT found:
  ❌ BLOCKED
  "cmp-remote-config requires cmp-user-tickets to be configured first.
   Run /sync-user-tickets to set up FeatureRequestConfig.init()."
```

---

## Gate 1: Gradle

### Check
```
1. Glob: gradle/libs.versions.toml
   → search "kmptoolkit-remote-config"
   → if found: verify version = 2.1.0
   → if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmptoolkit.remote.config"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
kmptoolkit-remote-config = "2.1.0"

# libs.versions.toml [libraries]
kmptoolkit-remote-config = { module = "io.github.mobilebytelabs:kmptoolkit-remote-config", version.ref = "kmptoolkit-remote-config" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmptoolkit.remote.config)
```

---

## Gate 2: Supabase Schema Sync

### Credential Resolution (priority order)
```
1. Env vars: $SUPABASE_URL + $SUPABASE_ANON_KEY
2. Source grep: FeatureRequestConfig.init(...) in existing code
3. AskUserQuestion: prompt user (never stored in files)
```

### Delta Rules (HARD — never bypass)
- NEVER DROP any column
- NEVER ALTER existing column types
- ONLY ADD missing columns
- Always use `IF NOT EXISTS` — idempotent, safe to re-run
- Re-fetch after applying → confirm expected columns exist

### device_impressions and RPCs
```
Check: SELECT table_name FROM information_schema.tables WHERE table_name = 'device_impressions'
If missing: CREATE TABLE IF NOT EXISTS (see SETUP.md for full SQL)

Check each RPC: SELECT proname FROM pg_proc WHERE proname IN
  ('get_device_impressions', 'record_config_impression', 'dismiss_config')
If missing: CREATE OR REPLACE FUNCTION (see SETUP.md for full SQL)
```

---

## Gate 3a — Koin Module

```
Grep: "remoteConfigModule" in **/*.kt
If found: ✅
If missing:
  → locate startKoin { modules(...) }
  → append: remoteConfigModule
  → add import: com.mobilebytelabs.remoteconfig.di.remoteConfigModule
```

## Gate 3b — RemoteConfigHost

```
Grep: "RemoteConfigHost" in **/*.kt
If found: ✅
If missing:
  → locate root App.kt composable (Box wrapping NavHost)
  → insert:
      RemoteConfigHost(
          onAction = { actionType, actionValue ->
              // TODO: handle ActionType.URL, DEEPLINK, STORE
          },
      )
  → add import: com.mobilebytelabs.remoteconfig.ui.RemoteConfigHost
  → add import: com.mobilebytelabs.remoteconfig.model.ActionType
```

---

## --check (Dry Run)

```
GATE 0  Prereq   ✅  FeatureRequestConfig.init() found
GATE 1  Gradle   ✅  kmptoolkit-remote-config:2.1.0
GATE 2  Supabase [WOULD ADD] content_json TEXT
GATE 3a Koin     [WOULD ADD] remoteConfigModule
GATE 3b UI       ✅  RemoteConfigHost() found
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-remote-config — COMPLETE                                  ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 0  Prereq    ✅  FeatureRequestConfig.init() found        ║
║  GATE 1  Gradle    ✅  kmptoolkit-remote-config:2.1.0           ║
║  GATE 2  Supabase  ⚡  1 column added (content_json)            ║
║            Tables  ✅  product_remote_config, device_impressions║
║            RPCs    ✅  3 RPCs present                           ║
║  GATE 3a Koin      ⚡  Added remoteConfigModule                 ║
║  GATE 3b UI Host   ✅  RemoteConfigHost() found                 ║
╠══════════════════════════════════════════════════════════════════╣
║  Schema: 26 columns — fully in sync                             ║
║  Docs: docs/remote-config/SETUP.md                             ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

When the library releases a new version:

1. **New column added** → append to `schema.columns`, bump `version`
2. **New RPC** → add to `supabase_rpcs:` list
3. **Config changed** → update `config:` section
4. **New DI module** → update `di:` section
