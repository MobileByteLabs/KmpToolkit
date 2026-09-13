#!/usr/bin/env bash
#
# Asserts every cmp-* module ships complete, non-dangling docs.
#
# The failures this catches, all of which actually happened in this repo:
#   D1  a module with no README.md at all — 13 of 27 were missing one, invisible because nothing
#       ever looked. A module copied from cmp-library inherits the template's docs and it is easy
#       to ship without ever writing your own.
#   D2  docs that do not point at TARGET_MATRIX.md, the single source of truth for which KMP
#       targets a module must ship. Without the pointer the next author guesses the matrix.
#   D3  a relative link to a file that does not exist. These are written by hand, they rot when a
#       module is renamed or split (cmp-observe -> cmp-observe-firebase), and a dead link in a
#       README is invisible until a reader clicks it.
#
# Relative links climbing three or more levels (`../../../…`) are SKIPPED: DEVELOPMENT.md links its
# GOAL.md in the framework superproject, which is not part of this repository, so such a target can
# never resolve from a repo checkout and is not a defect in the doc. Everything that stays inside the
# repo is checked.
#
# NOT checked: prose accuracy. A README can still name an API that does not exist — that is a
# review concern, not a mechanical one. (It happened here: four invented symbol names, caught by
# grepping each claim against the source. Worth doing by hand when you write one.)
#
# bash 3.2 compatible (macOS runners): no mapfile, no associative arrays, no ${ARR[@]} under set -u.

set -euo pipefail
cd "$(dirname "$0")/../.."

FAILED=0
CHECKED=0

for gradle_file in cmp-*/build.gradle.kts; do
    module="$(dirname "$gradle_file")"
    CHECKED=$((CHECKED + 1))

    # ── D1: both docs present ───────────────────────────────────────────────────────────────
    for doc in README.md DEVELOPMENT.md; do
        if [ ! -f "$module/$doc" ]; then
            echo "FAIL $module: no $doc"
            echo "     → every module ships both; scaffold DEVELOPMENT.md with"
            echo "       .claude-runtime/scripts/development-md-bootstrap.sh --workspace mbs/kmp-toolkit --apply"
            FAILED=$((FAILED + 1))
        fi
    done

    # ── D2: both reference the target SoT ───────────────────────────────────────────────────
    for doc in README.md DEVELOPMENT.md; do
        [ -f "$module/$doc" ] || continue
        if ! grep -q 'TARGET_MATRIX' "$module/$doc"; then
            echo "FAIL $module/$doc: no reference to TARGET_MATRIX.md"
            echo "     → it is the single source of truth for this module's target set"
            FAILED=$((FAILED + 1))
        fi
    done

    # ── D3: relative links resolve ──────────────────────────────────────────────────────────
    for doc in README.md DEVELOPMENT.md; do
        [ -f "$module/$doc" ] || continue
        # Extract ](target) for relative targets only: skip http(s), mailto and #anchors.
        grep -oE '\]\([^)#]+\)' "$module/$doc" 2>/dev/null \
            | sed -e 's/^](//' -e 's/)$//' \
            | grep -vE '^(https?:|mailto:|#)' \
            | sed 's/#.*$//' \
            | grep -vE '^(\.\./){3,}' \
            | sort -u \
            | while IFS= read -r target; do
                [ -z "$target" ] && continue
                if [ ! -e "$module/$target" ]; then
                    echo "FAIL $module/$doc: dead link -> $target"
                fi
            done > /tmp/docs-deadlinks-$$ 2>/dev/null || true
        if [ -s /tmp/docs-deadlinks-$$ ]; then
            cat /tmp/docs-deadlinks-$$
            FAILED=$((FAILED + $(grep -c . /tmp/docs-deadlinks-$$)))
        fi
        rm -f /tmp/docs-deadlinks-$$
    done
done

if [ "$CHECKED" -eq 0 ]; then
    echo "FAIL no cmp-*/build.gradle.kts matched — the glob or the layout changed"
    exit 1
fi

echo
if [ "$FAILED" -gt 0 ]; then
    echo "docs gate: $FAILED problem(s) across $CHECKED module(s)"
    exit 1
fi
echo "docs gate: all $CHECKED module(s) have complete docs with resolving links"
