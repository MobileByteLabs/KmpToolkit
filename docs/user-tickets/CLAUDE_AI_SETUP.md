# cmp-product-tickets — AI-Assisted Setup Guide

> Set up and keep `cmp-product-tickets` in sync using Claude Code in seconds.

---

## The `/lib-sync` Skill

`/lib-sync` is a verify-gated, AI-driven command that handles the full integration of any KmpToolkit module into your app — and keeps it in sync as the library evolves or your team adds new Supabase columns.

### Available from

| Context | Command |
|---------|---------|
| Framework (claude-product-cycle) | `/lib-sync cmp-product-tickets` |
| Inside kmp-toolkit repo | `/sync-product-tickets` |

---

## One-Shot First-Time Setup

Open Claude Code in your consuming app, then run:

```
/lib-sync cmp-product-tickets
```

Claude will:
1. **Add Gradle dependency** — inserts `kmptoolkit-product-tickets:3.0.0` into `libs.versions.toml` + `build.gradle.kts`
2. **Create Supabase schema** — runs the full `product_tickets` table SQL, RLS policies, `toggle_vote` and `add_comment` RPCs
3. **Wire app config** — adds `ProductTicketsConfig.init(...)` in the right place with placeholder values you fill in
4. **Install Koin module** — adds `productTicketsModule` to your DI setup
5. **Register navigation** — adds all 3 destinations to your `NavHost` with correct callback signatures

Each step is gated — if one fails, it stops and tells you exactly what to fix before continuing.

---

## Ongoing Sync (Team + Library Updates)

Run this any time:

```
/lib-sync cmp-product-tickets
```

**Scenario 1 — Library released a new version**

The skill detects the version mismatch in `libs.versions.toml`, updates it, and checks if the new version requires schema changes. If it does, it generates only the `ALTER TABLE` SQL needed — never drops your team's columns.

**Scenario 2 — Teammate added columns to `product_tickets`**

```
Your teammate ran:
  ALTER TABLE product_tickets ADD COLUMN admin_notes TEXT;
  ALTER TABLE product_tickets ADD COLUMN assigned_to UUID;
```

`/lib-sync` fetches the live schema from Supabase, compares with the library's expected schema, and shows:

```
Schema status:
  ✅ Library columns (23)  — all present
  ✅ Team columns (2)      — admin_notes, assigned_to — PRESERVED
  No migration needed.
```

**Scenario 3 — Library v3.1 adds a new required column**

```
Library v3.1 requires: escalated_at TIMESTAMPTZ

/lib-sync detects: escalated_at is MISSING from live schema
Generates:
  ALTER TABLE product_tickets
    ADD COLUMN IF NOT EXISTS escalated_at TIMESTAMPTZ;

Applies migration → re-fetches schema → confirms ✅
```

---

## Dry Run — See What Would Change

```
/lib-sync cmp-product-tickets --check
```

Shows the full status of all gates without making any changes. Useful before a release or when onboarding a new team member.

```
╔══════════════════════════════════════════════════════════════════╗
║  /lib-sync cmp-product-tickets --check (DRY RUN)                 ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle     ✅  kmptoolkit-product-tickets:3.0.0         ║
║  GATE 2  Supabase   ✅  23 columns — fully in sync               ║
║  GATE 3a Config     ✅  ProductTicketsConfig.init() found        ║
║  GATE 3b Koin       ✅  productTicketsModule installed            ║
║  GATE 3c Navigation ✅  All 3 destinations registered            ║
╚══════════════════════════════════════════════════════════════════╝
No changes needed.
```

---

## Targeted Flags

```bash
/lib-sync cmp-product-tickets --migrate-only   # Supabase schema delta only
/lib-sync cmp-product-tickets --wiring-only    # Config + Koin + Nav only
/lib-sync cmp-product-tickets --check          # Dry run, no writes
/lib-sync --all                                # Sync all installed KmpToolkit modules
```

---

## What the Gates Check

### Gate 1 — Gradle Dependency

| Check | Source |
|-------|--------|
| `kmptoolkit-product-tickets` in `libs.versions.toml` | `gradle/libs.versions.toml` |
| Correct version `3.0.0` | same file |
| Used in `commonMain.dependencies` | `build.gradle.kts` |

**Auto-fix:** Inserts the correct entry if missing or wrong version.

---

### Gate 2 — Supabase Schema

| Check | How |
|-------|-----|
| `product_tickets` table exists | Supabase REST API: `GET /rest/v1/product_tickets?limit=0` |
| All required columns present | Compare live schema vs expected 23 columns |
| `toggle_vote` RPC exists | Supabase REST API: `POST /rest/v1/rpc/toggle_vote` (dry call) |
| `add_comment` RPC exists | Supabase REST API: `POST /rest/v1/rpc/add_comment` (dry call) |
| RLS policies active | `information_schema` query |

**Expected columns (library v3.0.0, 23 total — NO `product_type`):**

| Column | Type | Default |
|--------|------|---------|
| `id` | UUID | `gen_random_uuid()` |
| `ticket_type` | TEXT NOT NULL | `'feature_request'` |
| `title` | TEXT NOT NULL | — |
| `description` | TEXT NOT NULL | — |
| `category` | TEXT | `'general'` |
| `status` | TEXT | `'pending'` |
| `priority` | TEXT | `'medium'` |
| `platform` | TEXT | NULL |
| `app_version` | TEXT | NULL |
| `milestone` | TEXT | NULL |
| `labels` | JSONB | `'[]'` |
| `attachments` | JSONB | `'[]'` |
| `is_private` | BOOLEAN | `false` |
| `user_id` | TEXT | NULL |
| `user_email` | TEXT | NULL |
| `device_info` | TEXT | NULL |
| `upvotes` | INT | `0` |
| `admin_response` | TEXT | NULL |
| `responded_at` | TIMESTAMPTZ | NULL |
| `severity` | TEXT | NULL |
| `resolution` | TEXT | NULL |
| `created_at` | TIMESTAMPTZ | `NOW()` |
| `updated_at` | TIMESTAMPTZ | `NOW()` |

> **Note**: `product_type` column has been removed in v3.0.0. Each app has its own Supabase project. Isolation is at the project level, not the table level.

**Delta rules:**
- NEVER drops columns
- NEVER alters existing column types
- ONLY adds missing columns via `ADD COLUMN IF NOT EXISTS`
- Team-added columns are preserved and shown in output

---

### Gate 3 — App Wiring

| Sub-gate | Check | File |
|----------|-------|------|
| 3a Config | `ProductTicketsConfig.init(...)` called | Application/App class |
| 3b Koin | `productTicketsModule` in module list | DI setup file |
| 3c Navigation | All 3 destinations in NavHost | NavGraph file |

**3 required destinations:**
```kotlin
productTicketsDestination(onBackClick, onNavigateToCreateTicket, onNavigateToTicketDetail)
createTicketDestination(onBackClick)
ticketDetailDestination(onBackClick)
```

---

## Providing Supabase Credentials

The skill looks for credentials in this order:

1. **Environment variables** (CI/team preferred):
   ```bash
   export SUPABASE_URL="https://your-project.supabase.co"
   export SUPABASE_ANON_KEY="your-anon-key"
   ```

2. **Prompted at runtime** — if not found in env, Claude asks you to paste them (never stored in files)

3. **Already in source** — if `ProductTicketsConfig.init()` already has real values, the skill reads them from there

---

## Team Onboarding Workflow

For a new team member integrating `cmp-product-tickets` for the first time:

```bash
# 1. Open the consuming app in Claude Code
# 2. Run:
/lib-sync cmp-product-tickets

# Claude handles everything. Expected output:
# GATE 1  Gradle     ⚡ Added kmptoolkit-product-tickets:3.0.0
# GATE 2  Supabase   ✅ Table exists, fully in sync
# GATE 3a Config     ⚡ Added ProductTicketsConfig.init() — fill in credentials
# GATE 3b Koin       ⚡ Added productTicketsModule
# GATE 3c Navigation ⚡ Added 3 destinations to NavHost
# Done. Fill in SUPABASE_URL and SUPABASE_ANON_KEY, then build.
```

Total time: under 60 seconds.

---

## Library Development Mode

If you're developing the `cmp-product-tickets` library itself (not a consumer app), use the `/tickets` matrix from the framework:

```
/tickets
→ [C] CMP sync    # Shows framework schema vs library schema diff
```

The `[C] CMP sync` option shows which columns in `SCHEMA_CONTRACT.yaml` are missing from the library's Kotlin models. Use `[G] Generate code` to auto-apply changes via the LIBRARY DEVELOPMENT MODE pipeline:

```
Step 1: Parse SCHEMA_CONTRACT.yaml diff
Step 2: Read SOURCE_MAP (exact Kotlin file paths)
Step 3: Edit UserTicket.kt — add missing fields
Step 4: Edit UserTicketInsert.kt — add user-submittable fields only
Step 5: Edit UserTicketDto.kt — add @SerialName fields
Step 6: Test gate — compile KMP (iOS + Android) MANDATORY
Step 7: Prompt release if tests pass
```

See [LIBRARY_DEV.md](LIBRARY_DEV.md) for the full library development guide.

---

## Migrating from cmp-user-tickets (v2.x)

Run the migration sync to update all API references:

```
/lib-sync cmp-product-tickets
```

The skill detects v2.x artifacts and guides you through:

| Change | From (v2.x) | To (v3.0.0) |
|--------|------------|-------------|
| Gradle artifact | `kmptoolkit-user-tickets` | `kmptoolkit-product-tickets` |
| Config class | `FeatureRequestConfig.init(url, key, productType, userId?)` | `ProductTicketsConfig.init(url, key, userId?)` |
| DI module | `featureRequestModule` | `productTicketsModule` |
| Nav destination | `featureWishlistDestination` | `productTicketsDestination` |
| Package | `com.mobilebytelabs.usertickets` | `com.mobilebytelabs.producttickets` |
| Table | `user_tickets` | `product_tickets` |
| Removed param | `productType` | *(removed — per-project isolation)* |
