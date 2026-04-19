#!/bin/sh

# Pre-commit hook — auto-fix formatting + static analysis
# Full verification: ./scripts/verify.sh

cd "$(git rev-parse --show-toplevel)"

# ── Branch check ──────────────────────────────────────────────────────
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" = "main" ] || [ "$CURRENT_BRANCH" = "master" ]; then
    echo ""
    echo "Warning: committing directly to '$CURRENT_BRANCH'."
    echo "Consider using a feature branch instead."
    echo "To bypass: git commit --no-verify"
    echo ""
fi

# ── Spotless Apply (auto-fix formatting) ──────────────────────────────
echo ""
echo "Pre-commit: formatting code..."

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

# ── Detekt (static analysis) ─────────────────────────────────────────
echo "Pre-commit: running static analysis..."

if ! ./gradlew detekt --daemon -q 2>/dev/null; then
    echo ""
    echo "Detekt found code quality issues:"
    ./gradlew detekt --daemon 2>&1 | grep -E "^.+\.kt:" | head -10
    echo ""
    echo "Fix issues or bypass: git commit --no-verify"
    exit 1
fi

echo "Pre-commit: static analysis passed."

GIT_USERNAME=$(git config user.name)
echo ""
echo "All pre-commit checks passed, $GIT_USERNAME!"
echo ""
exit 0
