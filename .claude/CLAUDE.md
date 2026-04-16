# KmpToolkit — Claude Context

This file provides module context for the `/lib-sync` and `/sync-user-tickets` skills.

---

## cmp-user-tickets

### Module Identity

```yaml
artifact:       io.github.mobilebytelabs:kmptoolkit-user-tickets
version:        2.1.0
package:        com.mobilebytelabs.usertickets
supabase_table: user_tickets
supabase_rpc:   upvote_ticket
```

### Config

```kotlin
import com.mobilebytelabs.usertickets.config.FeatureRequestConfig

FeatureRequestConfig.init(
    supabaseUrl    = "https://YOUR_PROJECT.supabase.co",
    supabaseAnonKey = "YOUR_ANON_KEY",
    productType    = "your_app_name",
    userId         = null,  // optional — enables Contact Support + My Tickets
)
```

### DI

```kotlin
import com.mobilebytelabs.usertickets.di.featureRequestModule
// add to startKoin { modules(...) }
```

### Navigation (all 3 required)

```kotlin
import com.mobilebytelabs.usertickets.ui.featureWishlistDestination
import com.mobilebytelabs.usertickets.ui.createTicketDestination
import com.mobilebytelabs.usertickets.ui.ticketDetailDestination

featureWishlistDestination(
    onBackClick = { navController.popBackStack() },
    onNavigateToCreateTicket = { type -> navController.navigateToCreateTicket(type) },
    onNavigateToTicketDetail = { id -> navController.navigateToTicketDetail(id) },
)
createTicketDestination(onBackClick = { navController.popBackStack() })
ticketDetailDestination(onBackClick = { navController.popBackStack() })
```

### Expected Schema (v2.1.0) — used by `/lib-sync` Gate 2

| Column | Type | Nullable | Default |
|--------|------|:--------:|---------|
| id | UUID | NO | gen_random_uuid() |
| product_type | TEXT | NO | — |
| ticket_type | TEXT | NO | 'feature_request' |
| title | TEXT | NO | — |
| description | TEXT | NO | — |
| category | TEXT | YES | 'general' |
| status | TEXT | YES | 'pending' |
| is_private | BOOLEAN | NO | false |
| user_id | TEXT | YES | NULL |
| user_email | TEXT | YES | NULL |
| device_info | TEXT | YES | NULL |
| resolution | TEXT | YES | NULL |
| admin_response | TEXT | YES | NULL |
| responded_at | TIMESTAMPTZ | YES | NULL |
| upvotes | INT | NO | 0 |
| created_at | TIMESTAMPTZ | YES | NOW() |
| updated_at | TIMESTAMPTZ | YES | NOW() |

### Quick Sync Commands

```bash
/sync-user-tickets            # Full sync from anywhere in this repo
/lib-sync cmp-user-tickets    # Same, from framework
/lib-sync cmp-user-tickets --check   # Dry run
```

### Docs

- [README](docs/user-tickets/README.md) — Module overview
- [SETUP](docs/user-tickets/SETUP.md) — Manual integration guide
- [CLAUDE_AI_SETUP](docs/user-tickets/CLAUDE_AI_SETUP.md) — AI-assisted setup
