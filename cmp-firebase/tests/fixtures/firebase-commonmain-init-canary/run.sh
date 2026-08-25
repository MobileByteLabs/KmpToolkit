#!/usr/bin/env bash
# Canary — proves the MP-tier auto-wire guard for cmp-firebase commonMain init.
# GREEN (config-reading actual) must read `measurementProtocol`; RED (hard-NoOp)
# must NOT. A future regression that reverts the actual to hard-NoOp fails here.
set -euo pipefail
DIR="$(cd "$(dirname "$0")" && pwd)"

green="$DIR/green/AnalyticsProvider.nonfirebase.kt"
red="$DIR/red/AnalyticsProvider.nonfirebase.kt"

if ! grep -q "measurementProtocol" "$green"; then
  echo "FAIL: GREEN fixture must read config.measurementProtocol (auto-wire)"; exit 1
fi
if grep -q "measurementProtocol" "$red"; then
  echo "FAIL: RED fixture must NOT read config.measurementProtocol (hard-NoOp bug)"; exit 1
fi
# Cross-check the live source matches the GREEN contract.
live="$DIR/../../../src/nonFirebaseMain/kotlin/io/github/mobilebytelabs/kmptoolkit/firebase/analytics/AnalyticsProvider.nonfirebase.kt"
if [ -f "$live" ] && ! grep -q "measurementProtocol" "$live"; then
  echo "FAIL: live nonFirebase provider regressed to hard-NoOp"; exit 1
fi
echo "canary OK"
