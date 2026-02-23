#!/bin/bash

# Setup Git Hooks for KMP Library Template
# This script installs pre-commit and pre-push hooks

set -e

# Colors
GREEN='\033[0;32m'
YELLOW='\033[1;33m'
BLUE='\033[0;34m'
NC='\033[0m'

echo -e "${BLUE}🔧 Setting up Git hooks...${NC}"

# Get the root directory of the git repository
GIT_ROOT=$(git rev-parse --show-toplevel 2>/dev/null)

if [ -z "$GIT_ROOT" ]; then
    echo -e "${YELLOW}⚠️  Not a git repository. Please initialize git first.${NC}"
    exit 1
fi

HOOKS_DIR="$GIT_ROOT/.git/hooks"
SCRIPTS_DIR="$GIT_ROOT/scripts"

# Create hooks directory if it doesn't exist
mkdir -p "$HOOKS_DIR"

# Install pre-commit hook
if [ -f "$SCRIPTS_DIR/pre-commit.sh" ]; then
    cp "$SCRIPTS_DIR/pre-commit.sh" "$HOOKS_DIR/pre-commit"
    chmod +x "$HOOKS_DIR/pre-commit"
    echo -e "${GREEN}✅ Pre-commit hook installed${NC}"
else
    echo -e "${YELLOW}⚠️  pre-commit.sh not found in scripts directory${NC}"
fi

# Install pre-push hook
if [ -f "$SCRIPTS_DIR/pre-push.sh" ]; then
    cp "$SCRIPTS_DIR/pre-push.sh" "$HOOKS_DIR/pre-push"
    chmod +x "$HOOKS_DIR/pre-push"
    echo -e "${GREEN}✅ Pre-push hook installed${NC}"
else
    echo -e "${YELLOW}⚠️  pre-push.sh not found in scripts directory${NC}"
fi

echo
echo -e "${GREEN}🎉 Git hooks setup complete!${NC}"
echo
echo "Installed hooks:"
echo "  - pre-commit: Runs Spotless formatting and Detekt analysis"
echo "  - pre-push: Runs full checks including tests"
echo
echo -e "${YELLOW}💡 To bypass hooks temporarily, use --no-verify flag:${NC}"
echo "   git commit --no-verify"
echo "   git push --no-verify"
echo
