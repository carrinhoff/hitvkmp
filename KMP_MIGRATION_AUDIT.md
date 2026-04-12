# KMP Migration Audit

**Date:** 2026-04-12
**Original Project:** `C:\Users\Fabio\StudioProjects\hitv` (Android-only)
**KMP Project:** `C:\Users\Fabio\StudioProjects\hitv-kmp` (Kotlin Multiplatform)
**Target Platforms:** Android + iOS (mobile only — no TV layouts)

---

## Migration Status: All 5 Tabs + Navigation DONE

| Component | Original (hitv) | KMP (hitv-kmp) | Status |
|-----------|-----------------|----------------|--------|
| **Navigation** | Compose Navigation + `@Serializable` routes | Voyager + ScreenRegistry | ✅ Done |
| **Scaffold** | `HitvMainScreen` (portrait/landscape/TV) | `AdaptiveScaffold` (portrait/landscape) | ✅ Done |
| **Channels Tab** | StreamViewModel + Paging + EPG + Search | StreamViewModel + Paging + EPG + Search | ✅ Done |
| **Movies Tab** | MovieViewModel + Paging + Sort + Home Feed | MovieViewModel + Paging + Sort + Home Feed | ✅ Done |
| **Series Tab** | SeriesViewModel + Paging + Sort + Home Feed | SeriesViewModel + Paging + Sort + Home Feed | ✅ Done |
| **Premium Tab** | Stateless + BillingManager | Stateless + BillingManager | ✅ Done |
| **More/Settings** | MoreOptionsViewModel + sub-screens | OptionsScreen + MoreOptionsViewModel | ✅ Done |
| **Login** | LoginViewModel + Xtream + M3U | LoginViewModel + Xtream + M3U | ✅ Done |

---

## Data Layer: FULLY MIGRATED

| Layer | Original | KMP | Status |
|-------|----------|-----|--------|
| **Database** | Room (22 entities, 7 DAOs) | SQLDelight (13 tables, 200+ queries) | ✅ Done |
| **Network** | Retrofit + Gson | Ktor + kotlinx.serialization | ✅ Done |
| **Repositories** | 6 repos (Hilt) | 6 repos (Koin) | ✅ Done |
| **Use Cases** | 8 use cases | 8 use cases | ✅ Done |
| **Models** | 40+ domain models | 40+ domain models | ✅ Done |
| **DI** | Hilt (KSP) | Koin (24 modules) | ✅ Done |
| **Sync** | WorkManager | WorkManager (Android), stub (iOS) | ✅ Done (Android) |
| **Billing** | Google Play Billing | Google Play Billing + fake mode | ✅ Done (Android) |
| **EPG** | Simple XML Framework (Java) | Pure Kotlin regex parser | ✅ Done (improved) |

---

## Key Architecture Changes (Original → KMP)

| Area | Original | KMP | Rationale |
|------|----------|-----|-----------|
| Navigation | Compose Navigation | **Voyager** | Better KMP support, ScreenRegistry pattern |
| DI | Hilt (KSP) | **Koin** | Multiplatform, no annotation processing |
| Database | Room | **SQLDelight** | Multiplatform, type-safe SQL |
| Network | Retrofit + Gson | **Ktor + kotlinx.serialization** | Multiplatform, Kotlin-native |
| Preferences | EncryptedSharedPreferences | **multiplatform-settings** | Platform secure storage abstraction |
| EPG Parser | Simple XML Framework (Java) | **Pure Kotlin regex** | No external dependency, multiplatform |

---

## Completed Features — Detail

### Channels Tab (Live TV)
- Search with real-time query filtering
- Category filtering with dropdown selector
- Bottom sheet for advanced category selection
- Category counts display
- Favorites management (toggle on long-click)
- Recently viewed channels tracking
- Pagination with Paging 3 (cash.paging)
- EPG caching and display
- Scroll-to-top signal handling
- Analytics integration
- Loading states and error handling

### Movies Tab
- **Home Feed sections:** All, Continue Watching, Favorites, Recently Viewed, Last Added, Category rows
- Search (case-insensitive, 500 item limit)
- Category filtering with dropdown + bottom sheet
- Scroll spy (tracks visible category name)
- Scroll to specific category from bottom sheet
- Category counts display
- Pagination with sorting (by Added Date, ascending/descending toggle)
- Favorites toggle with visual feedback
- Loading skeleton UI
- Focus state management for TV (pendingFocus, activeFocus)

### Series Tab (TV Shows)
- Same Home Feed layout as Movies (All, Continue Watching, Favorites, Recently Viewed, Last Added, Categories)
- Search, category filtering, scroll spy
- Sorting with ascending/descending
- Seasons & Episodes data fetching
- Focus state management for TV
- Loading skeletons

### Premium Tab
- Annual Premium tier with "BEST VALUE" badge
- Lifetime Premium tier
- Price display (€ formatted)
- Feature lists with checkmarks
- Purchase buttons with animations
- Premium status card (if already subscribed)
- Trial status display with expiration date
- Animated fade-in, gradient background

### More/Settings Tab
- Welcome header with expiration date display
- Live TV, Movies, TV Shows quick-access cards (gradient)
- Switch Account/Playlist button
- Theme/Premium button
- More Options button
- Channel preview toggle
- Account info display (username, hostname, expiration)
- Language selection

**More Options sub-features (original has, KMP status):**

| Feature | Original | KMP | Status |
|---------|----------|-----|--------|
| Player engine selection (ExoPlayer/VLC) | `playerEngine` in MoreOptionsUiState | MoreOptionsViewModel has it | ✅ Done |
| Live buffer size (small/medium/large/very_large) | `liveBufferSize` in MoreOptionsUiState | MoreOptionsViewModel has it | ✅ Done |
| Channel preview toggle | `channelPreviewEnabled` | MoreOptionsViewModel has it | ✅ Done |
| Background sync config (EPG/content intervals) | WorkerHelper integration | SyncManager | ✅ Done |
| Language selection with app restart | `currentLanguage` | MoreOptionsViewModel has it | ✅ Done |
| Theme settings (full UI with previews) | ThemeSettingsScreen | Not yet migrated | ⬜ P1 |
| Parental controls (PIN, per-category) | ParentalControlScreen | Not yet migrated | ⬜ P1 |
| Manage categories (reorder, visibility) | ManageCategoriesScreen | Not yet migrated | ⬜ P2 |
| Feedback/suggestion form | SuggestionScreen | Not yet migrated | ⬜ P2 |
| Discord community link | Button with URL | Basic button exists | ✅ Done |

### Login Screen
- TabRow: Xtream Credentials ↔ M3U URL
- Xtream: Username, Password (visibility toggle), Server URL fields with validation
- M3U: Playlist name, URL, EPG URL (optional) fields
- Error dialog, loading overlay
- Suggestions & Feedback button, Discord community link
- Analytics tracking

---

## Infrastructure — Real vs Stub

| Component | Status | Details |
|-----------|--------|---------|
| Android WorkManager Sync | ✅ Real | DataSyncWorker + EpgSyncWorker with progress tracking |
| Android Google Play Billing | ✅ Real | Full BillingClient + fake mode for testing |
| EPG XMLTV Parser | ✅ Real | Pure Kotlin regex parser, no external XML lib |
| Firebase Analytics | ✅ Real | 30+ event types (NoOp until google-services.json added) |
| Koin DI (24 modules) | ✅ Real | All layers wired, platform modules ready |
| iOS Billing | ⬜ Stub | IosBillingManager — needs StoreKit 2 interop |
| iOS Sync | ⬜ Stub | IosSyncScheduler — needs BGTaskScheduler Swift integration |
| PremiumStatusProvider | ⬜ Stub | Hardcoded `false` in AndroidPlatformModule |
| HitvApp hasAnnualOrLifetime | ⬜ Stub | Hardcoded `false`, should use BillingManager.isPremium |

---

## What's Still Needed (Not Yet Migrated)

### Screens / Features

| Screen | Original Location | Priority |
|--------|-------------------|----------|
| ~~Movie Detail~~ | ~~`app/.../movies/detail/MovieInfoContent.kt`~~ | ✅ Done |
| ~~Series Detail~~ | ~~`app/.../series/detail/SeriesInfoContent.kt`~~ | ✅ Done |
| ~~Movie Category Detail~~ | ~~`app/.../navigation/screens/CategoryDetailRoutes.kt`~~ | ✅ Done |
| ~~Series Category Detail~~ | ~~`app/.../navigation/screens/CategoryDetailRoutes.kt`~~ | ✅ Done |
| EPG Full Grid | `epg/` module + `DetailRoutes.kt` | P1 |
| Switch Account (full UI) | `feature/auth/.../switchaccount/` | P1 |
| QR Pairing | `feature/auth/.../qr/` | P2 |
| Theme Settings (full UI) | `feature/settings/.../theme/` | P1 |
| Parental Control (full UI) | `feature/settings/.../parental/` | P1 |
| Manage Categories | `feature/settings/.../categories/` | P2 |
| Feedback/Suggestion | `feature/settings/.../feedback/` | P2 |

### Player Activities

| Player | Original Location | Priority |
|--------|-------------------|----------|
| Movie Player | `app/.../movies/player/MoviePlayerActivity.kt` | P0 |
| Series Player | `app/.../series/player/SeriesPlayerActivity.kt` | P0 |
| Live Channel Player | `app/.../player/NewCastPlayerActivity.kt` | P0 |
| ~~YouTube Trailer Player~~ | ~~`app/.../player/YoutubePlayerActivity.kt`~~ | ✅ Done (opens YouTube via system browser/app) |
| Cast Player | `app/.../player/NewCastPlayerActivity.kt` (Chromecast) | P2 |

### ~~TV Layouts~~ — NOT NEEDED

> KMP migration targets mobile only (Android + iOS). All TV layouts (TvChannelsLayout, TvMoviesBrowser, TvSeriesBrowser, TV drawer nav, TV player controls) are **out of scope**.

### Platform Features

| Feature | Priority | Notes |
|---------|----------|-------|
| Deep link handling | P1 | `DeepLinkTarget` sealed class in core:navigation |
| PiP mode | P2 | Mobile only, in player Activities |
| Chromecast auto-switch | P2 | CastPlayer integration |
| External subtitles (SRT/VTT/ASS/TTML) | P1 | In player Activities |
| Sleep timer | P2 | In player Activities |
| Catch-Up TV playback | P1 | CatchUpUrlBuilder + EPG integration |
| Parental control PIN enforcement | P1 | ParentalControlManager |

---

## Project Statistics

| Metric | Original | KMP |
|--------|----------|-----|
| Modules | 18 | 24 (shared + androidApp) |
| LoC (approx) | ~50k | ~30k (shared) + growing |
| Unit Tests | 1,065 | TBD |
| Instrumented Tests | 229 | TBD |
| Room/SQLDelight Entities | 22 entities, 20 migrations | 13 tables (implicit migrations) |
| DAOs / Query files | 7 DAOs | 13 .sq files, 200+ queries |
| Network DTOs | 18+ | 20+ |
| Domain Models | 40+ | 40+ |

---

## Quality Assessment

The KMP project is **well-architected** with clean separation, proper layering, convention plugins, and real implementations (not stubs) for all core Android functionality. The 5 main tabs are feature-complete with search, filtering, paging, favorites, recently viewed, continue watching, and category browsing. The data layer is production-ready.

All detail screens are done (movie detail, series detail, movie category, series category) with cast, trailer, favorites, recently viewed, playback position, paged grids, search, sort, shimmer skeletons, and scroll position preservation. The main remaining work is **players and settings sub-screens**. TV layouts are out of scope (mobile-only target).
