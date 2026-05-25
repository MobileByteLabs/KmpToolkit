# /sync-intent-launcher

Alias for `/lib-sync cmp-intent-launcher`.

Syncs `cmp-intent-launcher` into the active consuming app — verify-gated Gradle check only
(zero-config at Gradle level: no Supabase, no DI, no nav wiring needed;
Android Activity wiring is a developer responsibility — see SETUP.md Step 4).

## Usage

```
/sync-intent-launcher           # Full sync
/sync-intent-launcher --check   # Dry run, no writes
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-intent-launcher.md`

Consumer docs: `docs/intent-launcher/CLAUDE_AI_SETUP.md`
