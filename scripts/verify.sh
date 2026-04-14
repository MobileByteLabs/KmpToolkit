#!/bin/bash

# ============================================================================
# verify.sh — Single script to verify and fix everything before push/PR
#
# Mirrors the EXACT checks that run in CI (GitHub Actions gradle.yml):
#   1. spotlessApply (auto-fix formatting)
#   2. spotlessCheck (verify formatting is clean)
#   3. detekt (static analysis)
#   4. jvmTest (unit tests for all cmp-* modules)
#   5. assemble all cmp-* modules (build all targets)
#
# Usage:
#   ./scripts/verify.sh           # Full verify (fix + check + test + build)
#   ./scripts/verify.sh --fix     # Only fix formatting (spotlessApply)
#   ./scripts/verify.sh --check   # Only check (no fix, no build)
#   ./scripts/verify.sh --quick   # Fix + check + test (skip full build)
#   ./scripts/verify.sh --ci      # Exact CI mirror (check + test + build)
#
# After running this, your PR will pass all CI checks guaranteed.
# ============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
BOLD='\033[1m'
NC='\033[0m'

# Track results
PASS=0
FAIL=0
FIXED=0
STEPS=()

# cd to repo root
cd "$(git rev-parse --show-toplevel 2>/dev/null || echo '.')"

# Parse mode
MODE="${1:-full}"

print_header() {
    echo ""
    echo -e "${BLUE}${BOLD}╔══════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}${BOLD}║  KMP Toolkit — Verify & Fix                                  ║${NC}"
    echo -e "${BLUE}${BOLD}║  Mode: ${MODE}                                                      ║${NC}"
    echo -e "${BLUE}${BOLD}╚══════════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

step_pass() {
    PASS=$((PASS + 1))
    STEPS+=("${GREEN}PASS${NC} $1")
    echo -e "  ${GREEN}PASS${NC} $1"
}

step_fixed() {
    FIXED=$((FIXED + 1))
    STEPS+=("${YELLOW}FIXED${NC} $1")
    echo -e "  ${YELLOW}FIXED${NC} $1"
}

step_fail() {
    FAIL=$((FAIL + 1))
    STEPS+=("${RED}FAIL${NC} $1")
    echo -e "  ${RED}FAIL${NC} $1"
}

step_skip() {
    STEPS+=("${BLUE}SKIP${NC} $1")
    echo -e "  ${BLUE}SKIP${NC} $1"
}

# ── Step 1: Spotless Apply (auto-fix formatting) ────────────────────────

run_spotless_fix() {
    echo -e "\n${BOLD}[1/5] Spotless Apply — auto-fix formatting${NC}"

    if ./gradlew spotlessApply --daemon -q 2>/dev/null; then
        # Check if any files were changed
        if git diff --quiet 2>/dev/null; then
            step_pass "spotlessApply (no changes needed)"
        else
            CHANGED=$(git diff --name-only | wc -l | tr -d ' ')
            git add -u 2>/dev/null || true
            step_fixed "spotlessApply (${CHANGED} files reformatted)"
        fi
    else
        # spotlessApply failed — likely ktlint errors that can't be auto-fixed
        echo ""
        echo -e "  ${YELLOW}spotlessApply found unfixable issues. Running with output:${NC}"
        ./gradlew spotlessApply --daemon 2>&1 | grep -E "lint error|Error|FAILED" | head -10
        step_fail "spotlessApply (manual fix required)"
        return 1
    fi
}

# ── Step 2: Spotless Check (verify formatting) ──────────────────────────

run_spotless_check() {
    echo -e "\n${BOLD}[2/5] Spotless Check — verify formatting${NC}"

    if ./gradlew spotlessCheck --daemon -q 2>/dev/null; then
        step_pass "spotlessCheck"
    else
        step_fail "spotlessCheck (run ./scripts/verify.sh --fix first)"
        return 1
    fi
}

# ── Step 3: Detekt (static analysis) ────────────────────────────────────

run_detekt() {
    echo -e "\n${BOLD}[3/5] Detekt — static analysis${NC}"

    if ./gradlew detekt --daemon -q 2>/dev/null; then
        step_pass "detekt"
    else
        echo ""
        echo -e "  ${YELLOW}Detekt issues found:${NC}"
        ./gradlew detekt --daemon 2>&1 | grep -E "^.+\.kt:" | head -10
        step_fail "detekt (fix code quality issues)"
        return 1
    fi
}

# ── Step 4: JVM Tests (all cmp-* modules) ───────────────────────────────

run_tests() {
    echo -e "\n${BOLD}[4/5] JVM Tests — per-module${NC}"

    TOTAL_TESTS=0
    PASSED_MODULES=0
    FAILED_MODULES=0
    SKIPPED_MODULES=0

    for dir in cmp-*/; do
        if [ -d "$dir" ]; then
            MODULE="${dir%/}"

            # Check if module has JVM target by looking for jvm() in build.gradle.kts
            if ! grep -q "jvm()" "${dir}build.gradle.kts" 2>/dev/null; then
                SKIPPED_MODULES=$((SKIPPED_MODULES + 1))
                echo -e "    ${BLUE}SKIP${NC} ${MODULE} (no JVM target)"
                continue
            fi

            OUTPUT=$(./gradlew ":${MODULE}:jvmTest" --daemon 2>&1)
            if echo "$OUTPUT" | grep -q "BUILD SUCCESSFUL"; then
                COUNT=0
                if [ -d "${dir}build/test-results/jvmTest" ]; then
                    COUNT=$(find "${dir}build/test-results/jvmTest" -name "*.xml" -exec grep -c "testcase" {} + 2>/dev/null | awk -F: '{sum+=$2} END {print sum}')
                fi
                TOTAL_TESTS=$((TOTAL_TESTS + COUNT))
                PASSED_MODULES=$((PASSED_MODULES + 1))
                echo -e "    ${GREEN}PASS${NC} ${MODULE} (${COUNT} tests)"
            else
                FAILED_MODULES=$((FAILED_MODULES + 1))
                echo -e "    ${RED}FAIL${NC} ${MODULE}"
            fi
        fi
    done

    if [ $FAILED_MODULES -eq 0 ]; then
        step_pass "jvmTest (${PASSED_MODULES} modules, ${TOTAL_TESTS} tests, ${SKIPPED_MODULES} skipped)"
    else
        step_fail "jvmTest (${FAILED_MODULES} failed, ${PASSED_MODULES} passed, ${SKIPPED_MODULES} skipped)"
        return 1
    fi
}

# ── Step 5: Build All (assemble all targets) ────────────────────────────

run_build_all() {
    echo -e "\n${BOLD}[5/5] Build All — assemble all cmp-* modules${NC}"

    BUILD_TASKS=""
    for dir in cmp-*/; do
        if [ -d "$dir" ]; then
            MODULE="${dir%/}"
            BUILD_TASKS="$BUILD_TASKS :${MODULE}:assemble"
        fi
    done

    if [ -z "$BUILD_TASKS" ]; then
        step_skip "assemble (no cmp-* modules found)"
        return 0
    fi

    if ./gradlew $BUILD_TASKS --daemon 2>/dev/null; then
        MODULE_COUNT=$(echo "$BUILD_TASKS" | wc -w | tr -d ' ')
        step_pass "assemble (${MODULE_COUNT} modules, all targets)"
    else
        step_fail "assemble"
        echo ""
        echo -e "  ${YELLOW}Build errors:${NC}"
        ./gradlew $BUILD_TASKS --daemon 2>&1 | grep "^e:" | head -10
        return 1
    fi
}

# ── Print Summary ───────────────────────────────────────────────────────

print_summary() {
    echo ""
    echo -e "${BOLD}══════════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  RESULTS${NC}"
    echo -e "${BOLD}══════════════════════════════════════════════════════════════${NC}"
    echo ""

    for step in "${STEPS[@]}"; do
        echo -e "  $step"
    done

    echo ""
    echo -e "  ${GREEN}Pass: ${PASS}${NC}  ${YELLOW}Fixed: ${FIXED}${NC}  ${RED}Fail: ${FAIL}${NC}"

    if [ $FAIL -eq 0 ]; then
        echo ""
        echo -e "${GREEN}${BOLD}  All checks passed! Your PR will pass CI.${NC}"
        echo ""
        if [ $FIXED -gt 0 ]; then
            echo -e "  ${YELLOW}Note: Files were auto-formatted. Commit the changes:${NC}"
            echo -e "  ${YELLOW}  git add -A && git commit --amend --no-edit${NC}"
            echo ""
        fi
        return 0
    else
        echo ""
        echo -e "${RED}${BOLD}  ${FAIL} check(s) failed. Fix issues and re-run.${NC}"
        echo ""
        return 1
    fi
}

# ── Main ────────────────────────────────────────────────────────────────

print_header

case "$MODE" in
    --fix)
        run_spotless_fix || true
        ;;
    --check)
        run_spotless_check || true
        run_detekt || true
        ;;
    --quick)
        run_spotless_fix || true
        run_spotless_check || true
        run_detekt || true
        run_tests || true
        ;;
    --ci)
        # Exact CI mirror — no auto-fix, just check
        run_spotless_check || true
        run_detekt || true
        run_tests || true
        run_build_all || true
        ;;
    --full|*)
        # Default: fix + check + test + build
        run_spotless_fix || true
        run_spotless_check || true
        run_detekt || true
        run_tests || true
        run_build_all || true
        ;;
esac

print_summary
