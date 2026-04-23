# cmp-remote-config — Integration Guide

> `io.github.mobilebytelabs:kmptoolkit-remote-config:3.1.0`

---

## Prerequisites

**cmp-product-tickets must be configured first.** `cmp-remote-config` reuses
`ProductTicketsConfig` for Supabase credentials. If you haven't done this yet:

```kotlin
ProductTicketsConfig.init(
    supabaseUrl     = "https://YOUR_PROJECT.supabase.co",
    supabaseAnonKey = "YOUR_ANON_KEY",
    boardType       = "your_app_name",  // used as product_type in remote config queries
)
```

See [docs/user-tickets/SETUP.md](../user-tickets/SETUP.md).

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
kmptoolkit = "3.1.0"

[libraries]
kmptoolkit-remote-config = { module = "io.github.mobilebytelabs:kmptoolkit-remote-config", version.ref = "kmptoolkit" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.kmptoolkit.remote.config)
}
```

---

## Step 2 — Add DI Module to Koin

```kotlin
import com.mobilebytelabs.remoteconfig.di.remoteConfigModule

startKoin {
    modules(
        productTicketsModule,   // required — provides credentials
        remoteConfigModule,     // add this
    )
}
```

---

## Step 3 — Add RemoteConfigHost to Root Composable

Place `RemoteConfigHost` at the root of your app so it can overlay any screen:

```kotlin
import com.mobilebytelabs.remoteconfig.ui.RemoteConfigHost
import com.mobilebytelabs.remoteconfig.model.ActionType

@Composable
fun App() {
    Box(modifier = Modifier.fillMaxSize()) {
        // Your main content / NavHost
        MainNavHost()

        // Remote config overlay — renders on top of everything
        RemoteConfigHost(
            onAction = { actionType: ActionType, actionValue: String? ->
                when (actionType) {
                    ActionType.URL      -> openUrl(actionValue ?: return@RemoteConfigHost)
                    ActionType.DEEPLINK -> navController.navigate(actionValue ?: return@RemoteConfigHost)
                    ActionType.STORE    -> openStore()
                    ActionType.DISMISS  -> { /* handled internally */ }
                    ActionType.NONE     -> { }
                }
            },
        )
    }
}
```

---

## Step 4 — Create Supabase Schema

### `product_remote_config` table

```sql
CREATE TABLE IF NOT EXISTS product_remote_config (
    id                     UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    product_type           TEXT        NOT NULL,
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

### `device_impressions` table

```sql
CREATE TABLE IF NOT EXISTS device_impressions (
    id           UUID        DEFAULT gen_random_uuid() PRIMARY KEY,
    config_id    UUID        NOT NULL REFERENCES product_remote_config(id) ON DELETE CASCADE,
    device_id    TEXT        NOT NULL,
    product_type TEXT        NOT NULL,
    impressions  INT         NOT NULL DEFAULT 0,
    dismissed    BOOLEAN     NOT NULL DEFAULT false,
    created_at   TIMESTAMPTZ DEFAULT NOW(),
    updated_at   TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE device_impressions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Device can manage own impressions" ON device_impressions
    USING (true) WITH CHECK (true);
```

### RPC functions

```sql
-- Get impression count for a device across all configs
CREATE OR REPLACE FUNCTION get_device_impressions(
    p_device_id   TEXT,
    p_product_type TEXT
) RETURNS TABLE (config_id UUID, impressions INT, dismissed BOOLEAN)
LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    RETURN QUERY
    SELECT di.config_id, di.impressions, di.dismissed
    FROM device_impressions di
    WHERE di.device_id = p_device_id AND di.product_type = p_product_type;
END; $$;

-- Record that a device saw a config
CREATE OR REPLACE FUNCTION record_config_impression(
    p_config_id   UUID,
    p_device_id   TEXT,
    p_product_type TEXT
) RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO device_impressions (config_id, device_id, product_type, impressions)
    VALUES (p_config_id, p_device_id, p_product_type, 1)
    ON CONFLICT (config_id, device_id)
    DO UPDATE SET impressions = device_impressions.impressions + 1,
                  updated_at  = NOW();
END; $$;

-- Mark a config as dismissed for a device
CREATE OR REPLACE FUNCTION dismiss_config(
    p_config_id   UUID,
    p_device_id   TEXT,
    p_product_type TEXT
) RETURNS VOID LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    INSERT INTO device_impressions (config_id, device_id, product_type, dismissed)
    VALUES (p_config_id, p_device_id, p_product_type, true)
    ON CONFLICT (config_id, device_id)
    DO UPDATE SET dismissed  = true,
                  updated_at = NOW();
END; $$;
```

---

## Step 5 — Insert Your First Config

```sql
-- Simple update dialog
INSERT INTO product_remote_config
    (product_type, title, description, display_type, action_text, action_type, action_value)
VALUES
    ('my_app', '🎉 New Features!', 'Version 2.0 is here with exciting updates.',
     'dialog', 'See What''s New', 'url', 'https://myapp.com/whats-new');
```

---

## Step 6 — Test It

The config will appear automatically when the app launches and the frequency conditions are met
(first impression, within schedule, platform match).

To force a re-show during testing: delete the device row from `device_impressions`.

---

## AI-Assisted Setup

```
/sync-remote-config           # Full verify-gated sync (4 gates)
/sync-remote-config --check   # Dry run
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
