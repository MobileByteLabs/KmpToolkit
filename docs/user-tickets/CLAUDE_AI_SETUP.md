# cmp-user-tickets — AI-Assisted Setup Guide

> Set up and keep `cmp-user-tickets` in sync using Claude Code in seconds.

---

## The `/lib-sync` Skill

`/lib-sync` is a verify-gated, AI-driven command that handles the full integration of any KmpToolkit module into your app — and keeps it in sync as the library evolves or your team adds new Supabase columns.

### Available from

| Context | Command |
|---------|---------|
| Framework (claude-product-cycle) | `/lib-sync cmp-user-tickets` |
| Inside kmp-toolkit repo | `/sync-user-tickets` |

---

## One-Shot First-Time Setup

Open Claude Code in your consuming app, then run:

```
/lib-sync cmp-user-tickets
```

Claude will:
1. **Add Gradle dependency** — inserts `kmptoolkit-user-tickets:2.1.0` into `libs.versions.toml` + `build.gradle.kts`
2. **Create Supabase schema** — runs the full `user_tickets` table SQL, RLS policies, and `upvote_ticket` RPC
3. **Wire app config** — adds `FeatureRequestConfig.init(...)` in the right place with placeholder values you fill in
4. **Install Koin module** — adds `featureRequestModule` to your DI setup
5. **Register navigation** — adds all 3 destinations to your `NavHost` with correct callback signatures

Each step is gated — if one fails, it stops and tells you exactly what to fix before continuing.

---

## Ongoing Sync (Team + Library Updates)

Run this any time:

```
/lib-sync cmp-user-tickets
```

**Scenario 1 — Library released a new version**

The skill detects the version mismatch in `libs.versions.toml`, updates it, and checks if the new version requires schema changes. If it does, it generates only the `ALTER TABLE` SQL needed — never drops your team's columns.

**Scenario 2 — Teammate added columns to `user_tickets`**

```
Your teammate ran:
  ALTER TABLE user_tickets ADD COLUMN admin_notes TEXT;
  ALTER TABLE user_tickets ADD COLUMN priority INT DEFAULT 0;
```

`/lib-sync` fetches the live schema from Supabase, compares with the library's expected schema, and shows:

```
Schema status:
  ✅ Library columns (17)  — all present
  ✅ Team columns (2)      — admin_notes, priority — PRESERVED
  No migration needed.
```

**Scenario 3 — Library v2.2 adds a new required column**

```
Library v2.2 requires: assigned_to UUID

/lib-sync detects: assigned_to is MISSING from live schema
Generates:
  ALTER TABLE user_tickets
    ADD COLUMN IF NOT EXISTS assigned_to UUID REFERENCES auth.users(id);

Applies migration → re-fetches schema → confirms ✅
```

---

## Dry Run — See What Would Change

```
/lib-sync cmp-user-tickets --check
```

Shows the full status of all gates without making any changes. Useful before a release or when onboarding a new team member.

```
╔══════════════════════════════════════════════════════════════════╗
║  /lib-sync cmp-user-tickets --check (DRY RUN)                    ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle     ✅  kmptoolkit-user-tickets:2.1.0            ║
║  GATE 2  Supabase   ✅  17 columns — fully in sync               ║
║  GATE 3a Config     ✅  FeatureRequestConfig.init() found        ║
║  GATE 3b Koin       ✅  featureRequestModule installed            ║
║  GATE 3c Navigation ✅  All 3 destinations registered            ║
╚══════════════════════════════════════════════════════════════════╝
No changes needed.
```

---

## Targeted Flags

```bash
/lib-sync cmp-user-tickets --migrate-only   # Supabase schema delta only
/lib-sync cmp-user-tickets --wiring-only    # Config + Koin + Nav only
/lib-sync cmp-user-tickets --check          # Dry run, no writes
/lib-sync --all                             # Sync all installed KmpToolkit modules
```

---

## What the Gates Check

### Gate 1 — Gradle Dependency

| Check | Source |
|-------|--------|
| `kmptoolkit-user-tickets` in `libs.versions.toml` | `gradle/libs.versions.toml` |
| Correct version `2.1.0` | same file |
| Used in `commonMain.dependencies` | `build.gradle.kts` |

**Auto-fix:** Inserts the correct entry if missing or wrong version.

---

### Gate 2 — Supabase Schema

| Check | How |
|-------|-----|
| `user_tickets` table exists | Supabase REST API: `GET /rest/v1/user_tickets?limit=0` |
| All required columns present | Compare live schema vs expected columns |
| `upvote_ticket` RPC exists | Supabase REST API: `POST /rest/v1/rpc/upvote_ticket` (dry call) |
| RLS policies active | `information_schema` query |

**Expected columns (library v2.1.0):**

| Column | Type | Default |
|--------|------|---------|
| `id` | UUID | `gen_random_uuid()` |
| `product_type` | TEXT NOT NULL | — |
| `ticket_type` | TEXT | `'feature_request'` |
| `title` | TEXT NOT NULL | — |
| `description` | TEXT NOT NULL | — |
| `category` | TEXT | `'general'` |
| `status` | TEXT | `'pending'` |
| `is_private` | BOOLEAN | `false` |
| `user_id` | TEXT | NULL |
| `user_email` | TEXT | NULL |
| `device_info` | TEXT | NULL |
| `resolution` | TEXT | NULL |
| `admin_response` | TEXT | NULL |
| `responded_at` | TIMESTAMPTZ | NULL |
| `upvotes` | INT | `0` |
| `created_at` | TIMESTAMPTZ | `NOW()` |
| `updated_at` | TIMESTAMPTZ | `NOW()` |

**Delta rules:**
- NEVER drops columns
- NEVER alters existing column types
- ONLY adds missing columns via `ADD COLUMN IF NOT EXISTS`
- Team-added columns are preserved and shown in output

---

### Gate 3 — App Wiring

| Sub-gate | Check | File |
|----------|-------|------|
| 3a Config | `FeatureRequestConfig.init(...)` called | Application/App class |
| 3b Koin | `featureRequestModule` in module list | DI setup file |
| 3c Navigation | All 3 destinations in NavHost | NavGraph file |

**3 required destinations:**
```kotlin
featureWishlistDestination(onBackClick, onNavigateToCreateTicket, onNavigateToTicketDetail)
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

3. **Already in source** — if `FeatureRequestConfig.init()` already has real values, the skill reads them from there

---

## Team Onboarding Workflow

For a new team member integrating `cmp-user-tickets` for the first time:

```bash
# 1. Open the consuming app in Claude Code
# 2. Run:
/lib-sync cmp-user-tickets

# Claude handles everything. Expected output:
# GATE 1  Gradle     ⚡ Added kmptoolkit-user-tickets:2.1.0
# GATE 2  Supabase   ✅ Table exists, fully in sync
# GATE 3a Config     ⚡ Added FeatureRequestConfig.init() — fill in credentials
# GATE 3b Koin       ⚡ Added featureRequestModule
# GATE 3c Navigation ⚡ Added 3 destinations to NavHost
# Done. Fill in SUPABASE_URL and SUPABASE_ANON_KEY, then build.
```

Total time: under 60 seconds.
