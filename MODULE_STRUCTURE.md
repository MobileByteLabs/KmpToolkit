# Module Structure

This document describes the modular architecture of KMP Toolkit and the pattern for adding new feature modules.

## Overview

KMP Toolkit uses a **modular architecture** where each feature is a standalone library module with independent Maven Central publishing. This allows consumers to import only the features they need.

## Current Modules

```
kmp-toolkit/
├── cmp-clipboard/              # Clipboard utilities
│   ├── build.gradle.kts        # io.github.mobilebytelabs:kmp-clipboard
│   └── src/
│       ├── commonMain/         # ClipboardObserver interface
│       ├── androidMain/        # Android (ClipboardManager + ProcessLifecycleOwner)
│       ├── iosMain/            # iOS (UIPasteboard)
│       ├── macosMain/          # macOS (NSPasteboard)
│       ├── jvmMain/            # JVM (AWT Toolkit + FlavorListener)
│       ├── jsMain/             # JS (navigator.clipboard)
│       ├── linuxMain/          # Linux (xclip/xsel)
│       └── mingwMain/          # Windows (Win32 API)
│
├── cmp-toast/                  # Toast/Snackbar for Compose Multiplatform
│   ├── build.gradle.kts        # io.github.mobilebytelabs:kmp-toast
│   └── src/
│       └── commonMain/         # Pure Compose implementation
│
├── cmp-in-app-update/          # In-App Update checking
│   ├── build.gradle.kts        # io.github.mobilebytelabs:kmp-in-app-update
│   └── src/
│       ├── commonMain/         # AppUpdate, AppUpdateConfig, resolvers
│       ├── androidMain/        # Google Play In-App Updates
│       ├── iosMain/            # iTunes Lookup API
│       ├── macosMain/          # Mac App Store
│       ├── jvmMain/            # Custom version check
│       └── ...                 # Platform implementations
│
├── cmp-library/                # Template module for new libraries
│   ├── build.gradle.kts        # io.github.mobilebytelabs:kmp-template
│   ├── TEMPLATE_README.md      # Instructions for using as template
│   └── src/
│       ├── commonMain/         # Greeting.kt (sample)
│       └── {platform}Main/     # Platform.*.kt (samples)
│
├── samples/
│   ├── sample-clipboard/       # Clipboard + Toast demo
│   └── sample-in-app-update/   # In-App Update demo
│
└── settings.gradle.kts         # Module includes
```

## Maven Coordinates

| Module | Group | Artifact | Version | Description |
|--------|-------|----------|:-------:|-------------|
| cmp-clipboard | `io.github.mobilebytelabs` | `kmp-clipboard` | `0.1.0` | Clipboard utilities |
| cmp-toast | `io.github.mobilebytelabs` | `kmp-toast` | `0.1.0` | Toast/Snackbar UI |
| cmp-in-app-update | `io.github.mobilebytelabs` | `kmp-in-app-update` | `0.5.0` | App update checking |
| cmp-library | `io.github.mobilebytelabs` | `kmp-template` | `1.0.0-template` | Template module |

## Adding a New Feature Module

### Quick Start

1. **Copy the template module:**
   ```bash
   cp -r cmp-library cmp-your-feature
   ```

2. **Follow the instructions in `cmp-library/TEMPLATE_README.md`**

3. **Add to settings.gradle.kts:**
   ```kotlin
   include(":cmp-your-feature")
   ```

4. **Create a sample app:**
   ```bash
   cp -r samples/sample-clipboard samples/sample-your-feature
   ```

### Detailed Steps

#### 1. Update build.gradle.kts

```kotlin
// Key changes for new module:
group = "io.github.mobilebytelabs"
version = "0.1.0"  // Start at 0.1.0

// Update namespace
androidLibrary {
    namespace = "io.github.mobilebytelabs.kmptoolkit.yourfeature"
}

// Update Maven coordinates
mavenPublishing {
    coordinates(group.toString(), "kmp-your-feature", version.toString())

    pom {
        name = "KMP Your Feature"
        description = "Cross-platform your-feature utilities for Kotlin Multiplatform"
    }
}

// Add dependencies as needed
sourceSets {
    commonMain.dependencies {
        implementation(libs.kotlinx.coroutines.core)
    }
}
```

#### 2. Implement expect/actual Pattern

**commonMain** - Declare expected functions:
```kotlin
package com.mobilebytelabs.kmptoolkit.yourfeature

expect object YourFeature {
    fun doSomething(): Result
}
```

**Platform source sets** - Provide actual implementations:
```kotlin
// YourFeature.android.kt
actual object YourFeature {
    actual fun doSomething(): Result {
        // Android-specific implementation
    }
}
```

#### 3. Add Tests

**commonTest** - Platform-agnostic tests:
```kotlin
class YourFeatureTest {
    @Test
    fun function_doesNotThrow() {
        val result = YourFeature.doSomething()
        assertNotNull(result)
    }
}
```

## Platform Support Matrix

| Platform | Targets | Notes |
|----------|---------|-------|
| Android | android | Uses androidLibrary plugin |
| iOS | iosX64, iosArm64, iosSimulatorArm64 | |
| macOS | macosX64, macosArm64 | |
| tvOS | tvosX64, tvosArm64, tvosSimulatorArm64 | Limited support |
| watchOS | watchosX64, watchosArm32, watchosArm64, watchosSimulatorArm64, watchosDeviceArm64 | Limited support |
| JVM | jvm | |
| JavaScript | js (browser, nodejs) | |
| WebAssembly | wasmJs (browser, nodejs), wasmWasi (nodejs) | |
| Linux | linuxX64, linuxArm64 | |
| Windows | mingwX64 | |

## Publishing

### Local Testing

```bash
./gradlew :cmp-your-feature:publishToMavenLocal
```

### Maven Central (via GitHub Actions)

The publish workflow automatically discovers all `cmp-*` modules with the `mavenPublishing` plugin:

1. Create a GitHub Release
2. Workflow auto-discovers and publishes all modules in parallel

### Manual Publishing

```bash
./gradlew :cmp-your-feature:publishAllPublicationsToMavenCentralRepository
```

## Module Types

### Pure Kotlin Modules
- `cmp-clipboard` - Uses expect/actual for platform APIs
- `cmp-in-app-update` - Complex multi-platform with resolvers

### Compose Multiplatform Modules
- `cmp-toast` - Pure Compose, no platform-specific code needed

### Template Modules
- `cmp-library` - Reference for creating new modules

## Design Principles

1. **Zero Configuration** - Features work out-of-the-box without setup
2. **Modular** - Import only what you need
3. **Consistent API** - Same function signatures across all modules
4. **Platform Graceful Degradation** - No-op implementations where features aren't supported
5. **Independent Versioning** - Each module has its own version lifecycle
6. **Automatic Publishing** - New modules auto-discovered by CI/CD
