# cmp-in-app-update

Cross-platform in-app update manager for Kotlin Multiplatform.

```
io.github.mobilebytelabs:kmp-in-app-update:2.1.0
```

---

## What It Does

Unified API for checking and triggering app updates across all KMP platforms. Uses the best
native mechanism on each platform with three resolver options for desktop/JVM.

---

## Platform Support

| Platform | Implementation |
|----------|---------------|
| Android | Google Play In-App Updates API |
| iOS | iTunes Lookup API + App Store |
| macOS | iTunes Lookup API + App Store |
| JVM | Custom version check (GitHub / Supabase / custom URL) |
| Linux | Custom version check |
| Windows | Custom version check |
| tvOS | Not supported (`UpdateResult.NotSupported`) |
| watchOS | Not supported (`UpdateResult.NotSupported`) |
| JS / Wasm | Not supported (`UpdateResult.NotSupported`) |

---

## Quick Start

```kotlin
// GitHub Releases (easiest for desktop)
val config = AppUpdateConfig.builder()
    .android("com.example.app")
    .ios("123456789")
    .github(owner = "myorg", repo = "my-app")
    .build()

// Check for updates
val result = AppUpdate.checkForUpdate(config)
when (result) {
    is UpdateResult.Success -> {
        if (result.updateInfo.isAvailable) {
            AppUpdate.startUpdate(UpdateType.FLEXIBLE, config)
        }
    }
    is UpdateResult.NotSupported -> { /* platform doesn't support updates */ }
    is UpdateResult.Error -> { /* handle error */ }
    is UpdateResult.Cancelled -> { /* user cancelled */ }
}
```

---

## API Reference

### AppUpdate (singleton object)

```kotlin
// Check for update
suspend fun checkForUpdate(config: AppUpdateConfig = AppUpdateConfig.Default): UpdateResult

// Start update flow
suspend fun startUpdate(
    updateType: UpdateType = UpdateType.FLEXIBLE,
    config: AppUpdateConfig = AppUpdateConfig.Default,
): UpdateResult

// Get current installed version
fun getCurrentVersion(): AppVersion

// Open store page for manual update
fun openStoreForUpdate(config: AppUpdateConfig = AppUpdateConfig.Default): Boolean

// Check if in-app updates are supported on this platform
fun isSupported(): Boolean
```

### AppUpdateConfig.Builder

```kotlin
AppUpdateConfig.builder()
    // Mobile
    .android("com.example.app")          // Android package name
    .ios("123456789")                    // iOS App Store numeric ID
    .macos("987654321")                  // macOS App Store ID (different from iOS)

    // Desktop version resolver (choose one)
    .github(owner = "org", repo = "app") // GitHub Releases (recommended)
    .supabase(                           // Supabase app_versions table
        projectUrl = "https://xxx.supabase.co",
        anonKey = "eyJ...",
        tableName = "app_versions",      // default: "app_versions"
    )
    .versionResolver(MyCustomResolver()) // Custom VersionResolver implementation

    // Optional desktop platform config
    .jvm(versionCheckUrl = null, downloadUrl = null)
    .linux(versionCheckUrl = null, storeUrl = null)
    .windows(versionCheckUrl = null, storeUrl = null)

    // Disable specific platforms
    .disableAndroid()
    .disableIos()
    .mobileOnly()    // shortcut: enables only Android + iOS
    .desktopOnly()   // shortcut: enables only macOS, JVM, Linux, Windows
    .build()
```

### UpdateResult

```kotlin
sealed class UpdateResult {
    data class Success(val updateInfo: UpdateInfo) : UpdateResult
    data class Error(val message: String, val cause: Throwable?) : UpdateResult
    object NotSupported : UpdateResult
    object Cancelled : UpdateResult
}

data class UpdateInfo(
    val isAvailable: Boolean,
    val currentVersion: String,
    val availableVersion: String?,
    val updateType: UpdateType,     // FLEXIBLE or IMMEDIATE
    val releaseNotes: String?,
    val downloadUrl: String?,
)
```

---

## Version Resolver Options

| Resolver | Backend | Setup |
|----------|---------|-------|
| `github(owner, repo)` | GitHub Releases | Public/private repo, reads tag + assets |
| `supabase(url, key)` | Supabase table | Requires `app_versions` table (SQL below) |
| `versionResolver(impl)` | Custom | Implement `VersionResolver` interface |

---

## Supabase Schema (optional — for `supabase()` resolver)

```sql
CREATE TABLE app_versions (
    id           SERIAL PRIMARY KEY,
    version      TEXT NOT NULL,
    update_type  TEXT DEFAULT 'FLEXIBLE',
    release_notes TEXT,
    download_url TEXT,
    platform     TEXT DEFAULT 'all',
    created_at   TIMESTAMPTZ DEFAULT NOW()
);

-- Enable RLS
ALTER TABLE app_versions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read" ON app_versions FOR SELECT USING (true);
```

---

## Docs

- [SETUP.md](SETUP.md) — Integration steps
- [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) — AI-assisted setup with `/sync-in-app-update`
