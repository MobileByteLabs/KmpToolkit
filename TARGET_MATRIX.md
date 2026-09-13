# Target Matrix — single source of truth

Which Kotlin Multiplatform targets every `cmp-*` module ships, why a module ships fewer, and what to
do when a dependency blocks one.

**Upstream reference:** <https://kotlinlang.org/docs/native-target-support.html> — JetBrains' tier list
is authoritative for what Kotlin/Native supports and how well. This document records what *this
toolkit* ships against it. When the two disagree, the upstream page wins and this file is stale.

> Module counts below were measured from `*/build.gradle.kts` on **2026-09-13**. Re-measure rather
> than trust them after any target change — the command is at the bottom.

---

## 1. What Kotlin Multiplatform supports — 25 targets

### Kotlin/Native (20), by JetBrains' support tier

| Tier | Guarantee | Targets |
|---|---|---|
| **1** | Tested on CI every release; compiler releases guarantee they work | `macosX64` `macosArm64` `iosArm64` `iosSimulatorArm64` `iosX64` |
| **2** | Tested on CI, best effort | `watchosArm32` `watchosArm64` `watchosSimulatorArm64` `watchosX64` `tvosArm64` `tvosSimulatorArm64` `tvosX64` `linuxX64` `linuxArm64` |
| **3** | Not guaranteed to be CI-tested; may break between releases | `androidNativeArm32` `androidNativeArm64` `androidNativeX86` `androidNativeX64` `mingwX64` `watchosDeviceArm64` |

### Non-native (5)

`jvm` · `android` · `js` · `wasmJs` · `wasmWasi`

Both Wasm targets are Alpha upstream. `wasmWasi` is the single most common gap in this toolkit —
`koin-core`, `kotlinx-html` and most networking libraries do not publish for it.

### Tier 3 is not theoretical
Two of this toolkit's recurring problems sit in Tier 3. `mingwX64` produced an intermittent
process-killing crash in `cmp-clipboard` (Win32 clipboard contention, fixed 2026-09-12), and
`watchosDeviceArm64` is one of three targets `postgrest-kt` does not publish for. Expect Tier 3 to
need more care per target than Tiers 1–2 combined.

---

## 2. This toolkit's tiers

### Headless — 21 targets
**Every KMP target except the four `androidNative*` ones.** This is the default and the ceiling: no
module in the repo declares more.

`androidNative*` is deliberately excluded, for three independent reasons:
1. All four are Tier 3 — no CI guarantee from JetBrains.
2. They target NDK binaries, not Android apps. An Android app uses the `android` (JVM) target; there
   is no meaningful clipboard, share or review implementation for a native NDK executable.
3. `koin-core`, `ktor` and `postgrest-kt` do not publish for them, so the modules that matter could
   not reach them regardless.

### Compose — 7 targets
`android` `jvm` `js` `wasmJs` `iosArm64` `iosSimulatorArm64` `macosArm64`

This is Compose Multiplatform's own ceiling, not a choice. Note `iosX64` and `macosX64` are **absent**:
Compose 1.12.0 publishes no artifact for either, so a Compose-bearing module cannot declare them even
though they are Tier 1 for Kotlin/Native.

### A Compose-bearing module cannot be a 21-target module
The Compose compiler plugin applies to **every** compilation in a module and fails with
`The Compose Compiler requires the Compose Runtime to be on the class path` on any target lacking the
runtime. Confining Compose to an intermediate source set does **not** work — this was attempted and
reverted during E2 (2026-09-12).

That is why the toolkit ships `X` / `X-compose` pairs: the headless half reaches 21, the Compose half
takes the 7 and depends on the core.

---

## 3. Measured coverage (2026-09-13)

### Headless at full 21 — 13 modules
`cmp-app-intents` `cmp-app-review` `cmp-bubble` `cmp-clipboard` `cmp-deep-link` `cmp-in-app-update`
`cmp-intent-launcher` `cmp-library` `cmp-network-monitor` `cmp-observe` `cmp-open-url`
`cmp-pdf-generator` `cmp-share`

### Below 21 — with the reason

| Module | Targets | Missing | Why |
|---|---:|---|---|
| `cmp-observe-koin` | 20 | `wasmWasi` | koin-core publishes no wasmWasi build |
| `cmp-observe-firebase` | 4 | everything but android + ios ×3 | GitLive Firebase publication set — this module exists so `cmp-observe` itself can stay at 21 (see §4) |
| `cmp-firebase` | 15 | `wasmWasi`, watchOS ×5 | GitLive Firebase publication set |
| `cmp-remote-config` | 15 | `iosX64` `macosX64` `linuxArm64` `wasmWasi` `watchosArm32` `watchosDeviceArm64` | `postgrest-kt` 3.2.6 lacks linuxArm64/watchosArm32/watchosDeviceArm64 · koin-core lacks wasmWasi · iosX64/macosX64 omitted so the `-compose` sibling can follow |
| `cmp-product-tickets` | 15 | same as above | same as above |

### Compose at 7 — 8 modules
`cmp-app-intents-compose` `cmp-firebase-compose` `cmp-intent-launcher-compose`
`cmp-network-monitor-compose` `cmp-product-tickets-compose` `cmp-remote-config-compose`
`cmp-share-compose` `cmp-toast`

---

## 4. When a dependency blocks a target

**Do not drop the target. Confine the dependency.** An intermediate source set keeps the dependency —
and the code that uses it — off the targets that cannot resolve it, while the rest of the module still
ships everywhere. Established examples in this repo:

| Source set | Confines | Because |
|---|---|---|
| `koinMain` / `koinTest` | `koin-core` | no `wasmWasi` artifact |
| `htmlMain` / `htmlTest` | `kotlinx-html` | no `wasmWasi` artifact |
| `webFallbackMain` | shared browser-based impl | JVM/Linux/JS/wasmJs behave identically |
| `appleStoreOnlyMain` | tvOS + watchOS behaviour | neither has `SKStoreReviewController` |
| `darwinDocMain` | iOS + macOS only | `NSData.length` is 32-bit on watchOS, 64-bit elsewhere — one source set cannot span both |

Only drop a target when **no** arrangement of source sets can satisfy it — and then record the reason
in the module's `build.gradle.kts` next to the target list, naming the artifact and version that
blocks it, so the next reader does not re-derive it.

---

## 5. Open candidates

- **`cmp-observe-firebase` 4 → 6** (macOS ×2). Measured 2026-09-13 against the GitLive publication
  set: `firebase-crashlytics` and `firebase-analytics` both publish `macosArm64` + `macosX64`, but
  `firebase-perf` publishes **neither**. So macOS is reachable only by confining
  `FirebasePerformanceHook` to an intermediate source set attached to android + ios, the way
  `cmp-firebase` already does to reach macOS with the same three dependencies. Source-set surgery
  plus a BCV baseline and doc updates — not attempted yet, and an unverified hypothesis until it is.
  `jvm` is **not** a candidate: GitLive publishes no jvm variant for crashlytics or performance
  (analytics only).

### Closed 2026-09-13

- **`cmp-library` 16 → 21** — watchOS ×5 added. The module had a `watchOS Targets` section header
  with nothing under it, so the TEMPLATE shipped 16 and every module copied from it started five
  targets short with nothing saying so. Verified by `:cmp-library:compileKotlinWatchosArm64`; the
  two `expect` declarations resolve from `appleMain`, which the default hierarchy attaches watchOS to.
- **`cmp-observe-koin` 15 → 20** — watchOS ×5 added; all five koin-core watchOS variants are
  published. `wasmWasi` stays blocked by koin-core. Verified by
  `:cmp-observe-koin:compileKotlinWatchosArm64`.

Both were recorded here as *unverified hypotheses* — the absence of a recorded reason is not proof
that a target compiles. Each was tried before being claimed, which is the bar for this section.

---

## 6. Re-measuring

Counts here are a snapshot. To regenerate:

```bash
python3 - <<'PY'
import re, glob, os
ALL = ["jvm","android","js","wasmJs","wasmWasi","macosX64","macosArm64","iosArm64",
 "iosSimulatorArm64","iosX64","watchosArm32","watchosArm64","watchosSimulatorArm64","watchosX64",
 "tvosArm64","tvosSimulatorArm64","tvosX64","linuxX64","linuxArm64","androidNativeArm32",
 "androidNativeArm64","androidNativeX86","androidNativeX64","mingwX64","watchosDeviceArm64"]
def targets(p):
    s=open(p).read(); s=re.sub(r'//.*','',s); s=re.sub(r'/\*.*?\*/','',s,flags=re.S)
    f={t for t in ALL+["androidLibrary","androidTarget"]
       if re.search(rf'^\s*{t}\s*[({{]',s,flags=re.M)}
    if {"androidLibrary","androidTarget"} & f: f-={"androidLibrary","androidTarget"}; f.add("android")
    return f
for p in sorted(glob.glob("cmp-*/build.gradle.kts")):
    t=targets(p)
    if t: print(f"{os.path.dirname(p):32s} {len(t)}")
PY
```

Two traps this matcher exists to avoid, both of which produced wrong numbers before:
- Targets are declared in **two forms** — `jvm()` and `wasmJs { }`. A regex expecting only `(`
  silently undercounts every brace-form target.
- Build directories hold **stale artifacts from older versions**. Counting published files without
  filtering to the current version inflates the result.

Prefer this over counting build output; it reads the declaration, which is the actual source of truth.
