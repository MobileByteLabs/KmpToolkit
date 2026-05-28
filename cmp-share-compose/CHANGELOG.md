# Changelog — cmp-share-compose

> Versioning: this module ships with the shared `kmptoolkit.version` (currently `3.3.2`, next bump to `3.4.0`). There is no separate per-module version.

## [Unreleased] (kmptoolkit 3.4.0 candidate)

### Added (initial release — inter-app-comms-compose-completeness Phase 6)
- `@Composable rememberShareLauncher()` — Compose-scoped accessor for the core `Share` object
- `@Composable ShareSheet(payload, onDismiss, ...)` — Material 3 modal bottom sheet with payload preview + Share button
- `@Composable ShareButton(payload, ...)` — Material 3 IconButton invoking `Share.share()` on click
- 9 Compose-MP-supported targets: `androidLibrary`, `jvm`, `iosX64`/`iosArm64`/`iosSimulatorArm64`, `macosX64`/`macosArm64`, `js`, `wasmJs`

### Dependencies (transitive)
- `org.jetbrains.compose.material3:material3` ~1.10.0-alpha05
- `org.jetbrains.compose.material:material-icons-extended` ~1.7.5 (matches Compose Multiplatform)
- `androidx.activity:activity-compose` ~1.12.2 (Android only)

### Rationale
Authored as part of `inter-app-comms-compose-completeness` (v0.4 → v1.0). Parallel to the existing `cmp-intent-launcher-compose` precedent. Closes the gap where v0.3-alpha shipped `cmp-share` core but no Compose adapter module.
