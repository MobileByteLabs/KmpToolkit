#!/bin/bash

# =============================================================================
# KmpToolkit - Unified Release & Publishing Tool
# =============================================================================
# Auto-discovers and publishes all cmp-* library modules to Maven Central.
# Credentials are provided via JSON file (--credentials flag or CREDENTIALS env).
#
# Usage:
#   ./scripts/release.sh publish --credentials /path/to/creds.json
#   ./scripts/release.sh publish --module cmp-user-tickets
#   ./scripts/release.sh local
#   ./scripts/release.sh list
#   ./scripts/release.sh verify
#
# JSON credentials format:
# {
#   "maven_central": {
#     "username": "...",
#     "password": "...",
#     "namespace": "io.github.mobilebytesensei",
#     "staging_url": "https://central.sonatype.com"
#   },
#   "gpg": {
#     "key_id": "...",
#     "full_key_id": "...",
#     "passphrase": "...",
#     "key_email": "..."
#   },
#   "github": {
#     "repo": "mobilebytesensei/KmpToolkit",
#     "org": "mobilebytesensei",
#     "fork": "therajanmaurya/KmpToolkit"
#   }
# }
# =============================================================================

set -e

# Colors
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m'

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
PROJECT_DIR="$(dirname "$SCRIPT_DIR")"

# =============================================================================
# Helpers
# =============================================================================

log_header() {
    echo ""
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo -e "${BOLD}  $1${NC}"
    echo -e "${CYAN}═══════════════════════════════════════════════════════════${NC}"
    echo ""
}

log_step()    { echo -e "\n${BLUE}▶${NC} ${BOLD}$1${NC}"; }
log_success() { echo -e "${GREEN}✓${NC} $1"; }
log_warn()    { echo -e "${YELLOW}⚠${NC} $1"; }
log_error()   { echo -e "${RED}✗${NC} $1"; }

# =============================================================================
# JSON Credentials
# =============================================================================

CREDS_FILE=""

parse_json_field() {
    local json_file="$1"
    local field="$2"
    # Use python3 for reliable JSON parsing (available on macOS + Linux)
    python3 -c "
import json, sys
with open('$json_file') as f:
    data = json.load(f)
keys = '$field'.split('.')
val = data
for k in keys:
    val = val[k]
print(val)
" 2>/dev/null
}

load_credentials() {
    if [ -z "$CREDS_FILE" ]; then
        log_error "No credentials file provided."
        echo ""
        echo "Usage: $0 <command> --credentials /path/to/credentials.json"
        echo ""
        echo "JSON format:"
        echo '  {'
        echo '    "maven_central": {'
        echo '      "username": "...", "password": "...",'
        echo '      "namespace": "io.github.mobilebytesensei",'
        echo '      "staging_url": "https://central.sonatype.com"'
        echo '    },'
        echo '    "gpg": {'
        echo '      "key_id": "...", "full_key_id": "...",'
        echo '      "passphrase": "...", "key_email": "..."'
        echo '    },'
        echo '    "github": {'
        echo '      "repo": "mobilebytesensei/KmpToolkit",'
        echo '      "org": "mobilebytesensei",'
        echo '      "fork": "therajanmaurya/KmpToolkit"'
        echo '    }'
        echo '  }'
        exit 1
    fi

    if [ ! -f "$CREDS_FILE" ]; then
        log_error "Credentials file not found: $CREDS_FILE"
        exit 1
    fi

    MAVEN_USERNAME=$(parse_json_field "$CREDS_FILE" "maven_central.username")
    MAVEN_PASSWORD=$(parse_json_field "$CREDS_FILE" "maven_central.password")
    MAVEN_NAMESPACE=$(parse_json_field "$CREDS_FILE" "maven_central.namespace")
    MAVEN_STAGING_URL=$(parse_json_field "$CREDS_FILE" "maven_central.staging_url")
    GPG_KEY_ID=$(parse_json_field "$CREDS_FILE" "gpg.key_id")
    GPG_FULL_KEY_ID=$(parse_json_field "$CREDS_FILE" "gpg.full_key_id")
    GPG_PASSPHRASE=$(parse_json_field "$CREDS_FILE" "gpg.passphrase")
    GPG_KEY_EMAIL=$(parse_json_field "$CREDS_FILE" "gpg.key_email")
    GITHUB_REPO=$(parse_json_field "$CREDS_FILE" "github.repo")
    GITHUB_ORG=$(parse_json_field "$CREDS_FILE" "github.org")
    GITHUB_FORK=$(parse_json_field "$CREDS_FILE" "github.fork")

    if [ -z "$MAVEN_USERNAME" ] || [ -z "$MAVEN_PASSWORD" ]; then
        log_error "maven_central.username or maven_central.password missing"
        exit 1
    fi

    if [ -z "$GPG_FULL_KEY_ID" ]; then
        log_error "gpg.full_key_id missing"
        exit 1
    fi

    if [ -z "$GITHUB_REPO" ]; then
        log_error "github.repo missing"
        exit 1
    fi

    log_success "Credentials loaded from $CREDS_FILE"
    log_success "Namespace: $MAVEN_NAMESPACE"
    log_success "GPG key: $GPG_FULL_KEY_ID ($GPG_KEY_EMAIL)"
    log_success "GitHub: $GITHUB_REPO (fork: $GITHUB_FORK)"
}

setup_gradle_signing() {
    log_step "Configuring Gradle signing..."

    export ORG_GRADLE_PROJECT_mavenCentralUsername="$MAVEN_USERNAME"
    export ORG_GRADLE_PROJECT_mavenCentralPassword="$MAVEN_PASSWORD"
    export ORG_GRADLE_PROJECT_signingInMemoryKeyId="$GPG_KEY_ID"
    export ORG_GRADLE_PROJECT_signingInMemoryKeyPassword="$GPG_PASSPHRASE"
    export ORG_GRADLE_PROJECT_signingInMemoryKey="$(gpg --batch --yes --pinentry-mode loopback --passphrase "$GPG_PASSPHRASE" --export-secret-keys --armor "$GPG_FULL_KEY_ID" 2>/dev/null)"

    if [ -z "$ORG_GRADLE_PROJECT_signingInMemoryKey" ]; then
        log_error "Failed to export GPG key. Is $GPG_FULL_KEY_ID in your keyring?"
        exit 1
    fi

    log_success "Gradle signing configured (in-memory)"
}

# =============================================================================
# Module Discovery
# =============================================================================

discover_modules() {
    local filter="$1"
    local modules=()

    cd "$PROJECT_DIR"
    for dir in cmp-*/; do
        if [ -d "$dir" ] && grep -q "mavenPublishing" "${dir}build.gradle.kts" 2>/dev/null; then
            local module="${dir%/}"
            if [ -n "$filter" ] && [ "$module" != "$filter" ]; then
                continue
            fi
            modules+=("$module")
        fi
    done

    echo "${modules[@]}"
}

get_module_info() {
    local module="$1"
    local build_file="$PROJECT_DIR/$module/build.gradle.kts"

    local version=$(grep -m1 'version = ' "$build_file" | sed 's/.*"\(.*\)".*/\1/')
    local artifact=$(grep 'coordinates(' "$build_file" | sed 's/.*coordinates([^,]*, "\([^"]*\)".*/\1/')
    local group=$(grep 'coordinates(' "$build_file" | sed 's/.*coordinates("\([^"]*\)".*/\1/')

    echo "$group:$artifact:$version"
}

# =============================================================================
# Commands
# =============================================================================

cmd_list() {
    log_header "Discoverable Modules"

    cd "$PROJECT_DIR"
    local modules=($(discover_modules ""))

    if [ ${#modules[@]} -eq 0 ]; then
        log_warn "No publishable modules found"
        return
    fi

    printf "%-25s %-45s %s\n" "MODULE" "ARTIFACT" "VERSION"
    printf "%-25s %-45s %s\n" "──────" "────────" "───────"

    for module in "${modules[@]}"; do
        local info=$(get_module_info "$module")
        local artifact=$(echo "$info" | cut -d: -f1-2)
        local version=$(echo "$info" | cut -d: -f3)
        printf "%-25s %-45s %s\n" "$module" "$artifact" "$version"
    done
    echo ""
}

cmd_verify() {
    log_header "Build Verification (All Platforms)"

    cd "$PROJECT_DIR"
    local modules=($(discover_modules "${TARGET_MODULE:-}"))

    for module in "${modules[@]}"; do
        log_step "Verifying $module..."
        local info=$(get_module_info "$module")
        echo "  Artifact: $info"

        local targets="compileKotlinJvm"
        # Add platform targets if they exist
        for target in compileKotlinIosArm64 compileKotlinIosSimulatorArm64 compileKotlinIosX64 \
                      compileKotlinMacosArm64 compileKotlinMacosX64 \
                      compileKotlinJs compileKotlinWasmJs; do
            targets="$targets :${module}:${target}"
        done

        if ./gradlew :${module}:compileKotlinJvm :${module}:compileKotlinIosArm64 \
            :${module}:compileKotlinMacosArm64 :${module}:compileKotlinJs \
            :${module}:compileKotlinWasmJs --no-configuration-cache --no-daemon 2>/dev/null; then
            log_success "$module — all platforms OK"
        else
            log_error "$module — build failed"
            exit 1
        fi
    done

    log_header "All Modules Verified"
}

cmd_local() {
    log_header "Publish to Maven Local"

    load_credentials
    setup_gradle_signing

    cd "$PROJECT_DIR"
    local modules=($(discover_modules "${TARGET_MODULE:-}"))

    for module in "${modules[@]}"; do
        log_step "Publishing $module to Maven Local..."
        local info=$(get_module_info "$module")

        if ./gradlew ":${module}:publishToMavenLocal" --no-configuration-cache --no-daemon; then
            log_success "$module → ~/.m2/repository ($info)"
        else
            log_error "$module publish failed"
            exit 1
        fi
    done

    log_header "Published to Maven Local"
    echo "Use with: repositories { mavenLocal() }"
}

cmd_publish() {
    log_header "Publish to Maven Central"

    load_credentials
    setup_gradle_signing

    cd "$PROJECT_DIR"
    local modules=($(discover_modules "${TARGET_MODULE:-}"))

    if [ ${#modules[@]} -eq 0 ]; then
        log_error "No modules to publish"
        exit 1
    fi

    echo "Modules to publish:"
    for module in "${modules[@]}"; do
        local info=$(get_module_info "$module")
        echo "  • $module ($info)"
    done
    echo ""

    if [ "$SKIP_CONFIRM" != "true" ]; then
        read -p "Proceed? [y/N]: " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Cancelled"
            exit 0
        fi
    fi

    for module in "${modules[@]}"; do
        log_step "Publishing $module..."
        local info=$(get_module_info "$module")

        if ./gradlew ":${module}:publishAllPublicationsToMavenCentralRepository" \
            --no-configuration-cache --no-daemon; then
            log_success "$module published ($info)"
        else
            log_error "$module publish failed"
            exit 1
        fi
    done

    log_header "Published to Maven Central"
    echo "Sync takes ~10-30 minutes."
    echo ""
    for module in "${modules[@]}"; do
        local info=$(get_module_info "$module")
        local artifact=$(echo "$info" | cut -d: -f2)
        local version=$(echo "$info" | cut -d: -f3)
        echo "  ${MAVEN_STAGING_URL:-https://central.sonatype.com}/artifact/${MAVEN_NAMESPACE:-io.github.mobilebytesensei}/$artifact/$version"
    done
    echo ""
}

cmd_release() {
    local bump_type="${1:-minor}"

    log_header "Full Release ($bump_type)"

    load_credentials

    cd "$PROJECT_DIR"
    local modules=($(discover_modules "${TARGET_MODULE:-}"))

    # Get current version from first module
    local first_module="${modules[0]}"
    local current_info=$(get_module_info "$first_module")
    local current_version=$(echo "$current_info" | cut -d: -f3)

    # Calculate new version
    IFS='.' read -r major minor patch <<< "$current_version"
    case $bump_type in
        major) major=$((major + 1)); minor=0; patch=0 ;;
        minor) minor=$((minor + 1)); patch=0 ;;
        patch) patch=$((patch + 1)) ;;
    esac
    local new_version="$major.$minor.$patch"

    echo "Current: $current_version → New: $new_version"
    echo "Modules: ${modules[*]}"
    echo ""

    if [ "$SKIP_CONFIRM" != "true" ]; then
        read -p "Proceed with release? [y/N]: " -n 1 -r
        echo ""
        if [[ ! $REPLY =~ ^[Yy]$ ]]; then
            echo "Cancelled"
            exit 0
        fi
    fi

    # Bump versions
    log_step "Bumping versions to $new_version..."
    for module in "${modules[@]}"; do
        local build_file="$PROJECT_DIR/$module/build.gradle.kts"
        sed -i '' "s/version = \"$current_version\"/version = \"$new_version\"/" "$build_file"
        log_success "$module → $new_version"
    done

    # Build verification
    cmd_verify

    # Publish
    setup_gradle_signing
    SKIP_CONFIRM=true cmd_publish

    # Git tag
    log_step "Creating git tag..."
    git add -A
    git commit -m "chore: release v$new_version" --no-verify || true
    git tag -a "v$new_version" -m "Release v$new_version"
    git push upstream main --tags --no-verify
    log_success "Tagged v$new_version and pushed"

    # GitHub release
    if command -v gh &>/dev/null; then
        log_step "Creating GitHub release..."
        local notes="## Modules\n"
        for module in "${modules[@]}"; do
            local info=$(get_module_info "$module")
            notes+="- \`$info\`\n"
        done
        gh release create "v$new_version" -R "${GITHUB_REPO:-mobilebytesensei/KmpToolkit}" \
            --target main --title "v$new_version" --notes "$(echo -e "$notes")" 2>/dev/null || \
            log_warn "GitHub release creation failed (create manually)"
    fi

    log_header "Release v$new_version Complete"
}

# =============================================================================
# Help
# =============================================================================

show_help() {
    echo ""
    echo -e "${BOLD}KmpToolkit — Release & Publishing Tool${NC}"
    echo ""
    echo -e "${BOLD}Usage:${NC} $0 <command> [options]"
    echo ""
    echo -e "${BOLD}Commands:${NC}"
    echo "  list                     List all publishable modules"
    echo "  verify                   Build all platforms (no publish)"
    echo "  local                    Publish to Maven Local (~/.m2)"
    echo "  publish                  Publish to Maven Central"
    echo "  release [major|minor|patch]  Full release: bump + verify + publish + tag"
    echo "  help                     Show this help"
    echo ""
    echo -e "${BOLD}Options:${NC}"
    echo "  --credentials FILE       Path to JSON credentials file"
    echo "  --module MODULE          Target specific module (e.g., cmp-user-tickets)"
    echo "  -y                       Skip confirmation prompts"
    echo ""
    echo -e "${BOLD}Examples:${NC}"
    echo "  $0 list"
    echo "  $0 verify"
    echo "  $0 publish --credentials ~/secrets/kmptoolkit.json"
    echo "  $0 publish --credentials ~/secrets/kmptoolkit.json --module cmp-user-tickets"
    echo "  $0 release minor --credentials ~/secrets/kmptoolkit.json"
    echo ""
    echo -e "${BOLD}Credentials JSON:${NC}"
    echo '  {'
    echo '    "maven_central": {'
    echo '      "username": "...", "password": "...",'
    echo '      "namespace": "io.github.mobilebytesensei",'
    echo '      "staging_url": "https://central.sonatype.com"'
    echo '    },'
    echo '    "gpg": {'
    echo '      "key_id": "...", "full_key_id": "...",'
    echo '      "passphrase": "...", "key_email": "..."'
    echo '    },'
    echo '    "github": {'
    echo '      "repo": "mobilebytesensei/KmpToolkit",'
    echo '      "org": "mobilebytesensei",'
    echo '      "fork": "therajanmaurya/KmpToolkit"'
    echo '    }'
    echo '  }'
    echo ""
}

# =============================================================================
# Main
# =============================================================================

TARGET_MODULE=""
SKIP_CONFIRM=""

# Parse flags
ARGS=()
while [[ $# -gt 0 ]]; do
    case "$1" in
        --credentials) CREDS_FILE="$2"; shift 2 ;;
        --module) TARGET_MODULE="$2"; shift 2 ;;
        -y) SKIP_CONFIRM="true"; shift ;;
        *) ARGS+=("$1"); shift ;;
    esac
done

COMMAND="${ARGS[0]:-}"

echo ""
echo -e "${CYAN}╔═══════════════════════════════════════════════════════════╗${NC}"
echo -e "${CYAN}║${NC}   ${BOLD}KmpToolkit — Release Tool${NC}                               ${CYAN}║${NC}"
echo -e "${CYAN}╚═══════════════════════════════════════════════════════════╝${NC}"

case "$COMMAND" in
    list)    cmd_list ;;
    verify)  cmd_verify ;;
    local)   cmd_local ;;
    publish) cmd_publish ;;
    release) cmd_release "${ARGS[1]:-minor}" ;;
    help|--help|-h|"") show_help ;;
    *) log_error "Unknown command: $COMMAND"; show_help; exit 1 ;;
esac
