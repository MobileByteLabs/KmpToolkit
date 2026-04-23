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
#   ./scripts/release.sh --env /path/to/.env      # explicit .env path (auto-discovered by default)
#   ./scripts/release.sh list                     # list publishable modules
#   ./scripts/release.sh verify                   # run quality gates only
#   ./scripts/release.sh publish                  # direct publish to Maven Central (no CI)
#
# Prerequisites:
#   - .env file with credentials (auto-discovered from workspace root; or --env flag)
#   - Keys required: MAVEN_CENTRAL_USERNAME, MAVEN_CENTRAL_PASSWORD, GPG_KEY_ID,
#                    GPG_FULL_KEY_ID, GPG_PASSPHRASE, GITHUB_REPO
#   - git clean (no uncommitted changes)
#   - gh CLI installed (for PR merge fallback + GitHub Release)
#
# Release Flow:
#   1. verify.sh --quick (quality gates)
#   2. verify.sh --platforms (multi-target verification)
#   3. Maven Local publish gate (MANDATORY — build proof before CI)
#   4. Version bump + commit
#   5. Merge dev→main + tag + push
#   6. GitHub Release (+ RELEASE_STATE markers) → triggers CI → Maven Central
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
ENV_FILE=""
DRY_RUN=false
SKIP_MERGE=false
LOCAL_MAVEN=false
BUMP_TYPE=""
SKIP_CONFIRM=false
COMMAND=""

# Auto-discover .env from workspace root → repo root
for _candidate in \
    "${ROOT_DIR}/../../.env" \
    "${ROOT_DIR}/../.env" \
    "${ROOT_DIR}/.env" \
    "${SCRIPT_DIR}/../.env"; do
    _candidate="$(cd "$(dirname "$_candidate")" 2>/dev/null && pwd)/$(basename "$_candidate")"
    if [[ -f "$_candidate" ]]; then ENV_FILE="$_candidate"; break; fi
done

while [[ $# -gt 0 ]]; do
    case "$1" in
        --version)     EXPLICIT_VERSION="$2"; shift 2 ;;
        --module)      TARGET_MODULE="$2"; shift 2 ;;
        --env)         ENV_FILE="$2"; shift 2 ;;
        --credentials) ENV_FILE="$2"; shift 2 ;;
        --dry-run)     DRY_RUN=true; shift ;;
        --skip-merge)  SKIP_MERGE=true; shift ;;
        --local)       LOCAL_MAVEN=true; shift ;;
        --bump)        BUMP_TYPE="$2"; shift 2 ;;
        -y)            SKIP_CONFIRM=true; shift ;;
        list|verify|local|publish|help)
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
    if [[ -z "$ENV_FILE" || ! -f "$ENV_FILE" ]]; then
        log_warn "No .env found — Maven Central + signing unavailable"
        log_warn "Searched: workspace .env, repo .env"
        log_warn "Pass --env /path/to/.env or set KMPTOOLKIT_ENV env var"
        return
    fi

    # Source only KEY=VALUE lines (skip comments and blank lines)
    set -a
    # shellcheck disable=SC1090
    source <(grep -E '^[A-Z_]+=' "$ENV_FILE" | grep -v '^#')
    set +a

    export ORG_GRADLE_PROJECT_mavenCentralUsername="${MAVEN_CENTRAL_USERNAME}"
    export ORG_GRADLE_PROJECT_mavenCentralPassword="${MAVEN_CENTRAL_PASSWORD}"
    export ORG_GRADLE_PROJECT_signingInMemoryKeyId="${GPG_KEY_ID}"
    export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="${GPG_PASSPHRASE}"
    export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --batch --yes \
        --pinentry-mode loopback --passphrase "${GPG_PASSPHRASE}" \
        --export-secret-keys --armor "${GPG_FULL_KEY_ID}" 2>/dev/null)"

    log_pass "Credentials loaded from $(basename "$ENV_FILE")"
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
# publish — Direct publish to Maven Central (no CI required)
# =============================================================================
cmd_publish() {
    load_credentials

    if [[ -z "${MAVEN_CENTRAL_USERNAME:-}" ]]; then
        log_fail "MAVEN_CENTRAL_USERNAME not set — check your .env file"
    fi

    local modules
    modules=($(discover_modules "$TARGET_MODULE"))

    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  ${BOLD}KmpToolkit — Direct Maven Central Publish${NC}"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
    for module in "${modules[@]}"; do
        echo "    • $module  $(get_group):$(get_artifact "$module"):$(get_version "$module")"
    done
    echo ""
    echo -e "   Target: ${BOLD}Maven Central${NC} (${MAVEN_CENTRAL_STAGING_URL:-https://central.sonatype.com})"
    echo ""

    if [[ "$SKIP_CONFIRM" != true ]]; then
        read -r -p "   Publish now? [y/N] " confirm
        [[ "$confirm" =~ ^[Yy]$ ]] || { echo "   Aborted."; exit 0; }
    fi

    # Step 1: Quality gate (fast)
    log_step "[1/3] Quality gates..."
    "$SCRIPT_DIR/verify.sh" --quick

    # Step 2: Maven Local sign+build gate (proves artifacts are signable)
    log_step "[2/3] Maven Local sign gate..."
    for module in "${modules[@]}"; do
        ./gradlew ":${module}:publishToMavenLocal" \
            --no-configuration-cache --quiet 2>/dev/null
        log_pass "$module signed + built locally"
    done

    # Step 3: Publish directly to Maven Central
    log_step "[3/3] Publishing to Maven Central..."
    for module in "${modules[@]}"; do
        local version
        version=$(get_version "$module")
        local artifact
        artifact=$(get_artifact "$module")

        log_step "  → $(get_group):${artifact}:${version}"
        ./gradlew ":${module}:publishAllPublicationsToMavenCentralRepository" \
            --no-configuration-cache 2>&1 | tail -5
        log_pass "$(get_group):${artifact}:${version} → Maven Central"
    done

    echo ""
    echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
    echo -e "${CYAN}║${NC}  ${GREEN}${BOLD}Published to Maven Central!${NC}"
    echo -e "${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}  Artifacts will be visible at:"
    echo -e "${CYAN}║${NC}    https://central.sonatype.com/namespace/io.github.mobilebytelabs"
    echo -e "${CYAN}║${NC}"
    echo -e "${CYAN}║${NC}  Propagation to search.maven.org: ~10–30 min"
    echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"
    echo ""
}

# =============================================================================
# release — Full release pipeline
# =============================================================================
cmd_release() {
    local github_repo="${GITHUB_REPO:-MobileByteLabs/KmpToolkit}"

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
    log_step "[1/8] Quality gates (verify.sh --quick)..."
    "$SCRIPT_DIR/verify.sh" --quick

    # ── Step 2: Platform verification ────────────────────────────
    log_step "[2/8] Platform verification (verify.sh --platforms)..."
    "$SCRIPT_DIR/verify.sh" --platforms

    # ── Step 3: Maven Local publish gate (MANDATORY) ─────────────
    log_step "[3/8] Maven Local publish gate — build proof..."
    load_credentials
    "$SCRIPT_DIR/verify.sh" --local
    log_pass "All modules build + sign locally — CI will succeed"

    if [ "$DRY_RUN" = true ]; then
        echo ""
        log_pass "Dry run complete. All checks passed for $TAG."
        echo "   Quality ✅  Platforms ✅  Maven Local ✅"
        echo "   Re-run without --dry-run to merge, tag, and push."
        exit 0
    fi

    # ── Step 4: Unify ALL module versions ──────────────────────────
    log_step "[4/8] Setting ALL modules to $VERSION..."
    local any_changed=false
    for module in "${modules[@]}"; do
        local mod_version=$(get_version "$module")
        if [ "$mod_version" != "$VERSION" ]; then
            sed -i '' "s/version = \"$mod_version\"/version = \"$VERSION\"/" "${module}/build.gradle.kts"
            log_pass "$module: $mod_version → $VERSION"
            any_changed=true
        else
            log_pass "$module: already $VERSION"
        fi
    done
    if [ "$any_changed" = true ]; then
        git add -A
        git commit -m "chore: bump version to $VERSION" --no-verify
        log_pass "Versions bumped to $VERSION"
    else
        log_step "[2/6] Version unchanged ($VERSION)"
    fi

    # ── Step 5: Merge development → main ─────────────────────────
    if [ "$SKIP_MERGE" = false ]; then
        log_step "[5/8] Merging development → main..."
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
        log_step "[5/8] Skipping merge (--skip-merge)"
    fi

    # ── Step 6: Check tag ────────────────────────────────────────
    log_step "[6/8] Checking tag $TAG..."
    if git tag -l | grep -q "^$TAG$"; then
        log_fail "Tag $TAG already exists locally"
    fi
    if git ls-remote --tags origin 2>/dev/null | grep -q "refs/tags/$TAG$"; then
        log_fail "Tag $TAG already exists on remote"
    fi
    log_pass "Tag $TAG is available"

    # ── Step 7: Skipped (Maven Local already done in Step 3) ─────
    log_step "[7/8] Maven Local — already verified in Step 3"

    # ── Step 8: Tag + push + GitHub Release ──────────────────────
    log_step "[8/8] Creating tag and pushing..."
    git tag -a "$TAG" -m "KmpToolkit $TAG"
    git push origin "$TAG"
    log_pass "Tag $TAG pushed to origin"

    # GitHub Release (triggers publish.yml) + RELEASE_STATE markers
    if command -v gh &>/dev/null; then
        local timestamp
        timestamp=$(date -u +"%Y-%m-%dT%H:%M:%SZ")

        local notes="## Modules\n\n"
        for module in "${modules[@]}"; do
            notes+="- \`$(get_group "$module"):$(get_artifact "$module"):$VERSION\`\n"
        done
        notes+="\n## Installation\n\n\`\`\`kotlin\ncommonMain.dependencies {\n"
        for module in "${modules[@]}"; do
            notes+="    implementation(\"$(get_group "$module"):$(get_artifact "$module"):$VERSION\")\n"
        done
        notes+="}\n\`\`\`\n"
        notes+="\n## Deployment Status\n"
        notes+="<!-- RELEASE_STATE_START -->\n"
        notes+="- maven_local: verified ($timestamp)\n"
        notes+="- maven_central: pending\n"
        notes+="- platforms_verified: pass ($timestamp)\n"
        notes+="<!-- RELEASE_STATE_END -->\n"

        gh release create "$TAG" --repo "$github_repo" \
            --title "$TAG" --notes "$(echo -e "$notes")" 2>/dev/null && \
            log_pass "GitHub Release created (with RELEASE_STATE markers)" || \
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
    echo "  Pipeline: quality → platforms → maven-local → version → merge → tag → CI"
    echo ""
    echo "  ./scripts/release.sh                          Full 8-step release"
    echo "  ./scripts/release.sh --bump minor             Auto-bump + release"
    echo "  ./scripts/release.sh --version 0.2.0          Explicit version"
    echo "  ./scripts/release.sh --dry-run                Steps 1-3 only (verify)"
    echo "  ./scripts/release.sh --module cmp-clipboard   Single module"
    echo "  ./scripts/release.sh --skip-merge             Skip dev→main merge"
    echo "  ./scripts/release.sh --env /path/.env         Explicit .env path"
    echo "  ./scripts/release.sh -y                       Skip confirmation"
    echo "  ./scripts/release.sh list                     List modules"
    echo "  ./scripts/release.sh verify                   Quality gates only"
    echo "  ./scripts/release.sh local                    Maven Local publish (~/.m2)"
    echo "  ./scripts/release.sh publish                  Direct Maven Central publish (no CI)"
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
    publish) cmd_publish ;;
    help)    cmd_help ;;
    release) cmd_release ;;
    *)       cmd_help ;;
esac
