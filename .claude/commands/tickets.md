# /tickets — User Ticket Management

Manage user tickets from Claude Code. Dashboard, sync to YAML, reply, status updates, roadmap.

**Standalone**: This skill works in any project that uses `cmp-user-tickets` with a Supabase backend.

## Usage

```
/tickets                           # Interactive dashboard
/tickets sync                      # Pull tickets → TICKETS.yaml (edit → push)
/tickets push                      # Push YAML edits to Supabase
/tickets reply {id} "message"      # Reply to a ticket
/tickets status {id} {status}      # Change status (pending|in_review|planned|in_progress|resolved|completed|closed)
/tickets roadmap                   # Show/create roadmap items
/tickets stats                     # Quick analytics
/tickets comment {id} "message"     # Add admin comment on a ticket
/tickets dashboard                 # Open web dashboard at localhost:7575
/tickets info                      # Show this help
```

## Setup

Requires `.env` in the project with:
```
SHARED_SUPABASE_URL=https://your-project.supabase.co
SUPABASE_SERVICE_ROLE_KEY=your-service-role-key
```
Get keys from: Supabase Dashboard → Settings → API

## How It Works

### Step 0: Load Environment

1. Find `.env` — search current directory, then parent dirs, then `server-layer/.env`
2. Parse `SHARED_SUPABASE_URL` and `SUPABASE_SERVICE_ROLE_KEY`
3. Derive `product_type` from `.env` `PROJECT_NAME` or ask user
4. If keys missing → show setup guide with Supabase dashboard link

### Step 1: Route Subcommand

| Input | Action |
|-------|--------|
| (no args) | Interactive dashboard |
| `sync` | Pull to TICKETS.yaml |
| `push` | Push YAML changes |
| `reply {id} "msg"` | Reply to ticket |
| `status {id} {status}` | Change status |
| `roadmap` | Roadmap view |
| `stats` | Analytics |
| `comment {id} "msg"` | Add admin comment on a ticket |
| `dashboard` | Open web dashboard at localhost:7575 |

---

### Interactive Dashboard (no args)

Fetch all tickets and show matrix. Re-render after each action (MATRIX-LOOP).

**Fetch:**
```bash
curl -s "${SUPABASE_URL}/rest/v1/user_tickets?product_type=eq.${PRODUCT_TYPE}&order=upvotes.desc&select=*" \
  -H "apikey: ${SERVICE_ROLE_KEY}" \
  -H "Authorization: Bearer ${SERVICE_ROLE_KEY}"
```

**Display:**
```
╔══════════════════════════════════════════════════════════════════════════════╗
║  USER TICKETS — {product_type}                                               ║
║  {total} total | {pending} pending | {in_progress} active | {resolved} done  ║
╠══════════════════════════════════════════════════════════════════════════════╣
║                                                                               ║
║  #  Title                              Status        Votes  Type             ║
║  ── ──────────────────────────────────  ────────────  ─────  ────             ║
║  1  {title_40chars}                    {status}      {n}    💡|🐛|📩         ║
║  2  ...                                                                       ║
║                                                                               ║
║  [R] Reply   [S] Status   [P] Priority   [Y] Sync YAML   [A] Add roadmap    ║
║  [T] Stats   [D] Done                                                        ║
╚══════════════════════════════════════════════════════════════════════════════╝
```

**Actions:**
- [R] → ask ticket #, ask message → PATCH admin_response + responded_at
- [S] → ask ticket #, ask status → PATCH status
- [P] → ask ticket #, ask priority → PATCH priority
- [Y] → run sync (see below)
- [A] → ask title+description → INSERT roadmap_item
- [T] → show stats
- [D] → exit

After each action, re-render the dashboard with fresh data.

---

### Sync (`/tickets sync`)

Pull ALL tickets → write `TICKETS.yaml` in current directory.

```yaml
# TICKETS.yaml — synced from Supabase
# Edit status, admin_response, resolution, priority then run /tickets push
synced_at: "2026-04-21T12:00:00Z"
supabase_url: "https://sgxloojlaywdrrglfjun.supabase.co"
product_type: "reels_downloader"

tickets:
  - id: "b9567127-..."  # read-only
    title: "Facebook video download"  # read-only
    description: "artificial choper"  # read-only
    type: "feature_request"  # read-only
    category: "new_feature"  # read-only
    status: "resolved"  # EDITABLE
    priority: "low"  # EDITABLE
    admin_response: "Fixed and verified."  # EDITABLE
    resolution: "Fixed in v2.1.1"  # EDITABLE
    upvotes: 1  # read-only
    user_email: null  # read-only
    created_at: "2026-04-11"  # read-only
```

---

### Push (`/tickets push`)

1. Read `TICKETS.yaml`
2. Fetch current state from Supabase for each ticket
3. Diff editable fields: status, priority, admin_response, resolution
4. Show changes summary
5. Confirm before applying
6. PATCH each changed ticket via PostgREST service_role
7. Auto-set `responded_at` when admin_response changes

---

### Reply (`/tickets reply {id} "message"`)

```bash
curl -s -X PATCH "${SUPABASE_URL}/rest/v1/user_tickets?id=eq.${ID}" \
  -H "apikey: ${SERVICE_ROLE_KEY}" \
  -H "Authorization: Bearer ${SERVICE_ROLE_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"admin_response":"${MESSAGE}","responded_at":"${TIMESTAMP}"}'
```

Accept full UUID or first 8 chars as short ID.

---

### Status (`/tickets status {id} {status}`)

Valid statuses: `pending`, `in_review`, `planned`, `in_progress`, `resolved`, `completed`, `closed`

```bash
curl -s -X PATCH "${SUPABASE_URL}/rest/v1/user_tickets?id=eq.${ID}" \
  -H "apikey: ${SERVICE_ROLE_KEY}" \
  -H "Authorization: Bearer ${SERVICE_ROLE_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"status":"${STATUS}"}'
```

---

### Comment (`/tickets comment {id} "message"`)

```bash
# /tickets comment b9567127 "Great idea, adding to roadmap"
curl -s -X POST "${SUPABASE_URL}/rest/v1/rpc/add_comment" \
  -H "apikey: ${SERVICE_ROLE_KEY}" \
  -H "Authorization: Bearer ${SERVICE_ROLE_KEY}" \
  -H "Content-Type: application/json" \
  -d '{"p_ticket_id":"${ID}","p_author_type":"admin","p_author_name":"Admin","p_content":"${MESSAGE}"}'
```

---

### Roadmap (`/tickets roadmap`)

Show tickets with status=planned or in_progress:

```
── 🚧 In Progress ──────────────────────────────────
💡 Video playback improvements         ⬆6   medium
🐛 Login crash fix                     ⬆2   high

── 📋 Planned ───────────────────────────────────────
💡 Download queue                      ⬆8   medium
🗺️ Multi-language support              ⬆0   medium

── ✅ Recently Shipped ──────────────────────────────
💡 Facebook video download             ⬆1   Apr 21

[A] Add roadmap item   [S] Change status   [D] Done
```

**Add roadmap item:** INSERT with `ticket_type=roadmap_item`, `status=planned`, `is_private=false`

---

### Stats (`/tickets stats`)

```
Total: {n}   Open: {n}   Resolved: {n}   Closed: {n}

By Status:    pending ████████ {n}  |  in_progress ████ {n}  |  resolved ██████ {n}
By Type:      💡 feature {n}  |  🐛 bug {n}  |  📩 support {n}
Top Voted:    1. {title} ⬆{n}  2. {title} ⬆{n}  3. {title} ⬆{n}
Needs Reply:  {n} tickets without admin_response
```

---

## Supabase Table Schema

```sql
-- user_tickets (existing)
CREATE TABLE user_tickets (
    id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    product_type TEXT NOT NULL,
    ticket_type TEXT NOT NULL DEFAULT 'feature_request',
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT DEFAULT 'general',
    status TEXT DEFAULT 'pending',
    priority TEXT DEFAULT 'medium',
    is_private BOOLEAN NOT NULL DEFAULT false,
    user_id TEXT,
    user_email TEXT,
    device_info TEXT,
    resolution TEXT,
    admin_response TEXT,
    responded_at TIMESTAMPTZ,
    upvotes INT NOT NULL DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX idx_user_tickets_product ON user_tickets(product_type);
CREATE INDEX idx_user_tickets_user ON user_tickets(user_id);

-- RPC: atomic upvote
CREATE OR REPLACE FUNCTION upvote_ticket(ticket_id UUID)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    UPDATE user_tickets SET upvotes = upvotes + 1, updated_at = NOW() WHERE id = ticket_id;
END;
$$;
```

$ARGUMENTS
