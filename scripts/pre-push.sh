#!/bin/sh

# Pre-push hook — runs comprehensive checks before push
# Full verification: ./scripts/verify.sh --full

cd "$(git rev-parse --show-toplevel)"

# ── Branch check ──────────────────────────────────────────────────────
CURRENT_BRANCH=$(git rev-parse --abbrev-ref HEAD)
if [ "$CURRENT_BRANCH" = "main" ] || [ "$CURRENT_BRANCH" = "master" ]; then
    echo ""
    echo "Warning: pushing to '$CURRENT_BRANCH'. Ensure you have approvals."
    echo ""
fi

echo ""
echo "Pre-push: running comprehensive checks..."
echo ""

# Step 1: Spotless check (formatting must be clean)
echo "[1/4] Spotless check..."
if ! ./gradlew spotlessCheck --daemon -q 2>/dev/null; then
    echo ""
    echo "Formatting issues found."
    echo "Attempting auto-fix..."
    if ./gradlew spotlessApply --daemon -q 2>/dev/null; then
        echo "Formatting fixed. Please commit the changes and push again."
    else
        echo "Could not auto-fix. Run: ./scripts/verify.sh --fix"
    fi
    exit 1
fi
echo "  PASS spotlessCheck"

# Step 2: Detekt (static analysis)
echo "[2/4] Detekt..."
if ! ./gradlew detekt --daemon -q 2>/dev/null; then
    echo ""
    echo "Detekt issues found. Run: ./scripts/verify.sh"
    exit 1
fi
echo "  PASS detekt"

# Step 3: JVM tests (auto-discover all cmp-* modules with JVM target)
echo "[3/4] JVM tests..."
TEST_TASKS=""
MODULE_COUNT=0
for dir in cmp-*/; do
    if [ -d "$dir" ] && grep -q "jvm()" "${dir}build.gradle.kts" 2>/dev/null; then
        TEST_TASKS="$TEST_TASKS :${dir%/}:jvmTest"
        MODULE_COUNT=$((MODULE_COUNT + 1))
    fi
done

if [ -n "$TEST_TASKS" ]; then
    if ! ./gradlew $TEST_TASKS --daemon -q 2>/dev/null; then
        echo ""
        echo "Tests failed. Run: ./scripts/verify.sh"
        exit 1
    fi
    echo "  PASS jvmTest ($MODULE_COUNT modules)"
else
    echo "  SKIP jvmTest (no cmp-* modules with JVM target)"
fi

# Step 4: API compatibility check (optional — skips if not configured)
echo "[4/4] API compatibility..."
if ./gradlew apiCheck --daemon -q 2>/dev/null; then
    echo "  PASS apiCheck"
else
    echo "  SKIP apiCheck (not configured)"
fi

GIT_USERNAME=$(git config user.name)
echo ""
echo "All pre-push checks passed, $GIT_USERNAME!"
echo ""
exit 0
