#!/bin/sh

# Pre-push hook — runs quick verification before push
# Full verification: ./scripts/verify.sh --full

cd "$(git rev-parse --show-toplevel)"

echo ""
echo "Pre-push: running quick checks..."
echo ""

# Step 1: Spotless check (formatting must be clean)
echo "[1/3] Spotless check..."
if ! ./gradlew spotlessCheck --daemon -q 2>/dev/null; then
    echo ""
    echo "Formatting issues found. Run: ./scripts/verify.sh --fix"
    exit 1
fi
echo "  PASS spotlessCheck"

# Step 2: Detekt (static analysis)
echo "[2/3] Detekt..."
if ! ./gradlew detekt --daemon -q 2>/dev/null; then
    echo ""
    echo "Detekt issues found. Run: ./scripts/verify.sh"
    exit 1
fi
echo "  PASS detekt"

# Step 3: JVM tests (all cmp-* modules)
echo "[3/3] JVM tests..."
TEST_TASKS=""
for dir in cmp-*/; do
    [ -d "$dir" ] && TEST_TASKS="$TEST_TASKS :${dir%/}:jvmTest"
done

if ! ./gradlew $TEST_TASKS --daemon -q 2>/dev/null; then
    echo ""
    echo "Tests failed. Run: ./scripts/verify.sh"
    exit 1
fi
echo "  PASS jvmTest"

echo ""
echo "All pre-push checks passed."
echo ""
exit 0
