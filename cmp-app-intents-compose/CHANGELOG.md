# Changelog — cmp-app-intents-compose

> Versioning: ships with shared `kmptoolkit.version` (currently `3.3.2`, next bump to `3.4.0`). No separate per-module version.

## [Unreleased] (kmptoolkit 3.4.0 candidate)

### Added (initial release — inter-app-comms-compose-completeness Phase 7)
- `@Composable AppIntentsRegistration(config: AppIntentsConfig)` — `DisposableEffect`-wrapped lifecycle-bound registration
- `@Composable rememberRegisteredAppIntents(config)` — memoized accessor variant
- `@Composable AppIntentsRegistry(modifier, onInvokeResult)` — Material 3 LazyColumn dev/debug UI for `AppIntents.invokeForTesting`
- 9 Compose-MP-supported targets

### Dependencies (transitive)
- `org.jetbrains.compose.material3:material3` ~1.10.0-alpha05
- `org.jetbrains.compose.material:material-icons-extended` ~1.7.5
- `org.jetbrains.compose.foundation:foundation` ~1.7.5

### Rationale
Authored alongside `cmp-share-compose` (Phase 6) to give the IPC suite uniform Compose-MP adapter coverage across all 3 core modules.
