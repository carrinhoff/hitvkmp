#!/usr/bin/env bash
# Asserts the BGTask identifiers agree across all three places they are written.
#
# Why this is a build-time check and not a runtime one:
#
# `-[BGTaskScheduler submitTaskRequest:error:]` raises an ObjC
# **NSInternalInconsistencyException** if the identifier is absent from
# `BGTaskSchedulerPermittedIdentifiers`, and Kotlin/Native cannot catch ObjC exceptions — the
# `catch (e: Throwable)` in BackgroundSyncManager.ios.kt does NOT cover it. So a drift between the
# Kotlin constants, the Swift registration and Info.plist is not a degraded sync, it is a hard
# crash the moment background sync is enabled. The same applies to
# `BGTaskScheduler.register(forTaskWithIdentifier:)` at launch.
#
# The identifier lives in three files that are edited independently, which is exactly the shape
# that drifts. Cheap to check, impossible to catch at runtime, so it is checked here.
#
#   bash scripts/check-bgtask-identifiers.sh

set -uo pipefail

KOTLIN_SRC="shared/core/sync/src/commonMain/kotlin/pt/hitv/core/sync/BackgroundSyncManager.kt"
SWIFT_SRC="iosApp/iosApp/iOSApp.swift"
PLIST="iosApp/iosApp/Info.plist"
# The iOS *actual* — the expect declaration in commonMain constructs no request.
KOTLIN_IOS_SRC="shared/core/sync/src/iosMain/kotlin/pt/hitv/core/sync/BackgroundSyncManager.ios.kt"

status=0
fail() { echo "::error::$1"; status=1; }

for f in "$KOTLIN_SRC" "$SWIFT_SRC" "$PLIST"; do
  [ -f "$f" ] || { fail "missing file: $f"; exit 1; }
done

# Kotlin: const val TASK_EPG: String = "pt.hitv.sync.epg"
kotlin_ids=$(grep -oE 'const val TASK_(EPG|CONTENT): String = "[^"]+"' "$KOTLIN_SRC" \
  | grep -oE '"[^"]+"' | tr -d '"' | sort)

# Swift: private static let taskIdEpg = "pt.hitv.sync.epg"
swift_ids=$(grep -oE 'static let taskId(Epg|Content) = "[^"]+"' "$SWIFT_SRC" \
  | grep -oE '"[^"]+"' | tr -d '"' | sort)

# Info.plist: the <string> entries inside the BGTaskSchedulerPermittedIdentifiers <array>
plist_ids=$(awk '
  /BGTaskSchedulerPermittedIdentifiers/ { inkey = 1; next }
  inkey && /<array>/ { inarray = 1; next }
  inarray && /<\/array>/ { exit }
  inarray { print }
' "$PLIST" | grep -oE '<string>[^<]+</string>' | sed -E 's|</?string>||g' | sort)

echo "Kotlin constants : $(echo "$kotlin_ids" | tr '\n' ' ')"
echo "Swift constants  : $(echo "$swift_ids" | tr '\n' ' ')"
echo "Info.plist       : $(echo "$plist_ids" | tr '\n' ' ')"

[ -n "$kotlin_ids" ] || fail "no TASK_EPG/TASK_CONTENT constants found in $KOTLIN_SRC"
[ -n "$swift_ids" ]  || fail "no taskIdEpg/taskIdContent constants found in $SWIFT_SRC"
[ -n "$plist_ids" ]  || fail "BGTaskSchedulerPermittedIdentifiers missing or empty in $PLIST"

if [ "$kotlin_ids" != "$plist_ids" ]; then
  fail "Kotlin BGTask identifiers do not match Info.plist BGTaskSchedulerPermittedIdentifiers. Submitting an unlisted identifier raises an uncatchable ObjC exception."
fi
if [ "$swift_ids" != "$plist_ids" ]; then
  fail "Swift BGTask identifiers do not match Info.plist BGTaskSchedulerPermittedIdentifiers. Registering an unlisted identifier crashes at launch."
fi

# ---------------------------------------------------------------------------
# Request type must agree between Kotlin, Swift and the declared background mode.
#
# Kotlin submits the first request and the Swift handler chains the next one. If they use
# different BGTaskRequest subclasses the follow-up run silently gets different duration and
# constraints than the first — no error, just a background sync that behaves differently on
# every other run. And the request type has to match UIBackgroundModes: submitting a
# processing request without the `processing` mode fails at runtime, while declaring a mode
# nothing uses is a routine App Review question.
kotlin_type=$(grep -oE 'BG(Processing|AppRefresh)TaskRequest\(identifier' "$KOTLIN_IOS_SRC" | head -1 | grep -oE 'BG(Processing|AppRefresh)TaskRequest')
swift_type=$(grep -oE 'BG(Processing|AppRefresh)TaskRequest\(identifier' "$SWIFT_SRC" | head -1 | grep -oE 'BG(Processing|AppRefresh)TaskRequest')
modes=$(sed -n '/<key>UIBackgroundModes<\/key>/,/<\/array>/p' "$PLIST" | grep -oE '<string>[^<]+</string>' | sed -E 's|</?string>||g' | sort | tr '
' ' ' | sed 's/ $//')

echo "Kotlin request   : ${kotlin_type:-<none>}"
echo "Swift request    : ${swift_type:-<none>}"
echo "UIBackgroundModes: ${modes:-<none>}"

[ -n "$kotlin_type" ] || fail "no BGTaskRequest construction found in $KOTLIN_IOS_SRC"
[ -n "$swift_type" ]  || fail "no BGTaskRequest construction found in $SWIFT_SRC"

if [ "$kotlin_type" != "$swift_type" ]; then
  fail "Kotlin submits $kotlin_type but Swift chains $swift_type. The two must match, or the follow-up run gets a different window and different constraints than the first."
fi

case "$kotlin_type" in
  BGProcessingTaskRequest)
    case " $modes " in
      *" processing "*) ;;
      *) fail "submitting BGProcessingTaskRequest requires 'processing' in UIBackgroundModes (found: ${modes:-<none>})" ;;
    esac
    case " $modes " in
      *" fetch "*) fail "UIBackgroundModes declares 'fetch' but nothing submits a BGAppRefreshTaskRequest. Unused background modes draw App Review questions." ;;
    esac
    ;;
  BGAppRefreshTaskRequest)
    case " $modes " in
      *" fetch "*) ;;
      *) fail "submitting BGAppRefreshTaskRequest requires 'fetch' in UIBackgroundModes (found: ${modes:-<none>})" ;;
    esac
    case " $modes " in
      *" processing "*) fail "UIBackgroundModes declares 'processing' but nothing submits a BGProcessingTaskRequest. Unused background modes draw App Review questions." ;;
    esac
    ;;
esac

if [ "$status" -ne 0 ]; then
  exit 1
fi

echo "OK: BGTask identifiers, request types and UIBackgroundModes all agree."
