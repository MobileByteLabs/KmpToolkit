---
title: "How do I issue a consumer anon-key for library events?"
reviewed_by:
  date: 2026-06
  version: 3.5.x
---

# How do I issue a consumer anon-key for library events?

Library events (T2 telemetry — `framework-supabase.library_events`) require a
per-consumer anon-key so that the framework's RLS policies can attribute
incoming rows to the right consumer app.

## Quick start (minimal MWE)

From the framework root (claude-product-cycle):

```bash
# 1. Issue a per-consumer anon-key for the framework-supabase project.
bash core/scripts/library-events-db.sh issue-consumer-anon-key \
    my-app-slug owner@my-org.com

# 2. Push the key to the active secrets vault.
/secrets push --category supabase \
              --id library-events-anon-key \
              --account-email owner@my-org.com

# 3. Pull into your consumer project so the cmp-observe hook reads it.
cd workspaces/<ws>/<consumer-project>/source/<consumer-project>
/secrets pull
```

```kotlin
// Inside your consumer init path:
val anonKey = BuildConfig.LIBRARY_EVENTS_ANON_KEY
SupabaseEventsHook(anonKey = anonKey).register()
```

## Caveats / per-platform notes

- The anon-key is per-consumer — DO NOT reuse one consumer's key in another
  app; the framework's `consumer_id` claim is derived from the key issuer.
- On rotation: re-run `issue-consumer-anon-key`, the old key remains valid for
  24h to drain in-flight events.
- iOS / Android consumer apps inherit the key via the standard `secrets-pull`
  materialization — `local.properties` (Android Gradle) / `Secrets.swift`
  (iOS BuildSettings).

## Related

- Module: [cmp-observe](../../modules/cmp-observe.md)
- See also: [Register Firebase hooks at app startup](register-firebase-hooks.md)
- Internal docs: [`docs/guides/library-observability/LIBRARY_OBSERVABILITY_GUIDE.md`](https://github.com/MobileByteLabs/KmpToolkit/blob/development/docs/guides/library-observability/LIBRARY_OBSERVABILITY_GUIDE.md) (framework-side)
