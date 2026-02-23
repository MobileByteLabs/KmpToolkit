# Contributing to TEMPLATE_LIBRARY_NAME

Thank you for your interest in contributing! This document provides guidelines and instructions for contributing.

## Getting Started

1. Fork the repository
2. Clone your fork locally
3. Set up the development environment

### Prerequisites

- JDK 17 or higher
- Android SDK (for Android target)
- Xcode (for iOS target, macOS only)
- Kotlin 2.0+

### Setup

```bash
# Clone your fork
git clone https://github.com/YOUR_USERNAME/TEMPLATE_REPO.git
cd TEMPLATE_REPO

# Set up git hooks
bash scripts/setup-hooks.sh

# Build the project
./gradlew build
```

## Development Workflow

### Branch Naming

- `feature/description` - New features
- `fix/description` - Bug fixes
- `docs/description` - Documentation updates
- `refactor/description` - Code refactoring

### Code Style

This project uses:
- **Spotless** with ktlint for code formatting
- **Detekt** for static analysis

Before committing, run:

```bash
# Format code
./gradlew spotlessApply

# Run static analysis
./gradlew detekt
```

### Running Tests

```bash
# Run all tests
./gradlew allTests

# Run specific platform tests
./gradlew jvmTest
./gradlew iosSimulatorArm64Test
./gradlew testAndroidHostTest
./gradlew linuxX64Test
```

## Pull Request Process

1. Create a feature branch from `main`
2. Make your changes
3. Ensure all tests pass
4. Update documentation if needed
5. Submit a pull request

### PR Checklist

- [ ] Code follows the project style guidelines
- [ ] Tests added/updated for changes
- [ ] Documentation updated
- [ ] Commit messages are clear and descriptive
- [ ] PR description explains the changes

## Reporting Issues

When reporting issues, please include:

- Library version
- Kotlin version
- Platform(s) affected
- Steps to reproduce
- Expected vs actual behavior
- Relevant logs or stack traces

## Code of Conduct

Please read and follow our [Code of Conduct](CODE_OF_CONDUCT.md).

## License

By contributing, you agree that your contributions will be licensed under the same license as the project.
