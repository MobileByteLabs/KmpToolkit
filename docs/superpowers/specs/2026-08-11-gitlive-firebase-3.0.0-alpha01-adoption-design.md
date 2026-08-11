# Design: Adopt GitLive firebase-kotlin-sdk `3.0.0-alpha01` in `cmp-firebase`

- **Date:** 2026-08-11
- **Repo:** `MobileByteLabs/KmpToolkit`
- **Branch / PR:** `session-kmp-toolkit-20260810131536717` → PR #155 (folded in)
- **Scope:** Toolkit only. Consumer-app iOS migration is out of scope (handled per-app later).
- **Status:** Approved (design) — pending spec review.

---

## 1. Background & motivation

`cmp-firebase` currently depends on GitLive `firebase-kotlin-sdk` `2.5.0` (a `2.4.0 → 2.5.0`
bump is in-flight and uncommitted on this branch). We are adopting **`3.0.0-alpha01`**.

The decisive fact that framed this design: **`3.0.0-alpha01` is functionally identical to
stable `2.6.0` plus exactly one change** — the Apple Firebase native dependency moved from
**CocoaPods → SwiftPM** ([PR #836](https://github.com/GitLiveApp/firebase-kotlin-sdk/pull/836),
published 2026-08-05). There are **no new APIs and no new/dropped KMP targets** versus 2.6.0.

We are adopting the alpha deliberately because **we are migrating our consumer apps to SwiftPM
anyway** — the alpha's SwiftPM iOS-linking direction aligns with where the apps are going, so
taking it now avoids a later re-migration.

### Why it forces a Kotlin bump
`3.0.0-alpha01` requires **Kotlin `2.4.0`**, because the SDK now uses the native KGP 2.4.0
feature *"Swift packages as dependencies"* (`swiftPMDependencies { swiftPackage(...) }`). That
is the whole reason GitLive cut a major/alpha. Our consumer apps are already on Kotlin 2.4.0;
the toolkit is on 2.3.10 and must move up (a 2.3.10 consumer also cannot consume a 2.4.0-built
klib — the metadata bump makes the Kotlin upgrade mandatory, not optional).

### Target matrix — unchanged (no source-set rework)
| Product | GitLive-backed targets (2.6.0 = alpha01) | Toolkit tier |
|---|---|---|
| analytics | `ios, macos, tvos, jvm, js, android` (11 KMP targets) | `firebaseMain` (rest → `nonFirebaseMain` Measurement-Protocol HTTP) |
| crashlytics | `ios, macos, android` (6 KMP targets; SDK's `jvm()` is commented out) | `crashlyticsFirebaseMain` (rest → `crashlyticsFallbackMain` Kermit `CrashReport`) |

The existing **6/13 crashlytics** and **11/10 analytics** source-set splits stay exactly valid.

---

## 2. Versions pinned by `3.0.0-alpha01` (from the SDK catalog at the tag)

| Dependency | alpha01 requires | Toolkit today | Action |
|---|---|---|---|
| Kotlin | `2.4.0` | `2.3.10` | **bump** (mandatory) |
| GitLive firebase | `3.0.0-alpha01` | `2.5.0` | **bump** |
| Firebase Android BOM | `34.17.0` | `33.7.0` | **bump** |
| kotlinx-coroutines | `1.10.2` | `1.10.2` | ✅ none |
| kotlinx-serialization | `1.9.0` | `1.8.1` | **bump** (safe) |
| kotlinx-datetime | `0.7.1` | `0.8.0` | ✅ none (ahead) |
| binary-compatibility-validator | `0.18.1` | `0.17.0` | **bump** (2.4.0 metadata) |
| firebase-ios-sdk (via SwiftPM) | `12.17.0` | (deferred to app) | contingent (see §4) |
| iOS / macOS / tvOS min deploy | `15.0` / `10.15` / `15.0` | n/a | documented for consumers |
| Compose Multiplatform | — (not an SDK dep) | `1.10.3` | **contingent bump** — CMP 1.10.3 predates Kotlin 2.4.0; if the compose-compiler rejects 2.4.0, bump to `1.11.x` |

> AGP is decoupled: the toolkit's AGP `9.2.1` is *ahead* of the SDK's build-time AGP `8.12.0`
> and is not a constraint on consuming the published artifacts.

---

## 3. The one real breaking change — Apple linking (consumer-facing only)

Not a toolkit blocker, but it must be **documented** in the module README/DEVELOPMENT because
it changes how every consuming app links Firebase on Apple targets:

- **Old (2.x):** app declares `pod("FirebaseAnalytics")` / `pod("FirebaseCrashlytics")` via CocoaPods.
- **New (3.x):** firebase-ios-sdk flows across the Maven boundary automatically — the app does
  **not** re-declare it — **but** each consuming app must:
  1. Build its shared framework **static** — `isStatic = true` on `binaries.framework`
     (Firebase's SwiftPM products are static libraries; a dynamic framework → **runtime crash**).
  2. Switch Xcode to **direct integration** — add the `embedAndSignAppleFrameworkForXcode`
     run-script build phase (drop `pod install`). Gradle then resolves the inherited
     firebase-ios-sdk, generates the synthetic Swift package, and embeds/signs on each build.
  3. Raise the iOS deployment target to **15.0** (macOS 10.15, tvOS 15.0).

The toolkit's own `sample-toolkit:composeApp` already sets `isStatic = true`, so the toolkit
side is already aligned on that axis.

---

## 4. Toolkit build topology & the SwiftPM question

`cmp-firebase/build.gradle.kts` declares Apple targets **plainly** (`iosArm64()`, `macosX64()`,
…) and re-exports GitLive with `api(libs.gitlive.firebase.analytics)` /
`api(libs.gitlive.firebase.crashlytics)`. There is **no** `native.cocoapods`, no `cocoapods {}`,
and no `swiftPMDependencies {}` block anywhere in the repo — native Firebase linking has always
been deferred to the final app. The repo is a **pure library**: no `.xcodeproj`, `Podfile`, or
`Package.swift` exists in it.

**Central technical unknown (the alpha's documented gap):** does a *re-exporting library* like
`cmp-firebase` need its own `swiftPMDependencies` plumbing, or does the SwiftPM metadata flow
transitively via Maven so only the final app needs anything? GitLive's iOS-linking doc says the
dependency "flows across the Maven boundary automatically" but explicitly does **not** address
the re-export scenario.

**This design resolves it empirically, not by assumption** (see §5, P3):
- **Expected path:** no structural change to `cmp-firebase/build.gradle.kts`; the `api(...)`
  re-export is enough and metadata flows transitively.
- **Contingency:** if the Apple framework link fails on unresolved Firebase symbols, add a
  `swiftPMDependencies { swiftPackage(url = firebase-ios-sdk, version = 12.17.0, products =
  [...]) }` block to `cmp-firebase` mirroring GitLive's, with the iOS/macOS/tvOS min-deploy
  entries added to the catalog.

### Consequent no-runtime-proof constraint
Because there is no in-repo iOS app, the toolkit-only validation ceiling is **compile +
framework-link**. `linkDebugFrameworkIosSimulatorArm64` is what exercises SwiftPM resolution
of firebase-ios-sdk and is the meaningful go/no-go here. **Runtime Firebase-init over SwiftPM is
validated later, in the consumer-app migration**, where a real Xcode app exists. This is an
accepted, explicit limitation of the toolkit-only scope (validation depth **Option A**).

---

## 5. Change surface

| File | Change |
|---|---|
| `gradle/libs.versions.toml` | `kotlin 2.3.10→2.4.0`, `gitliveFirebase 2.5.0→3.0.0-alpha01`, `firebaseBom 33.7.0→34.17.0`, `kotlinx-serialization 1.8.1→1.9.0`, `kotlinx-binary-compatibility-validator 0.17.0→0.18.1`; **contingent** `compose-multiplatform 1.10.3→1.11.x` |
| `cmp-firebase/build.gradle.kts` | Expected: **no structural change** (comments `2.5.x → 3.0.0`). Contingency: add `swiftPMDependencies {}` (§4). |
| `cmp-firebase/README.md`, `cmp-firebase/DEVELOPMENT.md` | Rewrite iOS setup → SwiftPM (static framework, `embedAndSignAppleFrameworkForXcode`, iOS 15). **Remove CocoaPods pod instructions.** |
| `**/api/*.api` (BCV dumps) | Regenerate via `apiDump` if Kotlin 2.4.0 metadata shifts signatures. |
| Other Kotlin-2.4.0-affected modules | Only if the global bump surfaces deprecations/warnings-as-errors (fix at source). |

---

## 6. Validation plan (build-driven; every phase gated by real exit codes)

Discipline: RULE-VERIFY-COMPLETION-001 (no "green" without exit codes) and
RULE-BUILD-WARMTH-001 (**never kill the Gradle/Kotlin daemon**; one cold reconfigure after the
Kotlin bump is expected).

- **P0 — de-risk CMP.** Determine whether Compose MP `1.10.3` accepts Kotlin `2.4.0`
  (compose-compiler check). Decide the `1.11.x` bump before touching anything else.
- **P1 — apply catalog bumps** (§5).
- **P2 — non-Apple tiers (fast, zero SwiftPM):** `:cmp-firebase:compileKotlinJvm` + `jvmTest`,
  Android, `js`, `wasmJs`, `linuxX64`, `mingwX64`, `compileKotlinMetadata`. Proves Kotlin 2.4.0 +
  alpha01 klib consumption on the majority of targets.
- **P3 — Apple / the real SwiftPM test (go/no-go):**
  `:cmp-firebase:compileKotlinIosSimulatorArm64` →
  `:samples:sample-toolkit:composeApp:linkDebugFrameworkIosSimulatorArm64`.
  On unresolved-symbol failure → apply the §4 contingency, re-run.
- **P4 — repo green:** build the other Kotlin-2.4.0-affected modules (targeted), then a full
  `./gradlew build`, so the global bump doesn't regress the rest of the toolkit.
- **P5 — docs:** SwiftPM consumer setup in README/DEVELOPMENT; strip CocoaPods.

---

## 7. Risk register

| # | Risk | Mitigation |
|---|---|---|
| R1 | Alpha instability in a *published* library | Pin exact `3.0.0-alpha01`; gate behind unmerged PR #155; one-line catalog rollback |
| R2 | CMP 1.10.3 ✗ Kotlin 2.4.0 | P0 decides; contingency bump to `1.11.x` |
| R3 | SwiftPM metadata may not flow transitively for a re-export | P3 is the empirical decider; §4 contingency adds `swiftPMDependencies` to `cmp-firebase` |
| R4 | iOS 15 min raises consumer floor | Documented; not a toolkit blocker |
| R5 | Kotlin bump invalidates config/build cache once | Expected single cold reconfigure — **do not kill the daemon** |
| R6 | BCV `.api` dumps shift under 2.4.0 | Regenerate with `apiDump` |
| R7 | Runtime Firebase-init cannot be proven in-repo | Explicit scope limit; validated in consumer migration (§4) |

---

## 8. Rollback

Revert the version-catalog lines (`kotlin`, `gitliveFirebase`, `firebaseBom`, `compose-multiplatform`,
serialization, BCV). Module source is untouched **unless** the §4 `swiftPMDependencies`
contingency was applied — in which case also revert that block. Trivial on the PR branch.

---

## 9. Out of scope

- Consumer-app iOS migration (static framework + direct integration + iOS 15) — per-app, later.
- Runtime Firebase-init verification over SwiftPM — belongs to the consumer migration.
- Any analytics/crashlytics API or source-set redesign — the matrix is unchanged.
