# KmpToolkit — Claude Context

This file provides module context for the `/lib-sync` and `/sync-product-tickets` skills.

## Rules (always active in this repo)

| Rule | Trigger | File |
|------|---------|------|
| RULE-SYNC-USER-TICKETS-001 | During `/sync-product-tickets` or `/lib-sync cmp-product-tickets` — strict 5-gate enforcement, FAIL = hard stop | `.claude-runtime/rules/RULE-SYNC-USER-TICKETS-001.md` |
| RULE-LIB-EVOLVE-TICKETS-001 | After Write/Edit on `cmp-product-tickets/src/**`, `build.gradle.kts`, `migrations/*.sql` — auto-update sync contract | `layers/tickets/rules/RULE-LIB-EVOLVE-TICKETS-001.md` (framework) |

> **RULE-LIB-EVOLVE-TICKETS-001 is mandatory**: every change to cmp-product-tickets source MUST trigger a sync contract update in the same session. Never commit a library change without the contract update.

---

## cmp-product-tickets

### Module Identity

```yaml
artifact:       io.github.mobilebytelabs:cmp-product-tickets
version:        3.0.0
package:        com.mobilebytelabs.producttickets
supabase_table: product_tickets   # per-project, NO product_type column
supabase_rpcs:
  - toggle_vote    # unique-per-user vote via ticket_votes table
  - add_comment    # via ticket_comments table
```

### Config

```kotlin
import com.mobilebytelabs.producttickets.config.ProductTicketsConfig

ProductTicketsConfig.init(
    supabaseUrl     = "https://YOUR_PROJECT.supabase.co",
    supabaseAnonKey = "YOUR_ANON_KEY",
    userId          = null,  // optional — enables Contact Support + My Tickets
    // NOTE: no productType param — each app has its own Supabase project
)
```

### DI

```kotlin
import com.mobilebytelabs.producttickets.di.productTicketsModule
// add to startKoin { modules(...) }
```

### Navigation (all 3 required)

```kotlin
import com.mobilebytelabs.producttickets.ui.productTicketsDestination
import com.mobilebytelabs.producttickets.ui.createTicketDestination
import com.mobilebytelabs.producttickets.ui.ticketDetailDestination

productTicketsDestination(
    onBackClick = { navController.popBackStack() },
    onNavigateToCreateTicket = { type -> navController.navigateToCreateTicket(type) },
    onNavigateToTicketDetail = { id -> navController.navigateToTicketDetail(id) },
)
createTicketDestination(onBackClick = { navController.popBackStack() })
ticketDetailDestination(onBackClick = { navController.popBackStack() })
```

### Expected Schema (v3.0.0) — 23 columns, NO product_type

| Column | Type | Nullable | Default |
|--------|------|:--------:|---------|
| id | UUID | NO | gen_random_uuid() |
| ticket_type | TEXT | NO | 'feature_request' |
| title | TEXT | NO | — |
| description | TEXT | NO | — |
| category | TEXT | YES | 'general' |
| status | TEXT | YES | 'pending' |
| priority | TEXT | YES | 'medium' |
| platform | TEXT | YES | NULL |
| app_version | TEXT | YES | NULL |
| milestone | TEXT | YES | NULL |
| labels | JSONB | YES | '[]' |
| attachments | JSONB | YES | '[]' |
| is_private | BOOLEAN | NO | false |
| user_id | TEXT | YES | NULL |
| user_email | TEXT | YES | NULL |
| device_info | TEXT | YES | NULL |
| upvotes | INT | NO | 0 |
| admin_response | TEXT | YES | NULL |
| responded_at | TIMESTAMPTZ | YES | NULL |
| severity | TEXT | YES | NULL |
| resolution | TEXT | YES | NULL |
| created_at | TIMESTAMPTZ | YES | NOW() |
| updated_at | TIMESTAMPTZ | YES | NOW() |

### Quick Sync Commands

```bash
/sync-product-tickets              # Full sync from anywhere in this repo
/lib-sync cmp-product-tickets      # Same, from framework
/lib-sync cmp-product-tickets --check   # Dry run
```

### Docs

- [README](docs/user-tickets/README.md) — Module overview
- [SETUP](docs/user-tickets/SETUP.md) — Manual integration guide
- [CLAUDE_AI_SETUP](docs/user-tickets/CLAUDE_AI_SETUP.md) — AI-assisted setup
- [LIBRARY_DEV](docs/user-tickets/LIBRARY_DEV.md) — Library contributor guide

### Migration from cmp-user-tickets (v2.x)

| v2.x | v3.0.0 |
|------|--------|
| `kmptoolkit-user-tickets:2.1.0` | `cmp-product-tickets:3.0.0` |
| `FeatureRequestConfig.init(url, key, productType, userId?)` | `ProductTicketsConfig.init(url, key, userId?)` |
| `featureRequestModule` | `productTicketsModule` |
| `featureWishlistDestination` | `productTicketsDestination` |
| `com.mobilebytelabs.usertickets` | `com.mobilebytelabs.producttickets` |
| `/sync-user-tickets` | `/sync-product-tickets` |
