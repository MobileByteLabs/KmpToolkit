#!/usr/bin/env bash
# Render the aggregated Kover result into the GitHub job summary.
#
# The XML artifact is only useful to someone who downloads and opens it. Printing the headline
# number into the run summary makes a coverage change visible in the PR itself, which is the point
# of running coverage on every PR rather than nightly.
#
# Deliberately does NOT fail the build: gating is `koverVerify`'s job (see pr-check.yml), and a
# summary step that can fail would mask the real test result behind a reporting error.
set -uo pipefail

# The ROOT report is the aggregated one. Every module ALSO emits its own report.xml, so a
# `find … -print -quit` would post whichever the filesystem happened to yield first — a
# single module's number masquerading as the project's. Take the root explicitly.
REPORT="build/reports/kover/report.xml"
if [ ! -f "$REPORT" ]; then
  REPORT=""
fi

if [ -z "$REPORT" ]; then
  echo "::warning::No Kover XML report found — nothing to summarise."
  {
    echo "### Kover Coverage"
    echo ""
    echo "No report produced. If \`koverXmlReport\` succeeded, the aggregation may have no"
    echo "registered modules — check that leaf modules apply the kover convention plugin."
  } >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
  exit 0
fi

read -r COVERED MISSED < <(
  python3 - "$REPORT" <<'PY'
import sys, xml.etree.ElementTree as ET
root = ET.parse(sys.argv[1]).getroot()
covered = missed = 0
# Only the report-level LINE counter — per-package counters would double count.
for counter in root.findall('counter'):
    if counter.get('type') == 'LINE':
        covered = int(counter.get('covered', 0))
        missed = int(counter.get('missed', 0))
print(covered, missed)
PY
)

TOTAL=$(( COVERED + MISSED ))
if [ "$TOTAL" -eq 0 ]; then
  PCT="n/a"
else
  PCT="$(( 100 * COVERED / TOTAL ))%"
fi

echo "Kover: ${COVERED}/${TOTAL} lines covered (${PCT})"

{
  echo "### Kover Coverage"
  echo ""
  echo "| Metric | Value |"
  echo "|---|---|"
  echo "| Lines covered | ${COVERED} |"
  echo "| Lines missed | ${MISSED} |"
  echo "| Total | ${TOTAL} |"
  echo "| **Coverage** | **${PCT}** |"
  echo ""
  echo "Aggregated at the root across every module applying the kover convention plugin."
  echo "Full HTML/XML report is attached to this run as the \`kover-report\` artifact."
} >> "${GITHUB_STEP_SUMMARY:-/dev/null}"
