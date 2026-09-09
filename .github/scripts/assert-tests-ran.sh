#!/usr/bin/env bash
# Fail unless a test task actually executed tests.
#
# A green test task is not evidence that tests ran. Gradle reports success when a task has no
# test sources wired, when a filter matches nothing, or when a target silently no-ops — and the
# run looks identical to a real pass. Every such case reads as "verified" while verifying
# nothing.
#
# Usage: assert-tests-ran.sh <task-dir-name> [min-tests]
#   e.g. assert-tests-ran.sh iosSimulatorArm64Test
#
# Reads the JUnit XML that Kotlin/Native test tasks emit under
# **/build/test-results/<task>/ and requires a positive total.
set -euo pipefail

TASK="${1:?usage: assert-tests-ran.sh <task-dir-name> [min-tests]}"
MIN="${2:-1}"

mapfile -t FILES < <(find . -path "*/build/test-results/${TASK}/*.xml" -type f 2>/dev/null || true)

if [ "${#FILES[@]}" -eq 0 ]; then
  echo "::error::No test-result XML found for '${TASK}'. The task reported success but produced"
  echo "::error::no results — nothing was executed. Treating as failure, not as a pass."
  exit 1
fi

TOTAL=0
FAILED=0
for f in "${FILES[@]}"; do
  # Each suite root carries tests= / failures= / errors= attributes.
  t=$(grep -o 'tests="[0-9]*"' "$f" | head -1 | grep -o '[0-9]*' || echo 0)
  fl=$(grep -o 'failures="[0-9]*"' "$f" | head -1 | grep -o '[0-9]*' || echo 0)
  er=$(grep -o 'errors="[0-9]*"' "$f" | head -1 | grep -o '[0-9]*' || echo 0)
  TOTAL=$((TOTAL + t))
  FAILED=$((FAILED + fl + er))
done

echo "${TASK}: ${TOTAL} tests executed across ${#FILES[@]} suite file(s), ${FAILED} failing."

if [ "$TOTAL" -lt "$MIN" ]; then
  echo "::error::${TASK} executed ${TOTAL} tests (minimum ${MIN}). A run that executes nothing"
  echo "::error::must not report green — see the 3.5.23 incident where an Apple-invalid"
  echo "::error::assertion shipped because no Apple test ever ran."
  exit 1
fi

if [ "$FAILED" -gt 0 ]; then
  echo "::error::${TASK} had ${FAILED} failing test(s)."
  exit 1
fi

{
  echo "### ${TASK}"
  echo ""
  echo "| Metric | Value |"
  echo "|---|---|"
  echo "| Tests executed | ${TOTAL} |"
  echo "| Suite files | ${#FILES[@]} |"
  echo "| Failures | ${FAILED} |"
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
