# cmp-product-tickets

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Feature requests, bug reports and support tickets backed by Supabase — headless data layer.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-product-tickets:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `ProductTicketsConfig.init(url, anonKey, userId?, boardType)` | One-time configuration |
| `ProductTicketsRepository` / `ProductTicketsService` | Read and mutate tickets |
| `UserTicket` / `UserTicketInsert` / `TicketComment` | Domain models |
| `TicketType` / `TicketCategory` / `TicketStatus` / `TicketPriority` | Vocabulary |
| `productTicketsDataModule` | Koin module (data layer) |

## Usage

```kotlin
ProductTicketsConfig.init(
    supabaseUrl     = "https://YOUR_PROJECT.supabase.co",
    supabaseAnonKey = "YOUR_ANON_KEY",
    userId          = currentUserId,   // optional — enables Contact Support + My Tickets
)
```

`userId` is optional: without it tickets are anonymous and the personal views stay hidden.

## UI

The Compose screens, navigation destinations and ViewModels live in
[cmp-product-tickets-compose](../cmp-product-tickets-compose/README.md).

## Supabase schema

One `product_tickets` table (23 columns, no `product_type` — each app has its own project) plus
`ticket_votes` and `ticket_comments`, with `toggle_vote` and `add_comment` RPCs. The expected schema
is documented in [.claude/CLAUDE.md](../.claude/CLAUDE.md); `/sync-product-tickets` verifies a
consumer against it.

## Observability

This module reports its own lifecycle through [cmp-observe](../cmp-observe/). Events carry operation
*shape*, never content. See `## §9 Observability Surface` in [DEVELOPMENT.md](DEVELOPMENT.md) for the
exact event list.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
