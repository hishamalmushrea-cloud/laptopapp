#!/usr/bin/env bash
#
# install-ci.sh — copy ci/build.yml into .github/workflows/ so GitHub Actions runs it.
#
# Kept out of .github/workflows by default because pushing workflow files requires a
# token with the `workflows` scope; the automation token used to seed this repo does
# not have it. Run this locally and push with your own credentials.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"

[ -f "$REPO_ROOT/ci/build.yml" ] || { printf 'error: ci/build.yml not found\n' >&2; exit 1; }

mkdir -p "$REPO_ROOT/.github/workflows"
cp -f "$REPO_ROOT/ci/build.yml" "$REPO_ROOT/.github/workflows/build.yml"

printf 'installed .github/workflows/build.yml\n'
printf 'next:  git add .github/workflows/build.yml && git commit -m "Enable CI" && git push\n'
