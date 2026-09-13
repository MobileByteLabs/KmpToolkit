#!/usr/bin/env bash
#
# Asserts that every module generating CmpMetadata actually REPORTS something.
#
# The failure this catches: applying `cmp-observe-metadata.gradle.kts` and depending on cmp-observe
# is cheap and looks like integration in a diff, but a module that never calls observeInit /
# observeLifecycle / observeClose emits nothing at runtime. That gap is invisible — the build is
# green, the dependency is present, and a consumer registering a hook simply sees no events from
# that library and cannot tell whether the library is quiet or unwired. Before this gate the
# integration sat at 1 of 23 modules for four months without any signal.
#
# Passes when, for each module applying the metadata generator:
#   - it declares cmp-observe in commonMain, AND
#   - at least one non-import call to observeInit / observeLifecycle / observeClose exists in src/
#
# Deliberately NOT checked: the `-compose` modules. Each delegates to its headless core, which is
# where reporting lives; reporting again in the Compose wrapper would double-count every event.
# They are excluded by name rather than silently passing on an empty result.
#
# bash 3.2 compatible (macOS runners): no mapfile, no associative arrays, no ${ARR[@]} under set -u.

set -euo pipefail
cd "$(dirname "$0")/../.."

FAILED=0
CHECKED=0

for gradle_file in cmp-*/build.gradle.kts; do
    module="$(dirname "$gradle_file")"

    # Only modules that opt into metadata generation are in scope.
    grep -q 'cmp-observe-metadata.gradle.kts' "$gradle_file" || continue

    # -compose wrappers delegate to their core; see the note above.
    case "$module" in
        *-compose) continue ;;
    esac

    CHECKED=$((CHECKED + 1))

    if ! grep -q 'project(":cmp-observe")' "$gradle_file"; then
        echo "FAIL $module: applies cmp-observe-metadata.gradle.kts but does not depend on cmp-observe"
        echo "     → the generated cmpMetadata() factory is only emitted when commonMain can see the type"
        FAILED=$((FAILED + 1))
        continue
    fi

    # Count real call sites: strip import lines so an unused import cannot satisfy the gate.
    calls=$(grep -rh --include='*.kt' -E 'observeInit|observeLifecycle|observeClose' "$module/src" 2>/dev/null \
        | grep -v '^[[:space:]]*import ' \
        | grep -c . || true)
    calls=${calls:-0}

    if [ "$calls" -eq 0 ]; then
        echo "FAIL $module: generates CmpMetadata and depends on cmp-observe, but never reports"
        echo "     → add observeInit(cmpMetadata()) { … } at init, or observeLifecycle(…) on its primary operation"
        FAILED=$((FAILED + 1))
    else
        echo "ok   $module ($calls call sites)"
    fi
done

if [ "$CHECKED" -eq 0 ]; then
    echo "FAIL no modules applied cmp-observe-metadata.gradle.kts — the glob or the convention changed"
    exit 1
fi

echo
if [ "$FAILED" -gt 0 ]; then
    echo "observability gate: $FAILED of $CHECKED module(s) unwired"
    exit 1
fi
echo "observability gate: all $CHECKED module(s) report"
