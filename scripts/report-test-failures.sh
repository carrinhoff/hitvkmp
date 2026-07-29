#!/usr/bin/env bash
# Turns Gradle test-result XML into GitHub Actions error annotations.
#
# Why this exists: a failing job annotates only "Process completed with exit code 1". The detail
# lives in the raw log and in the uploaded artifact, and **both need repository admin rights** to
# download through the API. Annotations, by contrast, are readable on a public repository by anyone
# — so emitting the actual failures as annotations makes a red build diagnosable without handing
# out credentials or asking someone to paste a log.
#
# Usage: report-test-failures.sh <test-results-dir-name>
#   e.g. report-test-failures.sh iosSimulatorArm64Test
#        report-test-failures.sh testDebugUnitTest
set -uo pipefail

TASK="${1:?usage: report-test-failures.sh <test-results-dir-name>}"

mapfile -t files < <(find . -path "*test-results/${TASK}/*.xml" -not -path '*/node_modules/*' 2>/dev/null)

if [ "${#files[@]}" -eq 0 ]; then
  echo "::warning::No test-result XML found for ${TASK} — the task may have failed before running any test."
  exit 0
fi

echo "Scanning ${#files[@]} result file(s) for ${TASK}."

python3 - "${files[@]}" <<'PY'
import sys, xml.etree.ElementTree as ET

total = failed = 0
for path in sys.argv[1:]:
    try:
        root = ET.parse(path).getroot()
    except Exception as e:
        print(f"::warning::Could not parse {path}: {e}")
        continue

    suite = root.get("name", path)
    total += int(root.get("tests", 0) or 0)

    for case in root.iter("testcase"):
        for kind in ("failure", "error"):
            node = case.find(kind)
            if node is None:
                continue
            failed += 1
            name = f'{case.get("classname", suite)}.{case.get("name", "?")}'
            msg = (node.get("message") or "").strip()
            body = (node.text or "").strip()

            # Annotations render as a single block; keep the first lines of the stack, which is
            # where a Kotlin/Native failure names the actual assertion or the OSStatus.
            detail = "\n".join([l for l in body.splitlines() if l.strip()][:12])
            print(f"::error title={name}::{msg}\n{detail}")

            # Also to stdout so the log (for whoever can read it) has the same content.
            print(f"--- FAILED: {name}")
            if msg:
                print(f"    message: {msg}")
            for line in detail.splitlines()[:12]:
                print(f"    {line}")

print(f"::notice::{failed} failing of {total} test(s) in this task.")
PY
