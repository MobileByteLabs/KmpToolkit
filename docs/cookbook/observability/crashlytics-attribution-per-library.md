---
title: "How do I attribute a crash to a specific cmp-* library?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I attribute a crash to a specific cmp-* library?

When `FirebaseCrashlyticsAttributionHook` is registered, every cmp-* library
auto-tags its own initialization, navigation, and uncaught exceptions with a
`cmp_library` custom key so you can filter Crashlytics issues per library
without per-call boilerplate.

## Quick start (minimal MWE)

```kotlin
import org.koin.core.context.startKoin
import com.mobilebytelabs.kmptoolkit.observe.koin.observeKoinModule
import com.mobilebytelabs.kmptoolkit.observe.koin.FirebaseCrashlyticsAttributionHook

startKoin {
    modules(
        observeKoinModule(
            hooks = listOf(FirebaseCrashlyticsAttributionHook()),
        ),
    )
}
```

In the Firebase console:

```text
Crashlytics → Issues → Filter → Custom key: cmp_library == "cmp-share"
```

## Caveats / per-platform notes

- Tag is set on the cmp-* `notifyInit` thread — if a crash happens during JVM
  startup before any cmp-* module initializes, the tag is absent.
- **iOS:** custom keys propagate to Crashlytics within 200ms (vs Android's
  synchronous setCustomKey).
- The `cmp_library` key reflects the LAST library to call `notifyInit` on the
  current thread; for genuine multi-library crashes, inspect the breadcrumbs
  trail rather than the tag alone.

## Related

- Module: [cmp-observe-koin](../../modules/cmp-observe-koin.md)
- Library observability surface: `RULE-LIB-OBSERVABILITY-SURFACE-001`
- See also: [Register Firebase hooks at app startup](register-firebase-hooks.md)
