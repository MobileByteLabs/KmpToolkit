# /sync-in-app-update

Alias for `/lib-sync cmp-in-app-update`.

Syncs `cmp-in-app-update` into the active consuming app — verify-gated Gradle check,
optional Supabase schema (if using Supabase resolver), and AppUpdateConfig wiring.

## Usage

```
/sync-in-app-update                 # Full sync
/sync-in-app-update --check         # Dry run, no writes
/sync-in-app-update --migrate-only  # Gate 2 only (Supabase schema)
/sync-in-app-update --wiring-only   # Gate 3 only (config wiring)
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-in-app-update.md`

Consumer docs: `docs/in-app-update/CLAUDE_AI_SETUP.md`
