# HITV — KMP Migration Audit

**Last Updated:** 2026-07-26
**Original Project:** `hitv` (Android-only, Kotlin + Jetpack Compose) — the spec
**KMP Project:** `hitv-kmp` (Compose Multiplatform — Android + iOS mobile)
**Target Platforms:** Android (mobile) + iOS (mobile) — no TV
**Working build:** iOS build 38 (Info.plist `CFBundleVersion`)

> **This document was rewritten from a full source-level re-audit on 2026-07-26.**
> The previous revision (dated 2026-04-26) asserted the port was feature-complete except
> for launch assets. That was wrong. It was written from module/file *names* rather than
> file *contents*, so screens that exist as files but are unregistered, unreachable, or
> wired to dead code were scored as ✅. The tables below are derived from reading both
> codebases side by side. **Do not re-score this document from filenames.**

---

## 1. Executive summary

The port's **data layer is in genuinely good shape** and the **UI layer is mostly present**.
What is not in good shape is **wiring**: a large number of ported screens and managers are
complete in source but never registered, never called, or gated behind a flag that is
hardcoded off. The result is an app that demos well on the happy path and falls over on the
second thing you tap.

Concretely, from this audit:

| | Count |
|---|---|
| Confirmed gaps vs. the original | **99** |
| of which P0 (store-blocking, crashing, or feature-dead) | **10** |
| of which P1 | **43** |
| Refuted on verification (not real gaps) | 3 |
| Reported but verification did not complete | 27 |

**Shared-code ratio is real** (~90%), and that is the migration's genuine win: nearly every
gap below is a *single* fix that lands on both platforms at once.

### The honest status line

*As first audited, before the fixes in §4.1:*

- **Android**: worked, but was a regression against the shipping `hitv` app — no EPG grid, no
  in-app trailer player, no series player, parental controls inert, billing dead.
- **iOS**: built and ran, and playback worked. But it inherited every shared-code gap above, plus
  iOS-specific defects (EPG parser memory blowup, no idle-timer handling, no playback-start
  watchdog).

*After §4.1:* nine of the ten P0s are fixed, the EPG grid and series player exist, parental
controls function, and the iOS code executes in CI (§1.3). Billing is the remaining P0. The honest
gate on a wide TestFlight is now the device pass in §7 Step 0, not the P0 list.

### Verification confidence

| Domain | Findings verified adversarially? |
|---|---|
| auth, channels+EPG, movies, series, players, data | ✅ yes — each gap independently re-checked against both repos |
| settings | ⚠️ **partial** — 22 findings reported, the automated verification pass never ran (token limit). The 3 P0s were re-verified by hand (§4). Spot-checking since then confirmed one security finding (un-protect without PIN — now fixed) and **refuted** another (change-PIN does surface its error). Treat the rest as high-signal-but-unconfirmed: the sample so far is roughly 1 real to 1 false. |
| iOS build/link, iOS runtime, iOS stubs | ⚠️ **not swept** — the three dedicated iOS agents failed before running. §6 is from my own direct inspection, so it is sound but **not exhaustive**. |

### Compile checks run on this working tree (Windows host)

| Check | Result |
|---|---|
| `compileCommonMainKotlinMetadata` | ✅ pass — `commonMain` has no JVM-only API leakage |
| `:androidApp:compileDebugKotlin` | ✅ pass |
| `expect`/`actual` coverage | ✅ every module's iOS actual count matches Android's exactly — no missing actuals |
| **`:shared:umbrella:compileKotlinIosArm64`** | ✅ **pass — all 20 modules' `iosMain` compile on Windows** |
| `compileKotlinIosSimulatorArm64` / `IosX64` | ✅ pass |
| `testDebugUnitTest` | ✅ **239 tests, 0 failures** — the project's first tests (see §1.2) |
| `connectedDebugAndroidTest` | ✅ **22 tests, 0 failures** on a Pixel 6a emulator — the shared data layer and the assembled DI graph on real embedded SQLite (see §1.2b) |
| `compileTestKotlinIosSimulatorArm64` | ✅ pass — the iOS test tree compiles on Windows too |
| Android app on emulator | ✅ launches, Koin resolves all 83 definitions, no `FATAL EXCEPTION` |

### 1.2 Runtime verification of the shared logic

Because ~90% of the code is shared, the changed logic can be exercised for real without an iPhone.
This is behavioural verification, not type-checking:

**239 unit tests, 0 failures**, plus 6 iOS-only suites that are written and compile but have **not run** — see the correction in §1.3.
The repo previously had **no test suites at all** — the audit found
"no tests ported for any auth/account code path", and that was true project-wide.

`scripts/check-no-hardcoded-credentials.sh` runs first in CI and fails the build on
credential-shaped literals in shipping source — the known-leaked values, credential form state
initialised from a string literal, and `scheme://user:pass@host` URLs. It was verified by
reintroducing the original bug and confirming it trips (it caught it on two independent rules),
then reverting. It checks the working tree only; the values already in history have to be rotated
at the provider.

`.github/workflows/verify.yml` now enforces all of this on every push and PR: common metadata,
Android, both iOS targets, and the test suite — on Linux, in a few minutes, instead of waiting on
the ~20 min macOS job. It also fails loudly if `kotlin.native.enableKlibsCrossCompilation` is ever
removed from `gradle.properties`, since without it the iOS compile tasks are *skipped* rather than
failed and the job would pass vacuously.

### 1.2b The shared layer now runs on a device

`209 unit tests` above link **desktop** SQLite (`JdbcSqliteDriver`). Neither shipping platform uses
that: Android runs `AndroidSqliteDriver` over Android's system SQLite, iOS runs `NativeSqliteDriver`
over Apple's. Both are *embedded* builds — older and more conservative than the desktop one — so SQL
the unit tests accept can still fail on a phone.

Since ~90% of this project is shared code and the data layer is shared in full, running it on an
Android emulator exercises the same statements, the same generated SQLDelight code and the same
reactive machinery that iOS will execute. **22 instrumented tests, 0 failures**, on a Pixel 6a
emulator (API 36):

| Suite | What it establishes on a real embedded SQLite |
|---|---|
| `SharedDataLayerDeviceTest` (12) | Schema, indexes, FTS4 virtual tables and triggers all create; FTS triggers actually mirror inserts; `INSERT OR REPLACE` really does renumber `channelId` (the custom-group data loss) while the differential path keeps it stable; the flexible-search `= ''` slot guard behaves; transactions coalesce notifications and roll back; EPG child rows are deletable by `userId` regardless of order; re-login updates credentials without renumbering the account |
| `ReactiveQueriesDeviceTest` (6) | Koin hands out **one** `SqlDriver` and one `HitvDatabase` — the assumption every converted flow and every paging source rests on; `asFlow().mapToList(…)` genuinely re-emits on the shipping driver after a favourite toggle and after a sync-style insert; an unrelated table does not wake it; a rolled-back transaction never reaches observers |
| `RepositoryGraphDeviceTest` (4) | The **whole graph assembled**: `commonModule` + `databaseModule` + `networkModule` + `dataModule` started for real, `StreamRepository`/`MovieRepository`/`CustomGroupRepository` all resolving, one shared `SqlDriver` across it, and the repository-level flows (`getFavoritesChannel`, `getAllChannelsFlow`) re-emitting after writes |

The single-driver check is the one worth calling out. If that Koin `single` ever became a `factory`,
each repository would open its own connection, writes on one would never notify listeners on
another, and every list in the app would silently revert to being stale — with nothing throwing and
nothing failing to compile. It is exactly the class of defect this audit kept finding, and it is now
guarded by an assertion rather than by reading the module.

One incidental finding from assembling the graph outside the app: `PreferencesHelper`,
`AppInfoProvider` and `PlatformDetector` all read a process-global `AndroidContextHolder.applicationContext`
that only `HitvApplication` sets. Constructing the graph without seeding it fails with
`lateinit property applicationContext has not been initialized`. Not a defect — but it is a hidden
initialisation-order dependency, and the iOS side has no equivalent holder, so it is worth knowing
before anything else tries to build these outside normal app startup.

The app itself was also installed and launched on the emulator: process alive, no `FATAL EXCEPTION`,
no Koin resolution failure, no `SQLiteException` on cold start.

**What this is not.** It is not a substitute for a pass on real iOS hardware. The drivers differ, the
platform actuals differ (Keychain, AVPlayer, BGTask, `NSXMLParser`), and nothing here exercises those.
What it does do is move the *shared* half of the port — the majority of it — from "logic verified on
desktop SQLite" to "verified running, assembled, on a device". The iOS-specific half is covered as
far as it can be by §1.3, and the remainder is §7 Step 0.

### 1.3 iOS test suites: **executed on a simulator in CI** — 129 of 139 passing

The workflow is committed and has run. The iOS suites are no longer hypothetical:

| Suite | Result |
|---|---|
| `EpgStreamingLoaderIosTest` | ✅ the new `NSXMLParser` streaming path, entity decoding, control-char sanitising, a 2,000-programme document |
| `DecompressContentIosTest` | ✅ real gzip inflation through zlib |
| `AVPlayerTeardownIosTest` | ✅ the `onDispose` sequence the three player hosts use |
| `AVPlayerPlaybackIosTest` | ✅ real HLS decode of Apple's bipbop stream |
| every `commonTest` | ✅ compiled and run against Kotlin/Native |
| `KeychainSettingsIosTest` (5) | ⚠️ **skipped — cannot run in this host** |
| `KeychainMigrationIosTest` (5) | ⚠️ **skipped — cannot run in this host** |

**The Keychain path is not covered by CI, and a green build does not imply it works.**

All ten Keychain tests failed with `errSecNotAvailable (-25291)`, "No keychain is available". That is
a property of the test host, not of the code: a Kotlin/Native test binary is a bare executable, not
a signed application bundle, so it has no bundle identifier and no keychain access group and the
Security framework has no keychain to give it. The real app is a signed bundle and gets the default
access group, which is why Keychain-backed preferences work there.

They now skip with a loud printed reason rather than failing, so a red build still means something
is genuinely broken. But the coverage is gone and must not be assumed: **the Keychain is verified
only by the device pass in §7 Step 0** — install over an existing build, confirm you are still
logged in and the parental PIN survived. Making these assertions runnable in CI needs an app-hosted
XCTest target, which cannot be added from a machine without Xcode. Recorded as follow-up.

*(An earlier revision of this section claimed these suites had run and passed. They had not; the
workflow was uncommitted. That correction stands — and this is what actually running them revealed.)*

The Linux job proves `iosMain` type-checks; it cannot prove the cinterop calls *behave*. A second
job, `verify-ios` (macos-15), is written to close that:

- **`./gradlew iosSimulatorArm64Test`** — runs the suite on a real iOS simulator. `commonTest`
  compiles for iOS too, so every test listed above executes there as well as on the JVM.
- **`KeychainSettingsIosTest`** (`shared/core/common/src/iosTest`) — the first test that exercises
  a platform actual for real, driving the Keychain through `SecItemAdd` / `SecItemCopyMatching`:
  write/read round-trip, overwrite rather than `errSecDuplicateItem`, delete, visibility from a
  fresh `Settings` instance (the property that keeps a user logged in across restarts), and the
  punctuation-heavy value shapes real credentials contain.
- **`AVPlayerTeardownIosTest`** — runs the exact `onDispose` sequence the three player hosts use
  (remove periodic time observer → pause → detach item), plus mid-session item replacement on the
  retry path, repeated add/remove cycles as happens on channel switching, and double-detach.
  `removeTimeObserver` raises `NSInvalidArgumentException` for a token that was never added,
  already removed, or belongs to another player — an uncaught ObjC exception that kills the app on
  *leaving* a player, which is easy to introduce and easy to miss because it only fires on the way
  out. Nothing but execution catches it.
- **`KeychainMigrationIosTest`** — plants legacy plaintext values in the `pt.hitv.secure` plist and
  invokes the real factory: values land in the Keychain, the plaintext copies are deleted, a newer
  Keychain value is not clobbered by a stale plist one (otherwise a password change would silently
  revert on next launch), clean installs are a no-op, and repeated launches are idempotent.
- **`AVPlayerPlaybackIosTest`** — actually **decodes a real HLS stream** on the simulator (Apple's
  long-standing `bipbop` reference stream): reaches `readyToPlay`, reports a finite duration, the
  clock advances under `play()`, the periodic time observer fires repeatedly, and `seekToTime`
  moves the playhead — the last of which backs the movie/series resume path. It also confirms an
  unreachable host never falsely reports `readyToPlay`, which is the premise the retry ladder and
  the watchdog rest on.
  This is the one network-dependent test in the suite, and it is deliberately *not* written to
  soft-pass when the network misbehaves: a test that silently succeeds when it did not run is the
  exact failure mode this audit spent its time removing. Quarantine it explicitly if it goes flaky.
- **`linkDebugFrameworkIosSimulatorArm64`** — proves the Xcode-facing link step works, independent
  of signing and TestFlight.
- **Builds and launches the real app in a simulator.** Unsigned simulator build via `xcodebuild`
  (the project's run-script phase calls `embedAndSignAppleFrameworkForXcode`, so the framework is
  built for the simulator automatically), then `simctl install` + `launch`, then asserts the
  process is still alive 20 s later and dumps any crash report if not. This is the **only** place
  the app's own launch path runs on iOS, and it covers two things no unit test can:
  - `BGTaskScheduler.register(forTaskWithIdentifier:)` in `iOSApp.swift` `init()` executing
    against the **real** `BGTaskSchedulerPermittedIdentifiers`. A mismatch there raises an ObjC
    exception Kotlin/Native cannot catch, killing the app at launch. A test binary carries the
    test host's Info.plist, not the app's, so this is unreachable from `iosSimulatorArm64Test`.
  - Koin init, the SQLDelight open, the tri-state boot check and the first Compose frame running
    on iOS — the cold-start path that had previously only been exercised on an Android emulator.

### 1.4 BGTask identifiers: a crash that cannot be caught

`-[BGTaskScheduler submitTaskRequest:error:]` raises an ObjC `NSInternalInconsistencyException`
when the identifier is absent from `BGTaskSchedulerPermittedIdentifiers`, and **Kotlin/Native
cannot catch ObjC exceptions** — so the `catch (e: Throwable)` in `BackgroundSyncManager.ios.kt`
does not cover it, despite appearances. The same applies to `register(forTaskWithIdentifier:)` at
launch. Drift between the identifier's three independently-edited homes (the Kotlin constants, the
Swift registration, `Info.plist`) is therefore a hard crash, not a degraded sync.

`scripts/check-bgtask-identifiers.sh` compares all three and fails the build on any mismatch. It
was verified by introducing a one-character typo into `Info.plist` and confirming it trips on both
the Kotlin and Swift comparisons, then reverting. The misleading `catch` now carries a comment
saying what it does and does not cover.

This is the closest thing to coverage for BGTask that exists off-device: a simulator unit test
cannot submit a task request without the host app's Info.plist, and attempting it would terminate
the test runner rather than fail a test.

It runs on master pushes and PRs rather than every branch push, because macOS runners bill at 10×
Linux; use `workflow_dispatch` when working on a branch that touches `iosMain`.

**Wiring this up immediately found a latent build bug.** `SqlDelightConventionPlugin` put the
JVM-only JDBC `sqldelight-sqlite-driver` on **`commonTest`**, so it was also pulled into
`iosSimulatorArm64TestCompileKlibraries`, where that artifact does not exist for Kotlin/Native —
every SqlDelight module failed to compile its iOS tests with
`Could not resolve app.cash.sqldelight:sqlite-driver`. It had been invisible for as long as nothing
compiled iOS test code, and would have failed the new job on its first run. Moved to
`androidUnitTest`, where it is actually usable.

- `SyncPreservesUserStateTest` (8) — runs against a real in-memory SQLite DB via `JdbcSqliteDriver`:
  favourites, recently-viewed timestamps and pinned/hidden/default categories all survive a
  simulated re-sync; the snapshot captures only touched rows and is scoped to one user; restore is
  key-matched rather than fuzzy. Includes a test that *reproduces the original wipe*, so the
  regression cannot return silently.

- `EpgParserFilterTest` (10) — the allowlist and ±window that bound the retained programme graph:
  case/whitespace-insensitive channel matching, expired and out-of-window programmes dropped,
  `<channel>` elements never filtered, programme ids still unique after the early-exit refactor,
  and XMLTV date parsing pinned to exact UTC instants so the window tests can't pass vacuously.
  *(Two of these failed on first run and caught a bad epoch constant in the test itself — the
  filter logic was correct.)*
- `PlaybackStartWatchdogTest` (8) — fires exactly once after the deadline; cancel before the
  deadline suppresses it; cancel *after* does not rewrite history; re-arming discards the stale
  deadline; repeated arm/cancel cycles never double-fire; `timeoutMs = 0` disables it; `cancel()`
  is safe unarmed and repeated. Uses `runTest`'s virtual clock, so the 25 s deadline is free.
- `SettingsScreenRegistrationTest` (3) — asserts every settings screen reachable from More Options
  resolves through `ScreenRegistry` (this is the exact production crash, so an unregistered screen
  fails here instead of on a user's tap), that FEEDBACK resolves to `FeedbackVoyagerScreen`, and
  that LIVE_EPG stays unregistered while its row is hidden. That last one deliberately fails when
  the EPG grid lands, as the reminder to flip `showEpgEntry`.

**On-device (emulator) run.** The Android app was installed and launched on a Pixel 6a emulator:
Koin started all 82 definitions in ~2 ms, the DB opened, the first Compose frame rendered, and the
process stayed alive with zero `FATAL EXCEPTION` entries. That covers the shared cold-start path
iOS also runs — Koin graph resolution, SQLDelight open, boot check, first frame — leaving only the
platform actuals unexercised.

**The credential fix was confirmed visually**, not just in source: the login screen renders all
three fields empty. `LoginScreen` is `commonMain`, so that is literally the composable iOS renders.

**Superseded:** an earlier revision said the iOS platform actuals were entirely unverified. Since
then AVPlayer teardown, real HLS decode, the Keychain and its migration, the framework link and the
app's own cold start all execute on an iOS simulator in CI — see §1.3. What is left is in §6.1.

### iOS Kotlin now compiles on this Windows host

This project previously assumed — and the older notes stated — that iOS Kotlin errors could only
surface in GitHub Actions. **That is not true.** Kotlin/Native can cross-compile iOS *klibs* from
any host; only *linking* a framework or IPA needs Xcode. Enabling one flag turns iOS Kotlin into
a locally type-checked source set:

```properties
# gradle.properties (now committed)
kotlin.native.enableKlibsCrossCompilation=true
```

```bash
./gradlew :shared:umbrella:compileKotlinIosArm64   # type-checks every iosMain source set
```

Without the flag Gradle marks `compileKotlinIosArm64` as `SKIPPED` on a non-Mac host, which is
what made it look impossible. This catches missing `actual`s, wrong cinterop signatures, and the
ObjC property-setter mistakes this codebase has hit before — all of which used to cost a CI round
trip. **Run it before every push.** It does *not* replace CI: framework linking, the Xcode
archive, dSYMs and TestFlight upload are still Mac-only.

**Verifying a cinterop signature without a Mac.** The iOS platform klibs ship with the Kotlin/Native
distribution, so exact Kotlin signatures can be read directly instead of guessed:

```bash
K=~/.konan/kotlin-native-prebuilt-windows-x86_64-2.1.21
"$K/bin/klib.bat" dump-metadata "$K/klib/platform/ios_arm64/org.jetbrains.kotlin.native.platform.UIKit"
```

That is how the signatures behind the §4.1 iOS changes were confirmed rather than assumed — e.g.
`BGTaskScheduler.submitTaskRequest(taskRequest, error): Boolean` (it returns a `Boolean` and keeps
the error out-param; it does **not** throw), `UIApplication.connectedScenes: Set<*>`,
`UIWindowScene.windows: List<*>`, `UIScene.activationState: Long`, and
`AVPlayerViewController.player: AVPlayer?`.

---

## 2. Architecture (accurate)

| Layer | Original (Android) | KMP (Android + iOS) |
|---|---|---|
| UI | Jetpack Compose | Compose Multiplatform |
| Navigation | Navigation Compose + `@Serializable` | Voyager + `ScreenRegistry` |
| DI | Hilt (KSP) | Koin |
| Database | Room (23 entities) | SQLDelight (23 tables — full coverage) |
| Network | Retrofit + Gson | Ktor + kotlinx.serialization |
| Preferences | EncryptedSharedPreferences | multiplatform-settings (EncryptedSharedPrefs on Android, **plaintext NSUserDefaults on iOS**) |
| Paging | AndroidX Paging 3 | Cash App Paging |
| Images | Coil 2 | Coil 3 |
| EPG parser | Simple XML Framework, streaming | `XmlPullParser` streaming (Android) / **whole-file regex (iOS)** |
| Players | ExoPlayer Activities | ExoPlayer (Android) / AVPlayer + AVKit (iOS) |
| Sync | WorkManager | WorkManager (Android) / **BGTaskScheduler (iOS — real, not a stub)** |
| Billing | Play Billing | Play Billing (Android, ported but unwired) / **StoreKit stub (iOS)** |
| Analytics | Firebase | NoOp both platforms (intentional) |

### Corrections to the previous revision

- iOS background sync is **implemented**, not a stub — `BGTaskScheduler` handlers are
  registered in `iosApp/iosApp/iOSApp.swift:20-40`, identifiers are declared in
  `Info.plist:61-65`, and `shared/umbrella/src/iosMain/kotlin/pt/hitv/SyncBridge.kt` drives
  the Kotlin sync. The defect is *what it calls* (see §5), not the plumbing.
- `shared/app-ios` is listed in the old module tree but **is not in `settings.gradle.kts`** and
  contains no sources. It is a dead directory; only stale `build/` output remains.
- SQLDelight table count is **23, matching Room**, not 13.
- Movie/series players do exist and work — but series episodes never reach the series player
  (§4). "✅ Series player" in the old doc scored a file that is never called.

---

## 3. What is genuinely well ported

Credit where due — these are faithful, verified ports, and they are the reason the remaining
work is tractable:

- **Database schema.** All 23 Room entities have SQLDelight equivalents, column-for-column,
  including sync-tracking (`lastUpdated`, `lastSeen`, `contentHash`, `syncVersion`), catch-up
  columns, unique indices, and FTS4 with its three mirror triggers per table. Only
  `Programme.last_updated` is missing.
- **`CatchUpUrlBuilder`** — line-for-line, all modes (XC / Flussonic / Shift / Default /
  Append) and all specifiers, server-timezone-aware. Only `java.time` → `kotlinx-datetime`.
- **Channel list UI** — `MobileChannelsLayout.kt` reproduces the category top bar, 1500 ms /
  3-char debounced search, history chips, category sheet with counts, and the
  single-tap-preview / double-tap-play / long-press-favourite interaction faithfully.
  `StreamViewModel` is a near line-for-line Koin port and *adds* a correct `syncVersion` trigger.
- **`LoginValidator`**, `ErrorDialog`, `EditAccountDialog`, `SwitchAccountViewModel` — faithful,
  including the "leave blank to keep password" semantics and auto-switch-on-delete.
- **Cold-start session restore** is *better than the original* and correctly mirrored on both
  platforms (`HitvApp.kt` and `MainViewController.kt` run the same tri-state boot check).
- **Movies/series data layer** — `MovieViewModel`, `MovieInfoViewModel`, `SeriesViewModel`,
  `TvShowRepositoryImpl` and their `.sq` queries are near line-for-line, including the
  `INSERT OR IGNORE` semantics that preserve `playbackPosition` across re-fetch.
- **Catch-up playback** — a real, complete feature on both platforms: EPG overlay, archive
  sheet with day grouping, seek slider, speed chip, LIVE pill, prev/next programme.
- **`SleepTimerManager`** — same presets and behaviour, rewritten on coroutines. Wired on all
  three players, both platforms.
- **Android EPG path** — `EpgStreamingLoader.android.kt` is a faithful streaming port of
  `XmltvParser` + `XmlSanitizingInputStream`.
- **Aspect-ratio cycling** — cleanly abstracted with real Android and iOS actuals.
- **iOS BGTask plumbing** — correct, and correctly ordered before Koin init.

---

## 4. P0 — blockers

Ten confirmed P0s. Every one of them is visible within about two minutes of using the app.
**All but billing are now fixed** (2026-07-26); see §4.1 for exactly what changed.

| # | Blocker | Where | Effort | Status |
|---|---|---|---|---|
| 1 | **Live provider credentials hardcoded into the login form** and shipped in release on both platforms | `shared/feature/auth/.../login/LoginScreen.kt:59-62` | S | ✅ fixed — **credentials still need rotating** |
| 2 | **"Live with EPG" crashes the app** — `HitvScreen.LIVE_EPG` is never registered | `HitvNavigation.kt:157` + `SettingsScreenRegistration.kt` | S | ✅ fixed (row hidden) |
| 3 | **"Feedback & Support" crashes the app** — `HitvScreen.FEEDBACK` is never registered | `HitvNavigation.kt:151` + `SettingsScreenRegistration.kt` | S | ✅ fixed (registered) |
| 4 | **Series episodes open the LIVE CHANNEL player** — no resume, no progress, no prev/next | `SeriesDetailVoyagerScreen.kt:52` | S | ✅ fixed |
| 5 | **Parental controls are entirely inert** — `PremiumStatusProvider` hardcoded `false` | `KoinIOS.kt:94-99`, `AndroidPlatformModule.kt:31-37` | M | ✅ fixed (un-gated — see §4.1) |
| 6 | **Nobody can buy premium on either platform** — billing wired into Koin, connected to nothing | `PremiumVoyagerScreen.kt:18-20` | L | ⬜ open — product decision |
| 7 | **iOS EPG parser loads the whole XMLTV feed into memory** and regex-scans it | `EpgStreamingLoader.ios.kt:60-64`, `EpgParser.kt:57-120` | L | 🟡 largely mitigated — see §4.1 |
| 8 | **Full EPG grid screen does not exist** on either platform | `shared/epg/` has zero `@Composable` | L | ✅ ported — reminders + paywall deferred (§4.1) |
| 9 | **iOS movie playback position is never saved or resumed** — `streamId` is always 0 | `PlayerLauncher.ios.kt` | S | ✅ fixed |
| 10 | **A content re-sync wipes favourites, recently-viewed and category preferences** | `StreamRepositoryImpl` sync paths | M | ✅ data loss fixed (see §4.1); true differential sync still open |

### 4.1 Fixed in the 2026-07-26 pass

All changes below compile clean under `compileCommonMainKotlinMetadata`,
`:androidApp:compileDebugKotlin`, **and all three iOS targets** — see §1. The iOS code is
type-checked, including every cinterop call, whose signatures were confirmed against the platform
klibs. What remains unverified is *runtime* behaviour on a device and the Xcode link/archive step.

**Crashes and dead wiring**
- Episodes now call `launchSeriesPlayer` instead of `launchChannelPlayer`
  (`SeriesDetailVoyagerScreen.kt`), reviving `SeriesPlayerActivity` and `SeriesPlayerHost.ios.kt`
  — both were fully-written dead code. Restores per-episode resume, progress saving, prev/next.
- New `FeedbackVoyagerScreen`, registered against `HitvScreen.FEEDBACK`. Ported from the
  original's `FeedbackRoute` (screen-view analytics, submit-result handling, 10-char guard;
  Toasts → snackbar). Also restored the `isSubmitting` field the port had dropped from
  `SuggestionUiState`.
- `LIVE_EPG` row hidden behind `MobileMoreOptionsScreen(showEpgEntry = false)`, with the
  registration site documenting that both flip together when the grid lands. Hiding rather than
  faking it, because the grid genuinely does not exist.
- Hardcoded credentials removed; the original's `defaultUsername`/`defaultPassword`/`defaultUrl`
  parameter indirection restored, defaulting to `""`.

**Playback reliability (the "iOS first-run" core)**
- New shared `PlaybackStartWatchdog` (`commonMain`, 25 s, coroutine-based) — a port of the
  original's `PlaybackStartWatchdog`, wired into the iOS channel, movie and series hosts.
  Deliberate divergence: the original's callback is analytics-only, which is useless here
  (analytics is NoOp) and insufficient on iOS, where nothing else detects a load failure. Here
  it surfaces a user-visible error. Documented in the class KDoc.
- Movie and series players got an error surface: `MoviePlayerScreen`/`SeriesPlayerScreen` now take
  `errorMessage`/`onRetry`/`onDismissError` and render the existing shared `PlaybackErrorDialog`.
  Previously a failed movie was a black rectangle with no affordance.
- `presentFromTop` resolves the window via `connectedScenes` instead of the deprecated
  `keyWindow`, whose nil-return silently made the player never appear.
- `KeepScreenOnAndFullscreen()` added to the movie and series iOS hosts — the screen used to dim
  and lock mid-playback. (The channel player already had it via `ChannelPlayerScreen`.)
- iOS movie `streamId` is now derived from the URL when the caller passes 0, mirroring
  `MoviePlayerActivity:154-156`, and the saved position is loaded and seeked to once resolved.
  Continue Watching on iOS was permanently empty before this.
- `AVPlayerSurface` detaches its player (`vc.player = null`) on dispose.

**EPG memory and correctness (P0 #7 mitigation)**
- `EpgParser.parse` / `parseProgrammes` and `EpgStreamingLoader.fetchAndParse` now accept
  `channelFilter`, `minEndTimeMs`, `maxStartTimeMs` — the allowlist and ±7-day window the
  original has (`XmltvParser.kt:64-65`) and the port had dropped. Rejections happen on the
  attribute match, *before* touching the element body, on both the iOS regex path and the
  Android streaming path.
- New `Channel.sq` query `selectEpgChannelIdsForUser` supplies the allowlist;
  `StreamRepositoryImpl` passes it plus the window on both EPG paths. Returns `null` (no
  filtering) when the channel table is empty, so a first sync can't discard the whole feed.
- Removed the **duplicate `insertEpgDB`** in `SyncManagerImpl.syncEpg` — `fetchEPG` already
  inserts on both branches, so every programme was being written twice with the parsed graph
  held alive across both.
- `fetchEPG` now falls back to the account's stored `epgUrl` when no override is given, so
  M3U/playlist accounts get an EPG at all (the sole caller passes `null`).

**Why #7 is "largely mitigated", not fixed:** the retained object graph is now bounded by the
user's own channels and a 14-day window, which is what actually blew the BGTask memory ceiling.
But the raw XML is *still* materialized as one Kotlin `String` on iOS. Fully fixing that needs a
streaming `NSXMLParser` actual, which is a blind rewrite on a Windows host and the single most
likely thing to break the CI build — deliberately deferred rather than shipped unverified.

**Background sync**
- `BackgroundSyncManager.ios` now honours `submitTaskRequest`'s return value; a rejected request
  (unregistered id, over the pending limit, Background App Refresh disabled) no longer reports
  success to the user. (Return type verified as `Boolean` against the BackgroundTasks klib.)

**Security**
- iOS secure storage moved from **plaintext NSUserDefaults to the Keychain**
  (`PreferencesHelper.ios.kt`), using multiplatform-settings' `KeychainSettings`, which was
  already on the classpath. The keys involved are `username`, `password`, `hostUrl` and
  `parental_control_pin` — the IPTV password and the parental PIN were readable from any
  unencrypted device backup. Includes a one-time migration that copies surviving values out of
  the old plist and deletes them, so upgrading users are not logged out. Android already used
  `EncryptedSharedPreferences` for the same key set, so this also removes a platform inconsistency.

**EPG full grid ported (P0 #8)**
- `getProgrammesForCategory` was a `return emptyList()` stub, so the grid had no data source even
  though `Programme.sq:selectProgrammesForCategory` already existed. Implemented, including
  `hasCatchUp` derived from the Channel row's `tvArchive` flag (one lookup per distinct channel,
  not per programme row).
- Ported `EpgScreenMobile` (Canvas timeline, draggable 24-hour programme blocks, pulsing
  now-indicator, tap-to-expand details card, "Now" jump button) and `EpgCategorySelectionScreen`
  (search, scroll-position preservation, three distinct empty states), plus `filterEpgData`,
  `EPGChannel.hasCatchUp`, `EPGEvent.isPast` and the date helpers.
  Wired through a new `LiveEpgVoyagerScreen`, `HitvScreen.LIVE_EPG` is registered, and the More
  Options row is visible again (`showEpgEntry = true`).
- **Lives in `feature:channels`, not the `epg` module.** `core:data` depends on `:shared:epg` for
  `EpgParser`, so putting Compose + navigation in `epg` would invert the dependency graph.
- **`Calendar`/`SimpleDateFormat` → `EpgUtils`** on kotlinx-datetime, so it runs on iOS.
- **Two things deliberately not ported, and deliberately not stubbed:**
  - *Programme reminders.* The original schedules an `AlarmManager` alarm; iOS needs a
    `UNUserNotificationCenter` actual, a permission flow and an Info.plist string. Rather than show
    a "Set reminder" button that silently does nothing, future programmes surface **no action**.
  - *Catch-up paywall.* Billing is unwired and `UngatedPremiumStatusProvider` grants premium to
    everyone, so the paywall branch would be unreachable. Catch-up plays directly; restore the gate
    when billing lands.
- Verified: compiles for Android **and both iOS targets**; 22 tests over the ported logic. The
  block-position arithmetic was extracted from the composable into `computeEventLayouts` precisely
  so it could be tested — an origin or scaling error there skews the whole grid against the
  timeline labels while still *looking* like a plausible guide. Tests cover contiguity, clipping an
  in-progress programme to the left edge, dropping ended/zero-duration entries, and the full
  24-hour span.
- **Rendered and inspected.** Reaching it through the app needs a logged-in account, so it was
  rendered on an Android emulator via a throwaway harness in `MainActivity` (synthetic channels and
  programmes anchored to the grid's own half-hour origin), screenshotted, then reverted. Since the
  grid is entirely `commonMain` Compose, what renders there is what renders on iOS bar platform
  font metrics. Confirmed visually: blocks contiguous and aligned to the timeline labels, past /
  live / future shading correct, per-block progress bars terminating at exactly the same x as the
  now-indicator (independent evidence that the layout maths and the indicator share an origin),
  "Now" button and channel column correct. Two things came out of actually looking:
  - The category screen's 48dp circular back button sat flush against the screen edge and was
    visibly clipped. The original has the same missing horizontal padding; added `start = 8.dp`,
    which also keeps it clear of the display cutout on iOS.
  - A suspected double status-bar inset turned out **not** to be a bug: `statusBarsPadding()` is
    consumption-aware, so the screen's own call on top of `TabContentHost`'s contributed zero.
    Removing it left the layout pixel-identical. Kept removed so the inset has a single owner, and
    the code comment records why rather than claiming a fix that never happened.

**Background sync is actually scheduled (P1)**
- The re-arm lived in `BackgroundSyncSettingsViewModel.init`, and that ViewModel is only
  constructed when the user opens **More Options**. So "enable background sync → quit the app →
  reopen it and go straight to watching" left the OS tasks unscheduled indefinitely, while the
  settings screen still showed the feature as on.
- **Worse on iOS than Android.** A `BGAppRefreshTaskRequest` is one-shot; the Swift launch handler
  chains the next one after each firing, but that chain only exists once something submits the
  first request. A missed re-arm means background sync stops *permanently*, not just late.
  Android's WorkManager keeps unique periodic work across restarts, so there it was closer to
  belt-and-braces.
- New `BackgroundSyncBootstrapper` in `core:sync`, called from the cold-start path of both
  platforms next to the parental-session clear. The pref keys moved from the settings feature into
  `core:sync` so the boot path can read them without depending on a UI module.
- Deliberately does **not** change the default — background sync stays off until the user asks for
  it. This only makes an already-expressed preference take effect.
- The decision is a pure `computeSyncSchedule(...)` rather than inline, because
  `BackgroundSyncManager` is an `expect class` and `PreferencesHelper` is concrete — neither can be
  substituted in `commonTest`, so extracting it is what makes it testable at all. 7 tests cover
  off/on, stored intervals, the fallback when a key was never written (`getStoredLongTag` returns
  0, and treating that as a literal interval would schedule a zero-delay task), negative values,
  and the wifi-only flag.
- Verified the Koin graph still resolves on-device: 83 definitions (was 82), no
  `NoDefinitionFoundException`, app alive after cold start.

**VOD players open in landscape (P1, Android)**
- The original locks `MoviePlayerActivity` and `SeriesPlayerActivity` to landscape in its manifest
  (`android:screenOrientation="landscape"`). The port dropped it, so a movie or episode opened in
  whatever orientation the phone was in — normally portrait, giving a letterboxed strip of video
  with most of the screen black.
- Restored on both activities. The live channel player is deliberately left unlocked in both
  projects: it has a rotation toggle and a sidebar that uses the extra height in portrait.
- Verified at the artifact level rather than the source: `aapt2 dump xmltree` on the built APK
  shows `screenOrientation=0` (`SCREEN_ORIENTATION_LANDSCAPE`) on both activities.
- **iOS is not covered by this.** There is no manifest equivalent; forcing landscape means
  overriding `supportedInterfaceOrientations` on the presented `ComposeUIViewController`, and an
  earlier build (32) deliberately reduced `toggleDeviceOrientation` to a no-op in favour of
  Info.plist. So on iOS a movie still opens in the current orientation and the user rotates, with
  the manual rotate button from build 27. Worth deciding explicitly rather than leaving as drift.

**Search history stops recording every keystroke (P2)**
- `rememberSearchTerm` was called on every query change with only a blank check, so typing
  "matrix" wrote six entries — `m`, `ma`, `mat`, `matr`, `matri`, `matrix` — and the recent-searches
  list filled with prefixes of a single search. The original debounces 1.5 s and requires 3
  characters (`MovieViewModel.kt:199`).
- Ported both guards to the movie and series ViewModels.
- **Process note:** the scripted edit that applied this silently deleted a top-level
  `data class PagingParams` from the end of both files — my "insert before the final brace"
  heuristic cut past the class body. The series module failed to compile and gave it away; the
  movies module still compiled because it resolved `PagingParams` from the series module, so it
  would have shipped a cross-module dependency that was never intended. Both restored and diffed to
  confirm only the intended change remains.

**Movie detail stops falling back to "Limited information" (P1)**
- The port gated the full detail screen on `info.name` **and** `info.movieImage`, so any movie
  without poster art — and any movie whose name the provider put in `movieData` rather than `info`,
  which is common — was dumped to the minimal "Limited information available" screen despite having
  perfectly usable metadata.
- The original is explicit that only a *resolvable* name matters, in a comment written to prevent
  exactly this: *"Everything else in `info` (image, plot, cast…) is best-effort and the UI already
  degrades gracefully when it's missing. The minimal 'limited information' screen is reserved for
  responses with no usable metadata at all."*
- Ported `resolveMovieName` (info.name, else movieData.name) and `canShowFullScreen` faithfully,
  and routed both call sites through them. 6 tests pin the behaviour, including the missing-poster
  and name-only-in-movieData cases.

**Continue Watching paging returns page 2 (P1)**
- `Movie.sq` and `TvShow.sq`'s `selectContinueWatching` were `LIMIT ?` with **no `OFFSET`**, while
  the paging sources passed only a page size. Every page therefore re-fetched rows 1..N: scrolling
  Continue Watching repeated the same items forever.
- Added `OFFSET ?` to both queries and threaded the real offset through the two paged call sites;
  the two home-row call sites pass `0` explicitly, since they take a fixed top-N.
- **I introduced half of this myself** in the fix immediately below: the new series Continue
  Watching branch mirrored the movie implementation, and inherited its missing offset. Caught by
  reading the next audit item rather than by any test — worth noting as a limitation of fixing by
  analogy.
- Verified on-device that the schema change is clean: app launches, no `SQLiteException`, no
  "no such column" errors.

**Series "See All" on Continue Watching returns results (P1)**
- `TvShowRepositoryImpl`'s paging had branches for Favorites, Recently Viewed and Last Added but
  **none for Continue Watching**, so the filter fell through to the generic category lookup and
  queried for a category literally named `"ContinueWatching"` — matching nothing. The row's
  "See All" opened an empty screen. The movie side had the branch all along, and the SQL the series
  side needed (`TvShow.sq:95 selectContinueWatching`) already existed.
- Root cause worth noting: `PagingConstants.kt` declares **two parallel sets of identical filter
  strings** (`MOVIE_FILTER_*` and `FILTER_*`). `SeriesScreen` passes one name, the repository
  branches on the other, and it only works because the values coincide. That aliasing is what let a
  missing branch look like a working one. Documented in the file, and `PagingFilterConstantsTest`
  now pins the two sets together — drift would silently empty screens rather than fail. Collapsing
  them into one set touches every call site and is left as a follow-up.

**Trailer links work and phantom Trailer buttons are gone (P1)**
- The port dropped the original's `YouTubeUrl` helper, then used the raw `youtube_trailer` field
  for two things it is not fit for: it built `https://www.youtube.com/watch?v=$raw` (so a field
  already holding a URL produced `watch?v=https://youtu.be/abc`), and it gated the Trailer button
  on `!isNullOrBlank()` (so any junk rendered a button that led nowhere).
- Ported `YouTubeUrl` into `core:common` verbatim — bare ids plus `watch?v=`, `youtu.be/`,
  `/embed/`, `/shorts/`, `/v/` — and routed both the link and the gating through it, on movie and
  series detail, portrait and landscape.
- 8 tests. One of them documents a non-bug: an arbitrary 11-character token *is* accepted, because
  it is structurally indistinguishable from a real video id. My first version of that test asserted
  null and was wrong; it is now pinned with the reasoning so nobody "fixes" it later.

**Series detail no longer spins forever on a failed fetch (P1)**
- `SeriesInfoContent` renders `SeriesLoadingScreen` whenever `seriesInfo == null`, and
  `loadSeriesInfo` left it null on failure: `Resources.Error` was logged to analytics and otherwise
  dropped, and the surrounding `catch (_: Exception) {}` swallowed thrown failures entirely. With
  nothing cached, the user sat on a spinner that never resolved and never explained itself.
- Added a `loadError` state and an error screen with **Retry** and **Go back**. The error is only
  raised when the cache also produced nothing — a stale-but-usable series still renders, which is
  the point of the cache-then-network order.
- Same shape as the player watchdog fix: the failure mode this port keeps producing is not a crash
  but an indefinite wait with no way forward.

**iOS EPG text is decoded correctly (P1, iOS-only)**
- Three defects in the regex parser, all invisible on Android because it streams through
  `XmlPullParser`, which handles them natively:
  1. **Numeric character references were never decoded.** `&#237;` / `&#x00E9;` stayed literal, so
     titles read `Not&#237;cias` instead of `Notícias` — which is essentially every programme in a
     Portuguese guide, the primary market for this app.
  2. **`.` does not match newlines in Kotlin**, so `<desc>`, `<title>` and `<display-name>` wrapped
     across lines did not match at all and came through empty. Real feeds wrap long synopses.
  3. **`&amp;` was decoded first**, so `&amp;lt;` became `&lt;` and was then decoded again into
     `<` — turning escaped text into markup. It is now decoded last, making the pass single-level.
- 9 tests cover decimal and hex references, the five predefined entities, the double-decode
  regression, multi-line `<desc>` and `<display-name>`, malformed references left untouched, and
  an astral-plane reference (surrogate pairing) not corrupting the string.

**DASH channels play, and ClearKey DRM works (P1, Android)**
- `createMediaSource` routed `.mpd` through `ProgressiveMediaSource` under a literal
  `// Would need DashMediaSource but skipping for now`. Progressive cannot parse an MPD manifest,
  so **every DASH channel failed** — while `media3-exoplayer-dash` was already a declared
  dependency, so nothing was actually blocking it.
- `licenseKey` was threaded from the Intent through `args` into the ViewModel and then **never
  used**, so DRM-protected channels could never play. SmoothStreaming (`.ism`/`.isml`) had no
  branch and fell through to progressive too.
- Extracted `LiveMediaSourceFactory`, a faithful port of the original's `MediaSourceFactory`:
  DASH / HLS / SmoothStreaming / progressive selection, plus ClearKey via a local licence response
  built from the `kidHex:keyHex` pair (no licence-server round trip). A malformed key degrades to
  plain DASH rather than crashing, as in the original.
- **Android only.** AVPlayer supports neither DASH nor ClearKey, so these channels remain
  unplayable on iOS whatever we do here — that is a genuine platform limit, not a port gap, and is
  worth knowing before a user reports "channel X works on my Android but not my iPhone".

**iOS actually decompresses gzipped EPG feeds (P1, iOS-only)**
- `decompressContent`'s iOS actual looked like it decompressed but never did: it copied the bytes
  and called `NSString.create(data:encoding:)` on the **still-gzipped** data, which returns null for
  compressed input, then fell through to `bytes.decodeToString()` — handing the EPG parser mojibake
  produced from binary. Its own comment admitted "this is a simplified implementation".
- Net effect: any provider serving a gzipped XMLTV payload gave **no EPG on iOS**, while Android
  inflated it correctly. A silent platform divergence in exactly the area this port most needed to
  match, and it failed as garbage rather than an error.
- Replaced with real inflation through zlib, which ships with the platform and has Kotlin/Native
  bindings. `inflateInit2(strm, 47)` — 15 window bits + 32 for gzip/zlib auto-detection — with
  chunked output since the inflated size is unknown up front. Every signature was checked against
  the platform klib before writing, not guessed.
- XZ stays unsupported (not part of the platform), but now returns empty instead of compressed
  noise, so callers report "invalid or non-XML content" rather than parsing junk.
- `DecompressContentIosTest` runs on the simulator against a **genuine gzip stream** generated
  outside this codebase — there is no encoder here for it to agree with, so only real inflation
  passes. Covers round-trip, the mojibake regression, pass-through of plain input, empty input,
  truncated streams failing closed, and the XZ contract.

**Channel list no longer truncates at the first protected channel (P1)**
- `ChannelPagingSource` queries `pageSize` rows, removes any in parental-protected categories, then
  computed `nextKey` from the **filtered** count. A single protected channel made the page look
  short, `nextKey` went null, and paging stopped — everything after that point was unreachable.
- Exhaustion is now decided by what the query returned. 6 tests pin the invariant, including the
  worst case where an entire page is protected and paging must still continue.
- **Another one this pass made live**: while `PremiumStatusProvider` forced parental controls off,
  `getProtectedCategoryIds` returned empty and nothing was ever filtered, so the bug could not fire.
  That is now five latent defects uncovered by enabling one dormant feature — blank-screen guard,
  missing session clear, PIN bypass, stale list, always-ask sentinel — plus this one. Parental
  control is the least-exercised code in this port and deserves the closest look on device.

**Background content sync is no longer a permanent no-op (P1 → arguably P0)**
- `performFullSync` skips any stage whose `SyncStageKeys.*_DONE` flag is set. Those flags exist so
  an **interrupted** sync can resume without redownloading completed stages — but they were only
  ever cleared by "Refresh Data" or logout, never on success. So every run after the first found
  all three set and skipped everything.
- Net effect: after one successful sync, periodic and background content sync did nothing, forever.
  The user's library silently stopped updating, and the sync UI still reported **success**, because
  skipping every stage returns `isSuccess = true`. Nothing looked wrong anywhere.
- Fixed by clearing the flags once all three stages complete, which preserves the intended
  semantics exactly: interrupted → flags survive → next attempt resumes; completed → flags cleared
  → next scheduled run does real work.
- Together with the app-start re-arm (also §4.1), background content sync now both *gets scheduled*
  and *does something when it fires*. Previously it failed on both counts, so the device-only
  question of whether iOS grants the BGTask a window was academic.

**EPG tables no longer grow without bound (P1 → arguably P0)**
- `insertEpgDB` **only appended**. `insertProgramme` uses an `AUTOINCREMENT` id, so every EPG sync
  created brand-new rows for every programme plus its title and description, and nothing ever
  deleted them. The only `deleteProgrammesByUserId` / `deleteTitlesByUserId` /
  `deleteDescriptionsByUserId` call sites were in account deletion.
- At a 6–12 hour sync cadence that is unbounded growth: real disk pressure on a phone and
  progressively slower EPG queries as the tables fill with duplicate generations of the same guide.
- Build 22's commit *"EPG query deduplicates overlapping programmes"* was a workaround for the
  symptom. This removes the cause; that dedup can stay as belt-and-braces.
- Guarded so a failed fetch cannot wipe the guide: the delete only runs when there is a parsed feed
  to replace it with. Clear-and-reinsert rather than a surgical diff because `Programme.last_updated`
  — the column the original uses for non-destructive sync — has no SQLDelight equivalent (§5). The
  trade-off is a brief mid-sync window with partial EPG, corrected by the next sync.
- Compounds with two earlier fixes in this pass: the duplicate `insertEpgDB` call in `syncEpg`
  (which doubled every generation) and the missing allowlist/window (which made each generation far
  larger than it needed to be).

**Background Sync status screen is reachable (P1)**
- `BackgroundSyncSettingsScreen` was complete — per-task last-run / next-run readouts and the
  battery-optimization row — and correctly registered against `HitvScreen.BACKGROUND_SYNC_SETTINGS`,
  with a `navigateToBackgroundSyncSettings()` helper. **Nothing ever called it.** The only sync UI
  the user could reach was the inline card in More Options, which has none of that.
- This is the third instance of the same wiring defect in this port (after the FEEDBACK and
  LIVE_EPG crashes): a screen written, registered, and then orphaned. Here it failed silently
  rather than crashing, so it was harder to notice.
- Added a "Sync Status & Battery" row to More Options that navigates to it.
- Directly relevant to the one gap that still needs a device: the last-run / next-run readout is
  how a user — or you, during the device pass — can tell whether a BGTask has actually fired.
  Without it there was no in-app evidence either way.

**"Never (always ask)" actually means always ask (P1, security)**
- The session-timeout picker stored `0` for "Never (always ask)". The session logic recognises
  `-2` (`SESSION_TIMEOUT_ALWAYS_ASK`), and `0` is not `> 0` either, so `getSessionTimeout()` fell
  through to its `else` branch and returned the **30-minute default**. A parent choosing the
  strictest option silently got the standard one — and the UI looked right, because the picker
  displayed the value it had stored.
- The sentinels moved onto the `ParentalControlManager` interface, with `ParentalControlManagerImpl`
  now aliasing them, so the picker and the session logic cannot drift apart again. 5 tests pin the
  values and the properties that allowed the bug: sentinels must be negative (so they can never be
  read as a real minute count), distinct, and never `0`.
- Live only because this pass made parental controls functional.

**Manage Categories reflects its own toggles (P1)**
- `getAllCategoryPreferences` was `flow { ...executeAsList()...; emit(all) }` — one shot — and
  `ManageCategoriesViewModel.togglePin` / `toggleHide` write to the DB **without reloading**. So
  pinning, hiding or setting a default category did nothing visible until the screen was rebuilt.
- Now combines four reactive sources (channel / movie / series categories + custom groups) via
  `asFlow().mapToList(...)`, preserving the original source ordering so the screen's grouping is
  unchanged. The old code carried a comment recommending exactly this.
- Same root cause as the locked-category list below; both are now reactive. The remaining one-shot
  flows (movies/series favourites, EPG) are listed in §5 and unchanged.

**"Live Buffer Size" now actually affects playback (P1)**
- The port read and wrote the preference, and the More Options row showed the selected value, but
  **no player consumed it on either platform** — changing it did nothing. The original feeds it to
  `DefaultLoadControl` via `PlaybackManager.createExoPlayer` and `ExoPlayerController`.
- Added `PlayerConfigFactory.liveBufferFor(...)` in `commonMain`, a verbatim port of the original's
  `createLiveLoadControl` mapping (`core/common/.../media/PlayerConfigFactory.kt:29-39`), wired
  into the Android channel player's `DefaultLoadControl` and into the iOS channel player's
  `AVPlayerItem.preferredForwardBufferDuration`.
- 7 tests pin all four profiles to the original's exact numbers, plus the `else -> medium`
  fallback, monotonic growth, and that live buffers stay shorter than VOD (live favours latency,
  VOD favours smoothness).
- Worth having on iOS specifically: buffer sizing is what a user reaches for when a stream stutters
  on mobile data, and it was a placebo control.

**Locked-category list is reactive (P1)**
- `getAllParentalControls` / `getProtectedCategoriesCount` were `flow { emit(executeAsList()) }` —
  one-shot, emitting once and completing, so the Locked Categories screen never reflected a toggle.
  The checkbox flipped locally while the list behind it stayed stale.
- Now `asFlow().mapToList(...)`, so SQLDelight re-queries on any write to the table. This is the
  first use of `sqldelight-coroutines` in the codebase, which was already a dependency with a
  standing comment noting reactive queries were the right answer.
- Matters directly because of the fix below: with un-protecting behind a PIN prompt, a stale list
  reads as "I entered the correct PIN and nothing happened".
- The same one-shot pattern is flagged elsewhere in §5 (Manage Categories, movies/series
  favourites, EPG). Those are unchanged — only the parental-control path was made reactive, since
  that is the one this pass made security-relevant.

**Un-protecting a locked category now requires the PIN (P1, security)**
- `CategoryLockScreen` called `toggleCategoryProtection` directly in both directions, so anyone
  who reached the screen could untick every locked category **without knowing the PIN** — which
  defeats the entire feature. Remove-PIN and Change-PIN were correctly gated; this one path was not.
- The original is explicit about the asymmetry (`PortraitParentalControl.kt:545-558`):
  *"Unprotecting requires PIN verification"* / *"Protecting doesn't require PIN (just enable)"*.
  Restored exactly that: protecting is unprivileged, un-protecting holds the change behind
  `PinDialog` + `validatePin`.
- Latent while `PremiumStatusProvider` forced parental controls off; it became reachable the moment
  they started working, so it is fixed in the same pass that enabled them.

**BGTask completion on iOS (P1)**
- Neither BGTask handler installed an `expirationHandler`. When iOS reclaims the execution window
  before a sync finishes, `setTaskCompleted` is never called; the system treats the task as having
  overrun and **progressively deprioritises future scheduling**, so background sync degrades to
  never running. A full content sync over tens of thousands of channels can easily exceed the
  ~30 s a `BGAppRefreshTask` typically gets, so this is the expected case, not the edge case.
- Both handlers now install one, routed through a `TaskCompletion` guard: adding an expiration
  handler creates a race between the normal completion callback and the OS reclaiming the window,
  and calling `setTaskCompleted` twice raises an exception that terminates the app **in the
  background**, where it is invisible until crash reports arrive.
- Swift cannot be compiled on this host, but the new `verify-ios` CI job builds the iOS app, so
  this is covered on the next run rather than at archive time.

**Sync no longer destroys user data (P0 #10)**
- `syncChannels` and `saveM3uData` write content with `INSERT OR REPLACE` passing literal
  `isFavorite = 0` / `lastViewedTimestamp = 0`, and `isPinned/isHidden/isDefault = 0` for
  categories. So **every content re-sync wiped every favourite, the whole recently-viewed list,
  and all pinned/hidden/default category preferences** — and on iOS that fires from the background
  BGTask with no user action at all.
- Fixed by snapshotting the user-owned columns and re-applying them inside the same transaction
  (`Channel.selectUserStateForSync` / `restoreUserStateForSync`, and the Category equivalents).
  Both snapshots are restricted to rows the user has actually touched, so they stay small on a
  50k-channel account.
- **Not** done with SQLite UPSERT (`ON CONFLICT DO UPDATE`) on purpose: minSdk is 26 → SQLite 3.19,
  and UPSERT needs 3.24. The project's SQLDelight dialect is `sqlite-3-38`, so an UPSERT would have
  compiled cleanly and then failed at runtime on Android 8. Worth remembering for future schema work.
- Covered by 8 tests against a real in-memory SQLite DB, including one that reproduces the original
  bug so the regression can't quietly return, and one asserting restore is key-matched (a channel
  the provider recategorises does not get its favourite resurrected under the new category).
- Note this closes the **data loss**, which was the harmful part. True differential sync — using
  `contentHash`/`syncVersion` to skip unchanged rows and avoid rewriting the whole table — is still
  not implemented, and remains the reason a full sync is slow.

**Parental controls made real (P0 #5)**
- `PremiumStatusProvider` was bound to a hardcoded `false` on both platforms, and
  `ParentalControlManagerImpl.isParentalControlEnabled()` returns `false` without premium — so
  `validatePin()` accepted **any** input, no category was ever protected, and the PIN was never
  requested. A parent could configure the whole feature and be given a false assurance. Since no
  purchase flow exists on either platform, no user could ever have unlocked it either.
  Replaced with a single documented `UngatedPremiumStatusProvider` (returns `true`) shared by both
  platform modules. **This is a knowing divergence** — the original does gate this behind Play
  Billing — chosen because the alternative is a safety feature that lies. It is consistent with
  Theme Studio, which the port already un-gated. Reverting when billing lands is one line per
  platform. `PremiumStatusProvider`'s only consumer is parental controls, so nothing else changes.
- Enabling the feature exposed two latent defects that were previously unreachable, both fixed:
  - `ParentalSessionGuard` rendered an **empty transparent scrim** once the PIN dialog was
    dismissed, leaving the tab permanently blank with no retry and no way out short of killing the
    app. It now shows an explicit "Content locked" state with an **Enter PIN** button.
  - `clearSessionOnAppStart()` had **no call site anywhere**. Now called on the cold-start path of
    both platforms (`HitvApp.kt`, `MainViewController.kt`). **Impact correction:** an earlier
    revision said this meant "until app closes" silently never expired. That overstated it — the
    timeout picker never offered that option (nor does the original), so the sentinel was
    unreachable. The wiring is correct and defensive rather than a fix for a live bug; the *live*
    bug in that picker was the always-ask value, below.
  - Noted in code: whole-tab gating is itself a divergence (the original gates per protected
    category). It is kept because Movies and Series have no per-category gating of their own, so
    this guard is currently their only protection — `MobileChannelsLayout:613` shows the ported
    per-category path that Channels uses.

**Channel search matched words only in the order typed.**
`SearchUtils.createFlexibleSearchQuery` in the original emits one `LOWER(name) LIKE '%word%'` per
word and ANDs them, via Room's `@RawQuery`, so the words may appear in any order — the DAO method is
literally named `searchChannelsFlexible` and documented "all words must appear in name (any order)".
All three port call sites had collapsed that to a single pattern, `"%" + words.joinToString("%") +
"%"`, which requires the typed order: searching *"sports hd"* missed a channel named **"HD Sports"**.
Multi-word channel names are the norm in IPTV playlists, so this bit constantly while looking like a
working search — you get *some* results, just not the right ones, which is why it survives testing.

SQLDelight has no dynamic SQL, so the fix spreads the words over a fixed set of slots guarded by
`(:wN = '' OR LOWER(name) LIKE :wN)` — the same shape `selectByCategorySorted` already uses. Words
past the slot count go to `SearchUtils.overflowSearchWords` and are applied in Kotlin rather than
dropped, so the match never becomes *looser* than the original's.

**The "add channels to a custom group" picker searched a userId that cannot exist.**
Found while fixing the above. `CustomGroupRepositoryImpl.searchAllChannelsList` and
`AllChannelsSearchPagingSource` both called the single-user query with a hardcoded `0L` — directly
under a comment reading *"Search across all users' channels"*. `UserCredentials.userId` is
`INTEGER PRIMARY KEY AUTOINCREMENT`, so SQLite issues **1** for the first account and never 0. The
predicate matched no row on any install: the picker's search box returned nothing, always, for
everyone. The original omits the userId predicate entirely, which is what the new
`searchAllByNameFlexible` does.

Both are pinned by tests. `SearchUtilsTest` (13, commonTest → runs on iOS too) covers slot packing
and the overflow tail; `FlexibleChannelSearchTest` (12, real in-memory SQLite) covers the SQL,
including the `= ''` guard — a wrong guard still returns rows, just the wrong ones, so it needs a
database to catch. One test executes the *old* collapsed pattern and asserts it finds nothing,
proving the bug rather than describing it.

**Every content sync silently emptied the user's custom groups.**
The most consequential defect found in this pass, and it is pure data loss.

`Channel.channelId` is `AUTOINCREMENT`, and `channel_unique` is a UNIQUE index on
(name, streamIcon, categoryCreatorId, userId). The port's channel sync wrote every row with
`INSERT OR REPLACE`. In SQLite a REPLACE that hits a unique-index conflict is a **DELETE followed by
an INSERT** — so a channel whose data had not changed at all came back with a *brand-new channelId
after every sync*. `CustomGroupChannel` rows still pointed at the old id, the membership join
stopped matching, and the group rendered empty. Nothing errored; the channels simply vanished from
the user's groups, and the stale rows were unreachable afterwards.

The original never had this because it syncs channels differentially
(`DAOChannel.performDifferentialChannelSync`): it matches existing rows on
`"${name}_${categoryCreatorId}"`, UPDATEs the ones whose `contentHash` changed, inserts the genuinely
new ones, marks the rest seen, and deletes only what the provider has not returned for seven days.
`channelId` is stable throughout.

Every query that needs was already ported — `selectAllForSync`, `updateById`, `insertOrIgnore`,
`markAsSeen`, `deleteStale`, `countRecentChannels` — and `contentHash` was already a column. Only
the orchestration was missing, and `contentHash` was written as a literal `null` at every call site.
This is the session's recurring theme in its purest form: **the pieces were ported, the wiring was
not.** Now implemented as `DifferentialChannelSync`, mirroring the original's algorithm, with two
platform-driven deviations that are called out in the class doc: `markAsSeen` is chunked (SQLite's
default variable limit is 999; the original passes every seen id to one `IN (...)`), and the 7-day
cutoff is a named constant.

Knock-on effects, all verified by test:
- Favourites and watch history now survive natively rather than via the snapshot/restore workaround
  added earlier in this pass. That workaround is **kept** as defence in depth — it is a narrow query
  and, after a correct differential sync, a no-op — because this path cannot be exercised on a real
  device from here.
- An unchanged sync now performs **no per-channel writes**, instead of 50k deletes and inserts.
- `updateById` was missing `catchupType`/`catchupSource`, so once sync stopped using REPLACE a
  channel that gained or lost catch-up would never reflect it. Added.

`DifferentialChannelSyncTest` (13, real SQLite) mirrors the original's own instrumentation tests —
`performDifferentialChannelSyncInsertsNewChannels` / `...DeletesStaleChannels` /
`...PreservesFavorites` — plus channelId stability, custom-group survival, and a case crossing the
`markAsSeen` batch boundary.

**Still exposed, deliberately:** `saveM3uData` continues to write with `INSERT OR REPLACE`, so M3U
playlists keep the churn. Reusing the differential path there does not work as-is, because it keys
on (name, categoryCreatorId) and `saveM3uData` synthesises `categoryCreatorId` from the category's
*ordinal* in the parsed playlist (`index + 1`) — the key shifts whenever the playlist's category
ordering changes. Making M3U safe means giving those categories a stable identity first.
`ChannelIdStabilityTest` pins the live behaviour and the reasoning.

**Foreign keys: a genuine divergence, still deliberately not "fixed".**
The port's schema declares five `ON DELETE CASCADE` relationships, matching the original's
`@ForeignKey` declarations on `EntityCustomGroupChannel`, `EntityProgramme`, `EntityTitle` and
`EntityDesc`. Room enables `PRAGMA foreign_keys = ON` on every connection; **neither SQLDelight
driver does**, and SQLite defaults it off per connection — so in the port those cascades have never
once fired.

It is still not a one-line fix, because enabling enforcement while any writer uses `INSERT OR
REPLACE` converts a stale reference into outright deletion: the REPLACE's internal DELETE would fire
the cascade and remove the membership row for real. The pragma stays off until every writer is
non-destructive — i.e. until the M3U path above is addressed.

*(An earlier revision of this section claimed the original declares no foreign keys at all. That was
wrong: the grep behind it used `entity/` in the plural and silently matched nothing. The original
declares exactly the same five.)*

**Deleting an account leaked its entire EPG text, permanently.**
`deleteTitlesByUserId` and `deleteDescriptionsByUserId` selected through the parent
(`WHERE programme_id IN (SELECT id FROM Programme WHERE userId = ?)`), which only works if they run
*before* `deleteProgrammesByUserId`. `AccountManagerRepositoryImpl.deleteUserAndRelatedData` ran
them **after** — so once the parent rows were gone the subqueries matched nothing and every Title
and Description row was stranded with no remaining path to reach it. Removing and re-adding an
account grew the database every time. The declared CASCADE does not save it, per the pragma above.
The original's `DAOEpg.deleteUserAndRelatedData` deletes children first.

Both tables carry `userId NOT NULL`, so the queries now filter on it directly: order-independent,
consistent with every sibling delete, and free of a correlated subquery over a table holding
hundreds of thousands of rows. The teardown was also **not transactional** — eighteen unwrapped
statements, so a failure partway left an account half-deleted with no clean retry. It is now one
transaction, and the EPG deletes are back in the original's children-first order.

**A finished sync never reached the screen.**
Every paging source in the port read with a one-shot `executeAsList()` and registered nothing, so
nothing ever told a visible list that the data underneath it had changed. New channels did not
appear after a sync, removed ones stayed, and a toggled favourite was absent from the Favourites
list until the screen was destroyed and recreated.

Room hands the original this for free — a Room-backed `PagingSource` registers with the
`InvalidationTracker` and self-invalidates when a table it reads is written. SQLDelight has the same
capability (`driver.addListener("Channel", …)`, keyed on the table names the generated
`notifyQueries { emit("Channel") }` emits) but does not wire it up, and neither did the port. This
was the one ⚠️-marked item in §5's sync section; it is now confirmed and fixed.

**This is worst precisely where it is hardest to notice from a developer machine.** On iOS the
BGTask sync completes while the app is suspended, so the user foregrounds the app to a list that
silently predates the sync — the most common path on the platform, not an edge case.

All six paging sources now invalidate: channels (also on `CustomGroupChannel` and `ParentalControl`,
both of which change what the list should show), movies, series, and the three custom-group sources.
Listeners are removed via `registerInvalidatedCallback`, since Paging builds a fresh source after
each invalidation and a leaked registration would accumulate one dead listener per refresh.

`SqlDriver` is now a first-class Koin singleton rather than a local inside the `HitvDatabase`
factory — one instance, because two drivers would mean two connections whose notifications never
cross.

`PagingInvalidationTest` (6) covers it against a real database. Worth its own suite because every
way this breaks is silent: the query keys are strings, so a typo or a table renamed in the `.sq`
neither fails to compile nor throws — the listener simply never fires and the screen goes quietly
back to being stale. One test therefore asserts the constants against what SQLDelight actually
emits, and another checks that an unrelated table does *not* invalidate (over-invalidation is not
harmless either: it restarts paging and throws the user's scroll position back to the top).

Enabling this surfaced a build-level issue: `androidUnitTest` runs against a stub `android.jar`, and
androidx Paging's `invalidate()` calls `android.util.Log`, so five of the six tests failed with
"Method isLoggable not mocked" — a failure that had nothing to do with what they assert.
`unitTests.isReturnDefaultValues` is now set in the KMP library convention plugin, so shared code
that touches an Android API incidentally is testable across every module.

**Seventeen repository `Flow`s emitted exactly once.**
Same root cause as the paging finding above, one layer up. A `Flow` return type is a promise of
updates, and every one of these was `flow { emit(query.executeAsList()) }` — a snapshot wearing a
`Flow`'s clothes. The screens bound to them showed whatever was true when they were first composed.

Converted to `asFlow().mapToList(…)`, the pattern already used by `ParentalControlManagerImpl`:
favourites and recently-viewed for channels, movies and series; all three category lists; the
channel lists; the parental-control list, per-category lookup and protected count; the account list;
and custom groups. Two audit items collapse into this one — *"Account list is a one-shot snapshot →
Switch Account never refreshes"* and *"Manage Categories toggles never update the UI"*.

Where the repository reads its `userId` from a computed property, the conversion keeps an outer
`flow { emitAll(…) }` so the id is still resolved at collection time rather than when the flow is
built. That preserves the previous semantics exactly; dropping it would have been a silent
behaviour change on account switch.

`getAllCustomGroups` needed more than a mechanical swap. It listed groups and then ran one count
query *per group* — an N+1, but worse, unfixable by reactivity alone: SQLDelight keys notifications
on the tables a query reads, and `selectAllGroups` reads only `CustomGroup`, so adding a channel to
a group changed the count with nothing to announce it. Replaced with a single
`selectAllGroupsWithChannelCount` that LEFT JOINs the membership table, so it is one query *and*
observes both tables.

That new SQL gets its own suite (`CustomGroupChannelCountTest`, 6) because the count is the kind of
thing that is quietly wrong rather than broken: with a LEFT JOIN, `COUNT(*)` counts the synthesised
all-null row and reports **1** for an empty group. The query uses `COUNT(gc.id)`; the empty-group
test is the reason the file exists.

Two `Flow`s are deliberately left one-shot: `fetchSeriesInfo` and `fetchSeasonsWithEpisodes` compose
several queries into a single structure for the series detail screen and are driven by an explicit
fetch. Their real problem is a different §5 item — that screen blocks on the network before showing
anything cached — and is not fixed by making the query observable.

**Six multi-statement mutations had lost their atomicity.**
Found by asking what *else* Room supplies implicitly, having established that its reactivity did not
survive translation. The answer is `@Transaction`: the original marks 23 DAO methods with it, and
the port — which has no DAO layer to carry the annotation — reproduced the statements bare.

Most of the 23 are read-side, where `@Transaction` is Room's relation-consistency requirement rather
than atomicity. Six write-side ones were live defects, now wrapped:

| Original (`@Transaction`) | Port | Consequence when it fails midway |
|---|---|---|
| `setDefaultCategory` | `CategoryPreferenceRepositoryImpl` | clear-then-set → **no** default at all |
| `deleteCustomGroupWithChannels` | `deleteCustomGroup` | an emptied group left behind |
| `reorderChannelsInGroup` | same | colliding positions, an order the user never chose |
| `replaceChannelsInGroup` | `addChannelsToGroup` | a partially populated group |
| `insertOrGetUserId` | `saveCredentials` | the wrong account id stored as the signed-in user |
| — | `createCustomGroup` | count→insert→read-back races on `sortOrder` and the returned id |

The last is not in the original's list but is the same read-modify-write shape, so it is wrapped too.

**The second half of this only became a defect once the flows above went reactive**, which is worth
recording as a lesson rather than a line item. SQLDelight holds change notifications until a
transaction commits. Unwrapped, `clearAllDefaults()` published on its own — so every observer
rendered an intermediate state with nothing selected before the real value arrived. Making the
repositories reactive is what turned a latent atomicity gap into a visible flicker in Manage
Categories, and `addChannelsToGroup` from one slow write into a few hundred separate commits each
waking every observer.

`TransactionNotificationTest` (5) is the direct evidence: the same two writes notify **twice**
unwrapped and **once** wrapped; a rollback restores the previous default and publishes nothing at
all; fifty membership inserts inside a transaction produce one notification rather than fifty.

**There is no database migration path at all.** Found, not fixed — see below for why.

The original is at `@Database(version = 25)` and chains 24 hand-written migrations,
`MIGRATION_1_2 .. MIGRATION_24_25`. The port has **zero** `.sqm` files, and no way to notice: with
no migration files SQLDelight pins the schema at version 1 permanently. Editing a `.sq` changes what
a *fresh* install creates and leaves every *existing* install untouched, with nothing failing at
build time. The app then queries a column its own database does not have.

A first release is unaffected, which is exactly why this survives to reach users on the second one —
and this working tree has been editing `.sq` files throughout the pass.

The intended fix is two lines in the SQLDelight convention plugin —
`schemaOutputDirectory` plus `verifyMigrations.set(true)` — then committing a `databases/1.db`
baseline, after which changing a table requires an `.sqm` and the build enforces it. **It is not
enabled**, and the reason matters: SQLDelight 2.0.2's Gradle plugin resolves sqlite-jdbc 3.34.0,
whose native library will not load on this host (`'void org.sqlite.core.NativeDB._open_utf8'`).
Forcing a newer sqlite-jdbc onto the root buildscript classpath does not reach the plugin's worker.
`verifyMigrations` attaches to `check`, so enabling it here would break `./gradlew check` and
`build` locally — a real regression traded for a guard that could not be verified. The exact
configuration and the condition for switching it on are recorded in the plugin itself.

**`./gradlew check` does not run on this Windows host — and that is pre-existing.**
SQLDelight registers `verifyCommonMainHitvDatabaseMigration` into `check` by default, and it fails
with the same sqlite-jdbc native error whether or not verification is configured. This was
previously masked by an earlier failure in the same task graph — the `sqlite-driver`-on-`commonTest`
bug fixed in this pass — so fixing that one uncovered this one.

Worth stating plainly because it shapes how everything else here was verified: **the verification in
§1.1 uses explicit compile and `testDebugUnitTest` tasks, not `check`.** Those work. `check` and
`build` do not, for an environmental reason unrelated to the code. Run them on Linux/macOS CI.

*(A note on counting: `check` also runs `testReleaseUnitTest`, so scraping every `test-results` XML
after one double-counts the same suites. The figures in this document are `testDebugUnitTest` only.)*

**Updating the existing Android app in place would wipe every user's data.** Recorded as a release
consideration rather than a bug, since it depends on how the port is shipped. The original's Room
database is named `hitv_database`; the port's SQLDelight database is `hitv.db`. Different files, so
nothing is corrupted and nothing crashes — but an existing hitv user who installs the KMP build as
an update gets an empty database: signed out, with favourites, watch history, custom groups and
category preferences gone. If the KMP Android app is meant to replace the shipping one rather than
sit alongside it, that needs either a one-time import from `hitv_database` or a deliberate decision
to accept the reset. iOS is unaffected — there is no prior install.

**Making the flows reactive introduced a collector leak — caught and fixed in the same pass.**
Recorded because it is the honest cost of the previous change, and because the failure mode is one
this whole audit keeps finding: nothing throws, nothing looks wrong, the app just degrades.

A one-shot `flow { emit(…) }` completes, so calling a loader twice was free. Several call sites
relied on exactly that — `LivePlayerViewModel.saveFavoriteChannel` calls `getFavorites()` after
**every** favourite toggle, and `SeriesViewModel`/`MovieViewModel` do the same. Once those flows
observed the database they stopped completing, so each toggle left another permanently-live
collector — and another SQLDelight listener — running behind the previous one, all writing the same
state. Twenty toggles, twenty collectors.

Every consumer was audited for this rather than spot-checked: no caller uses `toList`/`single`/
`last` (which would now hang forever), the one `.first()` still completes, and every `collect` is
terminal within its own `launch` with no code after it. The collectors that can be re-invoked are
now held in a `Job` and cancelled before relaunch — the pattern `SeriesInfoViewModel` already used.
`ParentalControlViewModel.loadData` and `SwitchAccountViewModel.deleteUser` are deliberately left
alone: the first is private and called once from `init`, the second uses `.first()`.

The post-mutation `getFavorites()` calls are now redundant — the flow updates itself — but are kept,
since with the guard they merely restart the collector and removing them is a wider change than the
fix requires.

**Background sync used the wrong iOS task type, and it dropped Android's only constraint.**
This was logged in §6.1 as a cosmetic App Review risk — an unused `processing` background mode.
Looking properly, the unused mode was the *symptom*; the request type was wrong on three counts.

Android schedules both syncs as `PeriodicWorkRequest`s with `NetworkType.CONNECTED`, at 6h (EPG)
and 24h (content). The port submitted `BGAppRefreshTaskRequest`, which is the analogue of a short
foreground-ish refresh, not of periodic constrained work:

- **Duration.** An app-refresh task gets roughly 30 seconds. The Swift handler's own comment already
  said a full content sync over tens of thousands of channels "can easily exceed the ~30s a
  BGAppRefreshTask typically gets" — meaning the expiration handler added earlier in this pass would
  fire and mark the sync failed, every time. A processing task gets minutes.
- **Constraints.** `BGAppRefreshTaskRequest` cannot express a network requirement at all, so the
  port silently dropped `NetworkType.CONNECTED` — the only constraint the original sets. The class
  doc had recorded this as an unavoidable platform divergence ("iOS does not expose network-type or
  charging constraints"). That is true of app-refresh and false of processing:
  `BGProcessingTaskRequest` carries `requiresNetworkConnectivity` and `requiresExternalPower`, so
  **both** Android constraints now survive the port — `requiresCharging` included, which the port
  had also been discarding with an apologetic note.
- **Declared intent.** `Info.plist` already listed `processing` and permitted both identifiers. Only
  the request type was never wired to match — the same shape as every other finding in this pass.

Changed in all three places that have to agree: the Kotlin submission, the Swift handler that chains
the next run, and `UIBackgroundModes` (now `processing` only, since nothing submits an app-refresh
request any more). The cinterop signature was confirmed against the platform klib before writing —
`requiresNetworkConnectivity` is exposed as a `var`, so property assignment is correct.

`scripts/check-bgtask-identifiers.sh` was extended to enforce it, and verified by introducing each
drift and confirming it trips: Kotlin and Swift must construct the *same* request subclass (chaining
a different type gives the follow-up run a different window and different constraints, silently),
and `UIBackgroundModes` must declare exactly the mode the code uses — no missing mode, no unused one.

**Trade-off, stated because it is real and I cannot measure it from here:** iOS schedules processing
tasks more conservatively than app-refresh ones, favouring idle and charging periods. For 6h/24h
cadences that is appropriate — Doze does the same to WorkManager on Android — but a sync is more
likely to land overnight than exactly on the interval. If device testing shows that is too lazy,
reverting is a one-line change of the request type in two files, and the guard will then require
`fetch` back in the plist.

**iOS held the whole XMLTV feed in memory three times over.** The last open iOS risk, and the one
most likely to make background EPG sync fail on a real phone.

Android streams the response through `XmlPullParser` and never materialises the document. The iOS
actual buffered the entire body into `NSData`, copied it into a `ByteArray`, then decoded that into
a Kotlin `String` — all three alive simultaneously, and a Kotlin string is UTF-16. An 80 MB feed
therefore peaked around **320 MB**. The foreground app might survive that on a recent device; a
`BGTask` is granted far less and gets jetsam-killed, which is why background EPG sync was unreliable
on iOS while Android was fine. The allowlist and window work earlier in this pass bounded what was
*retained*; they did nothing about peak transient memory, which is what actually kills the process.

Now: `downloadTaskWithRequest` writes the response straight to a temp file (download memory is a
fixed buffer regardless of feed size, and URLSession still decompresses gzip transparently), then
`NSXMLParser(contentsOfURL:)` reads it incrementally and emits SAX events into a new
`XmltvSaxAssembler`. Peak memory now tracks the **retained** EPG rather than the size of the feed.

The design point is that almost none of this is iOS code. `XmltvSaxAssembler` lives in `commonMain`
as a pure state machine over `startElement` / `characters` / `endElement`, so the logic that decides
what a channel or programme *is* runs on the JVM in `XmltvSaxAssemblerTest` (15 tests) — including
four direct parity assertions against `EpgParser`, the regex parser Android's path and every
existing test still use. Both must produce identical rows or the same account would hold different
EPG data on the two platforms. Parity extends to details that look incidental and are not: first
`display-name` and first `icon` win; allowlist is checked *before* date parsing so a rejected
programme never buffers its title; event ids advance only for kept events.

The iOS file is a thin adapter, and the seam it owns — whether `NSXMLParser` really emits what the
assembler expects — is the part no common test can reach. `EpgStreamingLoaderIosTest` (8) covers it
on the simulator in CI: parity with `EpgParser` over a fixture, allowlist and window applied while
streaming, entities decoded exactly once (`NSXMLParser` decodes for us, so applying
`decodeXmlEntities` on top would turn `&amp;amp;` into `&`), text reassembled across CDATA-split
callbacks, self-closing elements, a 2,000-programme document, and a malformed feed failing loudly
rather than returning an empty guide — silently returning empty would wipe the user's guide on the
next sync and look like the provider had no data.

Written and compiled from Windows, with every cinterop signature confirmed against the platform
klib first (`klib dump-metadata`) rather than guessed — `NSXMLParser(contentsOfURL:)`,
`downloadTaskWithRequest(request:completionHandler:)`, and the three
`NSXMLParserDelegateProtocol` callbacks. It compiled for `iosArm64` first try.

**Changing your password at the provider left the app permanently broken.**

`UserCredentials.insert` is `INSERT OR IGNORE` against `UNIQUE(username, hostname)`, and
`saveCredentials` did nothing else. So logging in again to an account the app already knew was a
**no-op**: the password just typed, the refreshed expiry date and the server's allowed output
formats were all discarded, and the app carried on using whatever it stored the first time.

The user-visible shape of this is bad. Change your password at the IPTV provider, re-enter it in
the app, and every request keeps failing — with the app showing the credentials you just entered.
Renew a lapsed subscription and it keeps displaying the old expiry. There is no way out from inside
the app short of deleting the account and adding it again.

The original handles it explicitly: `DAOUserCredentials.insertOrGetUserId` checks for the `-1`
rowid that signals an ignored insert and calls `updateCredentials` on the existing row. The port had
`updateCredentials` sitting in the schema **with no call site** — the same wiring gap as everything
else in this pass.

SQLDelight does not hand back the rowid, so the update is applied whenever the row already exists,
inside the transaction added earlier in this pass. Deliberately only the three provider-owned
columns, matching the original: `epgUrl` and `channelPreviewEnabled` are the user's own settings and
a re-login must not reset them.

`CredentialsReloginTest` (7) covers the SQL contract, starting with a test that performs the second
insert and asserts the old values survive — demonstrating the bug rather than describing it. It also
pins that the account keeps its `userId` across a re-login (everything the user owns is keyed on it,
so renumbering would orphan channels, favourites and watch history), that the same username on a
different host stays a separate account, and that updating one account does not touch another.

**The URL normalizer was missing, and had been replaced by three partial copies that disagreed.**

An earlier revision of this document listed *"Xtream credential extraction from a pasted M3U/get.php
URL missing entirely"*. Checking it properly, that was wrong in its particulars: the original has no
credential-extraction helper. What it does have — and the port did not — is
`core/common/url/ServerUrlNormalizer.kt`, a deliberately single "permissive home" for fixing the two
mistakes real users actually make when typing a server address. It ships with its own 227-line test
suite, so the spec is unambiguous.

In its place the port had grown **three** partial reimplementations, which had already drifted:

| Copy | Whitespace | Scheme | Collapse `//` | Strip `player_api.php` |
|---|---|---|---|---|
| `LoginValidator.sanitizeUrl` | all | ✅ | ✅ | ❌ |
| `AccountManagerRepositoryImpl.normalizeHostname` | ends only | ❌ | ❌ | ❌ |
| `PreferencesHelper.getHostUrl` | none | ❌ | ❌ | ❌ |

The repository copy is the damaging one. A host typed as `myserver.com:8080` was stored with no
scheme and failed every request, and interior whitespace — the single most common cause of IPTV
login failures, which is why the original strips it — survived. None of the three stripped a pasted
`…/player_api.php?username=…`, which is exactly the URL providers hand out, so pasting it produced a
host that could never authenticate.

Ported verbatim into `shared/core/common`, and all three call sites now delegate to it, including
the M3U branch of `saveCredentials` which used a bare `trim()` where the shape-preserving
`normalizePlaylistUrl` belongs — an M3U URL is fetched verbatim, so appending a slash can 404 a
valid playlist.

The original's test suite ported with it (24 tests), including the `hi tv.com` interior-whitespace
case its own comment calls "the crash input", and idempotence for both entry points — credentials
are re-saved on every login, so a normalizer that drifted on reapplication would corrupt a working
host over time.

**My own iOS streaming parser was less robust than Android's, in two ways that matter.**
Found by comparing the new iOS path against the Android actual rather than against `EpgParser` —
the two streaming implementations must agree, and they did not.

- **No control-character sanitising.** The original ships an `XmlSanitizingInputStream` that drops
  bytes XML forbids, and the Android actual pipes every response through it. It exists because real
  XMLTV feeds contain those bytes. `NSXMLParser` is strict and aborts the whole document on the
  first one — so a feed Android handles without complaint would have produced **no EPG at all** on
  iOS. Ported as a streaming copy with a fixed buffer, so the feed still never becomes resident.
- **All-or-nothing on malformed input.** Android skips a bad element and carries on, returning what
  it read; my iOS version threw the entire guide away. Feeds are routinely truncated mid-document.
  `parseXmltvFile` now reports whether the parse was clean and the caller keeps the partial result,
  failing loudly only when *nothing* parsed — because an empty `EpgDomainData` would make the sync
  clear the user's guide and look like the provider had no data.

Both were defects in code written earlier in this same pass, and neither would have shown up on the
platform they were written from. Covered by `EpgStreamingLoaderIosTest`: the raw feed is asserted to
be rejected by `NSXMLParser`, the sanitised one to parse cleanly with the illegal bytes gone, and a
truncated feed to report an unclean parse while still yielding the channel that did read.

**Worth recording as a structural note:** there are now three XMLTV implementations in this project —
`EpgParser` (regex, the shared reference), the Android `XmlPullParser` actual, and the iOS
`NSXMLParser` + `XmltvSaxAssembler` path. That is the same shape as the three partial URL
normalizers found earlier, and it will drift the same way. The assembler was written so the Android
actual could adopt it too, collapsing the streaming implementations to one; doing so is a
follow-up, not something to change blind while the Android path is the one currently shipping.

**Live TV could not play on iOS for a large class of accounts.**
Possibly the most consequential iOS-only defect found, and it is invisible from Android.

`MediaUrlNormalizer.normalize` appends the `output` preference, which is set to
`allowedOutputFormats.firstOrNull() ?: ""`. Two ordinary cases produce a URL `AVPlayer` cannot play:

- **`output` is empty** — always true for **M3U accounts**, which have no Xtream `user_info`, and
  true for any provider that omits the field. The URL is left extension-less and an Xtream server
  answers with raw MPEG-TS.
- **`output` is `"ts"`** — a perfectly normal thing for a provider to list first.

ExoPlayer plays raw MPEG-TS over HTTP without complaint, so the original — Android-only — never had
to care, and neither does this port's Android side. **AVFoundation does not.** It handles HLS and
progressive MP4/MOV. So exactly the accounts that work on Android would show a live channel that
never starts on iOS, with no actionable error.

`normalizeLiveForAvPlayer` now requests `.m3u8` whenever the URL carries no extension of its own,
and both iOS live call sites (full-screen player and channel preview) use it. If a provider truly
has no HLS endpoint the request 404s — but `.ts` would have failed to decode anyway, so this is
never worse. **A deliberate, platform-driven divergence from the original**, documented as such in
the function.

**Writing the tests for it exposed a second, cross-platform bug.** The extension check was
`endsWith` against the *whole* URL, so a provider that appends a token —
`…/12345.m3u8?token=abc` — looked extension-less and became `…/12345.m3u8?token=abc.ts`, which
cannot resolve. The original guards this with an extra `!url.contains(".m3u8")` check that the port
had dropped. Now the query and fragment are split off before both the check and the append, which is
the general form of that guard and also puts the extension on the path where it belongs.
`MediaUrlNormalizerTest` (13) pins all of it.

**The commonTest naming trap now has a guard.** A comma in a backtick test name broke
`compileTestKotlinIosSimulatorArm64` for the third time in this pass — legal on the JVM, rejected by
Kotlin/Native, and invisible to every Android task. `scripts/check-common-test-names.sh` scans every
`commonTest` source for characters Kotlin/Native rejects, runs in seconds without a toolchain, and is
wired into CI ahead of the compile steps. Verified by reintroducing the comma and confirming it trips.

**MKV and AVI titles fail silently on iOS.** Same root cause as the live-TV finding above, on the
VOD path — and unlike that one, it cannot be fixed, only reported honestly.

Xtream serves a title in whatever container the provider stored it in, and the URL carries it:
`…/movie/user/pass/12345.mkv`. ExoPlayer ships extractors for Matroska, AVI and FLV, so the
original — Android-only — plays them without anyone thinking about it. AVFoundation has no such
decoders. The file on the server really is an MKV, so no URL rewrite helps.

What *was* fixable is the failure mode. Previously the user tapped a movie, got a black screen, and
waited out the 25-second `PlaybackStartWatchdog` before seeing *"the stream may be unavailable"* —
slow, and wrong: the stream is fine and merely undecodable. Worse, the retry ladder spent three
attempts on a file that could never decode. Now `AvFoundationSupport` detects the container up
front, the message names the actual format, and the retry path short-circuits.

Deliberately a **deny-list** of containers known not to work rather than an allow-list of ones that
do: an unrecognised extension is let through so the player can try, since guessing wrong that way
merely reproduces the old behaviour, while an over-eager allow-list would block titles that play.
`.ts` is on the list for a subtler reason than the rest — HLS is *delivered* as `.ts` segments and
plays fine behind an `.m3u8` manifest, but a raw `.ts` file played progressively does not.

`AvFoundationSupportTest` (12) pins the list, the token-bearing URL case, and that unknown
containers stay allowed.

**Worth stating plainly for release planning:** this is a real functional gap between the two
platforms that no amount of porting closes. Any provider whose library is largely MKV will look
substantially worse on iOS than on Android. The options are server-side transcoding, a third-party
decoder (VLCKit), or setting expectations — not a code fix in this repo.

**Catch-up replay was unplayable on iOS too, for both provider conventions.**
Traced by following the same seam one more level: catch-up URLs flow into `currentChannelUrl` and
then through the iOS live playback path, so whatever that path does to a URL applies to replay as
well.

- **Xtream** catch-up (`/timeshift/{user}/{pass}/{duration}/{start}/{streamId}`) inherits its
  extension from the stored channel URL, which is extension-less — so it was already covered by the
  live HLS fix above.
- **Flussonic** catch-up is emitted as `…/timeshift_abs-{utc}.ts`. That is the documented Flussonic
  convention and correct on Android, but it *has* an extension, so it passed straight through and
  stayed a raw transport stream AVFoundation cannot open.

`normalizeLiveForAvPlayer` now rewrites a `.ts` path to `.m3u8` (query string preserved). Flussonic
serves the same recording as HLS at that variant; a provider without one fails either way, so the
rewrite is never worse. `CatchUpUrlBuilder` itself is untouched — it is shared with Android, where
`.ts` is right.

**Series episodes had the same container problem as movies**, and I had guarded only the movie host.
Episodes carry `containerExtension` from the provider just as movies do, so an MKV episode spent
three retries and 25 seconds before reporting a misleading error. `SeriesPlayerHost` now performs
the same up-front check.

**ClearKey-protected channels fail with a generic error on iOS.** Fourth finding on the same seam,
and the last one I can reach without hardware.

`licenseKey` is carried from the channel row all the way into the iOS player's `initFromArgs` — and
then never read. ExoPlayer supports ClearKey, and `LiveMediaSourceFactory` wires a
`DefaultDrmSessionManager` for any channel that has one. AVFoundation implements **FairPlay only**;
there is no ClearKey path at all. So the stream arrives encrypted, `AVPlayerItem` fails, the retry
ladder spends three attempts on something that can never decode, and ~25 seconds later the user sees
"Playback error".

Like the container gap, the decode limit itself is not fixable here. The failure mode is: the iOS
channel host now checks for a licence key before retrying and reports what is actually wrong.

### Corrections: two claims I got wrong about the iOS build path

Both were disproven by the repository itself, and both are recorded here because acting on either
would have caused damage.

**1. "The project has no shared scheme, so CI cannot build it." Wrong.**
`iosApp.xcodeproj` genuinely contains no committed `.xcscheme`, and `.gitignore` excludes
`*.xcuserdata`, so the reasoning looked sound. But `.github/workflows/ios-testflight.yml` has
archived and shipped to TestFlight **successfully, twice** using `-scheme iosApp` — because
`xcodebuild` autocreates schemes for a project's targets when none is shared. A shared scheme is
still committed (it makes the scheme explicit rather than generated), but it was never a blocker.

**2. "There is no job that produces an installable build." Wrong, and acting on it was harmful.**
`ios-testflight.yml` already exists and works: manual signing, `Apple Distribution`, provisioning
profile "AxonStream AppStore", bundle `pt.hitv.app`, uploaded via `xcrun altool`. That also settles
the bundle-identity question — `pt.hitv.app` *is* the AxonStream record.

A second TestFlight job was added to `verify.yml` before noticing this, using different (API-key)
signing and never executed. It has been removed. Two release paths with different signing, one of
them untested, is worse than one that works.

**The damage, and the fix.** Reasoning that the build number should come from a build setting,
`Info.plist`'s `CFBundleVersion` was changed from the literal `38` to `$(CURRENT_PROJECT_VERSION)`.
That setting is **3**. The existing workflow would then have archived as build 3 — below the 34
already in TestFlight — and Apple rejects a build number lower than one already uploaded for that
version. Restored to a literal, now `35`, matching the project's existing convention of bumping it
by hand per release (visible as the `(build NN)` suffixes in the git log).

**Unrelated, and the owner's call:** `ios-testflight.yml` contains a base64-encoded App Store
Connect API private key inline, along with its key ID and issuer ID. That is a different credential
from the IPTV provider one the owner has already chosen to leave exposed — this one can upload
builds to their App Store Connect account. Worth a deliberate decision rather than an accidental one.

### iOS platform assumptions checked and found correct

Recorded as negative results so they are not re-investigated. Each was a plausible "nothing works on
iOS" hypothesis; all three were checked against the actual artifacts rather than assumed.

| Hypothesis | Verdict |
|---|---|
| **App Transport Security blocks plain-HTTP providers** — Android sets `usesCleartextTraffic="true"`, and virtually every IPTV provider is HTTP. Without an ATS exception, every request and every stream would fail on iOS. | **Correct already.** `Info.plist` sets `NSAppTransportSecurity.NSAllowsArbitraryLoads`. |
| **Landscape not permitted, so the landscape-locked VOD players have nowhere to rotate.** | **Correct already.** Landscape declared for both iPhone and iPad. |
| **Coil 3's network fetcher never registers on iOS**, so no channel logo, poster or backdrop ever loads — the app would render structurally fine and completely artless. Nothing in this repo registers an `ImageLoader` or a fetcher factory, and Coil's `ServiceLoader` discovery is JVM-only. | **Correct already, but only just.** Dumping `coil-network-ktor3-iosarm64`'s klib metadata shows a `KtorNetworkFetcherServiceLoaderTarget` plus an `@EagerInitialization initHook` — Coil 3.0.4 ships a Native-specific auto-registration path that does not rely on `ServiceLoader`. The Ktor **engine** it needs is present too (`ktor-client-darwin` in `core:network`), and both artifacts link into the same framework. |

The Coil one is worth keeping written down: the reasoning that predicted a failure was sound, and the
answer came from the shipped klib rather than from the docs or from guessing. It is also fragile in a
way worth knowing — the auto-registration depends on Coil continuing to ship that eager initialiser,
so if artwork ever disappears wholesale on iOS after a Coil upgrade, register
`KtorNetworkFetcherFactory()` explicitly rather than looking anywhere else.

### The pattern across all four

Live TV, VOD containers, catch-up and DRM are one defect wearing four hats: **shared code encoding
an assumption that only holds because ExoPlayer is forgiving.** ExoPlayer plays raw MPEG-TS, Matroska,
AVI and ClearKey; AVFoundation plays none of them. Every one of these was invisible from Android,
compiled cleanly, passed every existing test, and would have presented to a user as "I tapped it and
nothing happened".

Two are genuinely fixed (live TV and catch-up now request HLS). Two cannot be — MKV/AVI titles and
ClearKey channels are simply not playable by AVFoundation — but both now fail in one second with an
accurate message instead of twenty-five with a misleading one.

**For release planning:** if the provider's library leans on MKV, or their premium channels use
ClearKey, iOS will be visibly worse than Android and no amount of porting changes that. The routes
are server-side transcoding, a third-party decoder such as VLCKit, or setting expectations.

**A commonTest name broke iOS test compilation.** *(This recurred while writing the tests above —
a comma in an `XmltvSaxAssemblerTest` name failed `compileTestKotlinIosSimulatorArm64` while every
Android task stayed green. Same trap, same platform asymmetry, caught the same way.)* The first version of `SearchUtilsTest` had a comma
in a backtick test name. That is legal on JVM and rejected by Kotlin/Native — `compileTestKotlin­
IosSimulatorArm64` failed while every Android task stayed green. Exactly the shape of defect this
audit keeps finding: invisible on the platform you develop on, fatal on the one you ship to. The
repo was swept for the same pattern; this was the only instance.

### Detail on the ones I verified by hand

**#1 — credentials.** `LoginScreen.kt:59-62` reads:

```kotlin
// Form fields — pre-filled with debug credentials
var username by remember { mutableStateOf("b7be78a330") }
var password by remember { mutableStateOf("8ba28474b8") }
var url by remember { mutableStateOf("http://xdooh.com/") }
```

The original deliberately avoided this: `hitv/.../login/LoginScreen.kt:60-62` exposes
`defaultUrl`/`defaultUsername`/`defaultPassword` parameters defaulting to `""`, fed from
`BuildConfig` (`app/build.gradle.kts:62-64` = `""` for release; 68-70 read the *gitignored*
`local.properties` for debug). The port removed that indirection entirely — there is no
`buildConfigField`, no `IS_DEBUG`, no expect/actual anywhere in the port to gate it. These
literals are committed (introduced in `e76fe84`) and compile into both release binaries.
Fixing needs both a scrub **and rotating those credentials**, since they are in git history.

**#2 / #3 — the two crashing menu rows.** `ScreenRegistry.create` (`HitvNavigation.kt:33-36`)
ends in `?: error("No screen factory registered for $screen")`. `HitvScreen.FEEDBACK` and
`HitvScreen.LIVE_EPG` exist in `Routes.kt:69-70` and are pushed at `HitvNavigation.kt:151`/`157`,
but `SettingsScreenRegistration.kt:11-19` registers nine screens and neither of those two. Both
rows are rendered and tappable from More Options (`MoreOptionsVoyagerScreen.kt:57-58`). On iOS
this is an uncaught `IllegalStateException` from Kotlin/Native → immediate process termination.
`SuggestionScreen` already exists, so #3 is a one-line registration.

**#5 — parental controls.** Verified directly.
`ParentalControlManagerImpl.isParentalControlEnabled()` (`:50-53`) returns `false` unless
`hasPremiumSubscription()`, and both platform modules bind a provider that returns a literal
`false`. Everything downstream degrades open: `validatePin()` returns `true` for *any* input,
`isCategoryProtected()` returns `false`, `getProtectedCategoryIds()` returns empty. A user can
set a PIN, lock categories, see them marked protected — and the app never asks for the PIN.
The gating logic itself is a correct port; the original just has real billing behind it.

**#4 — series player.** `launchSeriesPlayer` has **zero call sites** outside its own `expect`
declaration and two `actual`s. `SeriesDetailVoyagerScreen.kt:52` calls `launchChannelPlayer`
instead, so tapping an episode opens the live-TV UI (channel sidebar, EPG panel) over a VOD
stream. Both `androidApp/.../SeriesPlayerActivity.kt` and
`shared/feature/player/src/iosMain/.../SeriesPlayerHost.ios.kt` — the latter including its 1 s
progress-save loop and per-episode resume — are fully written **dead code**. This is the
highest value-per-line fix in the whole list.

---

## 5. P1 — significant gaps

Grouped by theme. All verified unless marked ⚠️.

### Sync and data integrity
- ~~Periodic/background content sync is a permanent no-op after the first successful sync~~ —
  ✅ **fixed** (§4.1): the per-stage resume flags are now cleared once all three stages succeed.
- ~~Background sync is never initialized at app start~~ — ✅ fixed (§4.1, `BackgroundSyncBootstrapper`).
  Still **off by default**, which is intentional: enabling it for users is a product decision.
- Multi-account sync missing — only the current `userId` is ever synced.
- **No database migrations at all** (the original has 24) — confirmed, **not fixed**; the guard
  cannot be enabled on a Windows host. Harmless on a first release, breaks the second. See §4.1.
- `./gradlew check` / `build` fail on this host inside SQLDelight's migration-verify task
  (sqlite-jdbc native load). Pre-existing and environmental; use explicit compile/test tasks
  locally and run `check` on CI. See §4.1.
- Sync lost streaming/chunked persistence, item-level resume, and partial-success semantics.
  Partly addressed: channel sync is now differential (§4.1), so it no longer rewrites the whole
  table each time. Chunked persistence and resume for the movie/series stages remain missing.
- ~~**Foreign keys are never enabled** on either SQLDelight driver → no `ON DELETE CASCADE` fires~~
  — confirmed and **deliberately left off**, with the two defects hiding behind it fixed instead
  (custom groups emptied by every sync; account deletion leaking EPG text). Enabling the pragma
  while `saveM3uData` still uses `INSERT OR REPLACE` would turn stale references into real
  deletions. See §4.1.
- ~~Paging sources never invalidate on DB writes ⚠️~~ — confirmed and ✅ **fixed** (§4.1). All six
  now register SQLDelight listeners; this was the sync section's only unverified item.
- Android sync workers never promote to foreground → long syncs get killed.

### EPG
- ~~`syncEpg()` **inserts the entire EPG dataset twice**~~ — ✅ fixed (§4.1); one `insertEpgDB` call site remains.
- ~~EPG sync has no user-channel allowlist and no ±7-day window~~ — ✅ fixed (§4.1); both applied at parse time.
- ~~Stored M3U `epgUrl` is **never used**~~ — ✅ fixed (§4.1) via a `storedEpgUrl()` fallback. Previously `epgUrlOverride` was hardcoded `null`, so M3U/playlist
  accounts get no EPG at all.
- ~~iOS XMLTV regex parser mangles text the original handles~~ — ✅ **fixed** (§4.1): numeric
  character references, multi-line elements, and entity decode order.
- ~~`getProgrammesForCategory()` is a stub returning `emptyList()`~~ — ✅ implemented (§4.1).
- ~~iOS gzip decompression is a **no-op stub**~~ — ✅ fixed (§4.1): real `inflateInit2(strm, 47)` zlib inflation. XZ remains unsupported, as in the original.
- ~~Paging `nextKey` is computed after parental filtering → truncated channel list~~ — ✅ **fixed**
  (§4.1): exhaustion is now decided by the query's row count, not the post-filter count.
- ~~EPG category-selection screen (step 1 of the mobile EPG flow) missing~~ — ✅ ported (§4.1).

### Players
- Live player was ported from the **retired v1 screen, not the shipping v2 player** — no
  gestures, no lock, no track selector, no settings sheet, no info dialog, no play/pause.
- ~~Live player ignores `licenseKey` and plays `.mpd` through `ProgressiveMediaSource`~~ —
  ✅ **fixed on Android** (§4.1). Still unplayable on iOS: AVPlayer supports neither DASH nor
  ClearKey, so those channels need a different engine there.
- ~~**No playback-start watchdog**~~ — ✅ fixed (§4.1): ported and wired into all three iOS hosts. The original built `PlaybackStartWatchdog` (25 s) precisely
  because a player "can sit in BUFFERING indefinitely without ever calling onPlayerError — a
  panel that accepts the connection and then sends nothing produces exactly that." The port has
  **zero** occurrences of it. See §6 for why this is worse on iOS.
- ~~No output-format fallback ladder on live error recovery~~ — **restated, and the real defect fixed**. There is no "ladder" in the original; what it has is `MediaSourceFactory.normalizePlaybackUrl`, appending the account's output format. The port has an equivalent (`MediaUrlNormalizer`) — but on iOS it was actively harmful. See §4.1.
- ~~Movie/series players are **not forced to landscape**~~ — ✅ fixed (§4.1): `screenOrientation="landscape"` on both VOD activities.
- ~~`"Live buffer size"` … **never read by any player**~~ — ✅ live buffer is now applied; **"Player engine" is still unread**, so that half stands.
- No external subtitle support anywhere; Android VOD subtitle button not enabled.
- Catch-up is **not premium-gated** (paywall absent); no catch-up deeplink from EPG.
- PiP: back button can dead-end on Android; unavailable and broken-by-config on iOS.
- `PlayerHost` expect/actual and the host-based `SeriesPlayerScreen` are **dead code**.

### Movies / series
- **Trailers open an external browser** instead of the in-app YouTube player, and the YouTube
  id is never normalized → broken URLs and phantom Trailer buttons.
- ~~Series detail **spins forever** when the network fetch fails and nothing is cached~~ — ✅ fixed (§4.1).
- ~~Series detail blocks on the network call before showing cached seasons/episodes~~ — ✅ fixed: cache is shown first, network refreshes over it.
- No auto-play-next — one episode as a single media item instead of the season as a playlist.
- ~~Movie detail drops to "Limited information" when the poster or `info.name` is missing~~ — ✅ fixed (§4.1).
- Search-history chips missing from both Movies and Series search bars.
- A configured default movie category no longer switches the tab into the paged grid — **confirmed,
  and deliberately not patched.** The port's Movies tab has *no grid mode at all*: `MoviesScreen`
  renders only the row feed, and tapping a category scrolls to its section rather than filtering
  (`MoviesScreen.kt:536-547`). Categories open a separate `MovieCategoryDetailScreen` instead.
  `loadDefaultCategory` is a faithful copy of the original and still sets `currentCategoryFilter`
  to the configured id, which puts the screen in a state (`isHomeFeed == false`) the layout does not
  handle — it keeps showing the "All" feed while scroll-spy naming and category-tap behaviour change.
  Making this faithful means adding a grid mode to the tab: a feature-sized UI change that needs a
  real library and visual review, not a blind edit. Recorded rather than papered over.
- ~~`"See All"` on the series Continue Watching row opens an empty screen~~ — ✅ fixed (§4.1).

### Auth
- ~~Re-login to an existing account silently keeps the old password / expiration / output formats~~ — ✅ **fixed** (§4.1).
- ~~Account list is a one-shot snapshot → **Switch Account never refreshes**~~ — ✅ fixed (§4.1).
- Switching or adding an account **never triggers a sync** — empty tabs until a cold restart.
- ~~Xtream credential extraction from a pasted M3U/`get.php` URL missing entirely~~ — **restated,
  and fixed** (§4.1). The original has no credential-*extraction* helper; the real omission was
  `ServerUrlNormalizer`, which the port had replaced with three partial copies.

### Settings ⚠️ (unverified set)
- **No localized strings anywhere** — the App Language picker changes nothing. 18 localizations
  lost. This one is corroborated independently: every domain audit found hardcoded English.
- ~~Manage Categories pin/hide/default toggles never update the UI (one-shot flow)~~ — ✅ fixed (§4.1).
- Locked-category toggles never update.
- ~~Custom Groups tab inside Manage Categories is a dead placeholder~~ — ✅ **fixed** (§4.1): no
  caller supplied `customGroupsContent`, so the tab rendered the words "Custom Groups" and nothing
  else. The mode switcher is now hidden unless that slot is filled. Custom groups remain reachable
  from the Channels screen, so this was a dead duplicate entry point, not a missing feature.
- Feedback submission has no backend — stub always returns `false`.
- ~~Change-PIN silently fails when the current PIN is wrong~~ — ❌ **refuted on verification**:
  `ParentalControlScreen.kt:175` sets `pinError = "Current PIN is incorrect"` and passes it to the
  dialog as `errorMessage`. Remove-PIN is likewise gated on the current PIN.
- ~~`clearSessionOnAppStart()` has no call site~~ — ✅ fixed (§4.1): called on cold start from both `HitvApp.kt` and `MainViewController.kt`.
- ~~Un-protecting a category no longer requires the PIN~~ — ✅ fixed (§4.1).
- Session-management UI missing (no banner, countdown, or End Session).
- Theme Studio removed the premium gate — all six themes free.
- ~~Add Channels loads the entire channel table into memory (Paging dropped, not replaced)~~ — ✅ **fixed**. The paged repository methods (`getAllChannels`, `searchAllChannels`) already existed and were already invalidation-wired; only `AddChannelsViewModel` was still calling the unpaged pair, holding the whole Channel table in a StateFlow. Its comment claimed Paging was dropped "for KMP compatibility", but Cash Paging is what the Channels, Movies and Series tabs already use — there was nothing to work around. Now `flatMapLatest` over the debounced query into the paged flow, `cachedIn(viewModelScope)`, and the screen consumes `collectAsLazyPagingItems()`. The two unpaged methods are left in place but documented as not-general-purpose so they are not reached for again.
- `ParentalSessionGuard` wrapping whole tabs and leaving a blank screen when dismissed is
  **invented behaviour, not in the original**.

---

## 6. iOS-specific risk

⚠️ The three dedicated iOS agents did not run. This section is from my own direct inspection —
sound, but **not an exhaustive sweep**. Re-run that pass before treating iOS as audited.

### Verified structural findings

- **`expect`/`actual` coverage is complete.** Every module's iOS actual count matches Android's
  exactly. No missing actuals — the most common CI failure mode is not present.
- **`commonMain` is clean** — `compileCommonMainKotlinMetadata` passes, so no JVM-only API leaks.
- **Umbrella exports** cover `model`, `common`, `navigation`, `designsystem`. The `koin-core`
  dependency is declared five times over (`commonMain`, `iosMain`, and all three
  `ios*Main`) with a comment saying it "proved insufficient across CI runs" — that is a smell
  worth resolving properly, but it is not currently broken.
- **`Info.plist` is in good shape**: ATS arbitrary loads (needed — IPTV is HTTP),
  `CADisableMinimumFrameDurationOnPhone`, `UIViewControllerBasedStatusBarAppearance=false`,
  all four iPad orientations, both BGTask identifiers.
- `UIBackgroundModes` declares `processing` but only `BGAppRefreshTaskRequest` is ever
  submitted. Unused background modes attract App Review questions — drop it or use it.

### iOS runtime defects

**Failure detection depends on a timer that failure stops.** All four iOS players detect
`AVPlayerItemStatusFailed` *inside* `addPeriodicTimeObserverForInterval`
(`ChannelPlayerHost.ios.kt:157`, `MoviePlayerHost.ios.kt:119`, `SeriesPlayerHost.ios.kt`,
`PlayerHost.ios.kt`). That block is invoked as the item's timeline advances and when playback
starts or stops. If an item **fails during load** — dead stream, expired token, 401 from the
panel — the timeline never advances and playback never starts, so the failure branch may never
execute. The user gets an infinite buffering spinner with no error and no retry.

This is exactly the scenario the original wrote `PlaybackStartWatchdog` for, and the port has
no watchdog on either platform. On iOS it is worse than on Android, because Android at least
has ExoPlayer's `onPlayerError` callback firing independently of the timeline.

There *is* a correct pattern already in the codebase — `PlayerHost.ios.kt:331-380` uses
`NSNotificationCenter` observers — but the three screen hosts don't use it, and `PlayerHost`
itself is dead code.

**The movie player has no error UI at all.** `MoviePlayerHost.ios.kt:142-157`: after
`maxRetries`, it sets `isBuffering = false` and does nothing else. No message, no dialog, no
retry affordance — a black screen. Compare the channel player, which correctly calls
`viewModel.setPlaybackError(...)`, and the original, which sets `errorMessage` *and* shows a
Toast (`MoviePlayerActivity.kt:944-950`).

**`presentFromTop` uses deprecated `keyWindow`.** `MoviePlayerHost.ios.kt:224`:
`UIApplication.sharedApplication.keyWindow?.rootViewController ?: return`. Deprecated since
iOS 13 and nil in several legitimate states — and the `?: return` means the player silently
never appears. Should iterate `connectedScenes`.

**Screen auto-locks during movie and series playback** — the idle timer is never disabled on
those paths.

**`submitTaskRequest(request, null)` discards its result.**
`BackgroundSyncManager.ios.kt:66` passes `null` for the `NSError**` out-param and ignores the
returned `Boolean`, then reports `scheduled = true` unconditionally. The surrounding
`catch (Throwable)` cannot fire for a non-throwing cinterop mapping, so a rejected submit is
reported to the user as success.

**Credentials and the parental PIN are stored in plaintext `NSUserDefaults`** on iOS
(`KoinIOS.kt:85-86`), where Android uses `EncryptedSharedPreferences`. `NSUserDefaults` is
readable from an unencrypted device backup. Should be Keychain.

**AVPlayer is never detached on dispose** — `AVPlayerSurface.ios.kt` never sets
`vc.player = null`, so `AVPlayerViewController` retains the player past teardown.

### iOS-only correctness

- The **EPG memory blowup** (P0 #7) is iOS-only: an 80 MB feed becomes ~80 MB `NSData` +
  ~80 MB `ByteArray` + ~160 MB Kotlin UTF-16 `String` held simultaneously, plus regex state
  and the full parsed graph. Very likely jetsam-killed on mid-tier devices, and *guaranteed*
  to be killed from the BGTask path, where the memory ceiling is far lower. Android is
  unaffected — it streams through `XmlPullParser`.
- **iOS movie position is never saved** because `streamId` is always 0 on that path (P0 #9),
  so Continue Watching stays permanently empty on iOS.

---

## 6.1 iOS risks still open

After the 2026-07-26 pass, these remain. None is a crash; all are real.

| Risk | Why it still matters |
|---|---|
| ~~Raw XMLTV still materialized as one `String` on iOS~~ | ✅ **fixed** (§4.1). Streams to a temp file via `downloadTaskWithRequest`, then parses off disk with `NSXMLParser` into `XmltvSaxAssembler`. Peak memory now tracks the *retained* EPG, not the feed size. Written and compiled from Windows against the platform klib; the adapter executes on the simulator in CI (`EpgStreamingLoaderIosTest`). |
| ~~`UIBackgroundModes` declares `processing`, only `BGAppRefreshTaskRequest` is submitted~~ | ✅ **fixed** (§4.1) — and it was hiding two larger problems than the unused mode. Both sync paths now submit `BGProcessingTaskRequest`; `fetch` is dropped from `UIBackgroundModes`; the guard script enforces request-type agreement across Kotlin, Swift and the plist. |
| ~~BGTask handlers set no `expirationHandler`~~ | ✅ **fixed** (§4.1). Both handlers now install one, behind a `TaskCompletion` guard so `setTaskCompleted` is called exactly once — with an expiration handler in place, the normal callback and the OS reclaiming the window can otherwise race, and completing twice terminates the app in the background. |
| `koin-core` declared five times across source sets in `shared/umbrella/build.gradle.kts` | Not broken, but the comment says it "proved insufficient across CI runs" — an unresolved build-config smell, and now diagnosable locally with the cross-compile flag. |
| Billing dead (P0 #6) | Premium tiers render with prices and the buy button does nothing. Parental controls no longer depend on it, but the Premium tab still misleads. |
| **iOS platform actuals: two residues left** | Written to execute on iOS in CI but **not yet run** (§1.3): the whole shared suite, the Keychain including its migration, AVPlayer teardown, real HLS decode, the framework link, and the app launching — which covers BGTask *registration* against the real Info.plist plus the iOS cold start. One thing genuinely cannot be driven off-device: a BGTask **actually firing** — iOS schedules those opportunistically and the only forced trigger is an LLDB-only private API. Note the failure modes *around* it are covered: registration (app-launch step), submission-result handling, identifier drift (§1.4) and the schedule decision (7 tests). A second item was **downgraded on evidence**: the Keychain migration across a signed over-the-top upgrade was called high-risk in earlier revisions, but the project has **no entitlements file and no `keychain-access-groups`**, so the default access group `$(AppIdentifierPrefix).pt.hitv.app` is stable across builds of the same app and team — an upgrade does not change it. The migration logic itself is covered by `KeychainMigrationIosTest`. Both remain in §7 Step 0, but the second is now a sanity check rather than a live worry. |
| General (non-sensitive) settings still `NSUserDefaults` | Correct and intended — only `SENSITIVE_KEYS` moved to the Keychain. Listed so it isn't mistaken for a miss. |

## 7. Recommended order

Ordered by value per unit of effort, not by severity.

**Step 0 — immediately:**
1. ~~**Rotate the leaked provider credentials.**~~ — **closed by the project owner**, who has
   reviewed this and is content to leave the values exposed for now. Recorded rather than removed
   so it is clear this was a decision, not an oversight: the values remain in git history at
   `e76fe84`, and removing the source line does not un-leak them.

   `scripts/check-no-hardcoded-credentials.sh` stays in CI. It is not about the values already
   published — it prevents *new* credential-shaped literals from reaching shipping source, which is
   a separate concern and still worth keeping.
2. **Run one device pass on TestFlight.** Most of §4.1 now executes in CI on a simulator, so this
   pass is about the two things a simulator cannot reach plus real-world content:
   - **Upgrade over an existing install** — install the current TestFlight build, log in, then
     install this one over it. You should stay logged in, and the parental PIN should survive.
     Earlier revisions called this the highest-risk item; that was overstated. The project has no
     entitlements file and no `keychain-access-groups`, so the default access group
     `$(AppIdentifierPrefix).pt.hitv.app` is stable across builds of the same app and team — an
     upgrade does not change it — and the migration logic itself is covered by
     `KeychainMigrationIosTest`. Still worth doing once, since it is the one path where being
     wrong logs out every existing user.
   - **Background sync actually firing.** Don't wait for iOS to schedule it — that can take hours
     and tells you nothing if it doesn't happen. Force it from Xcode:

     1. Run the app from Xcode on the device, enable background sync in More Options.
     2. Background the app (Home), so the task is submitted and pending.
     3. Pause the debugger, and in the LLDB console run:

        ```
        e -l objc -- (void)[[BGTaskScheduler sharedScheduler] _simulateLaunchForTaskWithIdentifier:@"pt.hitv.sync.epg"]
        ```

        then resume. The handler runs immediately. Repeat with `pt.hitv.sync.content`.
     4. To check the new `expirationHandler` path specifically, use
        `_simulateExpirationForTaskWithIdentifier:` with the same identifier. Nothing should crash,
        and the task should be marked complete exactly once — this is the path that used to leave
        the task uncompleted and get the app's future scheduling deprioritised.

     `_simulateLaunchForTaskWithIdentifier:` is a private debug-only API. It works from the LLDB
     console but must never be called from shipping code, which is why this is a manual step rather
     than something wired into CI.
   - **Your provider's real streams**, which CI only approximates with a reference HLS file: a
     dead channel (watchdog → error dialog after ~25 s, not an infinite spinner), a movie that
     404s (error dialog + retry), playing a movie twice (resume position), an episode (series
     player, prev/next), backgrounding mid-playback.
   - **The EPG grid with real guide data** — it has only been seen with synthetic data.
   - Setting a parental PIN, then dismissing the prompt (locked state with Enter PIN, not a
     blank tab).

**First — remaining correctness (a few days):**
3. Fix the periodic-sync no-op-after-first-successful-sync bug. (The *startup re-arm* half of this
   is now done — see §4.1 — but `SyncManagerImpl` still short-circuits later runs.)
4. Enable foreign keys on both SQLDelight drivers — no `ON DELETE CASCADE` currently fires.
5. Localization — restore `stringResource` usage, or the App Language picker is decoration.
6. True differential sync using `contentHash`/`syncVersion` (the data loss is fixed; the full-table
   rewrite and its cost are not).
7. Billing (P0 #6), then revert `UngatedPremiumStatusProvider` to a real entitlement.

**Second — remaining iOS hardening:**
7. Streaming `NSXMLParser` actual for the iOS EPG parse, finishing P0 #7. Do this *on a Mac* —
   it is a delegate-protocol implementation, and while the cross-compile flag would type-check it,
   only a device run will show whether the SAX events are handled correctly.
8. Drop the unused `processing` background mode; add BGTask `expirationHandler`s.

**Third — the large missing features:**
9. In-app trailer player, external subtitles, auto-play-next, the v2 live-player feature set
   (gestures, lock, track selector).
10. The two pieces deliberately left out of the EPG grid: programme reminders (needs an iOS
    notification `expect`/`actual`, permission flow and Info.plist string) and the catch-up
    paywall (follows billing).

**Also still owed from the audit itself:** the iOS build/link, iOS runtime, and iOS stub sweeps
never ran, and 27 settings/series findings were never verified. §6 is direct inspection, not an
exhaustive sweep.

---

## 8. Out of scope (unchanged)

TV layouts, QR pairing, Chromecast, VLC engine, Firebase Analytics (replaced with NoOp).
