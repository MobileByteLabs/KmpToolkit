# cmp-product-tickets-compose

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Compose Multiplatform UI for [cmp-product-tickets](../cmp-product-tickets/README.md) — screens, navigation and ViewModels.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-product-tickets-compose:<version>")
```

## Key API

| API | Purpose |
|---|---|
| `NavGraphBuilder.productTicketsDestination(...)` | Ticket list (tabs: all / mine) |
| `NavGraphBuilder.createTicketDestination(onBackClick)` | Create a ticket |
| `NavGraphBuilder.ticketDetailDestination(onBackClick)` | Detail, vote and comment |
| `NavController.navigateToProductTickets()` / `navigateToCreateTicket(type)` / `navigateToTicketDetail(id)` | Navigation |
| `ProductTicketsState` / `TicketDetailState` / `TicketsTab` | UI state |
| `productTicketsModule` | Koin module — the UI layer's ViewModels |

## Usage

All three destinations are required — the list navigates to the other two:

```kotlin
productTicketsDestination(
    onBackClick = { navController.popBackStack() },
    onNavigateToCreateTicket = { type -> navController.navigateToCreateTicket(type) },
    onNavigateToTicketDetail = { id -> navController.navigateToTicketDetail(id) },
)
createTicketDestination(onBackClick = { navController.popBackStack() })
ticketDetailDestination(onBackClick = { navController.popBackStack() })
```

Configure the data layer first — see [cmp-product-tickets](../cmp-product-tickets/README.md).

## Observability

This module reports nothing by design — every composable delegates to
[cmp-product-tickets](../cmp-product-tickets/README.md), which reports the operation. Reporting in both would double every count a
consumer's hook sees.

## Related

- [cmp-product-tickets](../cmp-product-tickets/README.md) — the headless core
- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
