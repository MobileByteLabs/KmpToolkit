# /sync-user-tickets - DEPRECATED

> ⚠️ **DEPRECATED as of 2026-04-22 (cmp-product-tickets v3.0.0)**
>
> This file describes the old `cmp-user-tickets` library (v2.x) which has been renamed to `cmp-product-tickets`.
>
> **USE INSTEAD:** `.claude-runtime/commands/sync-product-tickets.md`
>
> **Key breaking changes in v3.0.0:**
> - Module: `cmp-user-tickets` → `cmp-product-tickets`
> - Artifact: `kmptoolkit-user-tickets` → `kmptoolkit-product-tickets`
> - Table: `user_tickets` → `product_tickets` (per-project, no `product_type`)
> - Config: `FeatureRequestConfig.init(url, anonKey, productType, userId?)` → `ProductTicketsConfig.init(url, anonKey, userId?)`
> - DI: `featureRequestModule` → `productTicketsModule`
> - Nav: `featureWishlistDestination` → `productTicketsDestination`
> - Package: `com.mobilebytelabs.usertickets` → `com.mobilebytelabs.producttickets`
>
> This file is kept for historical reference only. Do not use for new integrations.

---

# /sync-user-tickets — cmp-user-tickets Sync (ARCHIVED)

> Archived content below. Use sync-product-tickets.md for all new work.

---

---

# /sync-user-tickets — cmp-user-tickets Sync

Verify-gated sync of `cmp-user-tickets` into a consuming KMP app.
Handles first-time setup and ongoing migration as the library or team schema evolves.

---

## Module Contract (update when library changes)

```yaml
module:         cmp-user-tickets
artifact:       io.github.mobilebytelabs:kmptoolkit-user-tickets
version:        2.1.0
package:        com.mobilebytelabs.usertickets
supabase_table: user_tickets
supabase_rpcs:
  - upvote_ticket      # legacy — increments upvotes (kept for backward compat)
  - toggle_vote        # v2.1 — unique per-user vote via ticket_votes table
  - add_comment        # v2.1 — inserts into ticket_comments table

config:
  class:  com.mobilebytelabs.usertickets.config.FeatureRequestConfig
  init:   FeatureRequestConfig.init(supabaseUrl, supabaseAnonKey, productType, userId?)
  note:   userId is optional — enables My Tickets tab + Contact Support private tickets

di:
  module:  featureRequestModule
  import:  com.mobilebytelabs.usertickets.di.featureRequestModule

nav:
  destinations:
    - featureWishlistDestination(onBackClick, onNavigateToCreateTicket, onNavigateToTicketDetail)
    - createTicketDestination(onBackClick)
    - ticketDetailDestination(onBackClick)
  imports:
    - com.mobilebytelabs.usertickets.ui.featureWishlistDestination
    - com.mobilebytelabs.usertickets.ui.createTicketDestination
    - com.mobilebytelabs.usertickets.ui.ticketDetailDestination
    - com.mobilebytelabs.usertickets.ui.navigateToFeatureWishlist
    - com.mobilebytelabs.usertickets.ui.navigateToCreateTicket
    - com.mobilebytelabs.usertickets.ui.navigateToTicketDetail

# EXPECTED SCHEMA — source of truth for Gate 2 delta engine
# When adding a new column: append here + bump version above
schema:
  table: user_tickets
  columns:
    # Base columns (v2.0)
    - { name: id,             type: UUID,        nullable: false, default: "gen_random_uuid()" }
    - { name: product_type,   type: TEXT,        nullable: false }
    - { name: ticket_type,    type: TEXT,        nullable: false, default: "'feature_request'" }
    - { name: title,          type: TEXT,        nullable: false }
    - { name: description,    type: TEXT,        nullable: false }
    - { name: category,       type: TEXT,        nullable: true,  default: "'general'" }
    - { name: status,         type: TEXT,        nullable: true,  default: "'pending'" }
    - { name: is_private,     type: BOOLEAN,     nullable: false, default: "false" }
    - { name: user_id,        type: TEXT,        nullable: true }
    - { name: user_email,     type: TEXT,        nullable: true }
    - { name: device_info,    type: TEXT,        nullable: true }
    - { name: resolution,     type: TEXT,        nullable: true }
    - { name: admin_response, type: TEXT,        nullable: true }
    - { name: responded_at,   type: TIMESTAMPTZ, nullable: true }
    - { name: upvotes,        type: INT,         nullable: false, default: "0" }
    - { name: created_at,     type: TIMESTAMPTZ, nullable: true,  default: "NOW()" }
    - { name: updated_at,     type: TIMESTAMPTZ, nullable: true,  default: "NOW()" }
    # Extended columns (v2.1 — migration 001, 005)
    - { name: priority,       type: TEXT,        nullable: true,  default: "'medium'" }
    - { name: milestone,      type: TEXT,        nullable: true }
    - { name: labels,         type: JSONB,       nullable: true,  default: "'[]'" }
    - { name: attachments,    type: JSONB,       nullable: true,  default: "'[]'" }

  # Related tables (v2.1) — verified separately from user_tickets columns
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

## Usage

```bash
/sync-user-tickets                  # Full sync — all gates
/sync-user-tickets --check          # Dry run — show status, no writes
/sync-user-tickets --migrate-only   # Gate 2 only (Supabase schema)
/sync-user-tickets --wiring-only    # Gate 3 only (Config + Koin + Nav)
```

---

## Workflow

```
/sync-user-tickets
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: kmptoolkit-user-tickets:2.1.0 in libs.versions.toml │
│  Check: used in commonMain.dependencies                      │
│  Fix:   Auto-insert correct entries                          │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                     │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema Sync                                │
│  Fetch:  Live schema via Supabase REST                       │
│  Compare: schema.columns above vs live columns               │
│  Delta:  Columns in schema but missing from live → ADD       │
│  Preserve: Extra live columns (team-added) → never touched   │
│  Apply:  ALTER TABLE ... ADD COLUMN IF NOT EXISTS            │
│  Verify: Re-fetch and confirm                                │
│  Result: ✅ UP-TO-DATE / ⚡ MIGRATED / ❌ BLOCKED            │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 3: App Wiring                                          │
│  3a. FeatureRequestConfig.init() present in app init?        │
│  3b. featureRequestModule in Koin modules list?              │
│  3c. All 3 nav destinations in NavHost?                      │
│  Fix: Auto-insert each missing piece                         │
│  Result: ✅ / ⚡ per sub-gate                                │
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
   → search "kmptoolkit-user-tickets" in [libraries] section
   → if found: read the version.ref value (e.g., "kmptoolkit-user-tickets" or "kmptoolkit")
   → resolve that ref in [versions] → get actual version number
   → if resolved version != 2.1.0: mark for fix
   → if library entry missing entirely: mark for fix

   NOTE: Some projects use a shared version ref (e.g., version.ref = "kmptoolkit")
   that covers multiple toolkit modules. Resolve the ref to get the actual version.
   A dedicated "kmptoolkit-user-tickets" version key is preferred for independent upgrades.

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmptoolkit.user.tickets"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
# Add a dedicated key — do NOT remove the shared "kmptoolkit" key (other modules use it)
kmptoolkit-user-tickets = "2.1.0"

# libs.versions.toml [libraries]
# Update version.ref from shared key to dedicated key
kmptoolkit-user-tickets = { module = "io.github.mobilebytelabs:kmptoolkit-user-tickets", version.ref = "kmptoolkit-user-tickets" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmptoolkit.user.tickets)
```

---

## Gate 2: Supabase Schema Sync

### Credential Resolution (priority order)
```
1. Env vars: $SUPABASE_URL + $SUPABASE_ANON_KEY
2. Source grep: FeatureRequestConfig.init(...) in existing code
3. AskUserQuestion: prompt user (never stored in files)
```

### Live Schema Fetch
```
GET {supabase_url}/rest/v1/{table}?limit=0
Headers: apikey: {anon_key}, Authorization: Bearer {anon_key}

Parse response headers / OpenAPI spec for column names + types
```

### Delta Computation
```
For each column in schema.columns:
  if column.name NOT IN live_columns → MISSING → add to delta

For each column in live_columns:
  if column.name NOT IN schema.columns → TEAM_ADDED → log as preserved, skip

For each MISSING column, generate:
  ALTER TABLE user_tickets
    ADD COLUMN IF NOT EXISTS {name} {type}
    {NOT NULL if nullable=false}
    {DEFAULT {default} if default present};
```

### Related Tables Check
```
For each table in schema.related_tables:
  GET {supabase_url}/rest/v1/{table}?limit=0
  → if 404: table MISSING → generate CREATE TABLE SQL + show for user to run
  → if 200: compare columns → ADD missing via ALTER TABLE IF NOT EXISTS

Also verify RPCs exist (optional — non-blocking warning if missing):
  GET {supabase_url}/rest/v1/rpc/{rpc_name} with empty body → 200 or 400 = exists, 404 = missing
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
Grep: "FeatureRequestConfig.init" in **/*.kt
If found:
  ✅ init() present
  → Check for "userId" parameter in the same call block
  → If userId missing: ⚠️ WARN (not blocking):
      "userId not passed — My Tickets tab and Contact Support private tickets disabled.
       Add: userId = null  (can be set later via setUserId() after auth)"
  → Warn if placeholder URL still present (contains "YOUR_PROJECT" or "YOUR_ANON_KEY")

If missing:
  → locate app init file (Application.kt / App.kt / @main)
  → insert:
      FeatureRequestConfig.init(
          supabaseUrl     = "https://YOUR_PROJECT.supabase.co",  // TODO
          supabaseAnonKey = "YOUR_ANON_KEY",                      // TODO
          productType     = "YOUR_APP_NAME",                      // TODO
          userId          = null,  // Set via setUserId() after auth
      )
  → add import: com.mobilebytelabs.usertickets.config.FeatureRequestConfig
```

### 3b — Koin Module
```
Grep: "featureRequestModule" in **/*.kt
If found: ✅
If missing:
  → locate startKoin { modules(...) }
  → append: featureRequestModule
  → add import: com.mobilebytelabs.usertickets.di.featureRequestModule
```

### 3c — Navigation Destinations
```
Grep: "featureWishlistDestination", "createTicketDestination", "ticketDetailDestination"

For each missing destination:
  → locate NavHost { } block
  → insert with correct signature + imports (see nav.imports above)

featureWishlistDestination(
    onBackClick = { navController.popBackStack() },
    onNavigateToCreateTicket = { type -> navController.navigateToCreateTicket(type) },
    onNavigateToTicketDetail = { id  -> navController.navigateToTicketDetail(id) },
)
createTicketDestination(onBackClick = { navController.popBackStack() })
ticketDetailDestination(onBackClick = { navController.popBackStack() })
```

---

## --check (Dry Run)

Show what WOULD change — no writes, no SQL.

```
GATE 1  Gradle     ✅  kmptoolkit-user-tickets:2.1.0
GATE 2  Supabase   [WOULD ADD] assigned_to UUID
GATE 3a Config     ✅  FeatureRequestConfig.init() found
GATE 3b Koin       [WOULD ADD] featureRequestModule
GATE 3c Navigation ✅  All 3 destinations registered
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-user-tickets — COMPLETE                                   ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle          ✅  kmptoolkit-user-tickets:2.1.0       ║
║  GATE 2  Supabase        ⚡  1 column added (assigned_to)        ║
║            Team columns  ✅  admin_notes, priority — preserved   ║
║  GATE 3a Config          ⚡  Added init() stub — fill credentials ║
║  GATE 3b Koin            ✅  featureRequestModule installed       ║
║  GATE 3c Navigation      ✅  All 3 destinations registered       ║
╠══════════════════════════════════════════════════════════════════╣
║  Schema: user_tickets 21 cols · ticket_votes ✅ · ticket_comments ✅ ║
║  Docs: docs/user-tickets/SETUP.md                                ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

When the library releases a new version:

1. **New column added** → append to `schema.columns`, bump `version`
2. **New nav destination** → add to `nav.destinations` + `nav.imports`
3. **Config signature changed** → update `config.init`
4. **New DI module** → update `di.module` + `di.import`

The framework's `/lib-sync` will automatically pick up the changes because it reads this file as the source of truth.
