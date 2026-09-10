#!/usr/bin/env bash
#
# fetch-assets.sh — download the heavy binary artifacts that are deliberately
# kept out of git (~480 MB) and verify them against scripts/assets.sha256.
#
# The artifacts are fetched from the *pinned* upstream commits recorded in
# scripts/upstream.env, so a build is reproducible: you always get the exact
# bytes the pinned revision shipped.
#
# Usage:
#   scripts/fetch-assets.sh                 # fetch everything missing, then verify
#   scripts/fetch-assets.sh --dry-run       # show what would happen, download nothing
#   scripts/fetch-assets.sh --only rootfs   # only paths matching "rootfs"
#   scripts/fetch-assets.sh --force         # re-download even if already present
#   scripts/fetch-assets.sh --dest /tmp/x   # place files elsewhere (testing/CI cache)
#   scripts/fetch-assets.sh --verify-only   # no download, just check what exists
#
set -euo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "$SCRIPT_DIR/.." && pwd)"
# shellcheck source=scripts/upstream.env
. "$SCRIPT_DIR/upstream.env"

MANIFEST="$SCRIPT_DIR/assets.sha256"
DEST="$REPO_ROOT"
CACHE="${WINLATOR_CACHE_DIR:-$REPO_ROOT/.cache/upstream}"
DRY_RUN=0
VERIFY_ONLY=0
FORCE=0
ONLY=""

die() { printf 'error: %s\n' "$*" >&2; exit 1; }
info() { printf '\033[1;34m==>\033[0m %s\n' "$*"; }

while [ $# -gt 0 ]; do
  case "$1" in
    --dry-run)     DRY_RUN=1 ;;
    --verify-only) VERIFY_ONLY=1 ;;
    --force)       FORCE=1 ;;
    --only)        shift; ONLY="${1:-}" ;;
    --dest)        shift; DEST="${1:-}" ;;
    --cache)       shift; CACHE="${1:-}" ;;
    -h|--help)     sed -n '2,20p' "$0" | sed 's/^# \{0,1\}//'; exit 0 ;;
    *)             die "unknown argument: $1 (try --help)" ;;
  esac
  shift
done

[ -f "$MANIFEST" ] || die "manifest not found: $MANIFEST"
[ -n "$DEST" ]     || die "--dest requires a path"

# ---------------------------------------------------------------- source repos
# manifest paths beginning with "app/" live in the winlator-app repo (which is
# vendored at ./app); everything else lives in the umbrella winlator repo.
clone_pinned() {
  local url="$1" sha="$2" dir="$3"
  if [ -d "$dir/.git" ] && [ "$(git -C "$dir" rev-parse HEAD 2>/dev/null || true)" = "$sha" ]; then
    return 0
  fi
  [ "$DRY_RUN" = 1 ] && { info "[dry-run] would fetch $url @ ${sha:0:9}"; return 0; }
  mkdir -p "$dir"
  if [ ! -d "$dir/.git" ]; then git init -q -b main "$dir"; fi
  git -C "$dir" remote get-url origin >/dev/null 2>&1 \
    && git -C "$dir" remote set-url origin "$url" \
    || git -C "$dir" remote add origin "$url"
  info "fetching ${url##*/} @ ${sha:0:9}"
  git -C "$dir" fetch -q --depth 1 origin "$sha"
  git -C "$dir" checkout -q --force FETCH_HEAD
}

NEED_APP=0
NEED_MAIN=0
while read -r _sum path; do
  [ -n "${path:-}" ] || continue
  case "$path" in "$ONLY"*|*"$ONLY"*) ;; *) continue ;; esac
  case "$path" in app/*) NEED_APP=1 ;; *) NEED_MAIN=1 ;; esac
done < "$MANIFEST"

if [ "$VERIFY_ONLY" = 0 ]; then
  [ "$NEED_MAIN" = 1 ] && clone_pinned "$UPSTREAM_WINLATOR_URL" "$UPSTREAM_WINLATOR_SHA" "$CACHE/winlator"
  [ "$NEED_APP"  = 1 ] && clone_pinned "$UPSTREAM_APP_URL"      "$UPSTREAM_APP_SHA"      "$CACHE/winlator-app"
fi

# -------------------------------------------------------------------- copy/verify
copied=0 skipped=0 failed=0 verified=0 missing=0
TMP_MANIFEST="$(mktemp)"
trap 'rm -f "$TMP_MANIFEST"' EXIT

while read -r sum path; do
  [ -n "${path:-}" ] || continue
  case "$path" in "$ONLY"*|*"$ONLY"*) ;; *) continue ;; esac

  case "$path" in
    app/*) src="$CACHE/winlator-app/${path#app/}" ;;
    *)     src="$CACHE/winlator/$path" ;;
  esac
  out="$DEST/$path"

  if [ "$VERIFY_ONLY" = 0 ] && { [ ! -f "$out" ] || [ "$FORCE" = 1 ]; }; then
    if [ "$DRY_RUN" = 1 ]; then
      printf '  [dry-run] %s\n' "$path"; copied=$((copied+1)); continue
    fi
    [ -f "$src" ] || { printf '  MISSING upstream: %s\n' "$path" >&2; missing=$((missing+1)); continue; }
    mkdir -p "$(dirname "$out")"
    cp -f "$src" "$out"
    copied=$((copied+1))
  elif [ -f "$out" ]; then
    skipped=$((skipped+1))
  else
    printf '  absent: %s\n' "$path"; missing=$((missing+1)); continue
  fi
  [ "$DRY_RUN" = 1 ] && continue
  printf '%s  %s\n' "$sum" "$out" >> "$TMP_MANIFEST"
done < "$MANIFEST"

if [ "$DRY_RUN" = 0 ] && [ -s "$TMP_MANIFEST" ]; then
  info "verifying sha256"
  if ( cd "$DEST" && sha256sum -c --quiet <(sed "s|  $DEST/|  |" "$TMP_MANIFEST") ); then
    verified="$(wc -l < "$TMP_MANIFEST")"
  else
    die "sha256 verification FAILED — do not build from these artifacts"
  fi
fi

info "copied=$copied present=$skipped verified=$verified missing=$missing failed=$failed"
[ "$missing" = 0 ] || { [ "$VERIFY_ONLY" = 1 ] && exit 0 || die "$missing artifact(s) could not be fetched"; }
