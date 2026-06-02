#!/usr/bin/env bash
# audit-kdoc-coverage.sh — enumerate public symbols without preceding KDoc block.
#
# Authored 2026-06-02 by kmp-toolkit-docs-mkdocs-cookbook sub-plan, Tier A T1.
# Per Locked Decision D7: 100% KDoc coverage gate.
#
# Usage:
#   bash scripts/audit-kdoc-coverage.sh                       # scan all cmp-* modules
#   bash scripts/audit-kdoc-coverage.sh --modules 'cmp-share' # single module
#   bash scripts/audit-kdoc-coverage.sh --include-test        # include commonTest
#
# Exit codes:
#   0 — every public symbol has a preceding `/** ... */` block (or @file:OptIn etc.)
#   1 — ≥1 public symbol is missing KDoc; details printed to stdout
#
# Heuristic:
#   A public symbol is "documented" if the nearest non-blank, non-annotation line
#   above its declaration is the closing `*/` of a doc-comment.
#   Annotations (`@OptIn(...)`, `@JsExport`, etc.) between the doc-comment and
#   the symbol are allowed.

set -uo pipefail

INCLUDE_TEST=0
TARGET_GLOB="cmp-*"
while [[ $# -gt 0 ]]; do
    case "$1" in
        --modules) TARGET_GLOB="$2"; shift 2 ;;
        --include-test) INCLUDE_TEST=1; shift ;;
        -h|--help) sed -n '2,18p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
        *) echo "Unknown arg: $1" >&2; exit 2 ;;
    esac
done

cd "$(dirname "$0")/.." || exit 2

missing_total=0
files_checked=0

for module_dir in $TARGET_GLOB; do
    [[ -d "$module_dir/src/commonMain" ]] || continue
    SRC_DIRS=("$module_dir/src/commonMain")
    [[ $INCLUDE_TEST -eq 1 && -d "$module_dir/src/commonTest" ]] && SRC_DIRS+=("$module_dir/src/commonTest")

    while IFS= read -r file; do
        files_checked=$((files_checked + 1))
        awk '
            # Track whether the last non-blank, non-annotation line was the closing `*/` of a doc-block.
            /^[[:space:]]*\*\// { last_was_doc_close=1; next }
            /^[[:space:]]*@/   { next }            # annotation — skip without resetting last_was_doc_close
            /^[[:space:]]*$/    { next }            # blank
            # Public declaration of interest (fun/class/object/interface/sealed/enum/typealias/val/var)
            /^[[:space:]]*public[[:space:]]+(expect[[:space:]]+|actual[[:space:]]+|external[[:space:]]+|inline[[:space:]]+|suspend[[:space:]]+|tailrec[[:space:]]+|operator[[:space:]]+|infix[[:space:]]+)*(fun|class|object|interface|sealed[[:space:]]+(class|interface)|enum[[:space:]]+class|data[[:space:]]+(class|object)|annotation[[:space:]]+class|typealias|val|var)[[:space:]]+/ {
                if (!last_was_doc_close) {
                    printf "%s:%d:%s\n", FILENAME, NR, $0
                    missing++
                }
                last_was_doc_close=0
                next
            }
            { last_was_doc_close=0 }
            END { exit (missing > 0 ? 1 : 0) }
        ' "$file" || missing_total=$((missing_total + 1))
    done < <(find "${SRC_DIRS[@]}" -name '*.kt' 2>/dev/null)
done

echo ""
echo "----------------------------------------"
echo "audit-kdoc-coverage results:"
echo "  Files scanned: ${files_checked}"
echo "  Files with missing-KDoc symbols: ${missing_total}"
echo "----------------------------------------"

[[ $missing_total -eq 0 ]] && exit 0 || exit 1
