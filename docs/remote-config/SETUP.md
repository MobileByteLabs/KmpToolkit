# cmp-remote-config — Integration Guide

> `io.github.mobilebytelabs:kmptoolkit-remote-config:4.0.0`
>
> **Standalone.** No prerequisite on `cmp-product-tickets` or any other kmp-toolkit
> module. Per-project Supabase model — bring your own `supabaseUrl` + `anonKey`.

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
kmptoolkit-remote-config = "4.0.0"

[libraries]
kmptoolkit-remote-config = { module = "io.github.mobilebytelabs:kmptoolkit-remote-config", version.ref = "kmptoolkit-remote-config" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.kmptoolkit.remote.config)
}
```

---

## Step 2 — Install via `remoteConfig { }` DSL

`cmp-remote-config` ships a Koin `Module` extension function. Drop it inside any
existing `module { }` block — typically your `networkModule` or its equivalent.
No separate top-level Koin module to register, no init function to call from
`Application`.

```kotlin
import com.mobilebytelabs.remoteconfig.remoteConfig
import com.mobilebytelabs.remoteconfig.model.ActionType

val networkModule = module {
    remoteConfig {
        supabaseUrl = "https://YOUR_PROJECT.supabase.co"
        supabaseKey = "YOUR_ANON_KEY"

        // Optional — app-specific action handlers (dispatched when RemoteConfigHost
        // is called without an explicit onAction lambda):
        action(ActionType.PREMIUM)  { _, _ -> navigator.navigateTo("paywall") }
        action("open_downloads")    { v, _ -> navigator.navigateTo("downloads/${v.orEmpty()}") }
    }

    // … the rest of your bindings stay unchanged …
}

startKoin {
    modules(networkModule, /* other modules */)
}
```

### Custom action types

`ActionType` is an open `@JvmInline value class`. Define your own typed constants:

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

Value-class equality is structural — `ActionType("open_downloads") ==
RemoteActions.OPEN_DOWNLOADS`. The strings round-trip exactly through Supabase JSON.

---

## Step 3 — Add `RemoteConfigHost` to the root composable

Place `RemoteConfigHost` at the root of your app so it can overlay any screen:

```kotlin
import com.mobilebytelabs.remoteconfig.ui.RemoteConfigHost
import com.mobilebytelabs.remoteconfig.model.ActionType

@Composable
fun App() {
    Box(modifier = Modifier.fillMaxSize()) {
        MainNavHost()

        // Option A — let the DSL-registered handlers (Step 2) dispatch actions:
        RemoteConfigHost()

        // Option B — explicit per-screen routing (escape hatch). Pass `onAction`
        //            to override the dispatcher entirely:
        // RemoteConfigHost(
        //     onAction = { actionType, actionValue ->
        //         when (actionType) {
        //             ActionType.URL      -> openUrl(actionValue ?: return@RemoteConfigHost)
        //             ActionType.DEEPLINK -> navController.navigate(actionValue ?: return@RemoteConfigHost)
        //             ActionType.STORE    -> openStore()
        //             ActionType.PREMIUM  -> navController.navigate("paywall")
        //             ActionType.DISMISS  -> { /* handled internally */ }
        //             ActionType.NONE     -> { }
        //             else                -> { /* custom — handle in `when` or rely on DSL handler */ }
        //         }
        //     },
        // )
    }
}
```

---

## Step 4 — Create Supabase Schema

### `product_remote_config` table

```sql
CREATE TABLE IF NOT EXISTS product_remote_config (
    id                     UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    platform               TEXT        NOT NULL DEFAULT 'all',
    min_app_version        TEXT,
    max_app_version        TEXT,
    title                  TEXT        NOT NULL,
    description            TEXT,
    image_url              TEXT,
    display_type           TEXT        NOT NULL DEFAULT 'dialog',
    priority               INT         NOT NULL DEFAULT 0,
    is_dismissible         BOOLEAN     NOT NULL DEFAULT true,
    action_text            TEXT,
    action_type            TEXT        NOT NULL DEFAULT 'none',
    action_value           TEXT,
    secondary_action_text  TEXT,
    secondary_action_type  TEXT        NOT NULL DEFAULT 'dismiss',
    secondary_action_value TEXT,
    max_impressions        INT         NOT NULL DEFAULT 1,
    cooldown_hours         INT         NOT NULL DEFAULT 24,
    start_at               TIMESTAMPTZ,
    end_at                 TIMESTAMPTZ,
    is_enabled             BOOLEAN     NOT NULL DEFAULT true,
    accent_color           TEXT,
    icon_emoji             TEXT,
    content_json           TEXT,
    created_at             TIMESTAMPTZ DEFAULT NOW(),
    updated_at             TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE product_remote_config ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read enabled configs" ON product_remote_config
    FOR SELECT USING (is_enabled = true);
```

> **Per-project model**: the `product_type` column from 3.x is no longer used by
> the client. If you are upgrading from 3.x, the column is harmless (ignored by
> the client) — drop it whenever convenient.

### `device_impressions` table

```sql
CREATE TABLE IF NOT EXISTS device_impressions (
    id           UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    config_id    UUID        NOT NULL REFERENCES product_remote_config(id) ON DELETE CASCADE,
    device_id    TEXT        NOT NULL,
    impressions  INT         NOT NULL DEFAULT 0,
    dismissed    BOOLEAN     NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW(),
    UNIQUE (config_id, device_id)
);

ALTER TABLE device_impressions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Device can manage own impressions" ON device_impressions
    USING (true) WITH CHECK (true);
```

### RPC functions

```sql
-- Get impression count for a device across all configs
CREATE OR REPLACE FUNCTION get_device_impressions(p_device_id TEXT)
    RETURNS TABLE (config_id UUID, impressions INT, dismissed BOOLEAN)
    LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    RETURN QUERY
    SELECT di.config_id, di.impressions, di.dismissed
    FROM device_impressions di
    WHERE di.device_id = p_device_id;
END; $$;

-- Record that a device saw a config
CREATE OR REPLACE FUNCTION record_config_impression(p_config_id UUID, p_device_id TEXT)
    RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO device_impressions (config_id, device_id, impressions)
    VALUES (p_config_id, p_device_id, 1)
    ON CONFLICT (config_id, device_id)
    DO UPDATE SET impressions = device_impressions.impressions + 1,
                  updated_at  = NOW();
END; $$;

-- Mark a config as dismissed for a device
CREATE OR REPLACE FUNCTION dismiss_config(p_config_id UUID, p_device_id TEXT)
    RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO device_impressions (config_id, device_id, dismissed)
    VALUES (p_config_id, p_device_id, true)
    ON CONFLICT (config_id, device_id)
    DO UPDATE SET dismissed  = true,
                  updated_at = NOW();
END; $$;
```

> Upgrading from 3.x? The previous RPC signatures took a `p_product_type TEXT`
> argument. The 4.0.0 client no longer passes it. Either keep the old signature
> with a default (`p_product_type TEXT DEFAULT ''`) for backward compatibility,
> or replace with the signatures above.

---


---

## `content_json` — Server-Driven Dynamic UI

When `content_json` is set on a config row, `RemoteConfigHost` **bypasses** the
static templates (Dialog/Banner/FullScreen/BottomSheet using `title` +
`description` + `action_text`) and renders the JSON tree instead. Use this when
the predefined layouts don't fit — e.g. multi-CTA cards, custom icons,
rich-text bodies, badges.

`content_json` is optional. If `null`, the static `title`/`description`/
`action_text` columns are used as before.

### Top-level shape

```json
{
  "root": {
    "type": "column",
    "padding": 24,
    "spacing": 16,
    "children": [
      { "type": "text", "content": "Welcome back!", "style": "headline" },
      { "type": "text", "content": "We've shipped a few updates worth checking out." },
      {
        "type": "button",
        "text": "What's new",
        "style": "primary",
        "action": { "type": "url", "value": "https://example.com/changelog" }
      }
    ]
  }
}
```

The outermost object must have a single `root` key whose value is a UiNode.

### Node types

| `type` | Fields | Notes |
|---|---|---|
| `column` | `children: UiNode[]`, `padding: Int=0`, `spacing: Int=0`, `alignment: "start"|"center"|"end"`, `background: String?` | Vertical layout |
| `row` | `children: UiNode[]`, `padding: Int=0`, `spacing: Int=0`, `alignment: "start"|"center"|"end"` | Horizontal layout |
| `box` | `children: UiNode[]`, `padding: Int=0`, `width: Int?`, `height: Int?`, `alignment: "center"|…`, `background: String?` | Z-stack / aligned container |
| `card` | `children: UiNode[]`, `padding: Int=16`, `cornerRadius: Int=12`, `elevation: Int=2`, `background: String?` | Elevated surface |
| `text` | `content: String` (required), `style: "headline"|"title"|"body"|"caption"|"label"=body`, `color: String?` (hex), `align: "start"|"center"|"end"`, `fontWeight: String?` (`"bold"`, `"semibold"`, …), `maxLines: Int?` | Body text |
| `button` | `text: String` (required), `style: "primary"|"outlined"|"text"=primary`, `action: { type, value }?`, `color: String?` (hex) | CTA — `action.type` flows to your registered `action(...)` handler / `RemoteConfigHost(onAction)` |
| `image` | `url: String` (required), `width: Int?`, `height: Int?`, `cornerRadius: Int=0`, `fit: "crop"|…="crop"` | Remote image |
| `spacer` | `height: Int=0`, `width: Int=0` | Empty space |
| `divider` | `color: String?` (hex), `thickness: Int=1` | Horizontal rule |
| `badge` | `text: String` (required), `color: String?` (hex), `backgroundColor: String?` (hex) | Small label chip |
| `icon` | `emoji: String` (required), `size: Int=24` | Emoji as icon |

Numeric fields are **density-independent pixels (dp)** — interpret as `.dp` in
Compose. Color strings are hex (`"#RRGGBB"` or `"#AARRGGBB"`). Unknown fields are
silently ignored (`ignoreUnknownKeys = true`); unknown `type` values log a Kermit
warning and the node is skipped.

### Actions inside `content_json`

Inside a `button`, the `action` object follows the same `ActionType` rules as
the top-level `action_type` column:

```json
{
  "type": "button",
  "text": "Upgrade",
  "action": { "type": "premium", "value": "monthly" }
}
```

- The string `"premium"` maps to `ActionType.PREMIUM`.
- `RemoteConfigHost` routes the action through your DSL-registered handler
  (`action(ActionType.PREMIUM) { _, _ -> … }`) when called without an explicit
  `onAction` lambda, or to your explicit `onAction` lambda otherwise.
- `value` is passed through as the second argument to the handler.
- The special type `"dismiss"` (`ActionType.DISMISS`) is handled internally —
  no handler needed.

### `display_type` interaction

`content_json` is rendered inside the same chrome that the `display_type`
column specifies:

| `display_type` | What wraps `content_json` |
|---|---|
| `dialog` | Centered `Dialog` with rounded `Surface` (24.dp padding) |
| `fullscreen` | Edge-to-edge scrollable `Surface` on the background color |
| `bottom_sheet` | Bottom-aligned `Surface` with rounded top corners |
| `banner` | Inline `DynamicUiRenderer` (no `Dialog`) — use when embedding in a sticky bar |

### Worked example — promo card

```json
{
  "root": {
    "type": "card",
    "padding": 20,
    "cornerRadius": 16,
    "children": [
      {
        "type": "row",
        "spacing": 12,
        "alignment": "center",
        "children": [
          { "type": "icon", "emoji": "🎉", "size": 32 },
          {
            "type": "column",
            "spacing": 4,
            "children": [
              { "type": "text", "content": "Pro Plan — 30% off", "style": "title", "fontWeight": "bold" },
              { "type": "text", "content": "Only for the next 7 days.", "style": "caption" }
            ]
          },
          { "type": "badge", "text": "NEW", "backgroundColor": "#FF5722", "color": "#FFFFFF" }
        ]
      },
      { "type": "spacer", "height": 16 },
      {
        "type": "button",
        "text": "Upgrade now",
        "style": "primary",
        "action": { "type": "premium", "value": "monthly" }
      }
    ]
  }
}
```

Stored as:
```sql
INSERT INTO product_remote_config
    (title, display_type, content_json)
VALUES
    ('Pro Plan promo', 'dialog', '<the JSON above>');
```

The `title` column is still required by the schema but is ignored when
`content_json` is set — the JSON tree owns the entire body.

## Step 5 — Insert Your First Config

```sql
-- Simple update dialog
INSERT INTO product_remote_config
    (title, description, display_type, action_text, action_type, action_value)
VALUES
    ('🎉 New Features!', 'Version 2.0 is here with exciting updates.',
     'dialog', 'See What''s New', 'url', 'https://myapp.com/whats-new');
```

---

## Step 6 — Test It

The config will appear automatically when the app launches and the frequency
conditions are met (first impression, within schedule, platform match).

To force a re-show during testing: delete the device row from `device_impressions`.

---

## Migrating from 3.x

1. Bump `libs.versions.toml` → `kmptoolkit-remote-config = "4.0.0"`.
2. Replace `RemoteConfigConfig.supabaseUrl = …; supabaseKey = …; productType = …` with
   the `remoteConfig { supabaseUrl = …; supabaseKey = … }` DSL block inside an
   existing Koin `module { }`. Drop the separate `initRemoteConfig()` function
   if you had one.
3. Drop `remoteConfigModule` from your `startKoin { modules(…) }` list — the DSL
   registers bindings inside the host module.
4. Remove any references to `ProductTicketsConfig`-based init for remote-config;
   the modules are no longer linked.
5. `ActionType.from(value)` was removed. Replace `ActionType.from("foo")` with
   `ActionType("foo")` (value-class constructor). Custom types now live in your
   own `object` and round-trip exactly.
6. **Server**: drop `product_remote_config.product_type` column whenever
   convenient (no longer used by client). RPCs: drop the `p_product_type`
   argument, or keep with a default for back-compat.

---

## AI-Assisted Setup

```
/sync-remote-config           # Full verify-gated sync (3 gates)
/sync-remote-config --check   # Dry run
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
