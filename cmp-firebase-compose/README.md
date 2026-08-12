# cmp-firebase-compose

Compose Multiplatform auto-tracking for [`cmp-firebase`](../cmp-firebase). Declarative analytics —
drop a composable in and screens, clicks, and lifecycle emit automatically through your app's
`AnalyticsHelper`. Compose-target only (Android, iOS, macOS, JVM, JS, WasmJS); the non-Compose
engine stays in `cmp-firebase`.

Every helper respects the engine's master `setCollectionEnabled(false)` opt-out and
`AnalyticsConfig.autoScreenTracking` — flip collection off and these stop emitting with no
per-call-site changes.

## Install

```kotlin
commonMain.dependencies {
    implementation("io.github.mobilebytelabs:cmp-firebase-compose:<version>")
    // transitively brings io.github.mobilebytelabs:cmp-firebase
}
```

## Provide the helper once

```kotlin
CompositionLocalProvider(LocalAnalyticsHelper provides analytics) {
    App()
}
```

Unset, `LocalAnalyticsHelper` defaults to `NoOpAnalyticsHelper`, so previews and tests never emit.

## APIs

| API | Emits |
|-----|-------|
| `NavController.trackScreenViews()` | `screen_view` + `screen_transition{from,to}` on every destination change — install once next to your `NavHost` |
| `TrackScreenView(screenName, sourceScreen?)` | `screen_view` on enter (per-screen, when not using the NavHost hook) |
| `TrackComposableLifecycle(componentName)` | `component_enter` / `component_exit` |
| `Modifier.trackClick(label, analytics, screen?) { onClick() }` | `button_click` then invokes `onClick` |
| `LocalAnalyticsHelper` / `rememberAnalyticsHelper()` | the composition's `AnalyticsHelper` |

### Whole-app screen tracking

```kotlin
val nav = rememberNavController()
nav.trackScreenViews()          // every destination → screen_view + screen_transition
NavHost(nav, startDestination = "home") { /* ... */ }
```

### Per-screen / per-component

```kotlin
@Composable fun ProfileScreen() {
    TrackScreenView("profile")
    Text("Save", Modifier.trackClick("save", rememberAnalyticsHelper(), "profile") { viewModel.save() })
}
```

## See also

- [`cmp-firebase`](../cmp-firebase) — the analytics engine (trackers, funnels, offline queue, network telemetry)
