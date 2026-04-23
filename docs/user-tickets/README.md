# cmp-product-tickets

> `io.github.mobilebytelabs:kmptoolkit-product-tickets:3.0.0`

A complete user feedback system for Kotlin Multiplatform apps. Covers three ticket types — feature requests, bug reports, and private support messages — with upvoting, status tracking, and per-project Supabase isolation.

---

## Screens

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│ Product Tickets    +  │  │ New Feature Request  │  │ Dark mode            │
│                       │  │                      │  │                      │
│ [Requests][Resolved]  │  │ What's your idea?    │  │ [Planned]         ▲5 │
│                       │  │ ┌──────────────────┐ │  │ ─────────────────── │
│ ┌───────────────────┐ │  │ │                  │ │  │                      │
│ │ Download queue    │ │  │ └──────────────────┘ │  │ I'd love a dark...   │
│ │ [Requested]    ▲2 │ │  │                      │  │                      │
│ └───────────────────┘ │  │ Tell us more         │  │ [     Upvote ▲    ]  │
│ ┌───────────────────┐ │  │ ┌──────────────────┐ │  │ [      Close      ]  │
│ │ Dark mode         │ │  │ │                  │ │  │                      │
│ │ [Planned]      ▲5 │ │  │ └──────────────────┘ │  └──────────────────────┘
│                       │  │                      │   Detail + Upvote
│ [+ New Request]       │  │ [    Submit     ]    │
└──────────────────────┘  └──────────────────────┘
  ProductTicketsScreen        CreateTicketScreen
```

---

## Ticket Types

| Type | Value | Visibility | Use For |
|------|-------|:----------:|---------|
| `FEATURE_REQUEST` | `feature_request` | Public | Wishlist items, upvoting |
| `BUG_REPORT` | `bug_report` | Public | Bugs, crashes, UI issues |
| `CONTACT_SUPPORT` | `contact_support` | Private (user only) | Billing, account, subscriptions |

---

## Features

- **Three ticket types** — Feature Request (public + upvote), Bug Report (public), Contact Support (private)
- **Upvoting** — unique per-user vote via `toggle_vote` RPC + `ticket_votes` table
- **Status tracking** — Pending → In Review → Planned → In Progress → Resolved/Completed
- **Admin comments** — via `add_comment` RPC + `ticket_comments` table
- **Resolution notes** — resolved tickets show how they were addressed
- **Admin response** — support tickets show admin replies with timestamp
- **Per-project isolation** — each app has its own Supabase project with `product_tickets` table (no shared table, no `productType` filter)
- **My Tickets** — private support messages visible only to the submitting user (requires `userId`)

---

## Ticket Statuses

| Status | Value | Tab |
|--------|-------|-----|
| Pending | `pending` | Requests |
| In Review | `in_review` | Requests |
| Planned | `planned` | Requests |
| In Progress | `in_progress` | Requests |
| Resolved | `resolved` | Resolved |
| Completed | `completed` | Resolved |
| Closed | `closed` | Resolved |

---

## Categories

| Category | Applies To |
|----------|-----------|
| ✨ New Feature | Feature Request |
| 🎨 UI/Design | Feature Request |
| ⚡ Performance | Feature Request, Bug Report |
| 📥 Download | Feature Request, Bug Report |
| 💥 Crash | Bug Report |
| 🖼️ UI Bug | Bug Report |
| 💳 Subscription | Contact Support |
| 👤 Account | Contact Support |
| 🧾 Billing | Contact Support |
| 📝 General | All types |

---

## Module Structure

```
com.mobilebytelabs.producttickets/
├── config/
│   └── ProductTicketsConfig.kt       # Init: supabaseUrl, anonKey, userId?
├── domain/
│   └── model/
│       ├── UserTicket.kt             # Data class (maps to product_tickets table)
│       ├── TicketType.kt             # Enums: TicketType, TicketCategory, TicketStatus
│       └── UserTicketInsert.kt       # Insert payload (user-submittable fields only)
├── data/
│   └── remote/
│       ├── dto/
│       │   └── UserTicketDto.kt      # Supabase JSON DTO
│       ├── ProductTicketsClient.kt   # Supabase client (internal)
│       ├── ProductTicketsService.kt  # getPublicTickets, submitTicket, toggleVote
│       └── ProductTicketsRepository.kt  # Business logic
├── di/
│   └── ProductTicketsModule.kt       # Koin module: productTicketsModule
└── ui/
    ├── ProductTicketsScreen.kt        # Wishlist (tabs: Requests / Resolved)
    ├── CreateTicketScreen.kt          # Submit form
    ├── TicketDetailScreen.kt          # Detail + upvote
    ├── ProductTicketsViewModel.kt     # State management
    └── ProductTicketsNavigation.kt    # Nav destinations + extension fns
```

---

## Quick Setup

See [SETUP.md](SETUP.md) for full integration guide.
For AI-assisted one-shot setup, see [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md).

---

## Per-Project Isolation

Each app has its own dedicated Supabase project with a `product_tickets` table. No shared tables, no `product_type` column needed.

| App | Supabase Project |
|-----|:----------------|
| Reels Downloader | `reels-downloader.supabase.co` |
| Byte Wallpaper | `byte-wallpaper.supabase.co` |
| Mood Movies | `mood-movies.supabase.co` |

Contact Support tickets (`is_private = true`) are additionally filtered by `user_id`.

---

## Migration from cmp-user-tickets (v2.x)

| Change | v2.x | v3.0.0 |
|--------|------|--------|
| Module | `cmp-user-tickets` | `cmp-product-tickets` |
| Artifact | `kmptoolkit-user-tickets` | `kmptoolkit-product-tickets` |
| Table | `user_tickets` | `product_tickets` |
| Config | `FeatureRequestConfig.init(url, key, productType, userId?)` | `ProductTicketsConfig.init(url, key, userId?)` |
| DI | `featureRequestModule` | `productTicketsModule` |
| Nav | `featureWishlistDestination` | `productTicketsDestination` |
| Package | `com.mobilebytelabs.usertickets` | `com.mobilebytelabs.producttickets` |

Run `/lib-sync cmp-product-tickets` to auto-migrate all references.
