# SYNC.md — KmpToolkit Consumer Sync Contract

> Machine-readable sync contract. Read by: `/lib-sync`, `/project-verify` CHECK-TICKETS-005, `/sync-product-tickets` Gate 1.
>
> Update this file whenever a module version bumps or its consumer contract changes.

---

## Module: cmp-product-tickets

```yaml
module:     cmp-product-tickets
version:    3.0.0
artifact:   io.github.mobilebytelabs:kmptoolkit-product-tickets
package:    com.mobilebytelabs.producttickets
updated:    2026-04-22
breaking_changes_from: "2.x — product_type removed, user_tickets→product_tickets, full rename"
```

### Consumer Gradle (`libs.versions.toml`)

```toml
[versions]
# Dedicated key (independent upgrade, no shared-ref collision)
kmptoolkit-product-tickets = "3.0.0"

[libraries]
kmptoolkit-product-tickets = { module = "io.github.mobilebytelabs:kmptoolkit-product-tickets", version.ref = "kmptoolkit-product-tickets" }
```

**Version resolution rule**: A project may use a shared ref (e.g., `version.ref = "kmptoolkit"`).
`/lib-sync` resolves the ref chain to the actual version number. If it resolves to anything other than `3.0.0`, the version is DRIFTED.

### Consumer Dependency (`build.gradle.kts`)

```kotlin
commonMain.dependencies {
    implementation(libs.kmptoolkit.product.tickets)
}
```

### Supabase Schema (Gate 2 — `/sync-product-tickets`)

```
Table: product_tickets     ← per-project table, NO product_type column
Expected columns (23 total):
  Base (v3.0):    id, ticket_type, title, description, category, status,
                  priority, platform, app_version, milestone, labels,
                  attachments, is_private, user_id, user_email, device_info,
                  upvotes, admin_response, responded_at, severity, resolution,
                  created_at, updated_at

  REMOVED vs v2.x: product_type  ← each project has its own Supabase project

Related tables:
  ticket_votes    — unique per-user vote (toggle_vote RPC)
  ticket_comments — admin + user comments (add_comment RPC)

RPCs:
  toggle_vote(p_ticket_id UUID, p_voter_id TEXT) RETURNS INT
  add_comment(p_ticket_id UUID, p_author_type TEXT, p_author_name TEXT, p_content TEXT)
```

### App Wiring (Gate 3 — `/sync-product-tickets`)

```kotlin
// Config (required) — NO productType param
ProductTicketsConfig.init(
    supabaseUrl     = "https://YOUR_PROJECT.supabase.co",
    supabaseAnonKey = "YOUR_ANON_KEY",
    userId          = null,   // optional — enables My Tickets + Contact Support
)

// DI (required)
startKoin { modules(productTicketsModule) }

// Navigation (all 3 required)
productTicketsDestination(
    onBackClick              = { navController.popBackStack() },
    onNavigateToCreateTicket = { type -> navController.navigateToCreateTicket(type) },
    onNavigateToTicketDetail = { id   -> navController.navigateToTicketDetail(id) },
)
createTicketDestination(onBackClick = { navController.popBackStack() })
ticketDetailDestination(onBackClick = { navController.popBackStack() })
```

### Sync Command

```bash
/lib-sync cmp-product-tickets          # full 5-gate sync
/sync-product-tickets                  # same (library alias)
/sync-product-tickets --check          # dry-run — show status without writes
```

---

## Drift Detection

`/lib-sync` and `/project-verify` CHECK-TICKETS-005 compare the consumer's resolved version against `version: 3.0.0` above.

| Consumer resolved version | Status |
|--------------------------|--------|
| `3.0.0` | ✅ UP TO DATE |
| `< 3.0.0` | ⚠️ DRIFTED — run `/lib-sync cmp-product-tickets` |
| `2.x` | ❌ MAJOR behind — breaking changes in v3.0.0, review migration guide |
| `> 3.0.0` | ⚠️ AHEAD — this SYNC.md may be stale, check library release |

---

## Migration from cmp-user-tickets (v2.x → v3.0.0)

| Change | Old (v2.x) | New (v3.0.0) |
|--------|-----------|--------------|
| Module | `cmp-user-tickets` | `cmp-product-tickets` |
| Artifact | `kmptoolkit-user-tickets` | `kmptoolkit-product-tickets` |
| Table | `user_tickets` | `product_tickets` |
| Config class | `FeatureRequestConfig` | `ProductTicketsConfig` |
| Config init | `init(url, anonKey, productType, userId?)` | `init(url, anonKey, userId?)` |
| DI module | `featureRequestModule` | `productTicketsModule` |
| Nav destination | `featureWishlistDestination` | `productTicketsDestination` |
| Package | `com.mobilebytelabs.usertickets` | `com.mobilebytelabs.producttickets` |

**Removed:** `product_type` column — each app gets its own Supabase project with `product_tickets` table.
No filtering needed. Isolation is at the Supabase project level.

---

## How to Update This File

When `cmp-product-tickets` releases a new version:

1. Update `version:` above
2. Update `Consumer Gradle` version number
3. Update `Supabase Schema` if new columns/tables were added
4. Update `App Wiring` if config/DI/nav API changed
5. Run RULE-LIB-EVOLVE-TICKETS-001 to also update `.claude-runtime/commands/sync-product-tickets.md`

---

## Legacy Module (DEPRECATED)

The old `cmp-user-tickets` module is no longer maintained. See deprecation notice in
`.claude-runtime/commands/sync-user-tickets.md`.
