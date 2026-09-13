# cmp-library

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.

Template module — copy this to create a new `cmp-*` library. It carries a complete
`build.gradle.kts` including the publishing block, whose coordinates you replace in step 2.

## Install

```kotlin
implementation("io.github.mobilebytelabs:cmp-library:<version>")
```

## Creating a new module

Step-by-step instructions live in [TEMPLATE_README.md](TEMPLATE_README.md): copy the module, update
`build.gradle.kts`, register it in `settings.gradle.kts`, rename the source files, and add a sample.

Two things are easy to miss, and both are checked:

1. **Targets** — declare the full matrix from [TARGET_MATRIX.md](../TARGET_MATRIX.md) (21 headless /
   7 Compose). Fewer needs a documented reason in that file's escape-hatch table.
2. **Docs** — a new module ships its own `README.md` *and* `DEVELOPMENT.md`. Scaffold the latter with
   `.claude-runtime/scripts/development-md-bootstrap.sh --workspace mbs/kmp-toolkit --apply`.

If the module reports through [cmp-observe](../cmp-observe/README.md) (it should), the CI gate
`.github/scripts/assert-observability-wired.sh` fails it for generating `CmpMetadata` without ever
reporting.

## Related

- [DEVELOPMENT.md](DEVELOPMENT.md) — module development state, per-platform parity matrix
- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — target policy for every module
