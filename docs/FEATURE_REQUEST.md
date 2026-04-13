# Feature Request Module

> `com.mobilebytesensei:kmptoolkit:1.0.0`

A complete feature request and wishlist system. Users can submit feature requests, browse existing requests, upvote ideas, and see implemented features with resolution notes.

## Screenshots

```
┌──────────────────────┐  ┌──────────────────────┐  ┌──────────────────────┐
│ Feature Wishlist   +  │  │ Request a Feature    │  │ Feature              │
│                       │  │                      │  │                      │
│ [Requested][Implement]│  │ What's your idea?    │  │ 💡 New app icons     │
│                       │  │ ┌──────────────────┐ │  │                   ▲  │
│ ┌───────────────────┐ │  │ │                  │ │  │ [Requested]       3  │
│ │ Download queue    │ │  │ └──────────────────┘ │  │ ─────────────────── │
│ │ [Requested]    ▲2 │ │  │                      │  │                      │
│ └───────────────────┘ │  │ Tell us more         │  │ Feature details      │
│ ┌───────────────────┐ │  │ ┌──────────────────┐ │  │                      │
│ │ Dark mode         │ │  │ │                  │ │  │ I would love to...   │
│ │ [Planned]      ▲5 │ │  │ │                  │ │  │                      │
│ └───────────────────┘ │  │ └──────────────────┘ │  │                      │
│                       │  │                      │  │ [     Close      ]   │
│ [+ Submit Request]    │  │ [  Submit Request  ] │  │                      │
└──────────────────────┘  └──────────────────────┘  └──────────────────────┘
   Wishlist Screen           Submit Form              Detail + Upvote
```

## Features

- **Feature Wishlist** — Two tabs: Requested & Implemented
- **Submit Request** — Title, description, category chips (💡 New Feature, 🎨 UI/Design, ⚡ Performance, ⬇️ Download, 🐛 Bug Fix), optional email
- **Feature Detail** — Full description with upvote button
- **Upvote** — Atomic increment via Supabase RPC
- **Multi-app** — `productType` filter lets all apps share one table
- **Status badges** — Requested, In Review, Planned, Shipped
- **Resolution** — Shows how implemented features were resolved

## Setup

### 1. Add dependency

```kotlin
// gradle/libs.versions.toml
[versions]
kmptoolkit = "1.0.0"

[libraries]
kmptoolkit = { module = "com.mobilebytesensei:kmptoolkit", version.ref = "kmptoolkit" }
```

```kotlin
// build.gradle.kts
commonMain.dependencies {
    implementation(libs.kmptoolkit)
}
```

### 2. Create Supabase table

Run in your shared Supabase project's SQL Editor:

```sql
CREATE TABLE feature_requests (
    id UUID DEFAULT gen_random_uuid() PRIMARY KEY,
    product_type TEXT NOT NULL,
    title TEXT NOT NULL,
    description TEXT NOT NULL,
    category TEXT DEFAULT 'general',
    status TEXT DEFAULT 'pending',
    resolution TEXT,
    user_email TEXT,
    device_info TEXT,
    upvotes INT DEFAULT 0,
    created_at TIMESTAMPTZ DEFAULT NOW(),
    updated_at TIMESTAMPTZ DEFAULT NOW()
);

CREATE INDEX idx_feature_requests_product ON feature_requests(product_type);

ALTER TABLE feature_requests ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Anyone can submit" ON feature_requests FOR INSERT WITH CHECK (true);
CREATE POLICY "Anyone can read" ON feature_requests FOR SELECT USING (true);

-- Upvote RPC (atomic increment)
CREATE OR REPLACE FUNCTION upvote_feature(feature_id UUID)
RETURNS void LANGUAGE plpgsql SECURITY DEFINER AS $$
BEGIN
    UPDATE feature_requests SET upvotes = upvotes + 1, updated_at = NOW() WHERE id = feature_id;
END;
$$;
```

### 3. Initialize

```kotlin
// App startup (Application.onCreate or Koin module)
FeatureRequestConfig.init(
    supabaseUrl = "https://your-project.supabase.co",
    supabaseAnonKey = "your-anon-key",
    productType = "your_app_name",
)
```

### 4. Install Koin module

```kotlin
import com.mobilebytesensei.featurerequest.di.featureRequestModule

modules(featureRequestModule)
```

### 5. Add navigation

```kotlin
import com.mobilebytesensei.featurerequest.ui.featureWishlistDestination
import com.mobilebytesensei.featurerequest.ui.navigateToFeatureWishlist

// In NavHost
featureWishlistDestination(onBackClick = navController::popBackStack)

// Navigate from anywhere
navController.navigateToFeatureWishlist()
```

## Multi-App Support

All apps write to the same `feature_requests` table, filtered by `product_type`:

| App | productType |
|-----|:-----------|
| Reels Downloader | `reels_downloader` |
| Byte Wallpaper | `wallpaper` |
| Mood Movies | `mood_movies` |

## Architecture

```
com.mobilebytesensei.featurerequest/
├── FeatureRequestConfig.kt          # Configuration
├── model/
│   └── FeatureRequest.kt            # Data classes
├── network/
│   ├── FeatureRequestClient.kt      # Supabase client
│   └── FeatureRequestService.kt     # Submit, list, upvote
├── data/
│   └── FeatureRequestRepository.kt  # Business logic
├── di/
│   └── FeatureRequestModule.kt      # Koin DI
└── ui/
    ├── FeatureWishlistScreen.kt      # Main screen (tabs + list + detail)
    ├── FeatureWishlistViewModel.kt   # State management
    ├── FeatureWishlistNavigation.kt  # Nav destination
    └── RequestFeatureScreen.kt       # Submit form
```
