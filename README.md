# KMP Toolkit

[![Maven Central](https://img.shields.io/maven-central/v/io.github.mobilebytelabs/kmp-toolkit)](https://central.sonatype.com/artifact/io.github.mobilebytelabs/kmp-toolkit)
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

### Option 1: Individual Modules (Recommended)

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // Clipboard utilities only
            implementation("io.github.mobilebytelabs:kmp-clipboard:0.1.0")

            // App Update utilities only (includes clipboard for convenience)
            implementation("io.github.mobilebytelabs:kmp-toolkit:0.5.0")
        }
    }
}
```

### Option 2: Full Toolkit

```kotlin
kotlin {
    sourceSets {
        commonMain.dependencies {
            // All utilities (clipboard + app-update)
            implementation("io.github.mobilebytelabs:kmp-toolkit:0.5.0")
            implementation("io.github.mobilebytelabs:kmp-clipboard:0.1.0")
        }
    }
}
```

**No setup required!** All libraries automatically initialize on all platforms.

## Modules

| Module | Artifact | Description | Version |
|--------|----------|-------------|:-------:|
| **kmp-clipboard** | `io.github.mobilebytelabs:kmp-clipboard` | Clipboard copy/paste/clear | `0.1.0` |
| **kmp-toolkit** | `io.github.mobilebytelabs:kmp-toolkit` | App Update checking | `0.5.0` |

Each module is independently publishable and can be used standalone.

## Features

| Feature | Description | Docs |
|---------|-------------|:----:|
| [Clipboard](https://github.com/MobileByteLabs/KmpToolkit/wiki/Clipboard) | Copy, paste, check & clear clipboard | [Wiki](https://github.com/MobileByteLabs/KmpToolkit/wiki/Clipboard) |
| [App Update](https://github.com/MobileByteLabs/KmpToolkit/wiki/App-Update) | In-app update checking & store redirect | [Wiki](https://github.com/MobileByteLabs/KmpToolkit/wiki/App-Update) |

## Platform Support

| Platform | Status | Targets |
|----------|:------:|---------|
| Android | ✅ | android |
| iOS | ✅ | iosX64, iosArm64, iosSimulatorArm64 |
| macOS | ✅ | macosX64, macosArm64 |
| JVM | ✅ | jvm |
| Linux | ✅ | linuxX64, linuxArm64 |
| Windows | ✅ | mingwX64 |
| JavaScript | ✅ | js (Browser, Node.js) |
| WebAssembly | ✅ | wasmJs, wasmWasi |
| tvOS | ⚠️ | tvosX64, tvosArm64, tvosSimulatorArm64 |
| watchOS | ⚠️ | watchosX64, watchosArm32, watchosArm64, watchosSimulatorArm64 |

**Legend:** ✅ Full support | ⚠️ Limited (see feature docs)

## Documentation

| Topic | Link |
|-------|------|
| **Features** | [Wiki Home](https://github.com/MobileByteLabs/KmpToolkit/wiki) |
| Clipboard API | [Clipboard](https://github.com/MobileByteLabs/KmpToolkit/wiki/Clipboard) |
| App Update API | [App Update](https://github.com/MobileByteLabs/KmpToolkit/wiki/App-Update) |
| **Development** | |
| Getting Started | [Development Guide](https://github.com/MobileByteLabs/KmpToolkit/wiki/Development-Guide) |
| Project Structure | [Architecture](https://github.com/MobileByteLabs/KmpToolkit/wiki/Architecture) |
| Publishing | [Publishing Guide](https://github.com/MobileByteLabs/KmpToolkit/wiki/Publishing) |
| **Contributing** | |
| Contributing Guide | [CONTRIBUTING.md](CONTRIBUTING.md) |
| Adding Features | [Adding New Features](https://github.com/MobileByteLabs/KmpToolkit/wiki/Adding-New-Features) |

## Contributing

We welcome contributions! See [CONTRIBUTING.md](CONTRIBUTING.md) and the [Adding New Features](https://github.com/MobileByteLabs/KmpToolkit/wiki/Adding-New-Features) wiki guide.

## License

```
Copyright 2025 MobileByteLabs

Licensed under the Apache License, Version 2.0
```

See [LICENSE](LICENSE) for details.
