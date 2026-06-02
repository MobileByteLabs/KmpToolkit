---
title: "How do I register Firebase hooks at app startup?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I register Firebase hooks at app startup?

## Quick start (minimal MWE)

```kotlin
import org.koin.core.context.startKoin
import com.mobilebytelabs.kmptoolkit.observe.koin.observeKoinModule
import com.mobilebytelabs.kmptoolkit.observe.koin.FirebaseCrashlyticsAttributionHook
import com.mobilebytelabs.kmptoolkit.observe.koin.FirebaseAnalyticsHealthHook

fun bootKoin() {
    startKoin {
        modules(
            observeKoinModule(
                hooks = listOf(
                    FirebaseCrashlyticsAttributionHook(),
                    FirebaseAnalyticsHealthHook(),
                ),
            ),
        )
    }
}
```

## Caveats / per-platform notes

- The hooks run inside cmp-* libraries' `notifyInit` paths — they are no-ops
  until at least one cmp-* module's init path executes.
- **iOS:** Firebase SDK must already be initialized (`FirebaseApp.configure()`)
  in your `AppDelegate` before `bootKoin()` runs.
- **Android:** Firebase auto-initializes via `FirebaseInitProvider`; no extra
  call needed.
- **JVM Desktop / wasmJs:** Firebase Crashlytics is not available; the
  attribution hook is a no-op on those platforms.

## Related

- Module: [cmp-observe-koin](../../modules/cmp-observe-koin.md) + [cmp-observe](../../modules/cmp-observe.md)
- See also: [Issue a consumer anon-key for library events](consumer-anon-key-setup.md)
- See also: [Attribute a crash to a specific cmp-* library](crashlytics-attribution-per-library.md)
