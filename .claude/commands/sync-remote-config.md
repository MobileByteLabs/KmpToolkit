# /sync-remote-config

Alias for `/lib-sync cmp-remote-config`.

Syncs `cmp-remote-config` into the active consuming app — prerequisite check (cmp-user-tickets),
verify-gated Gradle, Supabase schema delta (product_remote_config + device_impressions),
and app wiring (remoteConfigModule + RemoteConfigHost).

## Usage

```
/sync-remote-config                 # Full sync
/sync-remote-config --check         # Dry run, no writes
/sync-remote-config --migrate-only  # Schema delta only
/sync-remote-config --wiring-only   # App wiring only
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-remote-config.md`

Consumer docs: `docs/remote-config/CLAUDE_AI_SETUP.md`
