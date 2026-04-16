# cmp-remote-config

Server-driven UI system for Kotlin Multiplatform — powered by Supabase.

```
io.github.mobilebytelabs:kmptoolkit-remote-config:2.1.0
```

> **Prerequisite**: `cmp-user-tickets` must be configured first. `cmp-remote-config` reuses
> `FeatureRequestConfig` credentials (Supabase URL + anon key + product type).

---

## What It Does

Remotely push UI overlays to your users without an app update. Control dialogs, banners,
full-screen overlays, and bottom sheets from a Supabase dashboard — with frequency capping,
scheduling, platform targeting, and action routing.

```
┌─ DIALOG ──────────────┐  ┌─ FULLSCREEN ──────────┐
│                       │  │                   [X] │
│  🚀 Update Ready!     │  │     🎉 Big News!      │
│  New version 3.0      │  │   We've completely    │
│  [Update Now] [Later] │  │   [Get Started]       │
└───────────────────────┘  └───────────────────────┘

┌─ BANNER ──────────────┐  ┌─ BOTTOM_SHEET ────────┐
│ 🔔 New update! [→]    │  │  📣 Announcement      │
└───────────────────────┘  │  [Action] [Dismiss]   │
                            └───────────────────────┘
```

---

## Display Types

| Type | Use Case |
|------|----------|
| `dialog` | Announcements, confirmations, promotional offers |
| `fullscreen` | Onboarding, major feature reveals |
| `banner` | Inline alerts, non-blocking nudges |
| `bottom_sheet` | Soft prompts, feature discovery |

---

## Features

- **Frequency control**: `max_impressions` + `cooldown_hours` per device
- **Scheduling**: `start_at` + `end_at` for time-boxed campaigns
- **Platform targeting**: `platform = "android" | "ios" | "all"`
- **Version targeting**: `min_app_version` + `max_app_version`
- **Action routing**: URL, deep link, store, dismiss
- **Dynamic UI**: `content_json` for server-rendered composables
- **RLS-secured**: device impressions tracked server-side

---

## Module Identity

```yaml
artifact:       io.github.mobilebytelabs:kmptoolkit-remote-config:2.1.0
package:        com.mobilebytelabs.remoteconfig
supabase_table: product_remote_config
di_module:      remoteConfigModule
ui_composable:  RemoteConfigHost(onAction)
depends_on:     cmp-user-tickets (FeatureRequestConfig credentials)
```

---

## Quick Start

```kotlin
// 1. Prerequisites — cmp-user-tickets already configured:
FeatureRequestConfig.init(supabaseUrl, supabaseAnonKey, productType)

// 2. Add remoteConfigModule to Koin
startKoin {
    modules(
        featureRequestModule,
        remoteConfigModule,   // add this
    )
}

// 3. Add RemoteConfigHost to root composable
@Composable
fun App() {
    Box(modifier = Modifier.fillMaxSize()) {
        MainNavHost()

        RemoteConfigHost(
            onAction = { actionType, actionValue ->
                when (actionType) {
                    ActionType.URL      -> openUrl(actionValue)
                    ActionType.DEEPLINK -> navController.navigate(actionValue ?: return@RemoteConfigHost)
                    ActionType.STORE    -> openStore()
                    else -> {}
                }
            },
        )
    }
}
```

---

## Schema Overview

**Tables**: `product_remote_config` + `device_impressions`

**RPCs**: `get_device_impressions`, `record_config_impression`, `dismiss_config`

See [SETUP.md](SETUP.md) for full SQL.

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps + full SQL
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-remote-config`
- [../REMOTE_CONFIG_SAMPLES.md](../REMOTE_CONFIG_SAMPLES.md) — Sample config rows
