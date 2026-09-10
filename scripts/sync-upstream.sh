#!/usr/bin/env bash
#
# sync-upstream.sh — compare this vendored tree with upstream and (optionally)
# pull in upstream source changes.
#
#   scripts/sync-upstream.sh              # report: how far behind, what changed
#   scripts/sync-upstream.sh --apply      # overwrite source files with upstream's
#   scripts/sync-upstream.sh --diff FILE  # show the diff for one source file
#
# Only *source* files are touched. Binary artifacts (*.tzst, *.msi, *.sf2,
# jniLibs/) are never copied — use scripts/fetch-assets.sh --force for those.
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# shellcheck source=scripts/upstream.env
. "$SCRIPT_DIR/upstream.env"

MANIFEST_CHECK="$REPO_ROOT/scripts/upstream.env"
[ -f "$MANIFEST_CHECK" ] || {
  printf 'error: cannot locate repo root (looked for %s)\n' "$MANIFEST_CHECK" >&2
  exit 1
}
CACHE="${WINLATOR_CACHE_DIR:-$REPO_ROOT/.cache/upstream}"
APPLY=0
DIFF_FILE=""

while [ $# -gt 0 ]; do
  case "$1" in
    --apply) APPLY=1 ;;
    --diff)  shift; DIFF_FILE="${1:-}" ;;
    -h|--help) sed -n '2,12p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *) printf 'error: unknown argument %s\n' "$1" >&2; exit 1 ;;
  esac
  shift
done

remote_head() { git ls-remote "$1" HEAD | cut -f1; }

NEW_MAIN="$(remote_head "$UPSTREAM_WINLATOR_URL")"
NEW_APP="$(remote_head "$UPSTREAM_APP_URL")"

printf 'component        pinned                                   upstream HEAD\n'
printf 'winlator         %s   %s%s\n' "$UPSTREAM_WINLATOR_SHA" "$NEW_MAIN" \
  "$([ "$NEW_MAIN" = "$UPSTREAM_WINLATOR_SHA" ] && echo '  (up to date)' || echo '  (BEHIND)')"
printf 'winlator-app     %s   %s%s\n' "$UPSTREAM_APP_SHA" "$NEW_APP" \
  "$([ "$NEW_APP" = "$UPSTREAM_APP_SHA" ] && echo '  (up to date)' || echo '  (BEHIND)')"

if [ "$NEW_MAIN" = "$UPSTREAM_WINLATOR_SHA" ] && [ "$NEW_APP" = "$UPSTREAM_APP_SHA" ]; then
  printf '\nNothing to sync.\n'
  exit 0
fi

mkdir -p "$CACHE"
fetch() { # url sha dir
  local dir="$3"
  if [ ! -d "$dir/.git" ]; then git init -q -b main "$dir"; fi
  git -C "$dir" remote get-url origin >/dev/null 2>&1 \
    && git -C "$dir" remote set-url origin "$1" \
    || git -C "$dir" remote add origin "$1"
  git -C "$dir" fetch -q --depth 50 origin "$2"
  git -C "$dir" checkout -q --force FETCH_HEAD
}
fetch "$UPSTREAM_APP_URL"  "$NEW_APP"  "$CACHE/winlator-app-new"
fetch "$UPSTREAM_WINLATOR_URL" "$NEW_MAIN" "$CACHE/winlator-new"

# Paths excluded from syncing (binary artifacts, our own additions).
EXCLUDES=( -path '*/.git' -o -path '*/.idea' -o -name '*.tzst' -o -name '*.msi' -o -name '*.sf2'
           -o -path '*/jniLibs/*' -o -name '.gitkeep' )

changed=0
for pair in "winlator-app-new:app" "winlator-new:."; do
  src="$CACHE/${pair%%:*}"; dst="$REPO_ROOT/${pair##*:}"
  [ "$dst" = "$REPO_ROOT/." ] && dst="$REPO_ROOT"
  # Only look at dirs that upstream owns (skip our docs/scripts/.github additions
  # in the umbrella repo).
  while IFS= read -r f; do
    rel="${f#$src/}"
    # Our own additions + files we deliberately dropped (submodules are inlined).
    case "$rel" in
      docs/*|scripts/*|.github/*|README.md|NOTICE|UPSTREAM.md|.gitmodules|.gitignore) continue ;;
    esac
    if [ ! -f "$dst/$rel" ] || ! cmp -s "$f" "$dst/$rel"; then
      changed=$((changed+1))
      if [ -n "$DIFF_FILE" ] && [ "$rel" = "$DIFF_FILE" ]; then
        diff -u "$dst/$rel" "$f" || true
      elif [ "$APPLY" = 1 ]; then
        mkdir -p "$(dirname "$dst/$rel")"; cp -f "$f" "$dst/$rel"
        printf '  updated %s\n' "$rel"
      else
        printf '  %s\n' "$rel"
      fi
    fi
  done < <(find "$src" \( "${EXCLUDES[@]}" \) -prune -o -type f -print)
done

printf '\n%s source file(s) differ from upstream.\n' "$changed"
if [ "$APPLY" = 0 ] && [ "$changed" -gt 0 ]; then
  printf 'Run with --apply to copy them in, then review `git diff`.\n'
  printf 'Afterwards update the pins in scripts/upstream.env and UPSTREAM.md,\n'
  printf 'and refresh the checksums with scripts/refresh-checksums.sh.\n'
fi
