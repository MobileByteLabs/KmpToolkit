#!/usr/bin/env bash
# ci-local.sh — Run CI checks locally before pushing.
# Delegates to the framework-level ci-local.sh (mbl-actionhub v1.1.0 fast gate).
#
# Usage:
#   ./scripts/ci-local.sh                  # fast gate: quality + JVM + Linux (~3 min)
#   ./scripts/ci-local.sh --all            # + iOS Simulator (slow gate, ~15 min)
#   ./scripts/ci-local.sh --module cmp-deep-link
#   ./scripts/ci-local.sh --act            # run via act (requires Docker)

set -euo pipefail

# ── Locate framework root (walk up until FRAMEWORK_GRAPH.yaml found) ─────────
DIR="$(cd "$(dirname "$0")" && pwd)"
while [ "$DIR" != "/" ]; do
  [ -f "$DIR/FRAMEWORK_GRAPH.yaml" ] && break
  DIR="$(dirname "$DIR")"
done

FRAMEWORK_CI="${DIR}/layers/ci/scripts/ci-local.sh"

if [ ! -f "$FRAMEWORK_CI" ]; then
  echo "  [ERROR] Framework ci-local.sh not found at: $FRAMEWORK_CI"
  echo "  Ensure claude-product-cycle framework is set up."
  exit 1
fi

# Project root = one level up from scripts/ (reliable, no git dependency)
PROJECT_DIR="$(cd "$(dirname "$0")/.." && pwd)"
exec bash "$FRAMEWORK_CI" --project "$PROJECT_DIR" "$@"
