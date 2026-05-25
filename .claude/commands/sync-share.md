# /sync-share

Alias for `/lib-sync cmp-share`.

Syncs `cmp-share` into the active consuming app — verify-gated Gradle check only
(zero-config module: no Supabase, no DI, no nav wiring needed).

## Usage

```
/sync-share           # Full sync
/sync-share --check   # Dry run, no writes
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-share.md`

Consumer docs: `docs/share/CLAUDE_AI_SETUP.md`
