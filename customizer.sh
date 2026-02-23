#!/bin/bash
#
# KMP Library Template Customizer
# This script customizes the template for your new library project
#

# Colors and formatting
RED='\033[0;31m'
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
PURPLE='\033[0;35m'
CYAN='\033[0;36m'
BOLD='\033[1m'
NC='\033[0m' # No Color

# Emoji indicators
CHECK_MARK="✅"
WARNING="⚠️"
ROCKET="🚀"
GEAR="⚙️"
PACKAGE="📦"
CLEAN="🧹"
PENCIL="📝"

# Verify bash version. macOS comes with bash 3 preinstalled.
if [[ ${BASH_VERSINFO[0]} -lt 4 ]]; then
    echo -e "${RED}${WARNING} You need at least bash 4 to run this script.${NC}"
    echo -e "${CYAN}On macOS, you can install it with: brew install bash${NC}"
    exit 1
fi

# Exit when any command fails
set -e

# Print section header with design
print_section() {
    echo
    echo -e "${BLUE}╔════════════════════════════════════════════════════════════╗${NC}"
    echo -e "${BLUE}║${NC} ${BOLD}$1${NC}"
    echo -e "${BLUE}╚════════════════════════════════════════════════════════════╝${NC}"
    echo
}

# Print success message
print_success() {
    echo -e "${GREEN}${CHECK_MARK} $1${NC}"
}

# Print warning message
print_warning() {
    echo -e "${YELLOW}${WARNING} $1${NC}"
}

# Print info message
print_info() {
    echo -e "${CYAN}${GEAR} $1${NC}"
}

# Print processing message
print_processing() {
    echo -e "${PURPLE}${ROCKET} $1${NC}"
}

# Print error message
print_error() {
    echo -e "${RED}${WARNING} $1${NC}"
}

# Show usage
show_usage() {
    echo -e "${CYAN}Usage: bash customizer.sh <package> <library-name> [organization]${NC}"
    echo
    echo -e "${CYAN}Arguments:${NC}"
    echo "  package        Your package name (e.g., com.example.mylibrary)"
    echo "  library-name   Your library name (e.g., MyAwesomeLib)"
    echo "  organization   Optional: Your organization/username (e.g., myorg)"
    echo
    echo -e "${CYAN}Examples:${NC}"
    echo "  bash customizer.sh com.example.qrkit QRKit openMF"
    echo "  bash customizer.sh io.github.user.utils KotlinUtils"
    echo
}

if [[ $# -lt 2 ]]; then
    echo -e "${RED}${WARNING} Invalid arguments${NC}"
    show_usage
    exit 2
fi

PACKAGE=$1
LIBRARY_NAME=$2
ORGANIZATION=${3:-"your-org"}
SUBDIR=${PACKAGE//.//} # Replaces . with /
LIBRARY_NAME_LOWERCASE=$(echo "$LIBRARY_NAME" | tr '[:upper:]' '[:lower:]')
LIBRARY_NAME_UPPERCASE=$(echo "$LIBRARY_NAME" | tr '[:lower:]' '[:upper:]')

# Capitalize first letter
capitalize_first() {
    echo "$1" | awk '{print toupper(substr($0,1,1)) substr($0,2)}'
}
LIBRARY_NAME_CAPITALIZED=$(capitalize_first "$LIBRARY_NAME")

# Convert to kebab-case for artifact names
to_kebab_case() {
    echo "$1" | sed 's/\([A-Z]\)/-\L\1/g' | sed 's/^-//'
}
LIBRARY_ARTIFACT=$(to_kebab_case "$LIBRARY_NAME")

# Function to escape dots in package name for sed
escape_dots() {
    echo "$1" | sed 's/\./\\./g'
}

ESCAPED_PACKAGE=$(escape_dots "$PACKAGE")

# Update settings.gradle.kts
update_settings_gradle() {
    print_section "Updating settings.gradle.kts"

    local settings_file="settings.gradle.kts"

    if [ ! -f "$settings_file" ]; then
        print_error "settings.gradle.kts not found"
        return 1
    fi

    print_processing "Updating rootProject.name"

    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/rootProject.name = \".*\"/rootProject.name = \"$LIBRARY_NAME_LOWERCASE\"/" "$settings_file"
    else
        sed -i "s/rootProject.name = \".*\"/rootProject.name = \"$LIBRARY_NAME_LOWERCASE\"/" "$settings_file"
    fi

    print_success "Updated rootProject.name to '$LIBRARY_NAME_LOWERCASE'"
}

# Update library build.gradle.kts
update_library_build_gradle() {
    print_section "Updating cmp-library/build.gradle.kts"

    local build_file="cmp-library/build.gradle.kts"

    if [ ! -f "$build_file" ]; then
        print_error "cmp-library/build.gradle.kts not found"
        return 1
    fi

    print_processing "Updating group and namespace"

    if [[ "$OSTYPE" == "darwin"* ]]; then
        # Update group
        sed -i '' "s/group = \".*\"/group = \"$PACKAGE\"/" "$build_file"
        # Update namespace
        sed -i '' "s/namespace = \".*\"/namespace = \"$PACKAGE\"/" "$build_file"
        # Update library coordinates
        sed -i '' "s/coordinates(group.toString(), \".*\", version.toString())/coordinates(group.toString(), \"$LIBRARY_ARTIFACT\", version.toString())/" "$build_file"
        # Update POM name
        sed -i '' "s/name = \"My library\"/name = \"$LIBRARY_NAME\"/" "$build_file"
        sed -i '' "s/name = \"TEMPLATE_LIBRARY_NAME\"/name = \"$LIBRARY_NAME\"/" "$build_file"
        # Update POM description
        sed -i '' "s/description = \"A library.\"/description = \"$LIBRARY_NAME - A Kotlin Multiplatform library\"/" "$build_file"
        sed -i '' "s/description = \"TEMPLATE_DESCRIPTION\"/description = \"$LIBRARY_NAME - A Kotlin Multiplatform library\"/" "$build_file"
        # Update URL
        sed -i '' "s|url = \"https://github.com/kotlin/multiplatform-library-template/\"|url = \"https://github.com/$ORGANIZATION/$LIBRARY_NAME_LOWERCASE/\"|" "$build_file"
        sed -i '' "s|url = \"https://github.com/TEMPLATE_ORG/TEMPLATE_REPO/\"|url = \"https://github.com/$ORGANIZATION/$LIBRARY_NAME_LOWERCASE/\"|" "$build_file"
    else
        sed -i "s/group = \".*\"/group = \"$PACKAGE\"/" "$build_file"
        sed -i "s/namespace = \".*\"/namespace = \"$PACKAGE\"/" "$build_file"
        sed -i "s/coordinates(group.toString(), \".*\", version.toString())/coordinates(group.toString(), \"$LIBRARY_ARTIFACT\", version.toString())/" "$build_file"
        sed -i "s/name = \"My library\"/name = \"$LIBRARY_NAME\"/" "$build_file"
        sed -i "s/name = \"TEMPLATE_LIBRARY_NAME\"/name = \"$LIBRARY_NAME\"/" "$build_file"
        sed -i "s/description = \"A library.\"/description = \"$LIBRARY_NAME - A Kotlin Multiplatform library\"/" "$build_file"
        sed -i "s/description = \"TEMPLATE_DESCRIPTION\"/description = \"$LIBRARY_NAME - A Kotlin Multiplatform library\"/" "$build_file"
        sed -i "s|url = \"https://github.com/kotlin/multiplatform-library-template/\"|url = \"https://github.com/$ORGANIZATION/$LIBRARY_NAME_LOWERCASE/\"|" "$build_file"
        sed -i "s|url = \"https://github.com/TEMPLATE_ORG/TEMPLATE_REPO/\"|url = \"https://github.com/$ORGANIZATION/$LIBRARY_NAME_LOWERCASE/\"|" "$build_file"
    fi

    print_success "Updated library build configuration"
}

# Update Kotlin source files
update_kotlin_sources() {
    print_section "Updating Kotlin Source Files"

    local src_dirs=("commonMain" "commonTest" "androidMain" "androidHostTest" "jvmMain" "jvmTest" "appleMain" "appleTest" "linuxMain" "linuxTest" "mingwMain" "mingwTest" "jsMain" "jsTest" "wasmJsMain" "wasmJsTest" "wasmWasiMain" "wasmWasiTest")
    local library_dir="cmp-library/src"

    for src_dir in "${src_dirs[@]}"; do
        local kotlin_dir="$library_dir/$src_dir/kotlin"
        if [ -d "$kotlin_dir" ]; then
            print_processing "Processing $kotlin_dir"

            # Create new package directory
            mkdir -p "$kotlin_dir/$SUBDIR"

            # Find and process all Kotlin files
            while IFS= read -r -d '' file; do
                local filename=$(basename "$file")
                local new_file="$kotlin_dir/$SUBDIR/$filename"

                # Update package declaration and copy to new location
                if [[ "$OSTYPE" == "darwin"* ]]; then
                    sed "s/package io\.github\.kotlin\.fibonacci/package $PACKAGE/g; s/package io\.github\.template/package $PACKAGE/g" "$file" > "$new_file"
                else
                    sed "s/package io\.github\.kotlin\.fibonacci/package $PACKAGE/g; s/package io\.github\.template/package $PACKAGE/g" "$file" > "$new_file"
                fi

                # Remove old file if it's not in the new location
                if [ "$file" != "$new_file" ]; then
                    rm "$file"
                fi

                print_info "Moved: $filename -> $SUBDIR/"
            done < <(find "$kotlin_dir" -maxdepth 1 -name "*.kt" -print0 2>/dev/null)

            # Clean up old package directories
            rm -rf "$kotlin_dir/io" 2>/dev/null || true
        fi
    done

    print_success "Updated all Kotlin source files"
}

# Update imports in all files
update_imports() {
    print_section "Updating Import Statements"

    local count=0
    while IFS= read -r file; do
        if grep -q "io\.github\.kotlin\.fibonacci\|io\.github\.template" "$file" 2>/dev/null; then
            print_processing "Updating imports in: $file"
            if [[ "$OSTYPE" == "darwin"* ]]; then
                sed -i '' "s/io\.github\.kotlin\.fibonacci/$PACKAGE/g; s/io\.github\.template/$PACKAGE/g" "$file"
            else
                sed -i "s/io\.github\.kotlin\.fibonacci/$PACKAGE/g; s/io\.github\.template/$PACKAGE/g" "$file"
            fi
            count=$((count + 1))
        fi
    done < <(find . -type f \( -name "*.kt" -o -name "*.kts" \) -not -path "*/build/*" -not -path "*/.gradle/*")

    if [ $count -eq 0 ]; then
        print_warning "No files with template imports found"
    else
        print_success "Updated imports in $count file(s)"
    fi
}

# Update README
update_readme() {
    print_section "Updating README.md"

    local readme_file="README.md"

    if [ ! -f "$readme_file" ]; then
        print_warning "README.md not found, skipping"
        return 0
    fi

    print_processing "Customizing README"

    if [[ "$OSTYPE" == "darwin"* ]]; then
        sed -i '' "s/TEMPLATE_LIBRARY_NAME/$LIBRARY_NAME/g" "$readme_file"
        sed -i '' "s/TEMPLATE_PACKAGE/$PACKAGE/g" "$readme_file"
        sed -i '' "s/TEMPLATE_ORG/$ORGANIZATION/g" "$readme_file"
        sed -i '' "s/TEMPLATE_ARTIFACT/$LIBRARY_ARTIFACT/g" "$readme_file"
        sed -i '' "s/template-library/$LIBRARY_ARTIFACT/g" "$readme_file"
    else
        sed -i "s/TEMPLATE_LIBRARY_NAME/$LIBRARY_NAME/g" "$readme_file"
        sed -i "s/TEMPLATE_PACKAGE/$PACKAGE/g" "$readme_file"
        sed -i "s/TEMPLATE_ORG/$ORGANIZATION/g" "$readme_file"
        sed -i "s/TEMPLATE_ARTIFACT/$LIBRARY_ARTIFACT/g" "$readme_file"
        sed -i "s/template-library/$LIBRARY_ARTIFACT/g" "$readme_file"
    fi

    print_success "Updated README.md"
}

# Update GitHub workflows
update_github_workflows() {
    print_section "Updating GitHub Workflows"

    local workflow_dir=".github/workflows"

    if [ ! -d "$workflow_dir" ]; then
        print_warning "GitHub workflows directory not found"
        return 0
    fi

    while IFS= read -r file; do
        if grep -q "TEMPLATE_\|template-library" "$file" 2>/dev/null; then
            print_processing "Updating: $file"
            if [[ "$OSTYPE" == "darwin"* ]]; then
                sed -i '' "s/TEMPLATE_LIBRARY_NAME/$LIBRARY_NAME/g; s/template-library/$LIBRARY_ARTIFACT/g" "$file"
            else
                sed -i "s/TEMPLATE_LIBRARY_NAME/$LIBRARY_NAME/g; s/template-library/$LIBRARY_ARTIFACT/g" "$file"
            fi
        fi
    done < <(find "$workflow_dir" -type f -name "*.yml" -o -name "*.yaml")

    print_success "Updated GitHub workflows"
}

# Setup git hooks
setup_git_hooks() {
    print_section "Setting Up Git Hooks"

    if [ -f "scripts/setup-hooks.sh" ]; then
        print_processing "Running setup-hooks.sh"
        bash scripts/setup-hooks.sh
        print_success "Git hooks configured"
    else
        print_warning "setup-hooks.sh not found, skipping git hooks setup"
    fi
}

# Cleanup
cleanup() {
    print_section "Cleanup"

    print_processing "Removing backup files"
    find . -name "*.bak" -type f -delete 2>/dev/null || true
    find . -name "*-e" -type f -delete 2>/dev/null || true

    print_processing "Removing customizer script"
    # Optionally remove the customizer script after running
    # rm -f customizer.sh

    print_success "Cleanup completed"
}

# Print final summary
print_final_summary() {
    print_section "Customization Complete!"

    echo -e "${GREEN}${CHECK_MARK} Your KMP library has been customized:${NC}"
    echo
    echo -e "${CYAN}Library Configuration:${NC}"
    echo "  - Package: $PACKAGE"
    echo "  - Library Name: $LIBRARY_NAME"
    echo "  - Artifact ID: $LIBRARY_ARTIFACT"
    echo "  - Organization: $ORGANIZATION"
    echo
    echo -e "${CYAN}Files Updated:${NC}"
    echo "  - settings.gradle.kts"
    echo "  - cmp-library/build.gradle.kts"
    echo "  - Kotlin source files"
    echo "  - README.md"
    echo "  - GitHub workflows"
    echo
    echo -e "${YELLOW}${WARNING} Next Steps:${NC}"
    echo "  1. Review and update cmp-library/build.gradle.kts POM configuration"
    echo "  2. Update LICENSE file with your license"
    echo "  3. Configure GitHub secrets for Maven Central publishing:"
    echo "     - MAVEN_CENTRAL_USERNAME"
    echo "     - MAVEN_CENTRAL_PASSWORD"
    echo "     - SIGNING_KEY_ID"
    echo "     - SIGNING_PASSWORD"
    echo "     - GPG_KEY_CONTENTS"
    echo "  4. Replace sample code with your library implementation"
    echo "  5. Update README.md with your library documentation"
    echo
    echo -e "${GREEN}${ROCKET} Happy coding!${NC}"
}

# Print welcome banner
print_welcome_banner() {
    echo -e "${BLUE}"
    echo "╔══════════════════════════════════════════════════════════════╗"
    echo "║                                                              ║"
    echo "║           KMP Library Template Customizer                    ║"
    echo "║                                                              ║"
    echo "╚══════════════════════════════════════════════════════════════╝"
    echo -e "${NC}"
}

# Main execution
main() {
    print_welcome_banner

    print_section "Starting Customization"
    print_info "Package: $PACKAGE"
    print_info "Library Name: $LIBRARY_NAME"
    print_info "Organization: $ORGANIZATION"
    print_info "Artifact ID: $LIBRARY_ARTIFACT"

    update_settings_gradle
    update_library_build_gradle
    update_kotlin_sources
    update_imports
    update_readme
    update_github_workflows
    setup_git_hooks
    cleanup
    print_final_summary
}

# Execute main function
main
