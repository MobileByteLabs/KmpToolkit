# /sync-bubble

Alias for `/lib-sync cmp-bubble`.

Syncs `cmp-bubble` into the active consuming app — verify-gated Gradle check
and wiring (createBubble() usage).

## Usage

```
/sync-bubble                  # Full sync
/sync-bubble --check          # Dry run, no writes
/sync-bubble --wiring-only    # Wiring check only
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-bubble.md`

Consumer docs: `docs/bubble/CLAUDE_AI_SETUP.md`
