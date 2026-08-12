---
title: "Observability cookbook"
description: "Recipes for wiring Firebase Crashlytics + Analytics + Performance and Supabase telemetry — cmp-observe + cmp-observe-koin + cmp-firebase."
---

# Observability cookbook

Recipes for wiring kmp-toolkit's observability surface — Firebase Crashlytics
attribution, per-consumer Analytics health, and Supabase event ingest setup.

## Recipes

- [Register Firebase hooks at app startup](register-firebase-hooks.md)
- [Issue a consumer anon-key for library events](consumer-anon-key-setup.md)
- [Attribute a crash to a specific cmp-* library](crashlytics-attribution-per-library.md)

## Modules involved

- [cmp-observe](../../modules/cmp-observe.md)
- [cmp-observe-koin](../../modules/cmp-observe-koin.md)
- [cmp-firebase](../../modules/cmp-firebase.md)
