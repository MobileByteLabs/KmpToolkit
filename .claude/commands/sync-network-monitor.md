# /sync-network-monitor

Alias for `/lib-sync cmp-network-monitor`.

Syncs `cmp-network-monitor` into the active consuming app — verify-gated Gradle check only
(zero-config module: no Supabase, no DI, no nav wiring needed).

## Usage

```
/sync-network-monitor           # Full sync
/sync-network-monitor --check   # Dry run, no writes
```

Full instructions (single source of truth):
  `.claude-runtime/commands/sync-network-monitor.md`

Consumer docs: `docs/network-monitor/CLAUDE_AI_SETUP.md`
