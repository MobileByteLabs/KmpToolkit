# /sync-app-intents

Alias for `/lib-sync cmp-app-intents`.

Syncs `cmp-app-intents` into the active consuming app — verify-gated Gradle check only
(zero-config at Gradle level: no Supabase, no DI, no nav wiring needed;
iOS Swift bridge installation is a developer responsibility — see SETUP.md Step 4).

## Usage

```
/sync-app-intents           # Full sync
/sync-app-intents --check   # Dry run, no writes
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-app-intents.md`

Consumer docs: `docs/app-intents/CLAUDE_AI_SETUP.md`
