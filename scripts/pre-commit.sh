#!/bin/sh

# Pre-commit hook — lightweight formatting fix
# Full verification: ./scripts/verify.sh

cd "$(git rev-parse --show-toplevel)"

echo ""
echo "Pre-commit: formatting code..."

# Auto-fix formatting
if ! ./gradlew spotlessApply --daemon -q 2>/dev/null; then
    echo ""
    echo "Spotless found unfixable issues. Run: ./scripts/verify.sh"
    echo ""
    ./gradlew spotlessApply --daemon 2>&1 | grep "lint error" | head -5
    exit 1
fi

# Stage formatting changes
git add -u

echo "Pre-commit: formatting applied."
echo ""
exit 0
