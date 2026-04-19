# cmp-user-tickets

> `io.github.mobilebytelabs:kmptoolkit-user-tickets:2.1.0`

A complete user feedback system for Kotlin Multiplatform apps. Covers three ticket types — feature requests, bug reports, and private support messages — with upvoting, status tracking, and multi-app support via a shared Supabase table.

---

## Screens

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│ User Tickets       +  │  │ New Feature Request  │  │ Dark mode            │
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
  UserTicketsScreen          CreateTicketScreen
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
- **Upvoting** — atomic increment via Supabase RPC, prevents race conditions
- **Status tracking** — Pending → In Review → Planned → In Progress → Resolved/Completed
- **Resolution notes** — resolved tickets show how they were addressed
- **Admin response** — support tickets show admin replies with timestamp
- **Multi-app** — `productType` filter lets all your apps share one Supabase table
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
com.mobilebytelabs.usertickets/
├── config/
│   └── FeatureRequestConfig.kt       # Init: supabaseUrl, anonKey, productType, userId?
├── model/
│   ├── UserTicket.kt                 # Data class (maps to user_tickets table)
│   ├── TicketType.kt                 # Enums: TicketType, TicketCategory, TicketStatus
│   └── UserTicketInsert.kt           # Insert payload
├── data/
│   ├── FeatureRequestClient.kt       # Supabase client (internal)
│   ├── UserTicketsService.kt         # getPublicTickets, submitTicket, upvoteTicket
│   └── UserTicketsRepository.kt      # Business logic
├── di/
│   └── FeatureRequestModule.kt       # Koin module: featureRequestModule
└── ui/
    ├── UserTicketsScreen.kt           # Wishlist (tabs: Requests / Resolved)
    ├── CreateTicketScreen.kt          # Submit form
    ├── TicketDetailScreen.kt          # Detail + upvote
    ├── UserTicketsViewModel.kt        # State management
    └── UserTicketsNavigation.kt       # Nav destinations + extension fns
```

---

## Quick Setup

See [SETUP.md](SETUP.md) for full integration guide.
For AI-assisted one-shot setup, see [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md).

---

## Multi-App Support

All apps share one `user_tickets` table, filtered by `product_type`:

| App | productType |
|-----|:-----------|
| Reels Downloader | `reels_downloader` |
| Byte Wallpaper | `wallpaper` |
| Mood Movies | `mood_movies` |

Each app only sees its own tickets. Contact Support tickets (`is_private = true`) are also filtered by `user_id`.
