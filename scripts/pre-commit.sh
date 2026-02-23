#!/bin/sh

# Pre-commit hook for KMP Library Template
# Runs code formatting checks before each commit

# Function to check the current branch
check_current_branch() {
    printf "\n🚀 Checking the current git branch...\n"
    CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
    if [ "$CURRENT_BRANCH" = "main" ] || [ "$CURRENT_BRANCH" = "master" ]; then
        echo "🛑 Hold it right there! Committing directly to the '$CURRENT_BRANCH' branch?"
        echo "🚫 Direct commits to '$CURRENT_BRANCH' are not recommended!"
        printf "\n⚠️  Consider using a feature branch instead.\n"
        printf "To bypass this check, use: git commit --no-verify\n\n"
        # Warning only, not blocking for library development
    else
        echo "✅ You're on the '$CURRENT_BRANCH' branch. Let's continue! 🚀"
    fi
}

# Function to run Spotless checks
run_spotless_checks() {
    printf "\n🎨 Running Spotless to format your code...\n"

    if ! ./gradlew spotlessApply --daemon > /tmp/spotless-result 2>&1; then
        cat /tmp/spotless-result
        rm -f /tmp/spotless-result
        printf "\n*********************************************************************************\n"
        echo "   💥 Spotless found formatting issues that couldn't be auto-fixed!"
        echo "   💡 Please review the errors above and fix them manually."
        printf "*********************************************************************************\n"
        exit 1
    else
        rm -f /tmp/spotless-result
        echo "✅ Code formatting applied successfully! ✨"
    fi

    # Stage any formatting changes
    git add -u
}

# Function to run Detekt checks
run_detekt_checks() {
    printf "\n🔍 Running Detekt for static analysis...\n"

    if ! ./gradlew detekt --daemon > /tmp/detekt-result 2>&1; then
        cat /tmp/detekt-result
        rm -f /tmp/detekt-result
        printf "\n*********************************************************************************\n"
        echo "   💥 Detekt found code quality issues!"
        echo "   💡 Please review the report and fix the issues."
        printf "*********************************************************************************\n"
        exit 1
    else
        rm -f /tmp/detekt-result
        echo "✅ Detekt analysis passed! 🎯"
    fi
}

# Function to print success message
print_success_message() {
    GIT_USERNAME=$(git config user.name)
    printf "\n*********************************************************************************\n"
    echo "🎉 Excellent work, $GIT_USERNAME!"
    echo "Your code passed all pre-commit checks. Ready to commit! 🚀"
    printf "*********************************************************************************\n\n"
}

# Main script execution
check_current_branch
run_spotless_checks
run_detekt_checks
print_success_message

exit 0
