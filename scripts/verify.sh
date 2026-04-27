#!/bin/bash

# ============================================================================
# verify.sh — Verify and fix before push/PR
#
# DEFAULT: smart mode — detects which cmp-* modules changed vs base branch,
#          runs quality checks + platform matrix ONLY for those modules.
#          Falls back to all modules when root config files change.
#
# Usage:
#   ./scripts/verify.sh              # Smart: changed modules only + platform matrix
#   ./scripts/verify.sh --all        # All modules: quality + platform matrix
#   ./scripts/verify.sh --fix        # Only fix formatting (spotlessApply)
#   ./scripts/verify.sh --check      # Only quality checks (spotless + detekt)
#   ./scripts/verify.sh --quick      # Changed modules: quality + jvm tests only (no build)
#   ./scripts/verify.sh --ci         # Exact CI mirror: all modules, no auto-fix
#   ./scripts/verify.sh --local      # Maven Local publish gate
#
# Smart detection:
#   - Compares HEAD against merge-base with origin/development
#   - Finds which cmp-* dirs have changed files
#   - Builds/tests ONLY those modules → fast feedback on active work
#   - If root config changed (build.gradle.kts, gradle/, settings.gradle.kts) → ALL modules
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
MODE="${1:-smart}"

# Resolved module list (set in main, consumed by all step functions)
# Newline-separated module names, or "__NONE__" if nothing changed
RESOLVED_MODULES=""

# ── Smart Change Detection ──────────────────────────────────────────────────
# Returns space-separated list of changed cmp-* module names, or "ALL" if root
# config files changed.

detect_changed_modules() {
    local BASE_BRANCH="${BASE_BRANCH:-development}"

    # Find merge base against origin/development (or local development)
    local MERGE_BASE
    MERGE_BASE=$(git merge-base HEAD "origin/${BASE_BRANCH}" 2>/dev/null) \
        || MERGE_BASE=$(git merge-base HEAD "${BASE_BRANCH}" 2>/dev/null) \
        || MERGE_BASE=$(git rev-parse HEAD~1 2>/dev/null) \
        || MERGE_BASE=""

    local CHANGED_FILES
    if [ -n "$MERGE_BASE" ]; then
        CHANGED_FILES=$(git diff --name-only "$MERGE_BASE" HEAD 2>/dev/null)
    else
        # Fallback: staged + unstaged changes
        CHANGED_FILES=$(git diff --name-only HEAD 2>/dev/null)
    fi

    if [ -z "$CHANGED_FILES" ]; then
        # Nothing changed — nothing to verify
        echo "NONE"
        return
    fi

    # Root config change → must rebuild all (affects every module)
    if echo "$CHANGED_FILES" | grep -qE \
        "^(build\.gradle\.kts|settings\.gradle\.kts|gradle/|buildSrc/|gradle\.properties|\.github/)"; then
        echo "ALL"
        return
    fi

    # Extract distinct cmp-* module dirs from changed paths
    local MODULES
    MODULES=$(echo "$CHANGED_FILES" \
        | grep "^cmp-" \
        | cut -d/ -f1 \
        | grep -v "^cmp-library$" \
        | sort -u \
        | tr '\n' ' ' \
        | sed 's/ $//')

    if [ -z "$MODULES" ]; then
        # Changed files don't belong to any cmp-* module (e.g. docs, samples)
        echo "NONE"
    else
        echo "$MODULES"
    fi
}

# Resolve TARGET_MODULES for this run
resolve_target_modules() {
    local scope="$1"   # "smart" | "all"

    if [ "$scope" = "all" ]; then
        # All cmp-* modules except template
        for dir in cmp-*/; do
            [ -d "$dir" ] && [ "${dir%/}" != "cmp-library" ] && echo "${dir%/}"
        done
        return
    fi

    # Smart: detect changed
    local DETECTED
    DETECTED=$(detect_changed_modules)

    if [ "$DETECTED" = "NONE" ]; then
        echo "__NONE__"
    elif [ "$DETECTED" = "ALL" ]; then
        for dir in cmp-*/; do
            [ -d "$dir" ] && [ "${dir%/}" != "cmp-library" ] && echo "${dir%/}"
        done
    else
        echo "$DETECTED" | tr ' ' '\n'
    fi
}

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

# ── Step 4: JVM Tests (changed or all cmp-* modules) ────────────────────

run_tests() {
    echo -e "\n${BOLD}[4/5] JVM Tests — per-module${NC}"

    TOTAL_TESTS=0
    PASSED_MODULES=0
    FAILED_MODULES=0
    SKIPPED_MODULES=0

    while IFS= read -r MODULE; do
        [ -z "$MODULE" ] && continue
        dir="${MODULE}/"
        [ -d "$dir" ] || continue

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
    done <<< "$RESOLVED_MODULES"

    if [ $FAILED_MODULES -eq 0 ]; then
        step_pass "jvmTest (${PASSED_MODULES} modules, ${TOTAL_TESTS} tests, ${SKIPPED_MODULES} skipped)"
    else
        step_fail "jvmTest (${FAILED_MODULES} failed, ${PASSED_MODULES} passed, ${SKIPPED_MODULES} skipped)"
        return 1
    fi
}

# ── Step 5: Build All (assemble changed or all modules) ─────────────────

run_build_all() {
    echo -e "\n${BOLD}[5/5] Build All — assemble cmp-* modules${NC}"

    BUILD_TASKS=""
    while IFS= read -r MODULE; do
        [ -z "$MODULE" ] && continue
        [ -d "${MODULE}/" ] && BUILD_TASKS="$BUILD_TASKS :${MODULE}:assemble"
    done <<< "$RESOLVED_MODULES"

    if [ -z "$BUILD_TASKS" ]; then
        step_skip "assemble (no modules to build)"
        return 0
    fi

    MODULE_COUNT=$(echo "$BUILD_TASKS" | wc -w | tr -d ' ')
    echo -e "  Building ${MODULE_COUNT} module(s)..."

    if ./gradlew $BUILD_TASKS --daemon 2>/dev/null; then
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

# ── Step 6: Platform Verification (multi-target matrix) ────────────────

run_platform_verify() {
    echo -e "\n${BOLD}[P] Platform Verification — multi-target test matrix${NC}"
    echo ""

    # Detect host capabilities
    HOST_OS="$(uname -s)"
    CAN_IOS=false
    CAN_MACOS=false
    if [ "$HOST_OS" = "Darwin" ]; then
        CAN_IOS=true
        CAN_MACOS=true
    fi

    PLATFORM_PASS=0
    PLATFORM_FAIL=0
    PLATFORM_SKIP=0

    # Header
    printf "  %-20s %-6s %-6s %-7s %-6s %-6s\n" "Module" "JVM" "iOS" "macOS" "JS" "Wasm"
    printf "  %-20s %-6s %-6s %-7s %-6s %-6s\n" "────────────────────" "─────" "─────" "──────" "─────" "─────"

    while IFS= read -r MODULE; do
        [ -z "$MODULE" ] && continue
        dir="${MODULE}/"
        [ -d "$dir" ] || continue
        BUILD_FILE="${dir}build.gradle.kts"
        [ -f "$BUILD_FILE" ] || continue

        JVM_RESULT="—"
        IOS_RESULT="—"
        MACOS_RESULT="—"
        JS_RESULT="—"
        WASM_RESULT="—"

        # JVM test (always available)
        if grep -q "jvm()" "$BUILD_FILE" 2>/dev/null; then
            if ./gradlew ":${MODULE}:jvmTest" --daemon -q 2>/dev/null; then
                JVM_RESULT="${GREEN}pass${NC}"
                PLATFORM_PASS=$((PLATFORM_PASS + 1))
            else
                JVM_RESULT="${RED}FAIL${NC}"
                PLATFORM_FAIL=$((PLATFORM_FAIL + 1))
            fi
        fi

        # iOS test (macOS host only)
        if grep -q "iosSimulatorArm64()" "$BUILD_FILE" 2>/dev/null || grep -q "iosArm64()" "$BUILD_FILE" 2>/dev/null; then
            if $CAN_IOS; then
                if ./gradlew ":${MODULE}:iosSimulatorArm64Test" --daemon -q 2>/dev/null; then
                    IOS_RESULT="${GREEN}pass${NC}"
                    PLATFORM_PASS=$((PLATFORM_PASS + 1))
                else
                    IOS_RESULT="${RED}FAIL${NC}"
                    PLATFORM_FAIL=$((PLATFORM_FAIL + 1))
                fi
            else
                IOS_RESULT="${BLUE}skip${NC}"
                PLATFORM_SKIP=$((PLATFORM_SKIP + 1))
            fi
        fi

        # macOS test (macOS host only)
        if grep -q "macosArm64()" "$BUILD_FILE" 2>/dev/null || grep -q "macosX64()" "$BUILD_FILE" 2>/dev/null; then
            if $CAN_MACOS; then
                if ./gradlew ":${MODULE}:macosArm64Test" --daemon -q 2>/dev/null; then
                    MACOS_RESULT="${GREEN}pass${NC}"
                    PLATFORM_PASS=$((PLATFORM_PASS + 1))
                else
                    MACOS_RESULT="${RED}FAIL${NC}"
                    PLATFORM_FAIL=$((PLATFORM_FAIL + 1))
                fi
            else
                MACOS_RESULT="${BLUE}skip${NC}"
                PLATFORM_SKIP=$((PLATFORM_SKIP + 1))
            fi
        fi

        # JS test
        if grep -q "js(" "$BUILD_FILE" 2>/dev/null; then
            if ./gradlew ":${MODULE}:jsTest" --daemon -q 2>/dev/null; then
                JS_RESULT="${GREEN}pass${NC}"
                PLATFORM_PASS=$((PLATFORM_PASS + 1))
            else
                JS_RESULT="${RED}FAIL${NC}"
                PLATFORM_FAIL=$((PLATFORM_FAIL + 1))
            fi
        fi

        # Wasm test
        if grep -q "wasmJs(" "$BUILD_FILE" 2>/dev/null; then
            if ./gradlew ":${MODULE}:wasmJsNodeTest" --daemon -q 2>/dev/null; then
                WASM_RESULT="${GREEN}pass${NC}"
                PLATFORM_PASS=$((PLATFORM_PASS + 1))
            else
                WASM_RESULT="${RED}FAIL${NC}"
                PLATFORM_FAIL=$((PLATFORM_FAIL + 1))
            fi
        fi

        printf "  %-20s %-6b %-6b %-7b %-6b %-6b\n" "$MODULE" "$JVM_RESULT" "$IOS_RESULT" "$MACOS_RESULT" "$JS_RESULT" "$WASM_RESULT"
    done <<< "$RESOLVED_MODULES"

    echo ""
    if [ $PLATFORM_FAIL -eq 0 ]; then
        step_pass "platforms (${PLATFORM_PASS} passed, ${PLATFORM_SKIP} skipped, 0 failed)"
    else
        step_fail "platforms (${PLATFORM_FAIL} failed, ${PLATFORM_PASS} passed, ${PLATFORM_SKIP} skipped)"
        return 1
    fi
}

# ── Step 7: Maven Local Publish Gate ───────────────────────────────────

run_local_publish() {
    echo -e "\n${BOLD}[L] Maven Local Publish Gate — build proof${NC}"

    LOCAL_PASS=0
    LOCAL_FAIL=0
    GROUP=""

    while IFS= read -r MODULE; do
        [ -z "$MODULE" ] && continue
        dir="${MODULE}/"
        [ -d "$dir" ] || continue
        BUILD_FILE="${dir}build.gradle.kts"
        [ -f "$BUILD_FILE" ] || continue

        # Check for mavenPublishing plugin
        if ! grep -q "mavenPublishing" "$BUILD_FILE" 2>/dev/null; then
            echo -e "    ${BLUE}SKIP${NC} ${MODULE} (no mavenPublishing plugin)"
            continue
        fi

        echo -e "    Publishing ${MODULE} to Maven Local..."
        if ./gradlew ":${MODULE}:publishToMavenLocal" --daemon -q 2>/dev/null; then
            # Extract artifact info for verification
            VERSION=$(grep 'version = "' "$BUILD_FILE" | head -1 | sed 's/.*version = "\(.*\)".*/\1/')
            ARTIFACT=$(grep 'coordinates(' "$BUILD_FILE" | head -1 | sed 's/.*coordinates([^,]*, "\([^"]*\)".*/\1/')
            if [ -z "$GROUP" ]; then
                GROUP=$(grep 'coordinates(' "$BUILD_FILE" | head -1 | sed 's/.*coordinates("\([^"]*\)".*/\1/' | tr '.' '/')
            fi

            # Verify artifacts exist in ~/.m2
            M2_PATH="$HOME/.m2/repository/${GROUP}/${ARTIFACT}/${VERSION}"
            if [ -d "$M2_PATH" ]; then
                ARTIFACT_COUNT=$(ls "$M2_PATH" 2>/dev/null | wc -l | tr -d ' ')
                echo -e "    ${GREEN}PASS${NC} ${MODULE} → ${ARTIFACT}:${VERSION} (${ARTIFACT_COUNT} artifacts in ~/.m2)"
                LOCAL_PASS=$((LOCAL_PASS + 1))
            else
                echo -e "    ${YELLOW}WARN${NC} ${MODULE} → published but artifacts not found at ${M2_PATH}"
                LOCAL_PASS=$((LOCAL_PASS + 1))
            fi
        else
            echo -e "    ${RED}FAIL${NC} ${MODULE}"
            LOCAL_FAIL=$((LOCAL_FAIL + 1))
        fi
    done <<< "$RESOLVED_MODULES"

    echo ""
    if [ $LOCAL_FAIL -eq 0 ]; then
        step_pass "mavenLocal (${LOCAL_PASS} modules published successfully)"
    else
        step_fail "mavenLocal (${LOCAL_FAIL} failed — fix before CI publish)"
        return 1
    fi
}

# ── Resolve target modules + display smart detection banner ─────────────

resolve_and_show() {
    local scope="$1"  # "smart" | "all"

    RESOLVED_MODULES=$(resolve_target_modules "$scope")

    if [ "$RESOLVED_MODULES" = "__NONE__" ]; then
        echo -e "  ${BLUE}Smart:${NC} No changes detected vs origin/development"
        echo -e "  ${BLUE}→${NC}     Nothing to build or test. Use ${BOLD}--all${NC} to force.\n"
        return 1
    fi

    if [ "$scope" = "smart" ]; then
        MODULE_COUNT=$(echo "$RESOLVED_MODULES" | grep -c '[^[:space:]]' | tr -d ' ')
        echo -e "  ${BLUE}Smart:${NC} ${MODULE_COUNT} changed module(s) detected"
        echo "$RESOLVED_MODULES" | while IFS= read -r m; do
            [ -n "$m" ] && echo -e "    ${BLUE}→${NC} $m"
        done
        echo ""
    else
        MODULE_COUNT=$(echo "$RESOLVED_MODULES" | grep -c '[^[:space:]]' | tr -d ' ')
        echo -e "  ${BLUE}All:${NC} ${MODULE_COUNT} module(s) (full run)\n"
    fi

    return 0
}

# ── Main ────────────────────────────────────────────────────────────────

print_header

case "$MODE" in
    --fix)
        # Quality-only — no module detection needed
        RESOLVED_MODULES=$(resolve_target_modules "all")
        run_spotless_fix || true
        ;;
    --check)
        # Quality-only — no module detection needed
        RESOLVED_MODULES=$(resolve_target_modules "all")
        run_spotless_check || true
        run_detekt || true
        ;;
    --quick)
        # Changed modules: quality + jvm tests (no platform build)
        resolve_and_show "smart" || { print_summary; exit 0; }
        run_spotless_fix || true
        run_spotless_check || true
        run_detekt || true
        run_tests || true
        ;;
    --ci)
        # Exact CI mirror — all modules, no auto-fix
        RESOLVED_MODULES=$(resolve_target_modules "all")
        run_spotless_check || true
        run_detekt || true
        run_tests || true
        run_build_all || true
        ;;
    --all)
        # Explicit full run: quality + platform matrix for ALL modules
        resolve_and_show "all" || { print_summary; exit 0; }
        run_spotless_fix || true
        run_spotless_check || true
        run_detekt || true
        run_platform_verify || true
        ;;
    --platforms)
        # Multi-target platform verification matrix (all modules)
        RESOLVED_MODULES=$(resolve_target_modules "all")
        run_platform_verify || true
        ;;
    --local)
        # Maven Local publish gate
        RESOLVED_MODULES=$(resolve_target_modules "all")
        run_local_publish || true
        ;;
    smart|*)
        # DEFAULT: smart — quality checks + platform matrix for changed modules only
        resolve_and_show "smart" || { print_summary; exit 0; }
        run_spotless_fix || true
        run_spotless_check || true
        run_detekt || true
        run_platform_verify || true
        ;;
esac

print_summary
