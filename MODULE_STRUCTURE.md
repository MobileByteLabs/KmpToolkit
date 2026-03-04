# Module Structure

This document describes the modular architecture of KMP Toolkit and the pattern for adding new feature modules.

## Overview

KMP Toolkit uses a **modular architecture** where each feature is a standalone library module with independent Maven Central publishing. This allows consumers to import only the features they need.

## Current Modules

```
kmp-toolkit/
├── cmp-clipboard/          # Standalone clipboard utilities
│   ├── build.gradle.kts    # io.github.mobilebytelabs:kmp-clipboard
│   └── src/
│       ├── commonMain/     # expect declarations
│       ├── commonTest/     # Common tests
│       ├── androidMain/    # Android actual (ClipboardManager)
│       ├── iosMain/        # iOS actual (UIPasteboard)
│       ├── macosMain/      # macOS actual (NSPasteboard)
│       ├── tvosMain/       # tvOS actual (no-op)
│       ├── watchosMain/    # watchOS actual (no-op)
│       ├── jvmMain/        # JVM actual (AWT Toolkit)
│       ├── jsMain/         # JS actual (navigator.clipboard)
│       ├── wasmJsMain/     # Wasm JS actual (no-op)
│       ├── wasmWasiMain/   # Wasm WASI actual (no-op)
│       ├── linuxMain/      # Linux actual (xclip/xsel)
│       └── mingwMain/      # Windows actual (Win32 API)
│
├── cmp-library/            # App Update utilities
│   ├── build.gradle.kts    # io.github.mobilebytelabs:kmp-toolkit
│   └── src/
│       └── ...             # App update implementation
│
├── sample-app/             # Demo application
│   ├── build.gradle.kts    # Depends on both modules
│   └── src/
│
└── settings.gradle.kts     # Module includes
```

## Maven Coordinates

| Module | Group | Artifact | Current Version |
|--------|-------|----------|:---------------:|
| cmp-clipboard | `io.github.mobilebytelabs` | `kmp-clipboard` | `0.1.0` |
| cmp-library | `io.github.mobilebytelabs` | `kmp-toolkit` | `0.5.0` |

## Adding a New Feature Module

Follow this pattern to add new feature modules:

### 1. Create Directory Structure

```bash
mkdir -p cmp-{feature}/src/{commonMain,commonTest,androidMain,iosMain,macosMain,tvosMain,watchosMain,jvmMain,jsMain,wasmJsMain,wasmWasiMain,linuxMain,mingwMain}/kotlin/com/mobilebytelabs/kmptoolkit/{feature}
```

### 2. Create build.gradle.kts

Copy from `cmp-clipboard/build.gradle.kts` and update:

```kotlin
// Key changes for new module:
group = "io.github.mobilebytelabs"
version = "0.1.0"  // Start at 0.1.0

// Update namespace
androidLibrary {
    namespace = "io.github.mobilebytelabs.kmptoolkit.{feature}"
}

// Update Maven coordinates
mavenPublishing {
    coordinates(group.toString(), "kmp-{feature}", version.toString())

    pom {
        name = "KMP {Feature}"
        description = "Cross-platform {feature} utilities for Kotlin Multiplatform"
    }
}
```

### 3. Update settings.gradle.kts

```kotlin
include(":cmp-{feature}")
```

### 4. Implement expect/actual Pattern

**commonMain** - Declare expected functions:
```kotlin
// Clipboard.kt
package com.mobilebytelabs.kmptoolkit.{feature}

expect fun someFunction(): Result
```

**Platform source sets** - Provide actual implementations:
```kotlin
// Clipboard.android.kt
package com.mobilebytelabs.kmptoolkit.{feature}

actual fun someFunction(): Result {
    // Android-specific implementation
}
```

### 5. Add Tests

**commonTest** - Platform-agnostic tests:
```kotlin
class FeatureTest {
    @Test
    fun function_doesNotThrow() {
        val result = someFunction()
        assertNotNull(result)
    }
}
```

### 6. Update sample-app

```kotlin
// sample-app/build.gradle.kts
commonMain.dependencies {
    implementation(project(":cmp-{feature}"))
}
```

### 7. Android Manifest (if needed)

For features requiring auto-initialization:

```xml
<!-- cmp-{feature}/src/androidMain/AndroidManifest.xml -->
<manifest>
    <application>
        <provider
            android:name="com.mobilebytelabs.kmptoolkit.{feature}.{Feature}InitProvider"
            android:authorities="${applicationId}.kmptoolkit.{feature}.init"
            android:exported="false"
            android:initOrder="100" />
    </application>
</manifest>
```

## Platform Support Matrix

All modules support the same platform targets:

| Platform | Targets |
|----------|---------|
| Android | android |
| iOS | iosX64, iosArm64, iosSimulatorArm64 |
| macOS | macosX64, macosArm64 |
| tvOS | tvosX64, tvosArm64, tvosSimulatorArm64 |
| watchOS | watchosX64, watchosArm32, watchosArm64, watchosSimulatorArm64, watchosDeviceArm64 |
| JVM | jvm |
| JavaScript | js (browser, nodejs) |
| WebAssembly | wasmJs (browser, nodejs), wasmWasi (nodejs) |
| Linux | linuxX64, linuxArm64 |
| Windows | mingwX64 |

## Publishing

### Local Testing

```bash
./gradlew :cmp-{feature}:publishToMavenLocal
```

### Maven Central

```bash
./gradlew :cmp-{feature}:publishAllPublicationsToMavenCentralRepository
```

## Planned Modules

| Module | Artifact | Features | Status |
|--------|----------|----------|:------:|
| cmp-clipboard | kmp-clipboard | Clipboard utilities | ✅ Released |
| cmp-appupdate | kmp-appupdate | App version checking | 🔜 Planned |
| cmp-platform | kmp-platform | Platform detection | 🔜 Planned |
| cmp-datetime | kmp-datetime | Date/time utilities | 🔜 Planned |
| cmp-crypto | kmp-crypto | Hashing, encoding | 🔜 Planned |

## Design Principles

1. **Zero Configuration** - Features work out-of-the-box without setup
2. **Modular** - Import only what you need
3. **Consistent API** - Same function signatures across all modules
4. **Platform Graceful Degradation** - No-op implementations where features aren't supported
5. **Independent Versioning** - Each module has its own version lifecycle
