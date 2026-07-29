# HITV-KMP

This repo is the **Kotlin Multiplatform port** of the original Android-only HITV IPTV app.

- **Source of truth (original)**: `C:/Users/Fabio/StudioProjects/hitv` — Kotlin/Android, the canonical implementation. Has its own `CLAUDE.md`, `ARCHITECTURE.md`, and feature docs.
- **This repo (target)**: Compose Multiplatform, Android + iOS. Shared modules under `shared/` (`core`, `feature`, `epg`, `umbrella`, `app-ios`), plus `androidApp/` and `iosApp/`.

## Hard rule when porting features

**Always compare against the original `hitv/` repo — before and after.**

1. **Before writing anything**, locate the equivalent in `C:/Users/Fabio/StudioProjects/hitv/` (use Grep/Read on `hitv/app/` and its docs). Treat that as the spec.
2. **Port faithfully** — same behavior, same edge cases, same UX. Do not paraphrase, simplify, or "improve" without explicit approval.
3. **After writing**, diff your port against the original logic and confirm parity. If something must differ (e.g. platform constraint, library swap), call it out explicitly in your response — don't let it slip silently.

Skipping the compare step has caused real divergences that the user only catches by manual testing. Don't make them be the diff tool.

## Library mapping (hitv → hitv-kmp)

| Original (hitv) | KMP replacement |
|---|---|
| Hilt | Koin 4.x |
| Room | SQLDelight 2.x |
| Retrofit | Ktor 3.x |
| Navigation Compose | Voyager |
| ExoPlayer | `expect`/`actual` (ExoPlayer on Android, AVPlayer on iOS) |
| Firebase (Google) | GitLive Firebase KMP |
| Play Billing | `expect`/`actual` |
| DataStore | multiplatform-settings |
| Coil 2 | Coil 3 KMP |
| Lottie | compottie |
| AndroidX Paging | Cash App Paging |

When you see a hitv class using one of the left-column libraries, the KMP equivalent must use the right-column one. Don't import Android-only libs into shared code.

## Package layout

- Shared code: `pt.hitv.*`
- Android app: `pt.hitv.android`
- iOS app: under `iosApp/` (Swift + Compose Multiplatform via `app-ios` shared module)

## What not to do

- Don't use Hilt, Room, Retrofit, AndroidX-only Compose Navigation, or DataStore in shared modules.
- Don't invent behavior that doesn't exist in `hitv/`. If a feature is missing from the original, ask before adding it.
- Don't assume the 40-day-old auto-memory is current — re-check `hitv/` and this repo's actual state when in doubt.
