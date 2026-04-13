# Remote Config Module

> `com.mobilebytesensei:kmptoolkit-remote-config:1.0.0`

A Firebase Remote Config-like messaging system powered by Supabase. Remotely push dialogs, banners, full-screen overlays, and bottom sheets to users with frequency control, scheduling, targeting, and action buttons.

## Display Types

```
┌─ DIALOG ──────────────┐  ┌─ FULLSCREEN ──────────┐  ┌─ BANNER ──────────────┐
│                       │  │                   [X] │  │ 🚀 New update! [→]    │
│   🚀 Update Ready!   │  │                       │  └───────────────────────┘
│                       │  │     🎉 Big News!      │
│   New version 3.0     │  │                       │  ┌─ BOTTOM_SHEET ────────┐
│   is available with   │  │   We've completely    │  │                       │
│   exciting features!  │  │   redesigned the app  │  │   Rate Us ⭐           │
│                       │  │   for you...          │  │                       │
│  [Update Now]         │  │                       │  │   Enjoying the app?   │
│  [Maybe Later]        │  │  [Get Started]        │  │   [Rate Now] [Later]  │
│                       │  │  [Skip]               │  │                       │
└───────────────────────┘  └───────────────────────┘  └───────────────────────┘
```

## Features

- **4 display types** — Dialog, Full Screen, Banner, Bottom Sheet
- **Frequency control** — `max_impressions` (show N times, 0 = unlimited) + `cooldown_hours` (min hours between shows)
- **Scheduling** — `start_at` / `end_at` for time-limited campaigns
- **Action buttons** — Primary + secondary with URL, deeplink, store, or dismiss actions
- **Priority system** — Higher priority configs shown first
- **Dismissible control** — Force-show for critical updates (`is_dismissible: false`)
- **Local tracking** — Impression counters + timestamps survive app restarts (Settings)
- **Multi-app** — Same table, filtered by `product_type`
- **Platform targeting** — Filter by `platform` (all, android, ios, desktop)
- **Version targeting** — `min_app_version` / `max_app_version` for version-specific messages

## Use Cases

| Use Case | display_type | max_impressions | cooldown | action_type |
|----------|:----------:|:---------------:|:--------:|:-----------:|
| Force update | fullscreen | 0 (unlimited) | 0 | store |
| Rate us | dialog | 3 | 72h | store |
| New feature | bottom_sheet | 1 | 0 | deeplink |
| Maintenance | banner | 0 | 1h | dismiss |
| Promo / sale | fullscreen | 5 | 24h | url |
| Survey | dialog | 1 | 0 | url |

## Setup

### 1. Add dependency

```kotlin
// gradle/libs.versions.toml
[versions]
kmptoolkit = "1.0.0"

[libraries]
kmptoolkit-remote-config = { module = "com.mobilebytesensei:kmptoolkit-remote-config", version.ref = "kmptoolkit" }
```

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation(libs.kmptoolkit.remote.config)
}
```

### 2. Create Supabase table

Run in your shared Supabase project's SQL Editor:

```sql
CREATE TABLE product_remote_config (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    product_type TEXT NOT NULL,
    platform TEXT DEFAULT 'all',
    min_app_version TEXT,
    max_app_version TEXT,
    title TEXT NOT NULL,
    description TEXT,
    image_url TEXT,
    display_type TEXT NOT NULL DEFAULT 'dialog',
    priority INT DEFAULT 0,
    is_dismissible BOOLEAN DEFAULT true,
    action_text TEXT,
    action_type TEXT DEFAULT 'none',
    action_value TEXT,
    secondary_action_text TEXT,
    secondary_action_type TEXT DEFAULT 'dismiss',
    secondary_action_value TEXT,
    max_impressions INT DEFAULT 1,
    cooldown_hours INT DEFAULT 24,
    start_at TIMESTAMPTZ DEFAULT NOW(),
    end_at TIMESTAMPTZ,
    is_enabled BOOLEAN DEFAULT true,
    accent_color TEXT,
    icon_emoji TEXT,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_remote_config_product ON product_remote_config(product_type, is_enabled);

ALTER TABLE product_remote_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can read configs" ON product_remote_config FOR SELECT USING (true);
```

### 3. Initialize

Uses the same `FeatureRequestConfig` from the feature-request module:

```kotlin
FeatureRequestConfig.init(
    supabaseUrl = "https://your-project.supabase.co",
    supabaseAnonKey = "your-anon-key",
    productType = "your_app_name",
)
```

### 4. Install Koin module

```kotlin
import com.mobilebytesensei.remoteconfig.di.remoteConfigModule

modules(remoteConfigModule)
```

### 5. Add to any screen

```kotlin
import com.mobilebytesensei.remoteconfig.ui.RemoteConfigHost
import com.mobilebytesensei.remoteconfig.model.ActionType

@Composable
fun HomeScreen() {
    // Your content...

    RemoteConfigHost(
        onAction = { actionType, actionValue ->
            when (actionType) {
                ActionType.URL -> openUrl(actionValue)
                ActionType.STORE -> openPlayStore()
                ActionType.DEEPLINK -> navigate(actionValue)
                else -> {}
            }
        },
    )
}
```

## How It Works

```
App opens → RemoteConfigHost fetches active configs from Supabase
    ↓
Evaluator checks each config (sorted by priority):
├─ is_enabled?
├─ impressions < max_impressions?
├─ hours since last shown >= cooldown_hours?
├─ not permanently dismissed?
└─ If ALL pass → Show config → increment local counter
    ↓
Display based on display_type:
├─ dialog → Material Dialog
├─ fullscreen → Full screen overlay
├─ banner → Top banner bar
└─ bottom_sheet → Modal bottom sheet
```

## Insert Configs via Supabase

Use the SQL Editor or REST API to insert configs:

```sql
INSERT INTO product_remote_config (product_type, title, description, display_type, icon_emoji,
    action_text, action_type, secondary_action_text, secondary_action_type,
    max_impressions, cooldown_hours, priority, is_enabled)
VALUES (
    'reels_downloader',
    'Rate Us',
    'Enjoying the app? Leave us a review!',
    'dialog',
    '⭐',
    'Rate Now', 'store',
    'Maybe Later', 'dismiss',
    3, 72, 10, true
);
```

## Table Fields Reference

| Field | Type | Default | Description |
|-------|------|---------|-------------|
| product_type | TEXT | required | App identifier |
| platform | TEXT | 'all' | Target platform filter |
| title | TEXT | required | Message title |
| description | TEXT | null | Message body |
| display_type | TEXT | 'dialog' | dialog, fullscreen, banner, bottom_sheet |
| priority | INT | 0 | Higher = shown first |
| is_dismissible | BOOL | true | Can user close it? |
| action_text | TEXT | null | Primary button label |
| action_type | TEXT | 'none' | none, url, deeplink, store, dismiss |
| action_value | TEXT | null | URL / deeplink path |
| secondary_action_text | TEXT | null | Secondary button label |
| secondary_action_type | TEXT | 'dismiss' | Same as action_type |
| max_impressions | INT | 1 | Show N times (0 = unlimited) |
| cooldown_hours | INT | 24 | Min hours between shows |
| start_at | TIMESTAMP | NOW() | When to start showing |
| end_at | TIMESTAMP | null | When to stop (null = forever) |
| is_enabled | BOOL | true | Master on/off |
| icon_emoji | TEXT | null | Emoji icon (⭐, 🚀, ⚠️) |
| accent_color | TEXT | null | Hex color (#6366F1) |

## Architecture

```
com.mobilebytesensei.remoteconfig/
├── RemoteConfigEvaluator.kt        # Decides which config to show
├── model/
│   └── RemoteConfig.kt             # Data class + DisplayType + ActionType enums
├── network/
│   └── RemoteConfigService.kt      # Supabase fetch
├── local/
│   └── RemoteConfigLocalStore.kt   # Impression counters (Settings)
├── ui/
│   ├── RemoteConfigHost.kt         # Drop-in composable
│   ├── RemoteConfigViewModel.kt    # State management
│   ├── RemoteConfigDialog.kt       # Dialog display
│   ├── RemoteConfigFullScreen.kt   # Full screen display
│   ├── RemoteConfigBanner.kt       # Banner display
│   └── RemoteConfigBottomSheet.kt  # Bottom sheet display
└── di/
    └── RemoteConfigModule.kt       # Koin DI
```
