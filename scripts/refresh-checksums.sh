#!/usr/bin/env bash
#
# refresh-checksums.sh — regenerate scripts/assets.sha256 from the artifacts
# currently on disk. Run this after `fetch-assets.sh` whenever you bump the
# upstream pins in scripts/upstream.env.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
cd "$REPO_ROOT"

if [ ! -f app/app/src/main/assets/rootfs.tzst ]; then
  printf 'error: artifacts are not present. Run scripts/fetch-assets.sh first.\n' >&2
  exit 1
fi

out="scripts/assets.sha256"
tmp="$(mktemp)"
trap 'rm -f "$tmp"' EXIT

{
  find wine_addons installable_components -type f \( -name '*.msi' -o -name '*.tzst' \) 2>/dev/null
  find app/app/src/main/assets app/app/src/main/jniLibs -type f \
       \( -name '*.tzst' -o -name '*.sf2' -o -name '*.so' \) 2>/dev/null
} | grep -v '/\.gitkeep$' | sort | xargs sha256sum > "$tmp"

mv "$tmp" "$out"
trap - EXIT
printf 'wrote %s (%d entries, %s total)\n' "$out" "$(wc -l < "$out")" \
  "$(awk '{print $2}' "$out" | xargs du -ch 2>/dev/null | tail -1 | cut -f1)"
