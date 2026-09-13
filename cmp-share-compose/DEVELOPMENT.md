---
module: cmp-share-compose
artifact: io.github.mobilebytelabs:cmp-share-compose
version: UNKNOWN
package: com.mobilebytelabs.kmptoolkit.share.compose
api_tier: experimental
last_reviewed: 2026-05-30
goal_plan_ref: plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md
adr_refs: []
---

# cmp-share-compose — Development

> Single source of truth for development state of `cmp-share-compose` (KMP library module). Per RULE-LIB-DEVELOPMENT-MD-001.
> Bootstrap: `.claude-runtime/scripts/development-md-bootstrap.sh`. Refresh auto-gen sections: `development-md-scan.sh`.

---

## §1 Module Identity (auto-gen)

| Artifact | Package | Current version | Maven | Since | API tier |
|----------|---------|-----------------|-------|-------|----------|
| `io.github.mobilebytelabs:cmp-share-compose` | `com.mobilebytelabs.kmptoolkit.share.compose` | `UNKNOWN` | [Central](https://central.sonatype.com/artifact/io.github.mobilebytelabs/cmp-share-compose) | 2026-05-30 | experimental |

**Module purpose (one paragraph):** <!-- AUTHOR: WIP — initial draft from 2026-05-30. One-paragraph module purpose (≤200 words). Seed from idea-layer/cmp-share-compose/SPEC.md if present. -->

---

## §2 Per-Platform Parity Matrix (auto-gen)

| Target | Source-set present | Real impl | UnsupportedPlatform stub | .kt count | Last reviewed | Coverage | Notes |
|--------|:------------------:|:---------:|:------------------------:|:---------:|---------------|----------|-------|
| androidMain | ✅ | ✅ real | 0 | 1 | 2026-09-13 | (legacy:full) | — |

Legend (Real impl): ✅ real impl, 🟡 partial / wontfix-OS / wontfix-infra / legacy stub, ⛔ not declared, — N/A.
Legend (Coverage enum, since 2026-06-01): `full` (all public-API methods backed by OS primitive) · `partial` (most real; some typed UnsupportedPlatform fallbacks for contracts that don't apply) · `wontfix-OS` (OS lacks the primitive) · `wontfix-infra` (impl possible but CI/toolchain blocks it) · `(legacy:full|stub)` (auto-derived; pre-opt-in modules — add a `// LD-2-coverage: {enum}` comment to the platform's primary `.kt` file to graduate). See `RULE-LIB-DEVELOPMENT-MD-001` LD-2 + ADRs for accepted wontfix cases.

---

## §3 Public API Surface (auto-gen from api/*.api)

<!-- No api/*.api BCV baseline yet — scanned commonMain public declarations: -->
```kotlin
public fun ShareSheet(
public fun ShareButton(
public fun rememberShareLauncher(): Share = remember { Share }
```

---

## §4 Spec Snapshot (authored — LLM-seeded)

**Problem this module solves:** `cmp-share` is headless and ships to 15 targets, six of which
(tvOS, Linux ×2, Windows, and the two extra Apple architectures) have no Compose at all. Anything
typed in `androidx.compose.*` therefore cannot live there. This module is where the Compose-shaped
half goes: the `CompositionLocal`, the `remember*` accessors, the drop-in `ShareSheet` /
`ShareButton`, and `ImageBitmap` encoding.

**Core invariants:**
- **Compose types never leak downward.** If a declaration mentions `androidx.compose.*` it belongs
  here, not in `cmp-share`. This is what keeps the headless artifact's 15-target matrix possible.
- **Reading `LocalShareManager` without a provider must not throw.** It falls back to a real
  `ShareManagerImpl`. This is a deliberate divergence from `LocalNetworkMonitor`, which *does*
  throw — a network monitor needs configuration and a lifecycle, whereas sharing is stateless and
  zero-config on every target, so failing the common case would buy nothing.
- **Composition scenarios are asserted by rendering.** Claims about defaults, overrides and nested
  providers are unobservable from a direct function call; they run through `runComposeUiTest` on
  both JVM and the Android host.
- **The 7-target Compose matrix is a fact of Compose Multiplatform, not a choice.** CMP has no
  tvOS/Linux/mingw target. Do not "add" them.

**Out of scope (by design):**
- Any payload or platform logic — that is `cmp-share`'s `Share` engine. This module composes, it
  does not implement sharing.
- A Koin module. DI binding lives in `cmp-share` (`shareModule`) so non-Compose consumers get it
  too; here you `koinInject()` the `ShareManager` and hand it to `ProvideShareManager`.

---

## §5 Extension Recipes (authored — LLM-seeded)

### Recipe: Add a new platform actual

Almost nothing here needs one — Compose is the abstraction. The single existing `expect` is
`encodeImageAsPng`, and it splits exactly one way:

1. Prefer the `nonAndroidMain` source set. Android has its own bitmap codec; JVM, iOS, macOS, JS and
   wasmJs all render through Skia, so one `actual` covers five targets. `nonAndroidMain` is created
   explicitly in `build.gradle.kts` (`create("nonAndroidMain")`, not `by creating`, which is a
   Gradle-10 deprecation this repo has cleared) and every non-Android source set `dependsOn` it.
2. Only add a per-target `actual` when a target genuinely diverges from Skia. If you find yourself
   copying the same Skia call into five files, the source set wiring is what needs fixing.
3. Wrap platform codec calls in `runCatching { }.getOrNull()` and return `null` on failure. The
   caller turns that into a typed `ShareResult.Failed`, so a chooser is never raised for content
   that cannot be encoded.

### Recipe: Extend the public API

1. Decide the module first: does the declaration mention `androidx.compose.*`? If not, it belongs in
   `cmp-share`, where it reaches all 15 targets instead of 7.
2. Prefer an extension on `ShareManager` over a new top-level function, so it composes with whatever
   instance is in scope (real, injected, or faked) rather than reaching for the `Share` object.
3. Add scenarios to `ShareManagerCompositionScenarios` in `commonTest` — the abstract base, never a
   subclass. Both the JVM and Android-host subclasses inherit them automatically, which is the point
   of the split.
4. Refresh BCV: `./gradlew :cmp-share-compose:apiDump`.

### Recipe: Add a new variant under an existing platform

1. Check Compose Multiplatform actually supports the target before anything else. CMP ships no
   tvOS, Linux or mingw target — the headless `cmp-share` covers those consumers.
2. Declare it in `build.gradle.kts`, then add it to the `nonAndroidMain` wiring list if it is not
   Android. Forgetting that second step is silent: the source set is simply never created, and the
   `actual` you wrote compiles nowhere. That is exactly how nine modules in this repo ended up with
   an orphan `src/watchosMain/`.
3. Confirm with `./gradlew :cmp-share-compose:assemble` and check the target's compile task actually
   appears in the output.

---

## §6 Active Development Log (auto-gen)

| Date | Author | PR | Summary | State |
|------|--------|----|---------|-------|
| (no open PRs labeled `cmp-share-compose` — refresh via `gh pr list --label cmp-share-compose` then re-run scan) | — | — | — | — |

---

## §7 Cross-Platform Parity Recipes (authored — LLM-seeded)

### Pattern: One scenario suite, one runner per target

**When to use:** any behaviour that must be asserted inside a real composition on more than one
target.

The problem it solves: `androidx.compose.ui.test`'s Android environment reads
`android.os.Build.FINGERPRINT` to choose an idling strategy. Under the plain android.jar stub that
static field is null, so every composition dies before rendering — and the field is `final` and not
reflectively writable. The only real fix is to run the Android host variant under Robolectric, which
needs a JUnit4 `@RunWith` that a `commonTest` class cannot carry.

**Code shape** — write the scenarios once, let each target bring its runner:

```
commonTest/       abstract ShareManagerCompositionScenarios   ← every @Test lives here
jvmTest/          …JvmTest : …Scenarios()                     ← Skiko, via compose.desktop.currentOs
androidHostTest/  @RunWith(RobolectricTestRunner::class)
                  @Config(sdk = [ROBOLECTRIC_SDK])
                  …AndroidTest : …Scenarios()
```

**The build wiring this needs** (all four, or it fails in a way that misdirects):

```kotlin
withHostTestBuilder {}.configure {
    isReturnDefaultValues = true    // android.jar stubs THROW by default
    isIncludeAndroidResources = true // else: "Unable to resolve activity for Intent { MAIN }"
}
```
plus `compose.uiTest` in `commonTest`, `compose.desktop.currentOs` in `jvmTest`, and
`robolectric` + `junit` + `ui-test-manifest` in `androidHostTest`.

`ROBOLECTRIC_SDK` is code-generated from `robolectricSdk` in the version catalog — apply
`robolectric-host-test.gradle.kts` **and** wire its output:

```kotlin
kotlin.sourceSets.getByName("androidHostTest").kotlin.srcDir(
    tasks.named("generateRobolectricSdkConstant"),
)
```

Skipping that last line fails as `Unresolved reference 'robolectric'`, which reads like a missing
dependency rather than missing codegen.

---

## §8 Related

- [TARGET_MATRIX.md](../TARGET_MATRIX.md) — **single source of truth** for which KMP targets
  this module must ship (21 headless / 7 Compose) and how to handle a dependency that blocks one.
  Upstream reference: <https://kotlinlang.org/docs/native-target-support.html>.

| Type | Reference |
|------|-----------|
| GOAL.md | [consumer-library-ai-bridge](../../../../../../plan-layer/project-plans/mbs/kmp-toolkit/active/consumer-library-ai-bridge/GOAL.md) |
| ADRs | _List relevant ADR-NN entries (e.g. ADR-09 for inter-app-comms modules)._ |
| Sync rule | [RULE-LIB-DEVELOPMENT-MD-001](../../../../../../layers/framework/rules/RULE-LIB-DEVELOPMENT-MD-001.md) |
| External docs | [README](README.md) |

---

## §9 Observability Surface (authored — LLM-seeded)

Per RULE-LIB-OBSERVABILITY-SURFACE-001 (LD-9a..LD-9d).

**This module reports nothing, by design.** Every composable here delegates to
[`cmp-share`](../cmp-share/DEVELOPMENT.md), which reports the operation. Reporting again in the
Compose wrapper would emit two events for one user action and double every count a
consumer's hook sees.

| Signal Tier | Status | Details |
|-------------|--------|---------|
| T0–T4 | delegated | see [`cmp-share`](../cmp-share/DEVELOPMENT.md) §9 |

```yaml
tiers:
  T0: delegated
  T1: delegated
  T2: delegated
  T3: delegated
  T4: delegated
delegates_to: cmp-share
```
