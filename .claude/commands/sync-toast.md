# /sync-toast

Alias for `/lib-sync cmp-toast`.

Syncs `cmp-toast` into the active consuming app — verify-gated Gradle check and
ToastHost placement in root composable.

## Usage

```
/sync-toast           # Full sync
/sync-toast --check   # Dry run, no writes
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-toast.md`

Consumer docs: `docs/toast/CLAUDE_AI_SETUP.md`
