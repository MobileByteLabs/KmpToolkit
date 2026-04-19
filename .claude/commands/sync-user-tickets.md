# /sync-user-tickets

Alias for `/lib-sync cmp-user-tickets`.

Syncs `cmp-user-tickets` into the active consuming app — verify-gated Gradle check,
Supabase schema delta (team-safe), and app wiring (Config + Koin + Navigation).

## Usage

```
/sync-user-tickets                  # Full sync
/sync-user-tickets --check          # Dry run, no writes
/sync-user-tickets --migrate-only   # Schema delta only
/sync-user-tickets --wiring-only    # App wiring only
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-user-tickets.md`

Consumer docs: `docs/user-tickets/CLAUDE_AI_SETUP.md`
