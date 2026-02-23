# Getting Started with Your New Library

Congratulations on creating your new Kotlin Multiplatform library! Follow these steps to customize it.

## Quick Setup (Recommended)

### Option 1: Using GitHub Actions UI

1. Go to **Actions** tab in your new repository
2. Select **"Template Customization"** workflow
3. Click **"Run workflow"**
4. Fill in:
   - **Package name**: e.g., `com.example.mylibrary`
   - **Library name**: e.g., `MyAwesomeLib`
   - **Organization**: (optional - defaults to your GitHub username/org)
5. Click **"Run workflow"**

The workflow will automatically customize all files and commit the changes.

### Option 2: Using Command Line

```bash
# Clone your new repository
git clone https://github.com/YOUR_ORG/YOUR_REPO.git
cd YOUR_REPO

# Run the customizer script
bash customizer.sh com.example.mylibrary MyAwesomeLib your-org

# Set up git hooks for code quality
bash scripts/setup-hooks.sh

# Commit the changes
git add -A
git commit -m "chore: Initialize library from template"
git push
```

## After Customization

1. **Update README.md** - Replace template descriptions with your library's purpose
2. **Configure Publishing** - Set up GitHub secrets for Maven Central:
   - `MAVEN_CENTRAL_USERNAME`
   - `MAVEN_CENTRAL_PASSWORD`
   - `SIGNING_KEY_ID`
   - `SIGNING_PASSWORD`
   - `GPG_KEY_CONTENTS`
3. **Start Coding** - Your library code goes in `cmp-library/src/`

## Supported Platforms

Your library supports all Kotlin Multiplatform targets:

| Platform | Targets |
|----------|---------|
| Android | android |
| iOS | iosX64, iosArm64, iosSimulatorArm64 |
| macOS | macosX64, macosArm64 |
| tvOS | tvosX64, tvosArm64, tvosSimulatorArm64 |
| watchOS | watchosX64, watchosArm32, watchosArm64, watchosSimulatorArm64, watchosDeviceArm64 |
| JVM | jvm |
| Linux | linuxX64, linuxArm64 |
| Windows | mingwX64 |
| JavaScript | js (Browser, Node.js) |
| WebAssembly | wasmJs (Browser, Node.js), wasmWasi (Node.js) |

## Development Commands

```bash
# Run all checks before pushing
./ci-prepush.sh

# Format code
./gradlew spotlessApply

# Run static analysis
./gradlew detekt

# Run tests
./gradlew jvmTest
./gradlew iosSimulatorArm64Test  # macOS only

# Build all targets
./gradlew assemble
```

## Keeping Up-to-Date

To sync framework updates from the template repository:

```bash
./sync-dirs.sh
```

This will sync CI/CD configs, scripts, and tooling while preserving your library code.

---

**Delete this file** after you've completed the setup!
