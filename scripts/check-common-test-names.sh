#!/usr/bin/env bash
# Backtick test names in commonTest must be legal Kotlin/Native identifiers.
#
# Kotlin/Native rejects characters the JVM accepts inside a backtick-quoted name — notably
# commas, parentheses, colons and semicolons. Because `commonTest` compiles for iOS as well as
# the JVM, one of these breaks `compileTestKotlinIosSimulatorArm64` while every Android task
# stays green. It has caught this project out three times; each was found only by compiling the
# iOS test tree by hand.
#
# Runs in seconds and needs no Kotlin toolchain, so it belongs in front of the compile steps.
set -uo pipefail

status=0
fail() { echo "::error::$1"; status=1; }

# Directories whose sources compile for every target.
# NOTE: `mapfile` is bash 4+, and macOS ships bash 3.2 — a script using it dies with
# "mapfile: command not found" the moment it runs on a macos runner. Read into the array
# with a while-loop instead, which works on both.
files=()
while IFS= read -r line; do
  [ -n "$line" ] && files+=("$line")
done < <(find shared -type d -name commonTest -not -path '*/build/*' -exec find {} -name '*.kt' \; 2>/dev/null)

if [ "${#files[@]}" -eq 0 ]; then
  echo "No commonTest sources found — nothing to check."
  exit 0
fi

echo "Scanning ${#files[@]} commonTest file(s) for Kotlin/Native-illegal test names."

for f in "${files[@]}"; do
  # Backtick-quoted function names containing , ; : ( ) . or /
  while IFS= read -r hit; do
    [ -z "$hit" ] && continue
    fail "$f: illegal character in a backtick test name -> $hit"
    echo "        Kotlin/Native rejects , ; : ( ) . and / inside a backtick identifier."
  done < <(grep -nE 'fun `[^`]*[,;:().\/][^`]*`' "$f" || true)
done

if [ "$status" -ne 0 ]; then
  exit 1
fi

echo "OK: all commonTest names are valid Kotlin/Native identifiers."
