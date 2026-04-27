#!/usr/bin/env bash
# ci-local.sh — Run CI checks locally before pushing.
# Mirrors the exact task list that GitHub Actions (mbl-actionhub v1.0.3+) will run.
#
# Usage:
#   ./scripts/ci-local.sh             # quality + JVM + Linux tests (fast, ~2 min)
#   ./scripts/ci-local.sh --all       # + iOS Simulator (slow, ~10 min)
#   ./scripts/ci-local.sh --module cmp-deep-link  # single module
#   ./scripts/ci-local.sh --act       # run via act (requires Docker)

set -euo pipefail

PATTERN="cmp-"
SKIP_TEMPLATE="${PATTERN}library"
RUN_IOS=false
RUN_ACT=false
FILTER_MODULE=""

while [[ $# -gt 0 ]]; do
  case $1 in
    --all)    RUN_IOS=true ;;
    --act)    RUN_ACT=true ;;
    --module) FILTER_MODULE="$2"; shift ;;
    *) echo "Unknown flag: $1"; exit 1 ;;
  esac
  shift
done

cd "$(git rev-parse --show-toplevel)"

GREEN='\033[0;32m'; RED='\033[0;31m'; YELLOW='\033[1;33m'; NC='\033[0m'; BOLD='\033[1m'
PASS=0; FAIL=0

run_step() {
  local name="$1"; shift
  printf "  %-40s" "$name"
  if output=$("$@" 2>&1); then
    printf "${GREEN}PASS${NC}\n"
    PASS=$((PASS+1))
  else
    printf "${RED}FAIL${NC}\n"
    echo "$output" | tail -20
    FAIL=$((FAIL+1))
  fi
}

# ─── act mode ────────────────────────────────────────────────────────────────
if [ "$RUN_ACT" = true ]; then
  if ! command -v act &>/dev/null; then
    echo "act not found. Install: brew install act"
    exit 1
  fi
  if ! docker info &>/dev/null 2>&1; then
    echo "Docker not running. Start Docker Desktop first."
    exit 1
  fi
  echo -e "${BOLD}Running CI via act (Docker)...${NC}"
  act push --job ci -W .github/workflows/gradle.yml \
    --container-architecture linux/amd64 \
    -P ubuntu-latest=catthehacker/ubuntu:act-latest \
    -P macos-14=self-hosted
  exit $?
fi

# ─── Direct Gradle mode (no Docker needed) ───────────────────────────────────
echo -e "${BOLD}=== KMP Toolkit Local CI ===${NC}"
echo ""

# Discover modules
get_modules() {
  local list=""
  for dir in ${PATTERN}*/; do
    [ "${dir%/}" = "$SKIP_TEMPLATE" ] && continue
    [ -d "$dir" ] || continue
    local m="${dir%/}"
    [ -n "$FILTER_MODULE" ] && [ "$m" != "$FILTER_MODULE" ] && continue
    list="$list $m"
  done
  echo "$list"
}

# Filter modules that have a given source-set dir
has_src() { [ -d "${1}/src/${2}" ]; }

build_tasks() {
  local target="$1" src_dir="$2"
  local tasks=""
  for m in $(get_modules); do
    if [ -z "$src_dir" ] || has_src "$m" "$src_dir"; then
      tasks="$tasks :${m}:${target}"
    fi
  done
  echo "$tasks"
}

# ── Quality ──────────────────────────────────────────────────────────────────
echo -e "${BOLD}[1/4] Quality${NC}"
run_step "spotlessCheck" ./gradlew spotlessCheck -q
run_step "detekt" ./gradlew detekt -q

# ── JVM tests ────────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}[2/4] JVM Tests${NC}"
JVM_TASKS=$(build_tasks jvmTest jvmMain)
if [ -z "$JVM_TASKS" ]; then
  echo "  No modules with jvmMain — skipping"
else
  echo "  Modules:$(echo $JVM_TASKS | sed 's/ :/\n    /g; s/:[a-zA-Z]*//' | grep -v '^$' | tr '\n' ' ')"
  run_step "jvmTest" ./gradlew $JVM_TASKS --parallel -q
fi

# ── Linux tests ──────────────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}[3/4] Linux Native Tests${NC}"
LINUX_TASKS=$(build_tasks linuxX64Test linuxMain)
if [ -z "$LINUX_TASKS" ]; then
  echo "  No modules with linuxMain — skipping"
else
  run_step "linuxX64Test" ./gradlew $LINUX_TASKS --parallel -q
fi

# ── iOS tests (opt-in) ───────────────────────────────────────────────────────
echo ""
echo -e "${BOLD}[4/4] iOS Simulator Tests${NC}"
if [ "$RUN_IOS" = false ]; then
  echo "  Skipped (pass --all to include)"
else
  IOS_TASKS=$(build_tasks iosSimulatorArm64Test "")
  if [ -z "$IOS_TASKS" ]; then
    echo "  No modules — skipping"
  else
    run_step "iosSimulatorArm64Test" ./gradlew $IOS_TASKS --parallel -q
  fi
fi

# ── Summary ──────────────────────────────────────────────────────────────────
echo ""
echo "─────────────────────────────────────────"
if [ $FAIL -eq 0 ]; then
  echo -e "${GREEN}${BOLD}All checks passed ($PASS)${NC} — safe to push"
  exit 0
else
  echo -e "${RED}${BOLD}$FAIL check(s) failed${NC} ($PASS passed) — fix before pushing"
  exit 1
fi
