# /sync-clipboard

Alias for `/lib-sync cmp-clipboard`.

Syncs `cmp-clipboard` into the active consuming app — verify-gated Gradle check only
(zero-config module: no Supabase, no DI, no nav wiring needed).

## Usage

```
/sync-clipboard           # Full sync
/sync-clipboard --check   # Dry run, no writes
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-clipboard.md`

Consumer docs: `docs/clipboard/CLAUDE_AI_SETUP.md`
