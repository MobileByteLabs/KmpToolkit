# KMP Library Template

This module serves as a **template/reference** for creating new Kotlin Multiplatform (KMP) library modules in the KmpToolkit project.

## Quick Start: Creating a New Library

### Step 1: Copy This Module

```bash
cp -r cmp-library cmp-your-feature
```

### Step 2: Update build.gradle.kts

Edit `cmp-your-feature/build.gradle.kts`:

```kotlin
// 1. Update version
version = "0.1.0"

// 2. Update Android namespace
androidLibrary {
    namespace = "io.github.mobilebytelabs.kmptoolkit.yourfeature"
    // ...
}

// 3. Update Maven coordinates
mavenPublishing {
    coordinates(group.toString(), "kmp-your-feature", version.toString())

    pom {
        name = "KMP Your Feature"
        description = "Description of your feature"
        // ...
    }
}

// 4. Add your dependencies
sourceSets {
    commonMain.dependencies {
        // Your common dependencies
    }
    androidMain.dependencies {
        // Android-specific dependencies
    }
}
```

### Step 3: Add to settings.gradle.kts

```kotlin
include(":cmp-your-feature")
```

### Step 4: Rename Source Files

1. Replace `Greeting.kt` with your main class
2. Update `Platform.*.kt` files for platform-specific code
3. Update tests in `commonTest/` and platform test directories

### Step 5: Declare the full target matrix

Your module must ship **21 targets** (headless) or **7** (Compose) per
[TARGET_MATRIX.md](../TARGET_MATRIX.md). Verify what you actually declared:

```bash
./gradlew :cmp-your-feature:assemble     # a missing actual fails here, not at publish
```

If a dependency does not publish for a target, **do not drop the target** — confine the dependency to
an intermediate source set (`koinMain`, `htmlMain`, and similar; see the matrix §4). Drop a target only
when no source-set arrangement works, and record the blocking artifact and version in a comment next to
the target list.

### Step 6: Write the module README

Every module needs a `README.md`. Start it with the target-support note so the matrix stays discoverable:

```markdown
# cmp-your-feature

> **Target support:** see [TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth for
> which KMP targets every module ships and why.
```

Then cover: install snippet, usage, DI wiring, per-target behaviour table, and anything that will bite
a consumer. `cmp-app-review` is a worked example, including a separate `CONSUMPTION.md` for integration.

### Step 7: Create Sample App (Optional)

```bash
cp -r samples/sample-clipboard samples/sample-your-feature
```

Then update the sample to use your new library.

## Module Structure

```
cmp-library/
├── build.gradle.kts              # Build configuration with Maven publishing
├── TEMPLATE_README.md            # This file
└── src/
    ├── commonMain/kotlin/        # Shared code (expect declarations)
    │   └── Greeting.kt           # Example expect/actual class
    ├── androidMain/kotlin/       # Android implementation
    │   └── Platform.android.kt   # Example actual implementation
    ├── iosMain/kotlin/           # iOS implementation (uses appleMain)
    ├── appleMain/kotlin/         # Shared Apple code (iOS, macOS, tvOS, watchOS)
    │   └── Platform.apple.kt
    ├── jvmMain/kotlin/           # JVM/Desktop implementation
    │   └── Platform.jvm.kt
    ├── jsMain/kotlin/            # JavaScript implementation
    │   └── Platform.js.kt
    ├── wasmJsMain/kotlin/        # WebAssembly JS implementation
    │   └── Platform.wasmJs.kt
    ├── wasmWasiMain/kotlin/      # WebAssembly WASI implementation
    │   └── Platform.wasmWasi.kt
    ├── linuxMain/kotlin/         # Linux Native implementation
    │   └── Platform.linux.kt
    ├── mingwMain/kotlin/         # Windows implementation
    │   └── Platform.mingw.kt
    └── commonTest/kotlin/        # Shared tests
        └── GreetingTest.kt
```

## Supported Platforms

**The standard is 21 targets for a headless module, 7 for a Compose module.** See
[TARGET_MATRIX.md](../TARGET_MATRIX.md) — the single source of truth — for the full list, the
`androidNative*` exclusion, and what to do when a dependency blocks a target. Upstream reference:
<https://kotlinlang.org/docs/native-target-support.html>.

> ⚠️ **This template module currently declares 16 targets, not 21 — it is missing all five watchOS
> targets.** Copying it gives you 16. Add them (`watchosX64` `watchosArm32` `watchosArm64`
> `watchosSimulatorArm64` `watchosDeviceArm64`) unless a dependency blocks them, and record the reason
> in `build.gradle.kts` if it does. An earlier version of this section claimed the template supported
> "all KMP platforms" and listed watchOS in the table below, which was never true of the build file.

| Platform | Source Set | Notes |
|----------|------------|-------|
| Android | `androidMain` | Native Android (JVM target — not `androidNative*`) |
| iOS | `iosMain` / `appleMain` | Arm64, SimulatorArm64, X64 |
| macOS | `macosMain` / `appleMain` | X64, Arm64 |
| tvOS | `tvosMain` / `appleMain` | X64, Arm64, Simulator |
| watchOS | `watchosMain` / `appleMain` | X64, Arm32, Arm64, Simulator, DeviceArm64 |
| JVM | `jvmMain` | Desktop/Server |
| JavaScript | `jsMain` | Browser, Node.js |
| Wasm JS | `wasmJsMain` | Browser, Node.js |
| Wasm WASI | `wasmWasiMain` | Node.js — the most commonly blocked target |
| Linux | `linuxMain` | X64, Arm64 |
| Windows | `mingwMain` | X64 — Tier 3 upstream; expect more per-target care |

## Example: expect/actual Pattern

**commonMain/Greeting.kt:**
```kotlin
class Greeting {
    private val platform = getPlatform()

    fun greet(): String {
        return "Hello, ${platform.name}!"
    }
}

expect fun getPlatform(): Platform

interface Platform {
    val name: String
}
```

**androidMain/Platform.android.kt:**
```kotlin
actual fun getPlatform(): Platform = AndroidPlatform()

class AndroidPlatform : Platform {
    override val name: String = "Android ${android.os.Build.VERSION.SDK_INT}"
}
```

**jvmMain/Platform.jvm.kt:**
```kotlin
actual fun getPlatform(): Platform = JVMPlatform()

class JVMPlatform : Platform {
    override val name: String = "JVM ${System.getProperty("java.version")}"
}
```

## Testing

Tests are written in `commonTest` and can be platform-specific:

```kotlin
// commonTest/kotlin/GreetingTest.kt
class GreetingTest {
    @Test
    fun greetReturnsNonEmptyString() {
        val greeting = Greeting()
        assertTrue(greeting.greet().isNotEmpty())
    }
}
```

Run tests:
```bash
./gradlew :cmp-your-feature:allTests
```

## Publishing

Once your library is ready:

1. **Update version** in build.gradle.kts
2. **Create GitHub Release** with version tag
3. **Workflow auto-publishes** to Maven Central

Users install via:
```kotlin
implementation("io.github.mobilebytelabs:kmp-your-feature:0.1.0")
```

## Existing Libraries (Examples)

| Library | Module | Description |
|---------|--------|-------------|
| Clipboard | `cmp-clipboard` | Cross-platform clipboard |
| Toast | `cmp-toast` | Compose Multiplatform toast |
| In-App Update | `cmp-in-app-update` | Version checking |

## Questions?

See the existing library implementations for patterns:
- `cmp-clipboard/` - Pure KMP library
- `cmp-toast/` - Compose Multiplatform library
- `cmp-in-app-update/` - Complex multi-backend library
