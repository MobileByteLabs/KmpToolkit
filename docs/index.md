# kmp-toolkit

Cross-platform utilities for Kotlin Multiplatform apps — share sheets, deep
links, network monitoring, in-app updates, observability, and more.

Ships **21 `cmp-*` modules** to Maven Central at
`io.github.mobilebytelabs:*`, all aligned on a single `kmptoolkit.version`.

## Quick links

- [Getting started](getting-started.md) — install + Koin wiring + first-screen example
- [Modules](modules/cmp-share.md) — per-module landing pages (21)
- [Cookbook](cookbook/inter-app-comms/index.md) — task-oriented recipes
- [GitHub](https://github.com/MobileByteLabs/KmpToolkit) — source code

## Module index

| Domain | Modules |
|---|---|
| Inter-app comms | [cmp-share](modules/cmp-share.md), [cmp-intent-launcher](modules/cmp-intent-launcher.md), [cmp-app-intents](modules/cmp-app-intents.md), [cmp-open-url](modules/cmp-open-url.md), [cmp-deep-link](modules/cmp-deep-link.md) |
| Network | [cmp-network-monitor](modules/cmp-network-monitor.md) |
| Observability | [cmp-observe](modules/cmp-observe.md), [cmp-observe-koin](modules/cmp-observe-koin.md), [cmp-firebase-analytics](modules/cmp-firebase-analytics.md) |
| Storage / IO | [cmp-clipboard](modules/cmp-clipboard.md), [cmp-pdf-generator](modules/cmp-pdf-generator.md) |
| UI | [cmp-toast](modules/cmp-toast.md), [cmp-bubble](modules/cmp-bubble.md) |
| Updates / config | [cmp-in-app-update](modules/cmp-in-app-update.md), [cmp-remote-config](modules/cmp-remote-config.md) |
| App lifecycle | [cmp-product-tickets](modules/cmp-product-tickets.md) |
| Compose adapters | [cmp-share-compose](modules/cmp-share-compose.md), [cmp-intent-launcher-compose](modules/cmp-intent-launcher-compose.md), [cmp-app-intents-compose](modules/cmp-app-intents-compose.md), [cmp-network-monitor-compose](modules/cmp-network-monitor-compose.md) |
| Reference | [cmp-library](modules/cmp-library.md) |

## API reference

Each module ships its full Dokka HTML reference inside the Maven Central
`-javadoc.jar` artifact. IntelliJ / Android Studio surfaces it automatically
in hover popups, Quick Documentation, and Symbol search.

For an offline copy, download a module's `*-javadoc.jar`, rename to `.zip`,
and open `index.html`.
