# Remote Config — Sample SQL Scripts

Copy and paste these into the Supabase SQL Editor to create remote config entries.

---

## Static Templates

### 1. Rate Us Dialog (3 times, 72h apart)

```sql
INSERT INTO product_remote_config (product_type, title, description, display_type, icon_emoji,
    action_text, action_type, secondary_action_text, secondary_action_type,
    max_impressions, cooldown_hours, priority, is_enabled)
VALUES (
    'reels_downloader', 'Rate Us', 'Enjoying the app? Leave us a review on the Play Store!',
    'dialog', '⭐',
    'Rate Now', 'store', 'Maybe Later', 'dismiss',
    3, 72, 10, true
);
```

### 2. Force Update (non-dismissible, unlimited)

```sql
INSERT INTO product_remote_config (product_type, title, description, display_type, icon_emoji,
    action_text, action_type, is_dismissible,
    max_impressions, cooldown_hours, priority, is_enabled)
VALUES (
    'reels_downloader', 'Update Required', 'A critical update is available. Please update to continue.',
    'fullscreen', '🔄',
    'Update Now', 'store', false,
    0, 0, 100, true
);
```

### 3. Maintenance Banner (unlimited, 1h cooldown)

```sql
INSERT INTO product_remote_config (product_type, title, display_type, icon_emoji,
    action_text, action_type,
    max_impressions, cooldown_hours, priority, is_enabled)
VALUES (
    'reels_downloader', 'Scheduled maintenance at 2 AM UTC',
    'banner', '⚠️',
    'OK', 'dismiss',
    0, 1, 50, true
);
```

### 4. Changelog Bottom Sheet (show once)

```sql
INSERT INTO product_remote_config (product_type, title, description, display_type, icon_emoji,
    action_text, action_type,
    max_impressions, priority, is_enabled)
VALUES (
    'reels_downloader', 'What''s New in v3.0',
    '• TikWM provider for TikTok\n• Retry with exponential backoff\n• Circuit breaker for failing providers\n• Faster timeouts (15s search, 30s convert)',
    'bottom_sheet', '🎉',
    'Got It', 'dismiss',
    1, 20, true
);
```

---

## Dynamic UI (Server-Driven)

### 5. New Product Launch (fullscreen)

```sql
INSERT INTO product_remote_config (product_type, title, display_type,
    max_impressions, cooldown_hours, priority, is_enabled,
    content_json)
VALUES (
    'reels_downloader', 'Product Launch', 'fullscreen',
    3, 24, 30, true,
    '{"root":{"type":"column","padding":32,"spacing":16,"alignment":"center","children":[{"type":"image","url":"https://play-lh.googleusercontent.com/example.png","height":200,"cornerRadius":20},{"type":"text","content":"Introducing Mood Movies 🎬","style":"headline","align":"center"},{"type":"text","content":"AI-powered movie recommendations that match your mood. Discover your next favorite film.","style":"body","color":"#666666","align":"center"},{"type":"spacer","height":8},{"type":"button","text":"Download Free","style":"primary","action":{"type":"url","value":"https://play.google.com/store/apps/details?id=com.sensei.mood.movies"}},{"type":"button","text":"Not Now","style":"text","action":{"type":"dismiss"}}]}}'
);
```

### 6. Flash Sale (2-day campaign, 5 impressions max)

```sql
INSERT INTO product_remote_config (product_type, title, display_type,
    max_impressions, cooldown_hours, priority, is_enabled,
    start_at, end_at,
    content_json)
VALUES (
    'reels_downloader', 'Flash Sale', 'dialog',
    5, 4, 40, true,
    NOW(), NOW() + INTERVAL '2 days',
    '{"root":{"type":"column","padding":24,"spacing":12,"alignment":"center","children":[{"type":"icon","emoji":"🔥","size":48},{"type":"text","content":"Flash Sale — 50% Off Premium!","style":"headline","align":"center"},{"type":"text","content":"Upgrade to premium for just $0.99. Offer ends in 48 hours!","style":"body","color":"#666666","align":"center"},{"type":"badge","text":"LIMITED TIME","backgroundColor":"#EF4444","color":"#FFFFFF"},{"type":"spacer","height":8},{"type":"button","text":"Upgrade Now","style":"primary","color":"#EF4444","action":{"type":"url","value":"https://example.com/premium"}},{"type":"button","text":"Skip","style":"text","action":{"type":"dismiss"}}]}}'
);
```

### 7. Native Ad Banner (card layout)

```sql
INSERT INTO product_remote_config (product_type, title, display_type,
    max_impressions, cooldown_hours, priority, is_enabled,
    content_json)
VALUES (
    'reels_downloader', 'Sponsored', 'banner',
    5, 24, 5, true,
    '{"root":{"type":"card","cornerRadius":16,"padding":12,"children":[{"type":"row","spacing":12,"alignment":"center","children":[{"type":"image","url":"https://play-lh.googleusercontent.com/example-icon.png","width":48,"height":48,"cornerRadius":8},{"type":"column","spacing":2,"children":[{"type":"text","content":"Byte Wallpaper","style":"title","fontWeight":"bold"},{"type":"text","content":"Beautiful wallpapers for your phone","style":"caption","color":"#888888"}]},{"type":"button","text":"Install","style":"primary","action":{"type":"url","value":"https://play.google.com/store/apps/details?id=org.mobilebytesensei.wallpaper"}}]}]}}'
);
```

### 8. Survey Dialog (show once ever)

```sql
INSERT INTO product_remote_config (product_type, title, display_type,
    max_impressions, priority, is_enabled,
    content_json)
VALUES (
    'reels_downloader', 'Survey', 'dialog',
    1, 15, true,
    '{"root":{"type":"column","padding":24,"spacing":16,"alignment":"center","children":[{"type":"icon","emoji":"📋","size":40},{"type":"text","content":"Quick Survey","style":"headline","align":"center"},{"type":"text","content":"Help us improve! Take a 2-minute survey about your download experience.","style":"body","color":"#666666","align":"center"},{"type":"button","text":"Take Survey","style":"primary","action":{"type":"url","value":"https://forms.google.com/your-survey"}},{"type":"button","text":"No Thanks","style":"text","action":{"type":"dismiss"}}]}}'
);
```

---

## Time-Limited Campaigns

### 9. 7-Day Feature Announcement

```sql
INSERT INTO product_remote_config (product_type, title, display_type,
    max_impressions, priority, is_enabled,
    start_at, end_at,
    content_json)
VALUES (
    'reels_downloader', 'New Feature', 'bottom_sheet',
    2, 25, true,
    NOW(), NOW() + INTERVAL '7 days',
    '{"root":{"type":"column","padding":24,"spacing":12,"alignment":"center","children":[{"type":"icon","emoji":"✨","size":40},{"type":"text","content":"TikTok Downloads Just Got Faster!","style":"headline","align":"center"},{"type":"text","content":"We added TikWM — a dedicated TikTok provider. HD downloads, no watermark, direct URLs.","style":"body","color":"#666666","align":"center"},{"type":"button","text":"Try It Now","style":"primary","action":{"type":"dismiss"}},{"type":"button","text":"Learn More","style":"text","action":{"type":"url","value":"https://mobilebytesensei.com/blog/tikwm"}}]}}'
);
```

### 10. Holiday Promo (Dec 20-31, fullscreen)

```sql
INSERT INTO product_remote_config (product_type, title, display_type,
    max_impressions, cooldown_hours, priority, is_enabled,
    start_at, end_at,
    content_json)
VALUES (
    'reels_downloader', 'Holiday Promo', 'fullscreen',
    3, 48, 35, true,
    '2026-12-20T00:00:00Z', '2026-12-31T23:59:59Z',
    '{"root":{"type":"column","padding":32,"spacing":20,"alignment":"center","children":[{"type":"icon","emoji":"🎄","size":64},{"type":"text","content":"Holiday Special!","style":"headline","align":"center"},{"type":"text","content":"Gift premium to a friend and get 3 months free. Limited time holiday offer.","style":"body","color":"#666666","align":"center"},{"type":"spacer","height":8},{"type":"button","text":"Gift Premium","style":"primary","color":"#10B981","action":{"type":"url","value":"https://example.com/gift"}},{"type":"button","text":"Not Interested","style":"text","action":{"type":"dismiss"}}]}}'
);
```

---

## Visibility Control Examples

### 11. Show Once Ever (server-tracked, survives reinstall)

```sql
INSERT INTO product_remote_config (product_type, title, description, display_type, icon_emoji,
    action_text, action_type, secondary_action_text, secondary_action_type,
    max_impressions, priority, is_enabled)
VALUES (
    'reels_downloader', 'Welcome!', 'Thanks for installing Reels Downloader. Here''s how to get started.',
    'fullscreen', '👋',
    'Get Started', 'dismiss', NULL, 'dismiss',
    1, 5, true
);
```

### 12. Non-Dismissible Critical Alert

```sql
INSERT INTO product_remote_config (product_type, title, description, display_type, icon_emoji,
    action_text, action_type, is_dismissible,
    max_impressions, cooldown_hours, priority, is_enabled)
VALUES (
    'reels_downloader', 'Account Action Required',
    'Your account needs verification. Please verify your email to continue using the app.',
    'fullscreen', '🔒',
    'Verify Now', 'url', false,
    0, 0, 999, true
);
```

---

## Disable / Cleanup

```sql
-- Disable a config
UPDATE product_remote_config SET is_enabled = false WHERE title = 'Flash Sale';

-- Delete expired configs
DELETE FROM product_remote_config WHERE end_at < NOW();

-- View all active configs for a product
SELECT title, display_type, max_impressions, priority, content_json IS NOT NULL as is_dynamic
FROM product_remote_config
WHERE product_type = 'reels_downloader' AND is_enabled = true
ORDER BY priority DESC;
```
