# KMP Library Template

[![CI](https://github.com/MobileByteLabs/mbl-library-template-kmp/actions/workflows/gradle.yml/badge.svg)](https://github.com/MobileByteLabs/mbl-library-template-kmp/actions/workflows/gradle.yml)
[![GitHub Release](https://img.shields.io/github/v/release/MobileByteLabs/mbl-library-template-kmp?include_prereleases)](https://github.com/MobileByteLabs/mbl-library-template-kmp/releases)
[![Kotlin](https://img.shields.io/badge/kotlin-2.1.0-blue.svg?logo=kotlin)](http://kotlinlang.org)
[![License](https://img.shields.io/badge/License-Apache%202.0-blue.svg)](https://opensource.org/licenses/Apache-2.0)

A template for creating Kotlin Multiplatform libraries with full platform support and Compose Multiplatform sample app.

## Supported Platforms

| Platform | Targets | Status |
|----------|---------|--------|
| Android  | android | Supported |
| iOS      | iosX64, iosArm64, iosSimulatorArm64 | Supported |
| macOS    | macosX64, macosArm64 | Supported |
| tvOS     | tvosX64, tvosArm64, tvosSimulatorArm64 | Supported |
| watchOS  | watchosX64, watchosArm32, watchosArm64, watchosSimulatorArm64, watchosDeviceArm64 | Supported |
| JVM      | jvm | Supported |
| Linux    | linuxX64, linuxArm64 | Supported |
| Windows  | mingwX64 | Supported |
| JavaScript | js (Browser, Node.js) | Supported |
| WebAssembly | wasmJs (Browser, Node.js), wasmWasi (Node.js) | Supported |

## Installation

Add the dependency to your `build.gradle.kts`:

```kotlin
// In your shared module
kotlin {
    sourceSets {
        commonMain.dependencies {
            implementation("TEMPLATE_PACKAGE:template-library:1.0.0")
        }
    }
}
```

### Platform-specific setup

<details>
<summary>Android</summary>

No additional setup required.

</details>

<details>
<summary>iOS</summary>

No additional setup required.

</details>

## Quick Start

```kotlin
import TEMPLATE_PACKAGE.Greeting

fun main() {
    val greeting = Greeting()
    println(greeting.greet()) // Hello from [Platform]!
    println(greeting.greet("World")) // Hello, World! Welcome from [Platform].
}
```

## Documentation

For detailed documentation, visit [Documentation Link].

## Getting Started with Development

### Prerequisites

- JDK 17 or higher
- Android SDK (for Android development)
- Xcode 15+ (for iOS development, macOS only)

### Setup

1. Clone the repository:
```bash
git clone https://github.com/TEMPLATE_ORG/TEMPLATE_REPO.git
cd TEMPLATE_REPO
```

2. Customize the template (first time only):
```bash
bash customizer.sh com.yourpackage.library YourLibraryName your-org
```

3. Set up git hooks:
```bash
bash scripts/setup-hooks.sh
```

4. Build the project:
```bash
./gradlew build
```

### Running Tests

```bash
# All platforms
./gradlew allTests

# Specific platforms
./gradlew jvmTest
./gradlew iosSimulatorArm64Test
./gradlew testAndroidHostTest
./gradlew linuxX64Test
```

### Code Quality

```bash
# Format code
./gradlew spotlessApply

# Run static analysis
./gradlew detekt
```

### Sample App

A Compose Multiplatform sample app is included to test the library on all platforms:

```bash
# Run on Desktop (macOS, Windows, Linux)
./gradlew :sample-app:run

# Run on Android
./gradlew :sample-app:installDebug

# Run on iOS (requires Xcode on macOS)
# Open sample-app in Xcode or use KMM plugin in Android Studio

# Run on Web (WebAssembly)
./gradlew :sample-app:wasmJsBrowserRun
```

## Publishing to Maven Central

### Prerequisites

1. Create a [Sonatype account](https://central.sonatype.com/)
2. Generate a GPG key for signing
3. Configure GitHub secrets:
   - `MAVEN_CENTRAL_USERNAME` - Sonatype username
   - `MAVEN_CENTRAL_PASSWORD` - Sonatype password
   - `SIGNING_KEY_ID` - GPG key ID
   - `SIGNING_PASSWORD` - GPG key password
   - `GPG_KEY_CONTENTS` - Base64 encoded GPG private key

### Release Process

1. Update version in `cmp-library/build.gradle.kts`
2. Create a GitHub release with a tag (e.g., `v1.0.0`)
3. The publish workflow will automatically deploy to Maven Central

## Project Structure

```
.
├── cmp-library/                # Library module
│   └── src/
│       ├── commonMain/         # Common code (all platforms)
│       ├── commonTest/         # Common tests
│       ├── androidMain/        # Android-specific code
│       ├── jvmMain/            # JVM-specific code
│       ├── appleMain/          # Apple platforms (iOS, macOS, tvOS, watchOS)
│       ├── linuxMain/          # Linux platforms (linuxX64, linuxArm64)
│       ├── mingwMain/          # Windows (mingwX64)
│       ├── jsMain/             # JavaScript (Browser, Node.js)
│       ├── wasmJsMain/         # WebAssembly JS
│       └── wasmWasiMain/       # WebAssembly WASI
├── sample-app/                 # Compose Multiplatform sample app
│   └── src/
│       ├── commonMain/         # Shared UI code
│       ├── androidMain/        # Android app entry
│       ├── desktopMain/        # Desktop app entry
│       ├── iosMain/            # iOS app entry
│       └── wasmJsMain/         # Web app entry
├── scripts/                    # Automation scripts
│   ├── pre-commit.sh           # Pre-commit hook
│   ├── pre-push.sh             # Pre-push hook
│   └── setup-hooks.sh          # Hook setup script
├── config/
│   └── detekt/                 # Detekt configuration
├── .github/
│   ├── workflows/              # GitHub Actions
│   └── ISSUE_TEMPLATE/         # Issue templates
├── customizer.sh               # Template customization script
└── build.gradle.kts            # Root build configuration
```

## Contributing

We welcome contributions! Please see our [Contributing Guide](CONTRIBUTING.md) for details.

## License

```
Copyright 2025 MobileByteLabs

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.
```

## Acknowledgments

- [Kotlin Multiplatform](https://kotlinlang.org/docs/multiplatform.html)
- [Gradle Maven Publish Plugin](https://vanniktech.github.io/gradle-maven-publish-plugin/)
