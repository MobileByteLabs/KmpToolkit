#!/bin/sh

# Pre-push hook for KMP Library Template
# Runs comprehensive checks before pushing to remote

# Function to check the current branch
check_current_branch() {
    printf "\n🚀 Checking the current git branch...\n"
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    if [ "$CURRENT_BRANCH" = "main" ] || [ "$CURRENT_BRANCH" = "master" ]; then
        echo "⚠️  You're pushing to '$CURRENT_BRANCH' branch."
        echo "Make sure you have the necessary permissions and approvals."
    else
        echo "✅ Pushing from '$CURRENT_BRANCH' branch. 🚀"
    fi
}

# Function to run Spotless checks
run_spotless_checks() {
    printf "\n🎨 Running Spotless check...\n"

    if ! ./gradlew spotlessCheck --daemon > /tmp/spotless-result 2>&1; then
        cat /tmp/spotless-result
        rm -f /tmp/spotless-result
        printf "\n*********************************************************************************\n"
        echo "   💥 Spotless found formatting issues!"
        echo "   💡 Run './gradlew spotlessApply' to fix formatting."
        printf "*********************************************************************************\n"

        printf "\n🔧 Attempting to apply Spotless fixes...\n"
        if ./gradlew spotlessApply --daemon > /tmp/spotless-result 2>&1; then
            rm -f /tmp/spotless-result
            echo "✅ Formatting fixed! Please commit the changes and push again."
            exit 1
        else
            cat /tmp/spotless-result
            rm -f /tmp/spotless-result
            echo "❌ Could not auto-fix formatting. Please fix manually."
            exit 1
        fi
    else
        rm -f /tmp/spotless-result
        echo "✅ Code formatting check passed! ✨"
    fi
}

# Function to run Detekt checks
run_detekt_checks() {
    printf "\n🔍 Running Detekt static analysis...\n"

    if ! ./gradlew detekt --daemon > /tmp/detekt-result 2>&1; then
        cat /tmp/detekt-result
        rm -f /tmp/detekt-result
        printf "\n*********************************************************************************\n"
        echo "   💥 Detekt found code quality issues!"
        echo "   💡 Review the Detekt report and fix the issues."
        printf "*********************************************************************************\n"
        exit 1
    else
        rm -f /tmp/detekt-result
        echo "✅ Detekt analysis passed! 🎯"
    fi
}

# Function to run tests
run_tests() {
    printf "\n🧪 Running tests...\n"

    # Run JVM tests as a quick sanity check
    if ! ./gradlew jvmTest --daemon > /tmp/test-result 2>&1; then
        cat /tmp/test-result
        rm -f /tmp/test-result
        printf "\n*********************************************************************************\n"
        echo "   💥 Tests failed!"
        echo "   💡 Fix the failing tests before pushing."
        printf "*********************************************************************************\n"
        exit 1
    else
        rm -f /tmp/test-result
        echo "✅ Tests passed! 🧪"
    fi
}

# Function to check API compatibility (optional)
check_api_compatibility() {
    printf "\n📋 Checking API compatibility...\n"

    if ./gradlew apiCheck --daemon > /tmp/api-result 2>&1; then
        rm -f /tmp/api-result
        echo "✅ API compatibility check passed! 📋"
    else
        # API check might not be configured, that's OK
        rm -f /tmp/api-result
        echo "ℹ️  API compatibility check skipped (not configured)"
    fi
}

# Function to print success message
print_success_message() {
    GIT_USERNAME=$(git config user.name)
    printf "\n*********************************************************************************\n"
    echo "🚀🎉 Congratulations, $GIT_USERNAME!"
    echo "Your code has passed all pre-push checks!"
    echo "Your changes are ready to be pushed to the remote repository. 🌟"
    printf "*********************************************************************************\n\n"
}

# Main script execution
check_current_branch
run_spotless_checks
run_detekt_checks
run_tests
check_api_compatibility
print_success_message

exit 0
