# cmp-product-tickets — Integration Guide

> For AI-assisted one-shot setup, see [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md)

---

## Prerequisites

- Kotlin Multiplatform project with Compose Multiplatform
- Supabase project (URL + anon key) — **one dedicated project per app**
- Koin for dependency injection
- Jetpack Navigation (type-safe, `androidx.navigation:navigation-compose`)

---

## Step 1 — Add Dependency

```toml
# gradle/libs.versions.toml
[versions]
kmptoolkit-product-tickets = "3.0.0"

[libraries]
kmptoolkit-product-tickets = { module = "io.github.mobilebytelabs:kmptoolkit-product-tickets", version.ref = "kmptoolkit-product-tickets" }
```

```kotlin
// shared/build.gradle.kts (or your KMP module)
commonMain.dependencies {
    implementation(libs.kmptoolkit.product.tickets)
}
```

---

## Step 2 — Create Supabase Table

Run in your Supabase project's **SQL Editor**:

```sql
-- Table (no product_type — each app has its own Supabase project)
CREATE TABLE IF NOT EXISTS product_tickets (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_type TEXT NOT NULL DEFAULT 'feature_request',
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT DEFAULT 'general',
    status TEXT DEFAULT 'pending',
    priority TEXT DEFAULT 'medium',
    platform TEXT,
    app_version TEXT,
    milestone TEXT,
    labels JSONB DEFAULT '[]',
    attachments JSONB DEFAULT '[]',
    is_private BOOLEAN DEFAULT false,
    user_id TEXT,
    user_email TEXT,
    device_info TEXT,
    upvotes INT DEFAULT 0,
    admin_response TEXT,
    responded_at TIMESTAMPTZ,
    severity TEXT,
    resolution TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_product_tickets_status ON product_tickets(status);
CREATE INDEX IF NOT EXISTS idx_product_tickets_user ON product_tickets(user_id);
CREATE INDEX IF NOT EXISTS idx_product_tickets_type ON product_tickets(ticket_type);

-- Row Level Security
ALTER TABLE product_tickets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can submit public tickets"
    ON product_tickets FOR INSERT
    WITH CHECK (is_private = false OR user_id IS NOT NULL);

CREATE POLICY "Anyone can read public tickets"
    ON product_tickets FOR SELECT
    USING (is_private = false);

CREATE POLICY "Users can read their own private tickets"
    ON product_tickets FOR SELECT
    USING (is_private = true AND user_id = auth.uid()::text);

-- Toggle vote RPC (unique per-user via ticket_votes table)
CREATE TABLE IF NOT EXISTS ticket_votes (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES product_tickets(id) ON DELETE CASCADE,
    voter_id TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE(ticket_id, voter_id)
);

CREATE OR REPLACE FUNCTION toggle_vote(p_ticket_id UUID, p_voter_id TEXT)
RETURNS INT LANGUAGE plpgsql SECURITY DEFINER AS $$
DECLARE
    v_count INT;
BEGIN
    IF EXISTS (SELECT 1 FROM ticket_votes WHERE ticket_id = p_ticket_id AND voter_id = p_voter_id) THEN
        DELETE FROM ticket_votes WHERE ticket_id = p_ticket_id AND voter_id = p_voter_id;
        UPDATE product_tickets SET upvotes = upvotes - 1, updated_at = NOW() WHERE id = p_ticket_id;
    ELSE
        INSERT INTO ticket_votes (ticket_id, voter_id) VALUES (p_ticket_id, p_voter_id);
        UPDATE product_tickets SET upvotes = upvotes + 1, updated_at = NOW() WHERE id = p_ticket_id;
    END IF;
    SELECT upvotes INTO v_count FROM product_tickets WHERE id = p_ticket_id;
    RETURN v_count;
END;
$$;

-- Add comment RPC
CREATE TABLE IF NOT EXISTS ticket_comments (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    ticket_id UUID NOT NULL REFERENCES product_tickets(id) ON DELETE CASCADE,
    author_type TEXT NOT NULL DEFAULT 'user',
    author_name TEXT NOT NULL DEFAULT 'Anonymous',
    content TEXT NOT NULL,
    created_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE OR REPLACE FUNCTION add_comment(
    p_ticket_id UUID,
    p_author_type TEXT,
    p_author_name TEXT,
    p_content TEXT
) RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO ticket_comments (ticket_id, author_type, author_name, content)
    VALUES (p_ticket_id, p_author_type, p_author_name, p_content);
END;
$$;
```

> **Team note**: If your team has added extra columns to `product_tickets`, they are safe — this SQL uses `IF NOT EXISTS` and will not drop or alter existing columns.

---

## Step 3 — Initialize Config

Call this once at app startup, before any UI is shown:

```kotlin
// Android: Application.onCreate()
// iOS: AppDelegate / @main App
// KMP shared: app initialization block

import com.mobilebytelabs.producttickets.config.ProductTicketsConfig

ProductTicketsConfig.init(
    supabaseUrl = "https://your-project.supabase.co",
    supabaseAnonKey = "your-anon-key",
    userId = currentUser?.id,   // optional — enables Contact Support + My Tickets
)
```

> **Note**: There is no `productType` parameter in v3.0.0. Each app has its own Supabase project, so isolation is handled at the project level.

**`userId`** is optional. When provided:
- Contact Support tickets are private and visible only to that user
- "My Tickets" tab shows the user's own support messages

---

## Step 4 — Install Koin Module

```kotlin
import com.mobilebytelabs.producttickets.di.productTicketsModule

// In your Koin startKoin block:
startKoin {
    modules(
        productTicketsModule,
        // ... your other modules
    )
}
```

---

## Step 5 — Add Navigation

Register all three destinations in your `NavHost`:

```kotlin
import com.mobilebytelabs.producttickets.ui.productTicketsDestination
import com.mobilebytelabs.producttickets.ui.createTicketDestination
import com.mobilebytelabs.producttickets.ui.ticketDetailDestination
import com.mobilebytelabs.producttickets.ui.navigateToProductTickets
import com.mobilebytelabs.producttickets.ui.navigateToCreateTicket
import com.mobilebytelabs.producttickets.ui.navigateToTicketDetail
import com.mobilebytelabs.producttickets.domain.model.TicketType

NavHost(navController = navController, startDestination = HomeRoute) {

    // ... your other destinations

    productTicketsDestination(
        onBackClick = { navController.popBackStack() },
        onNavigateToCreateTicket = { type -> navController.navigateToCreateTicket(type) },
        onNavigateToTicketDetail = { id -> navController.navigateToTicketDetail(id) },
    )

    createTicketDestination(
        onBackClick = { navController.popBackStack() },
    )

    ticketDetailDestination(
        onBackClick = { navController.popBackStack() },
    )
}
```

**Navigate from anywhere in your app:**

```kotlin
// Open the tickets screen (Feature Requests / Bug Reports tabs)
navController.navigateToProductTickets()

// Open create form for a specific type
navController.navigateToCreateTicket(TicketType.FEATURE_REQUEST)
navController.navigateToCreateTicket(TicketType.BUG_REPORT)
navController.navigateToCreateTicket(TicketType.CONTACT_SUPPORT)

// Open ticket detail
navController.navigateToTicketDetail(ticketId)
```

---

## Verification Checklist

After integration, verify:

- [ ] App builds without errors
- [ ] `product_tickets` table exists in Supabase with all 23 columns
- [ ] `ticket_votes` and `ticket_comments` tables exist
- [ ] `toggle_vote` and `add_comment` RPCs exist in Supabase
- [ ] `ProductTicketsConfig.init()` called before navigation
- [ ] `productTicketsModule` in Koin modules list
- [ ] All 3 nav destinations registered in NavHost
- [ ] Navigating to tickets screen shows the screen (not a crash)
- [ ] Submitting a test ticket appears in Supabase dashboard
- [ ] Upvoting a ticket increments the count

Or run the AI check:

```
/lib-sync cmp-product-tickets --check
```

---

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `ProductTicketsConfig not initialized` | `init()` called too late or not at all | Call before Koin starts or in Application.onCreate |
| `No destination for ProductTicketsRoute` | Missing `productTicketsDestination` in NavHost | Add all 3 destinations |
| `Tickets not loading` | Supabase RLS or wrong project URL | Check RLS policies + SUPABASE_URL matches this app's project |
| `Toggle vote fails` | `toggle_vote` RPC or `ticket_votes` table missing | Run Step 2 SQL again |
| `Contact Support tickets missing` | `userId` is null | Pass `userId` in `ProductTicketsConfig.init()` |

---

## Supabase Keys Reference

The `/tickets` CLI skill, dashboard, and migrations require keys in a `.env` file in your project root.

### Required Keys

| Key | Purpose | Where to Get |
|-----|---------|-------------|
| `SUPABASE_URL` | Your Supabase project URL | Dashboard → Settings → API → Project URL |
| `SUPABASE_SERVICE_ROLE_KEY` | Service role key (bypasses RLS — admin only) | Dashboard → Settings → API → service_role |
| `SUPABASE_DB_URL` | Direct database connection (for migrations) | Dashboard → Settings → Database → Connection string → Session mode |

### .env Template

```bash
# .env (project root — add to .gitignore!)
SUPABASE_URL=https://your-ref.supabase.co
SUPABASE_SERVICE_ROLE_KEY=eyJ...

# Required only for running migrations with psql
SUPABASE_DB_URL=postgresql://postgres.your-ref:your-password@aws-0-region.pooler.supabase.com:5432/postgres
```

### psql Connection String Format

Always use the **session pooler** (port 5432) for migrations — never the transaction pooler (port 6543):

```bash
# Session pooler — port 5432 — supports DDL + transactions (CORRECT)
postgresql://postgres.{ref}:{password}@aws-0-{region}.pooler.supabase.com:5432/postgres

# Transaction pooler — port 6543 — DDL limited (DO NOT use for migrations)
postgresql://postgres.{ref}:{password}@aws-0-{region}.pooler.supabase.com:6543/postgres
```

### Running Migrations Manually

```bash
# Run a migration file with full error safety
psql "${SUPABASE_DB_URL}" \
  -v ON_ERROR_STOP=1 \
  -1 \
  -f migrations/001_initial_schema.sql
```

- `-v ON_ERROR_STOP=1` — halt immediately on any SQL error
- `-1` — wrap entire file in a transaction (all-or-nothing rollback on failure)

---

## Next Steps

- Run `/lib-sync cmp-product-tickets --check` to verify your integration is current with the latest library version
- See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) to automate setup and keep in sync with AI
- Library contributors: see [LIBRARY_DEV.md](LIBRARY_DEV.md)
