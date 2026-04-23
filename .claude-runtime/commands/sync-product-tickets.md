# /sync-product-tickets - Full Instructions

> **Single source of truth** for `cmp-product-tickets` sync contract.
> Renamed from cmp-user-tickets (v2.x). The framework `/lib-sync cmp-product-tickets` delegates to this file.
> Update this file when the library evolves (new columns, new version, new nav API).
>
> **BREAKING CHANGE from v2.x:**
> - `product_type` REMOVED — each project has its own `product_tickets` Supabase table
> - Table renamed: `user_tickets` → `product_tickets`
> - Package renamed: `com.mobilebytelabs.usertickets` → `com.mobilebytelabs.producttickets`
> - Config class: `FeatureRequestConfig` → `ProductTicketsConfig` (no productType param)

---

# /sync-product-tickets — cmp-product-tickets Sync

Verify-gated sync of `cmp-product-tickets` into a consuming KMP app.
Handles first-time setup and ongoing migration as the library or team schema evolves.

---

## Module Contract (update when library changes)

```yaml
module:         cmp-product-tickets
artifact:       io.github.mobilebytelabs:kmptoolkit-product-tickets
version:        "3.0.0"
package:        com.mobilebytelabs.producttickets
supabase_table: product_tickets    # per-project table — NO product_type filter
breaking_changes_from: "2.x — product_type removed, table renamed user_tickets→product_tickets"
supabase_rpcs:
  - toggle_vote        # unique per-user vote via ticket_votes table
  - add_comment        # inserts into ticket_comments table

config:
  class:  com.mobilebytelabs.producttickets.config.ProductTicketsConfig
  init:   ProductTicketsConfig.init(supabaseUrl, supabaseAnonKey, userId?)
  note:   userId is optional — enables My Tickets tab + Contact Support private tickets
  note2:  NO productType param — isolation is at Supabase project level, not column level

di:
  module:  productTicketsModule
  import:  com.mobilebytelabs.producttickets.di.productTicketsModule

nav:
  destinations:
    - productTicketsDestination(onBackClick, onNavigateToCreateTicket, onNavigateToTicketDetail)
    - createTicketDestination(onBackClick)
    - ticketDetailDestination(onBackClick)
  imports:
    - com.mobilebytelabs.producttickets.ui.productTicketsDestination
    - com.mobilebytelabs.producttickets.ui.createTicketDestination
    - com.mobilebytelabs.producttickets.ui.ticketDetailDestination
    - com.mobilebytelabs.producttickets.ui.navigateToProductTickets
    - com.mobilebytelabs.producttickets.ui.navigateToCreateTicket
    - com.mobilebytelabs.producttickets.ui.navigateToTicketDetail

# EXPECTED SCHEMA — source of truth for Gate 2 delta engine
# When adding a new column: append here + bump version above
schema:
  table: product_tickets
  columns:
    # Base columns (v3.0 — from framework SCHEMA_CONTRACT.yaml base_schema)
    - { name: id,             type: UUID,        nullable: false, default: "gen_random_uuid()" }
    - { name: ticket_type,    type: TEXT,        nullable: false, default: "'feature_request'" }
    - { name: title,          type: TEXT,        nullable: false }
    - { name: description,    type: TEXT,        nullable: true }
    - { name: category,       type: TEXT,        nullable: true,  default: "'general'" }
    - { name: status,         type: TEXT,        nullable: true,  default: "'pending'" }
    - { name: priority,       type: TEXT,        nullable: true,  default: "'medium'" }
    - { name: platform,       type: TEXT,        nullable: true }
    - { name: app_version,    type: TEXT,        nullable: true }
    - { name: milestone,      type: TEXT,        nullable: true }
    - { name: labels,         type: TEXT[],      nullable: true,  default: "ARRAY[]::TEXT[]" }
    - { name: attachments,    type: TEXT[],      nullable: true,  default: "ARRAY[]::TEXT[]" }
    - { name: is_private,     type: BOOLEAN,     nullable: false, default: "false" }
    - { name: user_id,        type: TEXT,        nullable: true }
    - { name: user_email,     type: TEXT,        nullable: true }
    - { name: device_info,    type: JSONB,       nullable: true }
    - { name: upvotes,        type: INT,         nullable: false, default: "0" }
    - { name: admin_response, type: TEXT,        nullable: true }
    - { name: responded_at,   type: TIMESTAMPTZ, nullable: true }
    - { name: severity,       type: TEXT,        nullable: true }
    - { name: resolution,     type: TEXT,        nullable: true }
    - { name: created_at,     type: TIMESTAMPTZ, nullable: true,  default: "NOW()" }
    - { name: updated_at,     type: TIMESTAMPTZ, nullable: true,  default: "NOW()" }

  # Related tables — verified separately from product_tickets columns
  related_tables:
    - table: ticket_votes
      purpose: "One vote per user per ticket (unique constraint on ticket_id + voter_id)"
      columns:
        - { name: id,         type: UUID, nullable: false, default: "gen_random_uuid()" }
        - { name: ticket_id,  type: UUID, nullable: false }
        - { name: voter_id,   type: TEXT, nullable: false }
        - { name: created_at, type: TIMESTAMPTZ, nullable: true, default: "NOW()" }
      rpcs:
        - toggle_vote(p_ticket_id UUID, p_voter_id TEXT) RETURNS INT

    - table: ticket_comments
      purpose: "Admin and user comments on tickets"
      columns:
        - { name: id,          type: UUID, nullable: false, default: "gen_random_uuid()" }
        - { name: ticket_id,   type: UUID, nullable: false }
        - { name: author_type, type: TEXT, nullable: false, default: "'user'" }
        - { name: author_name, type: TEXT, nullable: false, default: "'Anonymous'" }
        - { name: content,     type: TEXT, nullable: false }
        - { name: created_at,  type: TIMESTAMPTZ, nullable: true, default: "NOW()" }
      rpcs:
        - add_comment(p_ticket_id UUID, p_author_type TEXT, p_author_name TEXT, p_content TEXT)
```

---

## SOURCE_MAP — Exact File Paths for Auto-Edit (E2E-005)

Base path: `workspaces/mbs/kmp-toolkit/source/kmp-toolkit/`

| Symbol | File Path | Edit Type |
|--------|-----------|-----------|
| UserTicket.kt (domain model) | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/domain/model/UserTicket.kt` | Add fields |
| UserTicketInsert.kt (submit model) | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/domain/model/UserTicketInsert.kt` | Add fields if user-submittable |
| UserTicketDto.kt (DTO) | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/data/remote/dto/UserTicketDto.kt` | Add @SerialName fields |
| ProductTicketsConfig.kt | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/config/ProductTicketsConfig.kt` | Do NOT modify (stable API) |
| productTicketsModule (DI) | `cmp-product-tickets/src/commonMain/kotlin/com/mobilebytelabs/producttickets/di/ProductTicketsModule.kt` | Do NOT modify (stable API) |
| build.gradle.kts | `cmp-product-tickets/build.gradle.kts` | Bump version.name + version.code |
| SYNC.md | `SYNC.md` | Update columns + version |
| sync-product-tickets.md | `.claude-runtime/commands/sync-product-tickets.md` | Update schema.columns + version |

**User-submittable columns** (add to UserTicketInsert.kt when these columns are added):
```
title, description, category, ticket_type, priority, platform, app_version,
labels, attachments, is_private, user_id, user_email, device_info, severity
```

**Admin-only columns** (NEVER add to UserTicketInsert.kt):
```
id, status, milestone, admin_response, responded_at, resolution, upvotes,
created_at, updated_at
```

---

## Version Changelog (E2E-007)

> Auto-updated by RULE-LIB-EVOLVE-TICKETS-001 on each version bump.

| Version | Date | Changes |
|---------|------|---------|
| 3.0.0 | 2026-04-22 | **BREAKING**: Renamed cmp-user-tickets → cmp-product-tickets. `product_type` removed (per-project Supabase tables). Table: `user_tickets` → `product_tickets`. Config: `FeatureRequestConfig` → `ProductTicketsConfig` (no productType param). DI: `featureRequestModule` → `productTicketsModule`. Nav: `featureWishlistDestination` → `productTicketsDestination`. Package: `com.mobilebytelabs.usertickets` → `com.mobilebytelabs.producttickets`. Added columns: priority, milestone, labels, attachments, platform, app_version, severity. |
| 2.1.0 | — | Last cmp-user-tickets release. |

---

## Usage

```bash
/sync-product-tickets                  # Full sync — all gates
/sync-product-tickets --check          # Dry run — show status, no writes
/sync-product-tickets --migrate-only   # Gate 2 only (Supabase schema)
/sync-product-tickets --wiring-only    # Gate 3 only (Config + Koin + Nav)
```

---

## Workflow

```
/sync-product-tickets
        │
        ▼
┌──────────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                        │
│  Check: kmptoolkit-product-tickets:3.0.0 in libs.versions.toml  │
│  Check: used in commonMain.dependencies                          │
│  Fix:   Auto-insert correct entries                              │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                          │
└──────────────────────────────┬───────────────────────────────────┘
                               │ PASS
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema Sync                                     │
│  Fetch:  Live schema via Supabase REST (product_tickets table)   │
│  Compare: schema.columns above vs live columns                   │
│  Delta:  Columns in schema but missing from live → ADD           │
│  Preserve: Extra live columns (team-added) → never touched       │
│  Apply:  ALTER TABLE ... ADD COLUMN IF NOT EXISTS                │
│  Verify: Re-fetch and confirm                                    │
│  Result: ✅ UP-TO-DATE / ⚡ MIGRATED / ❌ BLOCKED                │
└──────────────────────────────┬───────────────────────────────────┘
                               │ PASS
                               ▼
┌──────────────────────────────────────────────────────────────────┐
│  GATE 3: App Wiring                                               │
│  3a. ProductTicketsConfig.init() present in app init?            │
│  3b. productTicketsModule in Koin modules list?                  │
│  3c. All 3 nav destinations in NavHost?                          │
│  Fix: Auto-insert each missing piece                             │
│  Result: ✅ / ⚡ per sub-gate                                    │
└──────────────────────────────┬───────────────────────────────────┘
                               │ ALL PASS
                               ▼
              ✅ SYNC COMPLETE — print state summary
```

---

## Gate 1: Gradle

### Check
```
1. Glob: gradle/libs.versions.toml
   → search "kmptoolkit-product-tickets" in [libraries] section
   → if found: read the version.ref value
   → resolve that ref in [versions] → get actual version number
   → if resolved version != 3.0.0: mark for fix
   → if library entry missing entirely: mark for fix

   NOTE: Some projects use a shared version ref (e.g., version.ref = "kmptoolkit").
   Resolve the ref to get the actual version.
   A dedicated "kmptoolkit-product-tickets" version key is preferred.

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmptoolkit.product.tickets"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
kmptoolkit-product-tickets = "3.0.0"

# libs.versions.toml [libraries]
kmptoolkit-product-tickets = { module = "io.github.mobilebytelabs:kmptoolkit-product-tickets", version.ref = "kmptoolkit-product-tickets" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmptoolkit.product.tickets)
```

---

## Gate 2: Supabase Schema Sync

### Credential Resolution (priority order)
```
1. Env vars: $SUPABASE_URL + $SUPABASE_ANON_KEY
2. Source grep: ProductTicketsConfig.init(...) in existing code
3. AskUserQuestion: prompt user (never stored in files)
```

### Live Schema Fetch
```
GET {supabase_url}/rest/v1/product_tickets?limit=0
Headers: apikey: {anon_key}, Authorization: Bearer {anon_key}
```

### Delta Computation
```
For each column in schema.columns:
  if column.name NOT IN live_columns → MISSING → add to delta

For each column in live_columns:
  if column.name NOT IN schema.columns → TEAM_ADDED → log as preserved, skip

For each MISSING column, generate:
  ALTER TABLE product_tickets
    ADD COLUMN IF NOT EXISTS {name} {type}
    {NOT NULL if nullable=false}
    {DEFAULT {default} if default present};
```

### Delta Rules (HARD — never bypass)
- NEVER DROP any column or table
- NEVER ALTER existing column types
- ONLY ADD missing columns
- Always use `IF NOT EXISTS` — idempotent, safe to re-run
- Re-fetch after applying → confirm expected columns exist

---

## Gate 3: App Wiring

### 3a — Config Init
```
Grep: "ProductTicketsConfig.init" in **/*.kt
If found:
  ✅ init() present
  → Check for "userId" parameter in the same call block
  → If userId missing: ⚠️ WARN (not blocking):
      "userId not passed — My Tickets tab and Contact Support private tickets disabled."
  → Warn if placeholder URL still present

If missing:
  → locate app init file (Application.kt / App.kt)
  → insert:
      ProductTicketsConfig.init(
          supabaseUrl     = "https://YOUR_PROJECT.supabase.co",  // TODO
          supabaseAnonKey = "YOUR_ANON_KEY",                     // TODO
          userId          = null,   // optional: set after auth
      )
  → add import: com.mobilebytelabs.producttickets.config.ProductTicketsConfig
```

### 3b — Koin Module
```
Grep: "productTicketsModule" in **/*.kt
If found: ✅
If missing:
  → locate startKoin { modules(...) }
  → append: productTicketsModule
  → add import: com.mobilebytelabs.producttickets.di.productTicketsModule
```

### 3c — Navigation Destinations
```
Grep: "productTicketsDestination", "createTicketDestination", "ticketDetailDestination"

For each missing destination:
  → locate NavHost { } block
  → insert with correct signature + imports (see nav.imports above)

productTicketsDestination(
    onBackClick = { navController.popBackStack() },
    onNavigateToCreateTicket = { type -> navController.navigateToCreateTicket(type) },
    onNavigateToTicketDetail = { id  -> navController.navigateToTicketDetail(id) },
)
createTicketDestination(onBackClick = { navController.popBackStack() })
ticketDetailDestination(onBackClick = { navController.popBackStack() })
```

---

## --check (Dry Run)

```
GATE 1  Gradle     ✅  kmptoolkit-product-tickets:3.0.0
GATE 2  Supabase   [WOULD ADD] platform TEXT
GATE 3a Config     ✅  ProductTicketsConfig.init() found
GATE 3b Koin       [WOULD ADD] productTicketsModule
GATE 3c Navigation ✅  All 3 destinations registered
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-product-tickets — COMPLETE                                 ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle          ✅  kmptoolkit-product-tickets:3.0.0    ║
║  GATE 2  Supabase        ⚡  1 column added (platform)           ║
║            Team columns  ✅  preserved                           ║
║  GATE 3a Config          ✅  ProductTicketsConfig.init() found   ║
║  GATE 3b Koin            ✅  productTicketsModule installed       ║
║  GATE 3c Navigation      ✅  All 3 destinations registered       ║
╠══════════════════════════════════════════════════════════════════╣
║  Schema: product_tickets 23 cols · ticket_votes ✅ · ticket_comments ✅ ║
║  Docs: docs/user-tickets/SETUP.md                                ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## LIBRARY DEVELOPMENT MODE (E2E-005, E2E-006, E2E-007, E2E-008)

Activated when: framework schema (`layers/tickets/templates/SCHEMA_CONTRACT.yaml`) has drifted from
library schema (schema.columns above). Trigger: `/tickets matrix [C] CMP sync` or when CMP_SCHEMA_PARITY = FAIL.

### Auto-Development Pipeline

1. **Read framework schema:**
   `layers/tickets/templates/SCHEMA_CONTRACT.yaml` → `base_schema` + `bug_specific_schema`

2. **Compute diff vs schema.columns above:**
   ```
   MISSING_IN_LIBRARY = base_schema columns NOT in schema.columns above
   EXTRA_IN_LIBRARY   = schema.columns NOT in base_schema (deprecated?)
   TYPE_MISMATCH      = columns present in both but different types
   ```

3. **For each MISSING_IN_LIBRARY column → generate Kotlin code:**

   **UserTicket.kt additions** (domain model):
   ```kotlin
   @SerialName("{snake_case}")
   val {camelCase}: {KotlinType}? = {default_or_null}
   ```

   **Type mapping:**
   ```
   TEXT        → String?
   INT         → Int = 0
   BOOLEAN     → Boolean = false
   TEXT[]      → List<String> = emptyList()
   JSONB       → String? (serialized JSON string)
   TIMESTAMPTZ → String? (ISO string)
   UUID        → String (non-null)
   ```

   **UserTicketDto.kt additions** (serialization):
   ```kotlin
   @SerialName("{snake_case}")
   val {camelCase}: {KotlinType}? = null,
   ```

   **UserTicketInsert.kt** (only if column is user-submittable — see SOURCE_MAP above):
   ```kotlin
   val {camelCase}: {KotlinType}? = null,
   ```

4. **Show complete diff to user for approval:**
   ```
   ═══ LIBRARY DEV: Schema Drift Detected ═══
   Missing in cmp-product-tickets:
     + {column_name} {type} (from migration {N})
     + {column_name} {type} (bug_specific)

   Generated Kotlin — UserTicket.kt:
   [full additions shown]

   Generated Kotlin — UserTicketDto.kt:
   [full additions shown]

   Generated Kotlin — UserTicketInsert.kt (user-submittable only):
   [additions if applicable]

   [A] Apply changes    [V] View full diff    [S] Skip
   ```

5. **If [A] approved — auto-edit using SOURCE_MAP paths:**
   - Edit `UserTicket.kt` — add fields after last existing field
   - Edit `UserTicketDto.kt` — add @SerialName fields
   - Edit `UserTicketInsert.kt` if any column is user-submittable
   - Bump version in `build.gradle.kts` (patch/minor/major per change type)
   - Update `schema.columns` in this file + `version` at top
   - Update `SYNC.md` (columns + version)
   - Append to `## Version Changelog` section of this file

6. **Test gate** (MANDATORY before prompting release — E2E-006):
   ```bash
   ./gradlew :cmp-product-tickets:compileKotlinIosArm64 :cmp-product-tickets:compileKotlinAndroid --quiet
   ```
   - If FAIL → show error output, DO NOT prompt release
     Show: [Fix] / [View error] / [Abort]
   - If PASS → show: "✅ Compilation passed (KMP: iOS + Android)"

7. **If test gate PASS → prompt:**
   ```
   Library changes compiled successfully.
   Changes: {summary of added columns}
   Version: {old} → {new}

   Next step: /tickets release to publish v{new_version}
   ```

---

## How to Evolve This File

When the library releases a new version:

1. **New column added** → append to `schema.columns`, bump `version`, append to `## Version Changelog`
2. **New nav destination** → add to `nav.destinations` + `nav.imports`
3. **Config signature changed** → update `config.init`
4. **New DI module** → update `di.module` + `di.import`
5. **SOURCE_MAP path changes** → update `## SOURCE_MAP` table

The framework's `/lib-sync` and RULE-LIB-EVOLVE-TICKETS-001 will automatically pick up changes.
