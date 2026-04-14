# HITV — KMP Migration Audit

**Last Updated:** 2026-04-14  
**Original Project:** `hitv` (Android-only, Kotlin + Jetpack Compose)  
**KMP Project:** `hitv-kmp` (Kotlin Multiplatform — Android + iOS)  
**Repository:** [github.com/carrinhoff/hitvkmp](https://github.com/carrinhoff/hitvkmp)  
**Target Platforms:** Android (mobile) + iOS (mobile) — no TV  

---

## 1. Executive Summary

HITV is an IPTV streaming app migrated from Android-only to Kotlin Multiplatform (KMP). The migration shares **~90% of code** between Android and iOS — including all UI (Jetpack Compose Multiplatform), ViewModels, repositories, database, networking, and navigation. Only video playback rendering is platform-specific (ExoPlayer on Android, AVPlayer on iOS).

**Current status:** The app is functional on both platforms. Login, data sync, all 5 main tabs, detail screens, category browsing, and live channel playback all work. The iOS build is deployed to TestFlight via GitHub Actions CI/CD. The main remaining gap is movie/series VOD playback.

---

## 2. Architecture Overview

### Tech Stack Comparison

| Layer | Original (Android) | KMP (Android + iOS) |
|-------|-------------------|---------------------|
| **UI** | Jetpack Compose | Compose Multiplatform |
| **Navigation** | Navigation Compose + `@Serializable` | Voyager + ScreenRegistry |
| **DI** | Hilt (KSP) | Koin (multiplatform) |
| **Database** | Room (22 entities, 7 DAOs, 20 migrations) | SQLDelight (13 tables, 200+ queries) |
| **Network** | Retrofit + Gson | Ktor + kotlinx.serialization |
| **Preferences** | EncryptedSharedPreferences | multiplatform-settings (NSUserDefaults on iOS) |
| **Paging** | AndroidX Paging 3 | Cash App Paging (multiplatform) |
| **Image Loading** | Coil 2 | Coil 3 (multiplatform) |
| **Async** | Coroutines + Flow | Coroutines + Flow (same) |
| **EPG Parser** | Simple XML Framework (Java) | Pure Kotlin regex parser |
| **Live Player** | ExoPlayer (Activity) | ExoPlayer (Android) / AVPlayerViewController (iOS) |
| **Preview Player** | ExoPlayer inline | ExoPlayer (Android) / AVPlayer + UIKitView (iOS) |
| **Sync** | WorkManager | WorkManager (Android) / stub (iOS) |
| **Billing** | Google Play Billing | Google Play Billing (Android) / stub (iOS) |
| **Analytics** | Firebase Analytics | NoOp (both platforms, Firebase removed) |

### Module Structure

```
hitv-kmp/
├── androidApp/                    # Android app (MainActivity, ChannelPlayerActivity)
├── iosApp/                        # iOS app (SwiftUI entry, Xcode project)
├── shared/
│   ├── core/
│   │   ├── model/                 # Data classes (Channel, Movie, TvShow, etc.)
│   │   ├── domain/                # Repository interfaces + use cases
│   │   ├── data/                  # Repository implementations, mappers, parsers
│   │   ├── database/              # SQLDelight schema + queries
│   │   ├── network/               # Ktor API services + DTOs
│   │   ├── common/                # PreferencesHelper, analytics, platform helpers
│   │   ├── navigation/            # Voyager routes, AdaptiveScaffold, screen registry
│   │   ├── designsystem/          # Theme, colors, shared components
│   │   ├── ui/                    # Shared UI components (cards, dialogs, skeletons)
│   │   ├── sync/                  # WorkManager sync (Android), stub (iOS)
│   │   └── billing/               # Google Play Billing (Android), stub (iOS)
│   ├── feature/
│   │   ├── auth/                  # Login, switch account
│   │   ├── channels/              # Live TV browsing, channel preview
│   │   ├── movies/                # Movie list, detail, category detail
│   │   ├── series/                # Series list, detail, category detail
│   │   ├── player/                # Player ViewModels, shared overlay UI, platform launchers
│   │   ├── premium/               # Premium subscription screen
│   │   └── settings/              # More options, theme, parental controls
│   ├── epg/                       # EPG XMLTV parser
│   └── umbrella/                  # iOS framework aggregator (produces "shared" framework)
├── build-logic/                   # Gradle convention plugins
└── .github/workflows/             # CI/CD (iOS TestFlight, Android APK)
```

### Platform-Specific Code (`expect/actual`)

| Component | commonMain | androidMain | iosMain |
|-----------|-----------|-------------|---------|
| `PreferencesHelper` | Settings interface | EncryptedSharedPrefs | NSUserDefaults |
| `PlatformDetector` | expect object | Android context checks | UIDevice checks |
| `DatabaseDriverFactory` | expect class | Android SQLite driver | iOS SQLite driver |
| `CryptoManager` | expect class | Passthrough (no encryption) | Passthrough (no encryption) |
| `QRCodeGenerator` | expect object | ZXing library | Stub (not needed) |
| `LocaleManager` | expect class | Android locale API | NSLocale |
| `launchChannelPlayer()` | expect fun | Intent → ChannelPlayerActivity | AVPlayerViewController |
| `launchMoviePlayer()` | expect fun | Intent → MoviePlayerActivity (TBD) | AVPlayerViewController (TBD) |
| `ChannelPreviewComposable` | expect composable | ExoPlayer + AndroidView | AVPlayer + UIKitView |

---

## 3. Feature Completion Matrix

### Screens

| Screen | Android | iOS | Notes |
|--------|---------|-----|-------|
| Login (Xtream credentials) | ✅ | ✅ | Username, password, server URL, validation |
| Login (M3U URL) | ✅ | ✅ | Playlist name, URL, EPG URL |
| Channels list | ✅ | ✅ | Paging, categories, search, favorites, EPG |
| Channel preview | ✅ | ✅ | Inline video, muted by default, expand/collapse |
| Channel player | ✅ | ✅ | Full-screen, PiP (Android), channel list sidebar, EPG overlay, sleep timer, aspect ratio, rotation |
| Movies home feed | ✅ | ✅ | All, Continue Watching, Favorites, Recently Viewed, Last Added, category rows |
| Movie detail | ✅ | ✅ | Poster, plot, TMDB cast, trailer button, play button, favorites |
| Movie category grid | ✅ | ✅ | Paged 3-column grid, search, sort (Added/Name/Rating), category bottom sheet |
| Series home feed | ✅ | ✅ | Same layout as movies |
| Series detail | ✅ | ✅ | Season tabs, episode list with thumbnails + progress bars, trailer |
| Series category grid | ✅ | ✅ | Same as movie category grid |
| Premium | ✅ | ✅ | Annual/Lifetime tiers, pricing, feature lists |
| More/Settings | ✅ | ✅ | Account info, quick access cards, language, player config |
| **Movie player** | ❌ | ❌ | **Activity not yet created** |
| **Series player** | ❌ | ❌ | **Activity not yet created** |
| Switch Account | ⬜ | ⬜ | ViewModel exists, UI not wired |
| Theme Settings | ⬜ | ⬜ | Not yet migrated |
| Parental Controls | ⬜ | ⬜ | Not yet migrated |
| EPG Full Grid | ⬜ | ⬜ | Not yet migrated |

### Data & Infrastructure

| Feature | Android | iOS | Notes |
|---------|---------|-----|-------|
| Xtream API login + data fetch | ✅ | ✅ | Full API integration |
| M3U playlist parsing | ✅ | ✅ | Shared parser |
| SQLDelight local database | ✅ | ✅ | 13 tables, 200+ queries |
| Offline-first data loading | ✅ | ✅ | Cache-then-network pattern |
| Differential sync | ✅ | ✅ | contentHash + syncVersion |
| Background sync (WorkManager) | ✅ | ⬜ Stub | iOS needs BGTaskScheduler |
| Paging (channels, movies, series) | ✅ | ✅ | Cash Paging multiplatform |
| FTS search (movies, series) | ✅ | ✅ | SQLDelight FTS4 |
| TMDB cast/crew fetching | ✅ | ✅ | API key in shared NetworkModule |
| EPG parsing (XMLTV) | ✅ | ✅ | Pure Kotlin regex parser |
| Favorites (channels, movies, series) | ✅ | ✅ | Toggle on long-click |
| Recently viewed tracking | ✅ | ✅ | Auto-saved on detail view |
| Continue watching | ✅ | ✅ | Playback position tracking |
| Playback position resume | ✅ | ✅ | Saved per movie/episode |
| Google Play Billing | ✅ | ⬜ Stub | iOS needs StoreKit 2 |
| Shimmer loading skeletons | ✅ | ✅ | Category rows + grids |

### Player Features

| Feature | Channel Player | Movie Player | Series Player |
|---------|---------------|-------------|---------------|
| Video playback | ✅ Android + iOS | ❌ Not built | ❌ Not built |
| HLS streaming | ✅ | — | — |
| URL normalization (output format) | ✅ | — | — |
| PiP mode | ✅ Android | — | — |
| Channel switching (prev/next) | ✅ | — | — |
| Channel list sidebar | ✅ | — | — |
| Category filter in sidebar | ✅ | — | — |
| Search in sidebar | ✅ | — | — |
| EPG overlay | ✅ | — | — |
| Sleep timer | ✅ | — | — |
| Aspect ratio toggle | ✅ | — | — |
| Screen rotation | ✅ | — | — |
| Auto-retry (3x) | ✅ | — | — |
| Buffering indicator | ✅ | — | — |
| Error dialog with retry | ✅ | — | — |

---

## 4. CI/CD Pipeline

### GitHub Actions (Primary)

**Workflow:** `.github/workflows/ios-testflight.yml`  
**Trigger:** Push to `master` or manual dispatch  
**Runner:** `macos-15` (Xcode 16+, iOS 18 SDK)  

Pipeline steps:
1. Checkout → JDK 17 → Gradle cache
2. Generate placeholder app icon
3. Install distribution certificate (.p12) + provisioning profile
4. Build KMP iOS framework (`shared:umbrella:linkReleaseFrameworkIosArm64`)
5. Xcode archive → Export IPA
6. Upload to TestFlight via `xcrun altool`

**Build time:** ~20 min (first run), ~10 min (cached)

### Codemagic (Backup)

**Config:** `codemagic.yaml`  
**Workflows:** iOS TestFlight + Android Debug APK

---

## 5. iOS-Specific Adaptations

| Adaptation | Why |
|-----------|-----|
| `NSAppTransportSecurity` → AllowsArbitraryLoads | IPTV servers use HTTP |
| `CADisableMinimumFrameDurationOnPhone` = true | Compose Multiplatform requires it for 120Hz |
| iPad orientations in Info.plist | App Store upload requires all 4 orientations for iPad |
| Umbrella module (`shared:umbrella`) | Single `shared` framework for Swift `import shared` |
| `KoinIOS.kt` in umbrella module | Avoids circular deps (core:navigation ↔ features) |
| QR Pairing removed | TV-only feature, not needed on mobile |
| Firebase removed | Not needed for mobile, was causing linker errors |
| CryptoManager stubbed | No encryption (same as Android KMP) |
| QRCodeGenerator stubbed | TV-only, returns null |
| PremiumStatusProvider = false | Billing not integrated on iOS yet |
| AVPlayer `isMuted` for preview | Kotlin/Native property access differs from JVM |
| URL forced to `.m3u8` on iOS | AVPlayer only supports HLS natively |

---

## 6. What's Still Needed

### P0 — Must Have Before Publishing

| Item | Type | Difficulty | Both Platforms |
|------|------|-----------|----------------|
| Movie Player | Feature | Medium | Android: ExoPlayer Activity, iOS: AVPlayerViewController |
| Series Player | Feature | Medium | Android: ExoPlayer Activity, iOS: AVPlayerViewController |
| App Icon | Asset | Design | Replace placeholder blue square |
| Privacy Policy URL | Legal | Easy | Required by both stores |
| Store Listing Graphics | Asset | Design | Feature graphic (Play Store), screenshots |

### P1 — Should Have

| Item | Type | Difficulty |
|------|------|-----------|
| Switch Account screen | Feature | Easy |
| EPG Full Grid | Feature | Medium-Hard |
| Theme Settings UI | Feature | Easy |
| Parental Controls UI | Feature | Medium |
| Deep link handling | Feature | Easy-Medium |
| iOS background sync (BGTaskScheduler) | Platform | Medium |
| iOS billing (StoreKit 2) | Platform | Medium |

### P2 — Nice to Have

| Item | Type | Difficulty |
|------|------|-----------|
| Manage Categories (reorder) | Feature | Easy |
| Feedback form | Feature | Easy |
| Catch-Up TV playback | Feature | Medium |
| External subtitles (SRT/VTT) | Feature | Medium |
| Chromecast | Platform | Hard |

### Removed from Scope

| Item | Reason |
|------|--------|
| TV layouts | Mobile only — no Android TV or Apple TV |
| QR Pairing | TV-only feature |
| VLC player engine | ExoPlayer/AVPlayer sufficient |
| Cast player | Not needed for launch |
| Firebase Analytics | Replaced with NoOp |

---

## 7. Project Statistics

| Metric | Original (Android) | KMP (Android + iOS) |
|--------|-------------------|---------------------|
| Platforms | 1 (Android) | 2 (Android + iOS) |
| Modules | 18 | 25 (+ umbrella + workflows) |
| Shared code | 0% | ~90% |
| Lines of code | ~50k | ~35k shared + ~2k platform |
| Database tables | 22 (Room) | 13 (SQLDelight) |
| SQL queries | ~100 (DAO methods) | 200+ (.sq files) |
| Network DTOs | 18+ | 20+ |
| Domain models | 40+ | 40+ |
| Use cases | 8 | 8 |
| Repositories | 6 | 6 |
| CI/CD pipelines | 0 | 2 (GitHub Actions + Codemagic) |
| TestFlight builds | 0 | ✅ Automated |
