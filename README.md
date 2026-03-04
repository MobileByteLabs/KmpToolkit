# KMP Toolkit

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/kmp-clipboard)](https://central.sonatype.com/search?q=io.github.mobilebytelabs)
[![CI](https://github.com/MobileByteLabs/KmpToolkit/actions/workflows/gradle.yml/badge.svg)](https://github.com/MobileByteLabs/KmpToolkit/actions/workflows/gradle.yml)
[![Kotlin](https://img.shields.io/badge/kotlin-2.2.20-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

Cross-platform utilities for Kotlin Multiplatform. Zero configuration, works immediately on all platforms.

## Table of Contents

- [Installation](#installation)
- [Modules](#modules)
- [Features](#features)
- [Platform Support](#platform-support)
- [Documentation](#documentation)
- [Contributing](#contributing)
- [License](#license)

## Installation

KMP Toolkit is available as modular libraries. Import only what you need:

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Clipboard utilities
            implementation("io.github.mobilebytelabs:kmp-clipboard:0.1.0")

            // Toast/Snackbar for Compose Multiplatform
            implementation("io.github.mobilebytelabs:kmp-toast:0.1.0")

            // In-App Update checking
            implementation("io.github.mobilebytelabs:kmp-in-app-update:0.5.0")
        }
    }
}
```

**No setup required!** All libraries automatically initialize on all platforms.

## Modules

| Module | Artifact | Description | Version |
|--------|----------|-------------|:-------:|
| **kmp-clipboard** | `io.github.mobilebytelabs:kmp-clipboard` | Clipboard copy/paste/clear/observe | `0.1.0` |
| **kmp-toast** | `io.github.mobilebytelabs:kmp-toast` | Toast/Snackbar for Compose Multiplatform | `0.1.0` |
| **kmp-in-app-update** | `io.github.mobilebytelabs:kmp-in-app-update` | In-app update checking | `0.5.0` |

Each module is independently publishable and can be used standalone.

## Features

### Clipboard

Copy, paste, check, and clear clipboard across all platforms.

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.Clipboard

// Copy text
Clipboard.copy("Hello, World!")

// Paste text
val text = Clipboard.paste()

// Check if clipboard has text
val hasText = Clipboard.hasText()

// Clear clipboard
Clipboard.clear()
```

### Clipboard Observer

Observe clipboard changes with automatic app foreground detection.

```kotlin
import com.mobilebytelabs.kmptoolkit.clipboard.createClipboardObserver

val observer = createClipboardObserver()
observer.startObserving()

// Collect changes via StateFlow
observer.clipboardContent.collect { content ->
    println("Clipboard changed: $content")
}

observer.stopObserving()
```

### Toast/Snackbar (Compose Multiplatform)

Cross-platform toast notifications for Compose apps.

```kotlin
import com.mobilebytelabs.kmptoolkit.toast.*

@Composable
fun MyScreen() {
    val toastState = rememberToastHostState()
    val scope = rememberCoroutineScope()

    Box(modifier = Modifier.fillMaxSize()) {
        Button(onClick = {
            scope.launch {
                toastState.showToast(
                    message = "Hello!",
                    duration = ToastDuration.SHORT,
                    style = ToastStyle.SUCCESS
                )
            }
        }) {
            Text("Show Toast")
        }

        ToastHost(hostState = toastState)
    }
}
```

**Features:**
- Duration: SHORT (2s), MEDIUM (3.5s), LONG (5s), INDEFINITE
- Position: TOP, CENTER, BOTTOM
- Style: DEFAULT, SUCCESS, ERROR, WARNING, INFO
- Action button support
- Swipe to dismiss

### In-App Update

Check for app updates with GitHub Releases, Supabase, or custom backends.

```kotlin
import com.mobilebytelabs.kmptoolkit.appupdate.*

// Check for updates using GitHub Releases
val config = AppUpdateConfig.builder()
    .github(owner = "YourOrg", repo = "YourApp")
    .build()

when (val result = AppUpdate.checkForUpdate(config)) {
    is UpdateResult.Success -> {
        if (result.updateInfo.isAvailable) {
            // Update available!
            println("New version: ${result.updateInfo.latestVersion}")
        }
    }
    is UpdateResult.Error -> println("Error: ${result.message}")
    is UpdateResult.NotSupported -> println("Not supported on this platform")
    is UpdateResult.Cancelled -> println("Cancelled")
}
```

## Platform Support

| Platform | Clipboard | Toast | In-App Update |
|----------|:---------:|:-----:|:-------------:|
| Android | ✅ | ✅ | ✅ (Play Store) |
| iOS | ✅ | ✅ | ✅ (App Store) |
| macOS | ✅ | ✅ | ✅ (App Store) |
| JVM | ✅ | ✅ | ✅ (Custom) |
| Linux | ✅ | ❌ | ✅ (Custom) |
| Windows | ✅ | ❌ | ✅ (Custom) |
| JavaScript | ✅ | ❌ | ❌ |
| WebAssembly | ✅ | ❌ | ❌ |
| tvOS | ⚠️ | ❌ | ⚠️ |
| watchOS | ⚠️ | ❌ | ⚠️ |

**Legend:** ✅ Full support | ⚠️ Limited | ❌ Not supported

## Documentation

| Topic | Link |
|-------|------|
| **Features** | [Wiki Home](https://github.com/MobileByteLabs/KmpToolkit/wiki) |
| Clipboard API | [Clipboard](https://github.com/MobileByteLabs/KmpToolkit/wiki/Clipboard) |
| Toast API | [Toast](https://github.com/MobileByteLabs/KmpToolkit/wiki/Toast) |
| In-App Update API | [In-App Update](https://github.com/MobileByteLabs/KmpToolkit/wiki/In-App-Update) |
| **Development** | |
| Getting Started | [Development Guide](https://github.com/MobileByteLabs/KmpToolkit/wiki/Development-Guide) |
| Adding Features | [Adding New Features](https://github.com/MobileByteLabs/KmpToolkit/wiki/Adding-New-Features) |

## Sample Apps

The `samples/` directory contains example applications:

| Sample | Description |
|--------|-------------|
| `sample-clipboard` | Clipboard + Observer + Toast integration |
| `sample-in-app-update` | In-App Update demo with GitHub Releases |

Run samples:

```bash
# Desktop (JVM)
./gradlew :samples:sample-clipboard:composeApp:run

# Android
./gradlew :samples:sample-clipboard:composeApp:installDebug
```

## Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) for guidelines.

### Creating a New Library Module

1. Copy the template module: `cp -r cmp-library cmp-your-feature`
2. Follow instructions in `cmp-library/TEMPLATE_README.md`
3. Add to `settings.gradle.kts`
4. Create a sample app in `samples/`

## License

```
Copyright 2025 MobileByteLabs

Licensed under the Apache License, Version 2.0
```

See [LICENSE](LICENSE) for details.
