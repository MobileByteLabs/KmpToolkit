# /sync-remote-config

Alias for `/lib-sync cmp-remote-config`.

Syncs `cmp-remote-config` 4.0.0+ into the active consuming app — Gradle dependency,
Supabase schema delta (`product_remote_config` + `device_impressions`), and app wiring
(the `remoteConfig { … }` DSL block inside an existing Koin module + `RemoteConfigHost`
in the root composable).

`cmp-remote-config` is standalone — no prerequisite on `cmp-product-tickets` or any
other kmp-toolkit module.

## Usage

```
/sync-remote-config                 # Full sync
/sync-remote-config --check         # Dry run, no writes
/sync-remote-config --migrate-only  # Schema delta only
/sync-remote-config --wiring-only   # App wiring only (DSL + RemoteConfigHost)
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-remote-config.md`

Consumer docs: `docs/remote-config/CLAUDE_AI_SETUP.md`
