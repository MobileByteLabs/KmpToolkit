<!--
  Cookbook recipe template — copy + replace placeholders.
  Discipline:
    - ≤ 80 lines total (AC12 — `wc -l` enforced in CI)
    - ≥ 1 ```kotlin``` code block (AC13 — `grep` enforced in CI)
    - `reviewed_by.date` + `version` enable yearly recipe-freshness audit
-->
---
title: "How do I {task}?"
reviewed_by:
  date: 2026-06   # YYYY-MM — bump when content reviewed against current API
  version: 3.5.x  # last verified kmp-toolkit version
---

# How do I {task}?

## Quick start (minimal MWE)

```kotlin
// ≤ 15 lines of copy-paste runnable code.
// Show the smallest path from import → call site → expected outcome.
```

## Caveats / per-platform notes

- **Platform X:** {gotcha or special handling}
- **Platform Y:** {falls back to / requires / etc.}

## Related

- Module: [cmp-{name}](../../modules/cmp-{name}.md)
- Sample: [`samples/sample-cmp-{name}/composeApp/.../{file}.kt`](https://github.com/MobileByteLabs/KmpToolkit/tree/development/samples/sample-cmp-{name})
- ADR: {link, if applicable}
