#!/usr/bin/env bash
# =============================================================================
# KmpToolkit — Unified Release Script
# =============================================================================
# Single script for the complete release pipeline:
#   1. Run quality gates (spotless + detekt + tests)
#   2. Merge development → main (with PR fallback)
#   3. Create git tag vX.Y.Z on main
#   4. Push tag → GitHub Actions → GitHub Release → Maven Central publish
#
# Usage:
#   ./scripts/release.sh                          # auto-detect version, full release
#   ./scripts/release.sh --version 0.2.0          # explicit version
#   ./scripts/release.sh --bump minor             # auto-bump (major|minor|patch)
#   ./scripts/release.sh --module cmp-clipboard   # release single module
#   ./scripts/release.sh --dry-run                # checks only, no merge/tag/push
#   ./scripts/release.sh --skip-merge             # skip development→main merge
#   ./scripts/release.sh --local                  # also publish to local Maven (~/.m2)
#   ./scripts/release.sh list                     # list publishable modules
#   ./scripts/release.sh verify                   # run quality gates only
#
# Prerequisites:
#   - Credentials JSON (--credentials or KMPTOOLKIT_CREDENTIALS env)
#   - git clean (no uncommitted changes)
#   - gh CLI installed (for PR merge fallback + GitHub Release)
#
# Release Flow:
#   verify.sh --quick → merge dev→main → tag → push → CI → Release → Maven
# =============================================================================
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
ROOT_DIR="$(cd "$SCRIPT_DIR/.." && pwd)"

# Colors
RED='\033[0;31m'; GREEN='\033[0;32m'; YELLOW='\033[1;33m'
BLUE='\033[0;34m'; CYAN='\033[0;36m'; BOLD='\033[1m'; NC='\033[0m'

log_step()    { echo -e "\n${BLUE}▶${NC}  ${BOLD}$1${NC}"; }
log_pass()    { echo -e "   ${GREEN}✅${NC} $1"; }
log_warn()    { echo -e "   ${YELLOW}⚠️${NC}  $1"; }
log_fail()    { echo -e "   ${RED}❌${NC} $1"; exit 1; }

# =============================================================================
# Parse Arguments
# =============================================================================
EXPLICIT_VERSION=""
TARGET_MODULE=""
CREDS_FILE="${KMPTOOLKIT_CREDENTIALS:-}"
DRY_RUN=false
SKIP_MERGE=false
LOCAL_MAVEN=false
BUMP_TYPE=""
SKIP_CONFIRM=false
COMMAND=""

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version)     EXPLICIT_VERSION="$2"; shift 2 ;;
        --module)      TARGET_MODULE="$2"; shift 2 ;;
        --credentials) CREDS_FILE="$2"; shift 2 ;;
        --dry-run)     DRY_RUN=true; shift ;;
        --skip-merge)  SKIP_MERGE=true; shift ;;
        --local)       LOCAL_MAVEN=true; shift ;;
        --bump)        BUMP_TYPE="$2"; shift 2 ;;
        -y)            SKIP_CONFIRM=true; shift ;;
        list|verify|local|help)
            COMMAND="$1"; shift ;;
        *) shift ;;
    esac
done

[ -z "$COMMAND" ] && COMMAND="release"
cd "$ROOT_DIR"

# =============================================================================
# Module Discovery
# =============================================================================
discover_modules() {
    local filter="${1:-}"
    for dir in cmp-*/; do
        if [ -d "$dir" ] && grep -q "mavenPublishing" "${dir}build.gradle.kts" 2>/dev/null; then
            local module="${dir%/}"
            if [ -n "$filter" ] && [ "$module" != "$filter" ]; then continue; fi
            echo "$module"
        fi
    done
}

get_version()  { grep -m1 'version = ' "$1/build.gradle.kts" | sed 's/.*"\(.*\)".*/\1/'; }
get_artifact() { grep 'coordinates(' "$1/build.gradle.kts" | grep -oE '"kmp[^"]*"|"kmptoolkit[^"]*"' | head -1 | tr -d '"'; }
get_group()    { echo "io.github.mobilebytelabs"; }

# =============================================================================
# Credentials
# =============================================================================
load_credentials() {
    if [ -z "$CREDS_FILE" ] || [ ! -f "$CREDS_FILE" ]; then
        log_warn "No credentials file — local Maven and signing unavailable"
        return
    fi

    export ORG_GRADLE_PROJECT_mavenCentralUsername=$(python3 -c "import json; print(json.load(open('$CREDS_FILE'))['maven_central']['username'])")
    export ORG_GRADLE_PROJECT_mavenCentralPassword=$(python3 -c "import json; print(json.load(open('$CREDS_FILE'))['maven_central']['password'])")

    local gpg_key_id=$(python3 -c "import json; print(json.load(open('$CREDS_FILE'))['gpg']['key_id'])")
    local gpg_passphrase=$(python3 -c "import json; print(json.load(open('$CREDS_FILE'))['gpg']['passphrase'])")
    local gpg_full_key_id=$(python3 -c "import json; print(json.load(open('$CREDS_FILE'))['gpg']['full_key_id'])")

    export ORG_GRADLE_PROJECT_signingInMemoryKeyId="$gpg_key_id"
    export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$gpg_passphrase"
    export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --batch --yes --pinentry-mode loopback --passphrase "$gpg_passphrase" --export-secret-keys --armor "$gpg_full_key_id" 2>/dev/null)"

    log_pass "Credentials loaded"
}

# =============================================================================
# list — Show all publishable modules
# =============================================================================
cmd_list() {
    echo ""
    echo -e "${BOLD}  Publishable Modules${NC}"
    echo ""
    printf "  %-22s %-40s %s\n" "MODULE" "ARTIFACT" "VERSION"
    printf "  %-22s %-40s %s\n" "──────" "────────" "───────"
    for module in $(discover_modules "$TARGET_MODULE"); do
        printf "  %-22s %-40s %s\n" "$module" "$(get_group "$module"):$(get_artifact "$module")" "$(get_version "$module")"
    done
    echo ""
}

# =============================================================================
# verify — Run quality gates (uses verify.sh)
# =============================================================================
cmd_verify() {
    log_step "[Gate] Running quality checks..."
    "$SCRIPT_DIR/verify.sh" --quick
}

# =============================================================================
# local — Publish to Maven Local
# =============================================================================
cmd_local() {
    load_credentials
    log_step "Publishing to local Maven (~/.m2)..."
    for module in $(discover_modules "$TARGET_MODULE"); do
        ./gradlew ":${module}:publishToMavenLocal" --no-configuration-cache --quiet 2>/dev/null
        log_pass "$module → ~/.m2 ($(get_group "$module"):$(get_artifact "$module"):$(get_version "$module"))"
    done
    echo ""
    log_pass "Use with: repositories { mavenLocal() }"
}

# =============================================================================
# release — Full release pipeline
# =============================================================================
cmd_release() {
    local github_repo
    github_repo=$(python3 -c "import json; print(json.load(open('$CREDS_FILE'))['github']['repo'])" 2>/dev/null || echo "MobileByteLabs/KmpToolkit")

    # ── Resolve version ──────────────────────────────────────────
    local modules=($(discover_modules "$TARGET_MODULE"))
    local first_module="${modules[0]}"
    local current_version=$(get_version "$first_module")

    if [ -n "$EXPLICIT_VERSION" ]; then
        VERSION="$EXPLICIT_VERSION"
    elif [ -n "$BUMP_TYPE" ]; then
        IFS='.' read -r major minor patch <<< "$current_version"
        case $BUMP_TYPE in
            major) major=$((major + 1)); minor=0; patch=0 ;;
            minor) minor=$((minor + 1)); patch=0 ;;
            patch) patch=$((patch + 1)) ;;
        esac
        VERSION="$major.$minor.$patch"
    else
        VERSION="$current_version"
    fi

    TAG="v$VERSION"

    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  ${BOLD}KmpToolkit Release: $TAG${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    for module in "${modules[@]}"; do
        echo "    • $module  $(get_group "$module"):$(get_artifact "$module"):$VERSION"
    done
    echo ""

    # ── Pre-flight checks ────────────────────────────────────────
    if ! git diff --quiet 2>/dev/null || ! git diff --cached --quiet 2>/dev/null; then
        log_fail "Uncommitted changes. Commit or stash first."
    fi

    # ── Step 1: Quality gates ────────────────────────────────────
    log_step "[1/6] Quality gates (verify.sh --quick)..."
    "$SCRIPT_DIR/verify.sh" --quick

    if [ "$DRY_RUN" = true ]; then
        echo ""
        log_pass "Dry run complete. All checks passed for $TAG."
        echo "   Re-run without --dry-run to merge, tag, and push."
        exit 0
    fi

    # ── Step 2: Bump versions ────────────────────────────────────
    if [ "$VERSION" != "$current_version" ]; then
        log_step "[2/6] Bumping: $current_version → $VERSION..."
        for module in "${modules[@]}"; do
            sed -i '' "s/version = \"$current_version\"/version = \"$VERSION\"/" "${module}/build.gradle.kts"
        done
        git add -A
        git commit -m "chore: bump version to $VERSION" --no-verify
        log_pass "Versions bumped to $VERSION"
    else
        log_step "[2/6] Version unchanged ($VERSION)"
    fi

    # ── Step 3: Merge development → main ─────────────────────────
    if [ "$SKIP_MERGE" = false ]; then
        log_step "[3/6] Merging development → main..."
        git fetch origin development main 2>/dev/null || true

        local behind=$(git rev-list origin/main..origin/development --count 2>/dev/null || echo "0")

        if [ "$behind" = "0" ]; then
            log_pass "main is up-to-date with development"
        else
            echo "   development is $behind commit(s) ahead"
            git checkout main
            git pull origin main --ff-only

            if git merge --ff-only origin/development 2>/dev/null; then
                git push origin main
                log_pass "Fast-forward merge succeeded"
            else
                log_warn "Fast-forward not possible — creating PR..."
                if ! command -v gh &>/dev/null; then
                    log_fail "gh CLI required. Install: https://cli.github.com"
                fi
                local pr_url=$(gh pr create --repo "$github_repo" \
                    --base main --head development \
                    --title "chore: release $TAG" \
                    --body "Automated merge for release $TAG" 2>&1 | tail -1)
                local pr_number=$(echo "$pr_url" | grep -oE '[0-9]+$')
                gh pr merge "$pr_number" --repo "$github_repo" --merge --admin
                git pull origin main --ff-only
                log_pass "Merged via PR #$pr_number"
            fi
        fi

        # Ensure on main
        if [ "$(git rev-parse --abbrev-ref HEAD)" != "main" ]; then
            git checkout main && git pull origin main --ff-only
        fi
    else
        log_step "[3/6] Skipping merge (--skip-merge)"
    fi

    # ── Step 4: Check tag ────────────────────────────────────────
    log_step "[4/6] Checking tag $TAG..."
    if git tag -l | grep -q "^$TAG$"; then
        log_fail "Tag $TAG already exists locally"
    fi
    if git ls-remote --tags origin 2>/dev/null | grep -q "refs/tags/$TAG$"; then
        log_fail "Tag $TAG already exists on remote"
    fi
    log_pass "Tag $TAG is available"

    # ── Step 5: Local Maven (optional) ───────────────────────────
    if [ "$LOCAL_MAVEN" = true ]; then
        log_step "[5/6] Publishing to local Maven..."
        load_credentials
        cmd_local
    else
        log_step "[5/6] Skipping local Maven (use --local)"
    fi

    # ── Step 6: Tag + push + GitHub Release ──────────────────────
    log_step "[6/6] Creating tag and pushing..."
    git tag -a "$TAG" -m "KmpToolkit $TAG"
    git push origin "$TAG"
    log_pass "Tag $TAG pushed to origin"

    # GitHub Release (triggers publish.yml)
    if command -v gh &>/dev/null; then
        local notes="## Modules\n\n"
        for module in "${modules[@]}"; do
            notes+="- \`$(get_group "$module"):$(get_artifact "$module"):$VERSION\`\n"
        done
        notes+="\n## Installation\n\n\`\`\`kotlin\ncommonMain.dependencies {\n"
        for module in "${modules[@]}"; do
            notes+="    implementation(\"$(get_group "$module"):$(get_artifact "$module"):$VERSION\")\n"
        done
        notes+="}\n\`\`\`\n"

        gh release create "$TAG" --repo "$github_repo" \
            --title "$TAG" --notes "$(echo -e "$notes")" 2>/dev/null && \
            log_pass "GitHub Release created" || \
            log_warn "GitHub Release failed — create manually"
    fi

    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  ${GREEN}${BOLD}$TAG released!${NC}"
    echo -e "${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}  GitHub Actions will now:"
    echo -e "${CYAN}║${NC}    1. CI workflow → quality + build all targets"
    echo -e "${CYAN}║${NC}    2. Publish workflow → Maven Central (~10 min)"
    echo -e "${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}  Track: https://github.com/$github_repo/actions"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# =============================================================================
# Help
# =============================================================================
cmd_help() {
    echo ""
    echo -e "${BOLD}KmpToolkit — Release Script${NC}"
    echo ""
    echo "  ./scripts/release.sh                          Full release"
    echo "  ./scripts/release.sh --bump minor             Auto-bump + release"
    echo "  ./scripts/release.sh --version 0.2.0          Explicit version"
    echo "  ./scripts/release.sh --dry-run                Checks only"
    echo "  ./scripts/release.sh --local                  + local Maven"
    echo "  ./scripts/release.sh --module cmp-clipboard   Single module"
    echo "  ./scripts/release.sh list                     List modules"
    echo "  ./scripts/release.sh verify                   Quality gates"
    echo "  ./scripts/release.sh help                     This help"
    echo ""
}

# =============================================================================
# Router
# =============================================================================
case "$COMMAND" in
    list)    cmd_list ;;
    verify)  cmd_verify ;;
    local)   cmd_local ;;
    help)    cmd_help ;;
    release) cmd_release ;;
    *)       cmd_help ;;
esac
