# Changelog — cmp-intent-launcher-compose

## [Unreleased]

### Added
- Initial release of `cmp-intent-launcher-compose` adapter module — holds `@Composable rememberIntentLauncher()` extracted from `cmp-intent-launcher` core (see `inter-app-comms-real-native-impls` Phase 1, 2026-05-28).
- 9 Compose-MP-supported targets: androidLibrary, jvm, iosX64/Arm64/SimulatorArm64, macosX64/Arm64, js, wasmJs.

### Migration
- v0.2 consumers using `import com.mobilebytelabs.kmptoolkit.intentlauncher.rememberIntentLauncher` must:
  1. Add `cmp-intent-launcher-compose` Gradle dep alongside the existing `cmp-intent-launcher` dep.
  2. Update import to `com.mobilebytelabs.kmptoolkit.intentlauncher.compose.rememberIntentLauncher`.
  3. No call-site changes needed.

### Rationale
- The Compose Compiler Gradle plugin is module-level and requires `compose.runtime` on every target classpath. This blocked `cmp-intent-launcher` from declaring tvOS/watchOS/Linux/mingw targets in v0.2. Split allows the core to reach 19 KMP targets without forcing Compose on consumers that don't need it.
