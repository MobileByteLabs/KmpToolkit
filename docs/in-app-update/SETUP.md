# cmp-in-app-update — Integration Guide

> `io.github.mobilebytelabs:kmp-in-app-update:2.1.0`

---

## Step 1 — Add Gradle Dependency

### `gradle/libs.versions.toml`

```toml
[versions]
kmp-in-app-update = "2.1.0"

[libraries]
kmp-in-app-update = { module = "io.github.mobilebytelabs:kmp-in-app-update", version.ref = "kmp-in-app-update" }
```

### `shared/build.gradle.kts`

```kotlin
commonMain.dependencies {
    implementation(libs.kmp.in.app.update)
}
```

---

## Step 2 — Build AppUpdateConfig

Choose the version resolver that fits your setup:

### Option A — GitHub Releases (recommended for open-source / desktop apps)

```kotlin
import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdateConfig

val config = AppUpdateConfig.builder()
    .android("com.example.myapp")
    .ios("123456789")               // App Store numeric ID
    .github(
        owner = "myorg",
        repo  = "my-app",
        token = null,               // for private repos: BuildConfig.GITHUB_TOKEN
    )
    .build()
```

### Option B — Supabase

```kotlin
val config = AppUpdateConfig.builder()
    .android("com.example.myapp")
    .ios("123456789")
    .supabase(
        projectUrl = "https://xxxx.supabase.co",
        anonKey    = "eyJ...",
        tableName  = "app_versions",   // default
    )
    .build()
```

### Option C — Custom resolver

```kotlin
class MyBackendResolver : VersionResolver {
    override suspend fun resolve(): VersionInfo {
        val response = httpClient.get("https://api.myapp.com/version")
        return VersionInfo(
            version      = response.version,
            updateType   = UpdateType.FLEXIBLE,
            releaseNotes = response.notes,
            downloadUrl  = response.downloadUrl,
        )
    }
}

val config = AppUpdateConfig.builder()
    .android("com.example.myapp")
    .versionResolver(MyBackendResolver())
    .build()
```

---

## Step 3 — Check for Updates on App Launch

```kotlin
import com.mobilebytelabs.kmptoolkit.appupdate.AppUpdate
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateResult
import com.mobilebytelabs.kmptoolkit.appupdate.UpdateType

// In ViewModel init or app startup
viewModelScope.launch {
    val result = AppUpdate.checkForUpdate(config)

    when (result) {
        is UpdateResult.Success -> {
            val info = result.updateInfo
            if (info.isAvailable) {
                when (info.updateType) {
                    UpdateType.IMMEDIATE -> {
                        // Must update now — block UI or force update
                        AppUpdate.startUpdate(UpdateType.IMMEDIATE, config)
                    }
                    UpdateType.FLEXIBLE -> {
                        // Show non-blocking update banner
                        showUpdateBanner(info.availableVersion)
                    }
                }
            }
        }
        is UpdateResult.NotSupported -> { /* iOS/macOS/desktop handled differently */ }
        is UpdateResult.Error -> { /* log, don't crash */ }
        is UpdateResult.Cancelled -> { /* user dismissed */ }
    }
}
```

---

## Step 4 — Trigger Update Flow

```kotlin
// When user taps "Update now"
viewModelScope.launch {
    AppUpdate.startUpdate(UpdateType.FLEXIBLE, config)
    // Android: triggers Play In-App Updates download
    // iOS/macOS: opens App Store
    // Desktop: opens download URL or store
}

// Or just open the store page
AppUpdate.openStoreForUpdate(config)
```

---

## Step 5 (Optional) — Supabase `app_versions` Table

Only needed when using the Supabase resolver (`Option B` above):

```sql
CREATE TABLE IF NOT EXISTS app_versions (
    id            SERIAL PRIMARY KEY,
    version       TEXT NOT NULL,
    update_type   TEXT NOT NULL DEFAULT 'FLEXIBLE',   -- FLEXIBLE | IMMEDIATE
    release_notes TEXT,
    download_url  TEXT,
    platform      TEXT NOT NULL DEFAULT 'all',         -- all | android | ios | desktop
    created_at    TIMESTAMPTZ DEFAULT NOW()
);

ALTER TABLE app_versions ENABLE ROW LEVEL SECURITY;
CREATE POLICY "Public read" ON app_versions FOR SELECT USING (true);

-- Insert a version
INSERT INTO app_versions (version, update_type, release_notes)
VALUES ('2.1.0', 'FLEXIBLE', 'New features in this release...');
```

---

## Troubleshooting

| Symptom | Cause | Fix |
|---------|-------|-----|
| `UpdateResult.NotSupported` on Android | Google Play not available | Expected on emulators / sideload builds |
| Desktop always shows no update | No version resolver configured | Add `.github()` or `.supabase()` |
| `libs.kmp.in.app.update` unresolved | Gradle alias format | Use `kmp-in-app-update` (not `kmpInAppUpdate`) |

---

## AI-Assisted Setup

```
/sync-in-app-update           # Full verify-gated sync
/sync-in-app-update --check   # Dry run
```

See [CLAUDE_AI_SETUP.md](CLAUDE_AI_SETUP.md) for full docs.
