#!/usr/bin/env bash
# Fails if provider credentials are hardcoded into shipping source.
#
# Why this exists: `LoginScreen.kt` shipped a real IPTV account's username, password and host
# pre-filled into the login form, in commonMain, for months — so it compiled into every Android
# AND iOS release build and any user could tap Login and consume someone else's subscription.
# The original project avoided this with a BuildConfig indirection that reads gitignored
# local.properties in debug and is empty in release; the KMP port dropped that indirection.
#
# Runs in CI (.github/workflows/verify.yml) and locally:
#   bash scripts/check-no-hardcoded-credentials.sh
#
# Scope note: this checks the WORKING TREE, not git history. The values already committed remain
# in history — they have to be rotated at the provider, which no script can do.

set -uo pipefail

# Source that ships to users. Excludes build output and test sources (fixtures may legitimately
# contain credential-shaped strings).
# NOTE: `mapfile` is bash 4+, and macOS ships bash 3.2. Use a while-loop so this stays runnable
# on a macos runner as well as ubuntu.
FILES=()
while IFS= read -r line; do
  [ -n "$line" ] && FILES+=("$line")
done < <(
  find shared androidApp iosApp -type f \( -name '*.kt' -o -name '*.kts' -o -name '*.swift' \) \
    -not -path '*/build/*' \
    -not -path '*/commonTest/*' \
    -not -path '*/androidUnitTest/*' \
    -not -path '*/iosTest/*' \
    -not -path '*/androidTest/*' \
    -not -path '*/test/*' \
    2>/dev/null
)

status=0

fail() {
  echo "::error file=$1,line=$2::$3"
  status=1
}

# ---------------------------------------------------------------------------
# 1. Known-leaked values must never reappear, in any form.
# ---------------------------------------------------------------------------
# Kept as split literals so this script does not itself become a copy of the secret.
LEAKED_USER="b7be78a330"
LEAKED_PASS="8ba28474b8"
LEAKED_HOST="xdooh"

for f in "${FILES[@]}"; do
  while IFS=: read -r line _; do
    [ -n "$line" ] && fail "$f" "$line" \
      "Re-introduces a known-leaked provider credential. These were removed and must be rotated, not restored."
  done < <(grep -nE "$LEAKED_USER|$LEAKED_PASS|$LEAKED_HOST" "$f" 2>/dev/null | cut -d: -f1)
done

# ---------------------------------------------------------------------------
# 2. Credential-bearing form state must not be initialised with a literal.
# ---------------------------------------------------------------------------
# Catches the exact shape of the original bug:
#   var password by remember { mutableStateOf("hunter2") }
# Empty strings and references to parameters/constants are fine.
for f in "${FILES[@]}"; do
  while IFS= read -r hit; do
    line="${hit%%:*}"
    fail "$f" "$line" \
      "Credential form field initialised with a non-empty string literal. Pass a default parameter (see LoginScreen's defaultUsername/defaultPassword/defaultUrl) instead of hardcoding."
  done < <(
    grep -nE '(var|val)[[:space:]]+(username|password|passwd|pass|url|serverUrl|host|hostUrl)[[:space:]]*(:[^=]*)?(by[[:space:]]+remember[[:space:]]*\{[[:space:]]*)?mutableStateOf\([[:space:]]*"[^"]+"' \
      "$f" 2>/dev/null
  )
done

# ---------------------------------------------------------------------------
# 3. No credentials embedded in a URL literal (user:pass@host).
# ---------------------------------------------------------------------------
for f in "${FILES[@]}"; do
  while IFS= read -r hit; do
    line="${hit%%:*}"
    fail "$f" "$line" "URL literal appears to embed credentials (scheme://user:pass@host)."
  done < <(grep -nE '"https?://[^"/:]+:[^"/@]+@' "$f" 2>/dev/null)
done

if [ "$status" -ne 0 ]; then
  echo ""
  echo "Hardcoded credentials found in shipping source. See scripts/check-no-hardcoded-credentials.sh"
  echo "and KMP_MIGRATION_AUDIT.md P0 #1 for the pattern to use instead."
  exit 1
fi

echo "OK: no hardcoded credentials in shipping source (${#FILES[@]} files scanned)."
