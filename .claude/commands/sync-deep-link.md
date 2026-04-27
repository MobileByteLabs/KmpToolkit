# /sync-deep-link

Alias for `/lib-sync cmp-deep-link`.

Syncs `cmp-deep-link` into a consuming KMP app — verify-gated across 3 gates:
- **Gate 1**: Gradle dependency (`kmp-deep-link:3.2.1`)
- **Gate 2**: N/A (no Supabase backend)
- **Gate 3**: Platform wiring per active targets:
  - **Android** — `AndroidManifest.xml` intent-filter (zero Kotlin required)
  - **iOS/macOS** — `swift/DeepLinkPlugin.swift` copy + `.deepLinkAutoHandle()` note
  - **JVM Desktop** — `handleLaunchArgs(args)` in `main()`
  - **Browser** — zero-config (HASH auto-init); stale `initBrowser(HASH)` detection

## Usage

```
/sync-deep-link                  # Full sync — all gates
/sync-deep-link --check          # Dry run, no writes
/sync-deep-link --wiring-only    # Gate 3 only
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-deep-link.md`

Consumer docs: `cmp-deep-link/docs/` (ANDROID.md, IOS.md, DESKTOP.md, WEB.md, MACOS.md)
