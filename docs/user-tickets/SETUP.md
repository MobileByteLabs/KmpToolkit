# cmp-user-tickets — Integration Guide

> For AI-assisted one-shot setup, see [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md)

---

## Prerequisites

- Kotlin Multiplatform project with Compose Multiplatform
- Supabase project (URL + anon key)
- Koin for dependency injection
- Jetpack Navigation (type-safe, `androidx.navigation:navigation-compose`)

---

## Step 1 — Add Dependency

```toml
# gradle/libs.versions.toml
[versions]
kmptoolkit-user-tickets = "2.1.0"

[libraries]
kmptoolkit-user-tickets = { module = "io.github.mobilebytelabs:kmptoolkit-user-tickets", version.ref = "kmptoolkit-user-tickets" }
```

```kotlin
// shared/build.gradle.kts (or your KMP module)
commonMain.dependencies {
    implementation(libs.kmptoolkit.user.tickets)
}
```

---

## Step 2 — Create Supabase Table

Run in your Supabase project's **SQL Editor**:

```sql
-- Table
CREATE TABLE IF NOT EXISTS user_tickets (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    product_type TEXT NOT NULL,
    ticket_type TEXT NOT NULL DEFAULT 'feature_request',
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT DEFAULT 'general',
    status TEXT DEFAULT 'pending',
    is_private BOOLEAN DEFAULT false,
    user_id TEXT,
    user_email TEXT,
    device_info TEXT,
    resolution TEXT,
    admin_response TEXT,
    responded_at TIMESTAMPTZ,
    upvotes INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

-- Indexes
CREATE INDEX IF NOT EXISTS idx_user_tickets_product ON user_tickets(product_type);
CREATE INDEX IF NOT EXISTS idx_user_tickets_user ON user_tickets(user_id);

-- Row Level Security
ALTER TABLE user_tickets ENABLE ROW LEVEL SECURITY;

CREATE POLICY "Anyone can submit public tickets"
    ON user_tickets FOR INSERT
    WITH CHECK (is_private = false OR user_id IS NOT NULL);

CREATE POLICY "Anyone can read public tickets"
    ON user_tickets FOR SELECT
    USING (is_private = false);

CREATE POLICY "Users can read their own private tickets"
    ON user_tickets FOR SELECT
    USING (is_private = true AND user_id = auth.uid()::text);

-- Upvote RPC (atomic — prevents race conditions)
CREATE OR REPLACE FUNCTION upvote_ticket(ticket_id UUID)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    UPDATE user_tickets
    SET upvotes = upvotes + 1, updated_at = NOW()
    WHERE id = ticket_id;
END;
$$;
```

> **Team note**: If your team has added extra columns to `user_tickets`, they are safe — this SQL uses `IF NOT EXISTS` and will not drop or alter existing columns.

---

## Step 3 — Initialize Config

Call this once at app startup, before any UI is shown:

```kotlin
// Android: Application.onCreate()
// iOS: AppDelegate / @main App
// KMP shared: app initialization block

import com.mobilebytelabs.usertickets.config.FeatureRequestConfig

FeatureRequestConfig.init(
    supabaseUrl = "https://your-project.supabase.co",
    supabaseAnonKey = "your-anon-key",
    productType = "your_app_name",   // e.g. "reels_downloader", "mood_movies"
    userId = currentUser?.id,        // optional — enables Contact Support + My Tickets
)
```

**`productType` values** (must match across all app sessions):

| App | productType |
|-----|:-----------|
| Reels Downloader | `reels_downloader` |
| Byte Wallpaper | `wallpaper` |
| Mood Movies | `mood_movies` |

**`userId`** is optional. When provided:
- Contact Support tickets are private and visible only to that user
- "My Tickets" tab shows the user's own support messages

---

## Step 4 — Install Koin Module

```kotlin
import com.mobilebytelabs.usertickets.di.featureRequestModule

// In your Koin startKoin block:
startKoin {
    modules(
        featureRequestModule,
        // ... your other modules
    )
}
```

---

## Step 5 — Add Navigation

Register all three destinations in your `NavHost`:

```kotlin
import com.mobilebytelabs.usertickets.ui.featureWishlistDestination
import com.mobilebytelabs.usertickets.ui.createTicketDestination
import com.mobilebytelabs.usertickets.ui.ticketDetailDestination
import com.mobilebytelabs.usertickets.ui.navigateToFeatureWishlist
import com.mobilebytelabs.usertickets.ui.navigateToCreateTicket
import com.mobilebytelabs.usertickets.ui.navigateToTicketDetail
import com.mobilebytelabs.usertickets.model.TicketType

NavHost(navController = navController, startDestination = HomeRoute) {

    // ... your other destinations

    featureWishlistDestination(
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
// Open the wishlist (Feature Requests / Bug Reports tabs)
navController.navigateToFeatureWishlist()

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
- [ ] `user_tickets` table exists in Supabase with all columns
- [ ] `upvote_ticket` RPC function exists in Supabase
- [ ] `FeatureRequestConfig.init()` called before navigation
- [ ] `featureRequestModule` in Koin modules list
- [ ] All 3 nav destinations registered in NavHost
- [ ] Navigating to wishlist shows the screen (not a crash)
- [ ] Submitting a test ticket appears in Supabase dashboard
- [ ] Upvoting a ticket increments the count

---

## Troubleshooting

| Error | Cause | Fix |
|-------|-------|-----|
| `FeatureRequestConfig not initialized` | `init()` called too late or not at all | Call before Koin starts or in Application.onCreate |
| `No destination for FeatureWishlistRoute` | Missing `featureWishlistDestination` in NavHost | Add all 3 destinations |
| `Tickets not loading` | Wrong `productType` or Supabase RLS | Check RLS policies + productType matches |
| `Upvote fails silently` | `upvote_ticket` RPC missing | Run Step 2 SQL again |
| `Contact Support tickets missing` | `userId` is null | Pass `userId` in `FeatureRequestConfig.init()` |

---

## Next Steps

- Run `/lib-sync cmp-user-tickets --check` to verify your integration is current with the latest library version
- See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) to automate setup and keep in sync with AI
