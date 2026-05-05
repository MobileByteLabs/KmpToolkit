# /sync-firebase-analytics

Alias for `/lib-sync cmp-firebase-analytics`.

Verify-gated sync of `cmp-firebase-analytics` into the active consuming KMP app.
5 gates: Gradle dep + Firebase config files + DI wiring + PROJECT_CONFIG analytics block + (conditional) MP API secret for nonFirebase platforms.

## Usage

```
/sync-firebase-analytics           # Full sync — walks all 5 gates
/sync-firebase-analytics --check   # Dry run — show status, no writes
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-firebase-analytics.md`

Consumer docs: `docs/firebase-analytics/CLAUDE_AI_SETUP.md`
