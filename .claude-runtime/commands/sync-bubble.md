# /sync-bubble - Full Instructions

> **Single source of truth** for `cmp-bubble` sync contract.
> The framework `/lib-sync cmp-bubble` delegates to this file.
> Update this file when the library evolves (new version, API changes).

---

# /sync-bubble — cmp-bubble Sync

Verify-gated sync of `cmp-bubble` into a consuming KMP app.
Gate 1: Gradle dependency. Gate 2: N/A (no Supabase). Gate 3: createBubble() wiring.

---

## Module Contract (update when library changes)

```yaml
module:    cmp-bubble
artifact:  io.github.mobilebytelabs:kmp-bubble
version:   2.1.0
package:   com.mobilebytelabs.kmptoolkit.bubble
supabase:  false
di:        false
nav:       false

config:
  class:  com.mobilebytelabs.kmptoolkit.bubble.BubbleConfig
  note:   optional — BubbleConfig.Default used if not provided

api:
  - createBubble(config: BubbleConfig = BubbleConfig.Default): Bubble
  - bubble.show(title, message, icon?, actions, style, onTap, autoDismissMs)
  - bubble.showScreen(title, route, screenConfig, icon?, style)
  - bubble.showPersistent(title, message, actions, style)
  - bubble.update(title?, message?, actions?)
  - bubble.dismiss()
  - bubble.capability: BubbleCapability
  - bubble.state: StateFlow<BubbleState>
```

---

## Usage

```bash
/sync-bubble                  # Full sync — all gates
/sync-bubble --check          # Dry run — show status, no writes
/sync-bubble --wiring-only    # Gate 3 only (wiring check)
```

---

## Workflow

```
/sync-bubble
        │
        ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 1: Gradle Dependency                                   │
│  Check: kmp-bubble:2.1.0 in libs.versions.toml              │
│  Check: used in commonMain.dependencies                      │
│  Fix:   Auto-insert correct entries                          │
│  Result: ✅ PASS / ⚡ FIXED / ❌ BLOCKED                     │
└─────────────────────────┬────────────────────────────────────┘
                          │ PASS
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 2: Supabase Schema — SKIPPED (no backend)              │
└─────────────────────────┬────────────────────────────────────┘
                          │ SKIP
                          ▼
┌──────────────────────────────────────────────────────────────┐
│  GATE 3: Wiring                                              │
│  3a. createBubble() call present in project?                 │
│  Fix: Auto-insert example if missing                         │
│  Result: ✅ / ⚡ per sub-gate                                │
└─────────────────────────┬────────────────────────────────────┘
                          │ ALL PASS
                          ▼
              ✅ SYNC COMPLETE — print state summary
```

---

## Gate 1: Gradle

### Check
```
1. Glob: gradle/libs.versions.toml
   → search "kmp-bubble"
   → if found: verify version = 2.1.0
   → if missing or wrong: mark for fix

2. Glob: **/build.gradle.kts (KMP shared module)
   → search "kmp.bubble"
   → if missing: mark for fix
```

### Fix
```toml
# libs.versions.toml [versions]
kmp-bubble = "2.1.0"

# libs.versions.toml [libraries]
kmp-bubble = { module = "io.github.mobilebytelabs:kmp-bubble", version.ref = "kmp-bubble" }
```
```kotlin
// build.gradle.kts commonMain.dependencies
implementation(libs.kmp.bubble)
```

---

## Gate 2: Supabase Schema

**SKIPPED** — `cmp-bubble` has no Supabase backend. No schema checks needed.

---

## Gate 3: Wiring

### 3a — createBubble() usage

```
Grep: "createBubble" in **/*.kt
If found: ✅ (warn if outdated BubbleConfig usage)
If missing:
  → locate ViewModel or App init file
  → insert:
      import com.mobilebytelabs.kmptoolkit.bubble.createBubble
      import com.mobilebytelabs.kmptoolkit.bubble.BubbleConfig

      val bubble = createBubble()   // or createBubble(BubbleConfig.Default)
```

---

## --check (Dry Run)

Show what WOULD change — no writes, no modifications.

```
GATE 1  Gradle   ✅  kmp-bubble:2.1.0
GATE 2  Supabase N/A
GATE 3  Wiring   [WOULD ADD] createBubble() example
```

---

## State Summary Output

```
╔══════════════════════════════════════════════════════════════════╗
║  /sync-bubble — COMPLETE                                         ║
╠══════════════════════════════════════════════════════════════════╣
║  GATE 1  Gradle     ✅  kmp-bubble:2.1.0                        ║
║  GATE 2  Supabase   N/A  no backend                             ║
║  GATE 3  Wiring     ⚡  Added createBubble() example            ║
╠══════════════════════════════════════════════════════════════════╣
║  Docs: docs/bubble/SETUP.md                                     ║
╚══════════════════════════════════════════════════════════════════╝
```

---

## How to Evolve This File

When the library releases a new version:

1. **Version bump** → update `version: 2.1.0` above
2. **New API method** → update `api:` section
3. **Config class changed** → update `config:` section

The framework's `/lib-sync` will automatically pick up the changes because it reads this file
as the source of truth.
