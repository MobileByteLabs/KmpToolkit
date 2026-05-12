# /sync-remote-config - Full Instructions

> **Single source of truth** for `cmp-remote-config` sync contract.
> The framework `/lib-sync cmp-remote-config` delegates to this file.
> Update this file when the library evolves (new columns, version, DSL surface).

---

# /sync-remote-config — cmp-remote-config Sync

Verify-gated sync of `cmp-remote-config` 4.0.0+ into a consuming KMP app.
Gate 1: Gradle. Gate 2: Supabase schema delta. Gate 3a: `remoteConfig { … }`
DSL placement. Gate 3b: `RemoteConfigHost` placement.

> **No Gate 0 prerequisite.** `cmp-remote-config` 4.0.0+ is standalone —
> no dependency on `cmp-product-tickets` or any other module.

---

## Module Contract (update when library changes)

```yaml
module:         cmp-remote-config
artifact:       io.github.mobilebytelabs:kmptoolkit-remote-config
version:        4.0.0
package:        com.mobilebytelabs.remoteconfig
supabase_table: product_remote_config   # per-project, NO product_type column
supabase_rpcs:
  - get_device_impressions(p_device_id TEXT)
  - record_config_impression(p_config_id UUID, p_device_id TEXT)
  - dismiss_config(p_config_id UUID, p_device_id TEXT)

prerequisite:   none   # standalone module — no cross-module deps

install:
  api:        fun Module.remoteConfig(block: RemoteConfigBuilder.() -> Unit)
  import:     com.mobilebytelabs.remoteconfig.remoteConfig
  usage:      |
    val networkModule = module {
        remoteConfig {
            supabaseUrl = "..."
            supabaseKey = "..."
            action(ActionType.PREMIUM) { _, _ -> navigateToPaywall() }
            action("open_downloads")   { v, _ -> navigateToDownloads(v) }
        }
        // … other bindings …
    }

config:
  via: remoteConfig { supabaseUrl = ...; supabaseKey = ... } DSL block
  fields:
    - supabaseUrl: String   # required, non-blank
    - supabaseKey: String   # required, non-blank
  removed_in_4.0.0:
    - productType / boardType — per-project Supabase model
  validation: require() at module-build time — fails Koin graph build with a named error

action_handlers:
  api:    action(type, handler) inside remoteConfig { } DSL
  type:   ActionType  (value class, not enum)
  builtin: NONE, URL, DEEPLINK, STORE, DISMISS, PREMIUM
  custom: ActionType("my_type")  — consumers define typed constants:
            object MyActions { val FOO = ActionType("foo") }

ui:
  composable: RemoteConfigHost(onAction: ((ActionType, String?) -> Unit)? = null)
  import:     com.mobilebytelabs.remoteconfig.ui.RemoteConfigHost
  routing:
    - If onAction lambda is passed: consumer takes full control (escape hatch).
    - If onAction is null/omitted: dispatch via ActionDispatcher to handlers
      registered through `action(...)` DSL in remoteConfig { }.

# EXPECTED SCHEMA — source of truth for Gate 2 delta engine
# When adding a new column: append here + bump version above
schema:
  table: product_remote_config
  columns:
    - { name: id,                     type: UUID,        nullable: false, default: "gen_random_uuid()" }
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
  removed_in_4.0.0:
    - { name: product_type, reason: "Per-project Supabase model. Client no longer reads this column. Drop whenever convenient (NEVER drop automatically — DBA decision)." }

device_impressions_schema:
  table: device_impressions
  columns:
    - { name: id,           type: UUID,        nullable: false, default: "gen_random_uuid()" }
    - { name: config_id,    type: UUID,        nullable: false }
    - { name: device_id,    type: TEXT,        nullable: false }
    - { name: impressions,  type: INT,         nullable: false, default: "0" }
    - { name: dismissed,    type: BOOLEAN,     nullable: false, default: "false" }
    - { name: created_at,   type: TIMESTAMPTZ, nullable: true,  default: "NOW()" }
    - { name: updated_at,   type: TIMESTAMPTZ, nullable: true,  default: "NOW()" }
  unique_constraint: (config_id, device_id)
  removed_in_4.0.0:
    - product_type   # no longer used by client; drop whenever convenient
```

---

## Usage

```bash
/sync-remote-config                 # Full sync — all gates
/sync-remote-config --check         # Dry run — show status, no writes
/sync-remote-config --migrate-only  # Gate 2 only (Supabase schema)
/sync-remote-config --wiring-only   # Gates 3a+3b only (DSL + RemoteConfigHost)
```

---

## Workflow

```
/sync-remote-config
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: kmptoolkit-remote-config:4.0.0+ in libs.versions.toml│
│  Check: used in commonMain.dependencies                      │
│  Fix:   Auto-insert correct entries                          │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                     │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema Sync                                │
│  Fetch:  Live schema for product_remote_config table         │
│  Compare: schema.columns above vs live columns               │
│  Delta:  Columns in schema but missing from live → ADD       │
│  Preserve: Extra live columns (team-added or legacy product  │
│            _type) → never touched, NEVER DROP                │
│  Apply:  ALTER TABLE ... ADD COLUMN IF NOT EXISTS            │
│  Also:   Check device_impressions table + 3 RPCs exist       │
│  Result: ✅ UP-TO-DATE / ⚡ MIGRATED / ❌ BLOCKED            │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 3a: remoteConfig { … } DSL                             │
│  Grep: "remoteConfig {" in **/*.kt                           │
│  If found: ✅                                                │
│  If missing:                                                 │
│    - Locate a Koin module { … } (prefer networkModule)       │
│    - Insert remoteConfig { supabaseUrl = ...; supabaseKey   │
│      = ... } at the top of the module body                   │
│    - Add import: com.mobilebytelabs.remoteconfig.remoteConfig│
│    - Add import: com.mobilebytelabs.remoteconfig.model.      │
│      ActionType (only if action(...) calls are inserted)     │
├──────────────────────────────────────────────────────────────┤
│  GATE 3b: RemoteConfigHost Composable                        │
│  Grep: "RemoteConfigHost" in **/*.kt                         │
│  If found: ✅                                                │
│  If missing: insert in root App.kt composable Box            │
│             add import: com.mobilebytelabs.remoteconfig.ui.  │
│             RemoteConfigHost                                 │
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
   → search "kmptoolkit-remote-config"
   → if found: verify version ≥ 4.0.0
   → if missing or older: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmptoolkit.remote.config"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
kmptoolkit-remote-config = "4.0.0"

# libs.versions.toml [libraries]
kmptoolkit-remote-config = { module = "io.github.mobilebytelabs:kmptoolkit-remote-config", version.ref = "kmptoolkit-remote-config" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmptoolkit.remote.config)
```

### 3.x Cleanup (auto-applied during migration)
If the project still has `RemoteConfigConfig.supabaseUrl = …`, `initRemoteConfig()`,
or `remoteConfigModule` in `modules(…)`, mark those for rewrite in Gate 3a.

---

## Gate 2: Supabase Schema Sync

### Credential Resolution (priority order)
```
1. Env vars: $SUPABASE_URL + $SUPABASE_ANON_KEY
2. Source grep: remoteConfig { supabaseUrl = ... } in existing code
3. AskUserQuestion: prompt user (never stored in files)
```

### Delta Rules (HARD — never bypass)
- NEVER DROP any column (including the legacy `product_type` — DBA decides when)
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
If missing: CREATE OR REPLACE FUNCTION (see SETUP.md for full SQL — note the
4.0.0 signatures drop the p_product_type argument; if older signature exists,
keeping it harmless — client no longer passes it)
```

---

## Gate 3a — remoteConfig { … } DSL

```
Grep: "remoteConfig {" in **/*.kt
If found: ✅
If missing:
  → locate a Koin module declaration (prefer one named `networkModule`,
    else the first `module { … }` block in the wiring file)
  → insert at the top of the module body:

        remoteConfig {
            supabaseUrl = "https://YOUR_PROJECT.supabase.co"
            supabaseKey = "YOUR_ANON_KEY"
            // TODO: register custom action handlers as needed:
            // action(ActionType.PREMIUM) { _, _ -> /* navigate to paywall */ }
        }

  → add import: com.mobilebytelabs.remoteconfig.remoteConfig
  → if any action(...) lines are inserted: add
        import com.mobilebytelabs.remoteconfig.model.ActionType
```

### Legacy 3.x rewrite (auto)

Detect any of:
- `import com.mobilebytelabs.remoteconfig.di.remoteConfigModule`
- `remoteConfigModule,` inside `modules(…)`
- `import com.mobilebytelabs.remoteconfig.di.RemoteConfigConfig`
- `RemoteConfigConfig.supabaseUrl = …`
- `fun initRemoteConfig()` and its `Application` call site

For each match:
1. Delete the import + module-list entry + initRemoteConfig() function + its call.
2. Replace with the `remoteConfig { … }` block inside an existing module.
3. Drop the `productType` line from the new block (per-project model — see CHANGELOG).

## Gate 3b — RemoteConfigHost

```
Grep: "RemoteConfigHost" in **/*.kt
If found: ✅
If missing:
  → locate root App.kt composable (Box wrapping NavHost)
  → insert:
      // Option A — let DSL handlers route actions:
      RemoteConfigHost()

      // Option B — explicit per-screen routing (uncomment if needed):
      // RemoteConfigHost(
      //     onAction = { actionType, actionValue ->
      //         // TODO: handle ActionType.URL, DEEPLINK, STORE, PREMIUM, …
      //     },
      // )
  → add import: com.mobilebytelabs.remoteconfig.ui.RemoteConfigHost
  → add import: com.mobilebytelabs.remoteconfig.model.ActionType
    (only if the explicit onAction `when (actionType)` is uncommented)
```

---

## --check (Dry Run)

```
GATE 1  Gradle   ✅  kmptoolkit-remote-config:4.0.0
GATE 2  Supabase [WOULD ADD] content_json TEXT
GATE 3a DSL      [WOULD ADD] remoteConfig {…} to networkModule
GATE 3b UI       ✅  RemoteConfigHost() found
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-remote-config — COMPLETE                                  ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle    ✅  kmptoolkit-remote-config:4.0.0           ║
║  GATE 2  Supabase  ⚡  1 column added (content_json)            ║
║            Tables  ✅  product_remote_config, device_impressions║
║            RPCs    ✅  3 RPCs present                           ║
║  GATE 3a DSL       ⚡  Added remoteConfig {…} to networkModule  ║
║  GATE 3b UI Host   ✅  RemoteConfigHost() found                 ║
╠══════════════════════════════════════════════════════════════════╣
║  Schema: 25 columns — fully in sync                             ║
║  Docs: docs/remote-config/SETUP.md                              ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

When the library releases a new version:

1. **New column added** → append to `schema.columns`, bump `version`
2. **New RPC** → add to `supabase_rpcs:` list
3. **DSL surface changed** → update `install:` + `config:` + `action_handlers:` sections
4. **New built-in ActionType** → update `action_handlers.builtin:` list
