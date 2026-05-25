# KmpToolkit — Claude Context

This file provides module context for `/lib-sync`, `/sync-product-tickets`, `/sync-network-monitor`,
`/sync-share`, `/sync-intent-launcher`, and `/sync-app-intents` skills.

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

---

## cmp-network-monitor

### Module Identity

```yaml
artifact:       io.github.mobilebytelabs:cmp-network-monitor
version:        3.2.1
package:        io.github.mobilebytelabs.kmptoolkit.networkmonitor
supabase:       false
di:             false   # factory function (provideNetworkMonitor), no Koin module
nav:            false
config:         none    # zero-config — Android auto-init via ContentProvider
targets:        21 (all KMP targets)
```

### Key APIs

| API | Description |
|-----|-------------|
| `createNetworkMonitor(config?)` | Create a platform-specific monitor |
| `provideNetworkMonitor(config?)` | DI factory function |
| `NetworkMonitorProvider.install()` | Singleton lifecycle manager |
| `isOnline: StateFlow<Boolean>` | Hot-shared connectivity state |
| `networkStatus: StateFlow<NetworkStatus>` | Rich status (Available/Unavailable/CaptivePortal) |
| `networkChanges: SharedFlow<NetworkChangeEvent>` | Discrete transition events |
| `networkQuality(): Flow<NetworkQuality>` | Quality signal (Excellent/Good/Fair/Poor/Offline) |
| `requireOnline()` / `ensureOnline()` | Fail-fast / suspend-until-online |
| `ifOnline { }` / `ifOffline { }` | Conditional execution |
| `retryOnReconnect(max) { }` | Retry on network reconnect |
| `FakeNetworkMonitor()` | Test double with state/event history |

### Compose Module (cmp-network-monitor-compose)

```yaml
artifact:       io.github.mobilebytelabs:cmp-network-monitor-compose
package:        io.github.mobilebytelabs.kmptoolkit.networkmonitor.compose
targets:        9 (Android, iOS, macOS, JVM, JS, WasmJS — Compose platforms only)
```

| API | Description |
|-----|-------------|
| `NetworkAwareContent(monitor, offlineContent, captivePortalContent, onlineContent)` | Auto UI switching |
| `ConnectivityBanner(modifier, monitor, message, backgroundColor, contentColor)` | Animated offline banner |
| `rememberNetworkMonitor(config)` | Singleton lifecycle |
| `rememberScopedNetworkMonitor(config)` | Per-composition lifecycle |
| `collectIsOnlineAsState()` | Boolean state |
| `collectNetworkStatusAsState()` | NetworkStatus state |
| `collectNetworkQualityAsState()` | NetworkQuality state |
| `LocalNetworkMonitor` | CompositionLocal provider |

### Sync Commands

```bash
/sync-network-monitor              # Gate 1 Gradle only (zero-config)
/lib-sync cmp-network-monitor      # Same, from framework
/sync-network-monitor --check      # Dry run
```

### Docs

- [README](docs/network-monitor/README.md) — Module overview
- [SETUP](docs/network-monitor/SETUP.md) — Manual integration guide
- [CLAUDE_AI_SETUP](docs/network-monitor/CLAUDE_AI_SETUP.md) — AI-assisted setup

---

## cmp-share

### Module Identity

```yaml
artifact:  io.github.mobilebytelabs:cmp-share
version:   3.2.11
package:   com.mobilebytelabs.kmptoolkit.share
supabase:  false
di:        false   # expect object + extension functions — no Koin module
nav:       false
config:    none    # zero-config
api_tier:  v0.1.0 experimental (@ExperimentalShareApi)
```

### Quick Sync Commands

```bash
/sync-share              # Gate 1 Gradle only (zero-config)
/lib-sync cmp-share      # Same, from framework
/sync-share --check      # Dry run
```

### Docs

- [README](docs/share/README.md) — Module overview + API reference
- [SETUP](docs/share/SETUP.md) — Manual integration guide
- [CLAUDE_AI_SETUP](docs/share/CLAUDE_AI_SETUP.md) — AI-assisted setup

---

## cmp-intent-launcher

### Module Identity

```yaml
artifact:  io.github.mobilebytelabs:cmp-intent-launcher
version:   3.2.11
package:   com.mobilebytelabs.kmptoolkit.intentlauncher
supabase:  false
di:        false   # rememberIntentLauncher() Compose helper — no Koin module
nav:       false
config:    none    # zero Gradle config; Activity wiring = developer responsibility
api_tier:  v0.1.0 experimental (@ExperimentalIntentLauncherApi)
```

### Quick Sync Commands

```bash
/sync-intent-launcher              # Gate 1 Gradle only
/lib-sync cmp-intent-launcher      # Same, from framework
/sync-intent-launcher --check      # Dry run
```

### Docs

- [README](docs/intent-launcher/README.md) — Module overview + API reference
- [SETUP](docs/intent-launcher/SETUP.md) — Manual integration guide (incl. Android Activity wiring)
- [CLAUDE_AI_SETUP](docs/intent-launcher/CLAUDE_AI_SETUP.md) — AI-assisted setup

---

## cmp-app-intents

### Module Identity

```yaml
artifact:  io.github.mobilebytelabs:cmp-app-intents
version:   3.2.11
package:   com.mobilebytelabs.kmptoolkit.appintents
supabase:  false
di:        false   # AppIntents.register() at app startup — no Koin module
nav:       false
config:    none    # zero Gradle config; Swift bridge = developer responsibility
api_tier:  v0.1.0 experimental (@ExperimentalAppIntentsApi)
```

### Quick Sync Commands

```bash
/sync-app-intents              # Gate 1 Gradle only
/lib-sync cmp-app-intents      # Same, from framework
/sync-app-intents --check      # Dry run
```

### Docs

- [README](docs/app-intents/README.md) — Module overview + API reference
- [SETUP](docs/app-intents/SETUP.md) — Manual integration guide (incl. iOS Swift bridge)
- [CLAUDE_AI_SETUP](docs/app-intents/CLAUDE_AI_SETUP.md) — AI-assisted setup
