# cmp-remote-config

Server-driven UI system for Kotlin Multiplatform — powered by Supabase.

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/kmptoolkit-remote-config.svg?label=Maven%20Central)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/kmptoolkit-remote-config)

> **Standalone module** — no dependency on other kmp-toolkit modules. Per-project Supabase
> model: bring your own `supabaseUrl` + `anonKey`; no `productType` filter required.

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
artifact:       io.github.mobilebytelabs:kmptoolkit-remote-config:4.0.0   # next release — see CHANGELOG
package:        com.mobilebytelabs.remoteconfig
supabase_table: product_remote_config   # per-project, NO product_type column
install:        Module.remoteConfig { … }  # Koin DSL extension
ui_composable:  RemoteConfigHost(onAction)
depends_on:     —   # standalone, no kmp-toolkit cross-module dependencies
```

---

## Quick Start

```kotlin
// 1. Install inside any existing Koin module
import com.mobilebytelabs.remoteconfig.remoteConfig

val networkModule = module {
    remoteConfig {
        supabaseUrl = "https://YOUR_PROJECT.supabase.co"
        supabaseKey = "YOUR_ANON_KEY"

        // Optional — app-specific action handlers (custom action_type strings):
        action(ActionType.PREMIUM)     { _, _ -> navigator.navigateTo("paywall") }
        action("open_downloads")       { v, _ -> navigator.navigateTo("downloads/${v.orEmpty()}") }
    }

    // … your other bindings …
}

// 2. Add RemoteConfigHost to root composable.
//    Pass onAction for explicit per-screen control, or omit to route through
//    handlers registered above via the action(...) DSL.
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
                    ActionType.PREMIUM  -> navController.navigate("paywall")
                    else -> {}
                }
            },
        )
    }
}
```

## Custom Action Types

`ActionType` is an open value-class — extend with your own typed constants:

```kotlin
object RemoteActions {
    val OPEN_DOWNLOADS = ActionType("open_downloads")
    val CLEAR_CACHE    = ActionType("clear_cache")
    val OPEN_PAYWALL   = ActionType("open_paywall")
}

remoteConfig {
    supabaseUrl = "…"; supabaseKey = "…"
    action(RemoteActions.OPEN_DOWNLOADS) { v, _ -> navigator.navigateTo("downloads/${v.orEmpty()}") }
    action(RemoteActions.CLEAR_CACHE)    { _, _ -> cacheManager.clearAll() }
}
```

`ActionType("open_downloads") == RemoteActions.OPEN_DOWNLOADS` — value class equality is
structural, so types round-trip exactly through Supabase JSON.

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
