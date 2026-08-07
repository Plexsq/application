# PLEX MOBILE — BETA 1.0 · FINALIZED PLAN

Status: ALL PHASES COMPLETE (2026-08-07). Phase 1-7 code done + compiles; Phase 8 icon generated (adaptive foreground from logo via sharp, verified in debug+release APKs); emulator smoke test clean (app installs/launches, no crashes); release build passes R8/minify.
Local SDK installed on VPS — compile via `./gradlew assembleDebug` locally (see AGENTS.md). No Android SDK on VPS → compile/APK via GitHub Actions `release.yml` (tag `BETA-X.Y`).

## Locked decisions
- Playlists/liked/recents = FULL ACCOUNT SYNC (desktop↔mobile, no restart).
- Streams = DIRECT-FIRST on-device innertube resolution, server proxy fallback.
- Settings = full desktop import (profile name/username/avatar, dark/light, accent).
- Stats = SERVER-AGGREGATED (new `stats` table + 2 endpoints; desktop switches from localStorage).
- Notification = media3 `MediaSessionService` (real media notification, seekbar, controls on top).
- Icon = deferred to publish step (empty foreground vector = robot cause).
- Rules: never delete files, never rewrite whole files, no alert()/confirm(), NO client secret on mobile.

## Verified root causes (don't re-diagnose)
- 1-min playback start: cold stream chain (worker resolve up to 12s + googlevideo fetch + 403 re-resolve + Render local fallback batch ~24s). Fix = device-side resolve.
- Downloads die on tab switch: `rememberCoroutineScope` in SearchScreen.kt:73/338, LikedScreen:50, RecentsScreen:50; cancelled on nav → `OfflineRepository.download` `runCatching` swallows CancellationException → partial `.m4a` orphaned.
- Download silently fails: `Http.client` readTimeout 20s (Http.kt:23) used by `OfflineRepository.download` (line 99) — full-song proxy download > 20s.
- Memory/jank: `OfflineRepository` reads/writes index on CALLING thread (withContext0 = identity, line 192); `recordPlay` (line 182) fired on Main inside 500ms `widgetUpdaterJob` (PlaybackController.kt:277) → main-thread JSON parse + file write every new song.
- Notification: `PlaybackService.kt:148` `onBind` returns null; hand-built transport notification. MediaSession exists (PlaybackController.kt:212) but no MediaSessionService. Media3 default notification needs `MediaMetadata.artworkData` bytes (setArtworkUri alone → blank art).
- Splash start: `SplashScreen.kt:50` returns `Destinations.SEARCH` → change to HOME.
- saveUserDataNow (PlaybackController.kt:337-348) omits playlists/playtime/play_counts.
- restore() (PlaybackController.kt:309-324) loads server data only at startup.
- PlaylistStore is local-only, no image field (add `image: String?` for desktop parity).
- Icon: `ic_launcher_foreground.xml` is an EMPTY vector; `ic_launcher_photo.png` == web `icon-512.png` (md5 a7fa6e90...) = the logo.

## Desktop references (mirror these)
- Share link format: `window.location.origin + '/track/' + songId` (player.js:3518).
- Song menu: Play next / Add-or-remove queue / ─ / Go to artist / Add to playlist / ─ / Share (player.js:3442-3448).
- Playlist menu: Rename / Change cover / ─ / Delete (player.js:3794-3822).
- Search "show more" pattern: `.artist-show-more` button reveals hidden section (player.js:1519-1526).
- Settings HTML sections: index.html:620-698 (General/Appearance/Discord RPC/Mobile App/Statistics). NO client secret on mobile.
- QR: desktop POST `/api/qr/session` → `plexqr://swap?token=…`; mobile scans. Swap = `/api/qr/swap` (server.js:328-346) is userId-based → works for Google-OAuth desktop accounts. AuthRepository.qrSwap + QrScanner already exist.
- Stats endpoints to add in server.js; `_collectUserData` already syncs playtime/play_counts via user-data blob (player.js:478-494) — replace source of truth with server.
- `getPlaytimeStats`/`getTopPlayed` localStorage impl: player.js:3185-3229.

## PHASES

### Phase 1 — Data layer & account sync ✅ DONE
- Models.kt: add `PlaytimeData(daily=0, monthly=0)`; add `playtime` + `play_counts: Map<String,Int>` (defaults) to `UserData`.
- PlaylistStore.kt: add `syncFromServer(server: List<UserPlaylist>)` — adopt server if local empty, else union by id (keep local for matches, add server-only). Add `image` field to local UserPlaylist.
- PlaybackController.kt:
  - restore(): after fetch, `playlists.syncFromServer(d.playlists)`, merge playtime/play_counts (max-merge, don't wipe non-empty local with empty server).
  - new `refresh()`: re-fetch + re-merge (favorites, recentlyPlayed, playlists, stats); debounced ~1s.
  - saveUserDataNow(): include playlists (from services.playlists), playtime, play_counts.
  - On PlaylistStore write → trigger debounced save (wire via PlexApp.Services or callback).
- MainActivity.kt `onResume` → `PlaybackController.refresh()`.
- Offline stays device-local; already live via `version` flow.

### Phase 2 — Statistics, server-aggregated
- server.js: add `stats` table (user_id, day_key, month_key, play_counts JSON) via existing ensureTables/migration helper. POST `/api/stats/play` (auth, throttled) {songId?, seconds?}; GET `/api/stats` → {daily, monthly, top:[{song, plays}]}.
- Desktop player.js: POST play/seconds events (keep 10s tick + trackPlay), render Statistics from GET /api/stats; localStorage = optimistic fallback only.
- Mobile: new StatsRepository (POST/GET); widget updater sends ~60s seconds heartbeat + play event at 30s threshold; Settings Statistics section reads GET /api/stats.

### Phase 3 — Fast playback (direct-first)
- PlaybackController: make resolve async — try `InnertubeResolver` on-device (ConfigRepository innertubeKey + innertubeClients, ~6s cap) → direct googlevideo URL; fallback `https://music.plexs.me/api/embed/stream/<id>`. Pre-resolve next queued track during playback.
- InnertubeResolver.kt: already correct; wire ConfigRepository config.
- Tighten timeouts: worker stream.ts resolve 12→6s, fetchAudio 30→20s; server.js metaProxyStream abort 20→12s. Deploy worker + push desktop repo.
- Offline `?low=1` downloads use same direct-first resolver.

### Phase 4 — DownloadManager (survives navigation + progress)
- New `data/offline/DownloadManager.kt` singleton: app-lifetime CoroutineScope + Dispatchers.IO. StateFlow<Map<id, DownloadState>>, DownloadState = Idle|Downloading(progress:Float)|Done|Failed(msg). Dedup per id. Own OkHttp client with 120s read timeout. On failure/cancel: delete partial file. Bump offline.version on done.
- Replace screen-scoped `scope.launch { downloadSong() }` in SearchScreen.kt:338, LikedScreen, RecentsScreen with `DownloadManager.download(song)` + collect state. Playlist downloads via manager too.
- PlaybackController.maybeAutoDownload (line 287) → route through DownloadManager.

### Phase 5 — Navigation: swipeable tabs + start on Home
- MainActivity.kt: keep SPLASH/SIGN_IN/SIGN_UP/FORGOT/NOW_PLAYING as NavHost destinations; add single MAIN destination hosting HorizontalPager of 5 category screens (Destinations.CATEGORIES), each wrapped in PlayerShell. Pager page ↔ tab synced; clicking active tab = guarded no-op. HomeScreen.onOpenSection → animateScrollToPage. PlayerShell/CategoryBar refactor to selectedIndex + onSelect(index).
- SplashScreen.kt:50 → `Destinations.HOME`.

### Phase 6 — Screens
- HomeScreen.kt: LazyRow of 120dp LibraryTiles (lines 121-170) → scrollable 2-col LazyVerticalGrid of vertical cards (square thumb top, name, count). Recently/Liked/Offline become cards. REMOVE NewPlaylistTile (lines 167-169, 325-347); keep header button. Keep offline section (search + list) below grid. Long-press card → PlaylistContextMenu.
- PlaylistContextMenu (new): Play, Play next, Add to queue, ─, Rename, Delete (confirm).
- Common.kt SongOverflowMenu (lines 99-128): add Play next, Add to queue, Share (Icons + separators, PC-clean). Share = ACTION_SEND `https://music.plexs.me/track/<id>`.
- PlaybackController: add playNext(song) (insert after current), addToQueue(song) (append), removeFromQueue(id).
- SearchScreen: artists → 1 + "Show more" toggle; spacing/typography cleanup (CardRow/SongRow).
- NowPlayingScreen: add Lyrics toggle (fetch CatalogRepository.lyrics(song.id, title, artist); scrollable text; no artwork). Keep queue view.
- PlayerBar.kt: Spotify-style — art ~52dp, title/artist, big play/pause + next, thin progress bar, drop shuffle/repeat/prev clutter.
- SettingsScreen.kt (rewrite sections):
  - Account: name/username/avatar edit via POST `/api/profile` multipart (needs new Http.postForm()).
  - Appearance: dark/light toggle + accent picker, persisted (SessionStore/DataStore), applied in Theme.kt (needs lightColorScheme + accent override).
  - Link to desktop: QrScanner → vm.qrScan(token) → refresh() (fixes Google-OAuth linking).
  - Discord support → ACTION_VIEW https://discord.gg/AQEUbdPX6p.
  - Statistics (server-backed), Offline mgmt (downloads + progress + delete), About/version. NO client secret.

### Phase 7 — Memory & perf
- OfflineRepository: in-memory index cache + Mutex; all read/write via Dispatchers.IO; recordPlay off Main. Removes main-thread JSON churn.
- PlaybackController: no main-thread disk access.

### Phase 8 — Beta 1.0 release (incl. icon)
- Icon: generate adaptive foreground from logo (sharp available at /root/Desktop/music.plexs.me/node_modules). drawable-nodpi/ic_launcher_foreground.png = logo mark scaled to safe zone on transparent canvas; ic_launcher.xml + ic_launcher_round.xml → bg @drawable/ic_launcher_background (solid #131313), fg = new PNG. Keep raw/keep.xml.
- Tag `BETA-1.0` → versionName 1.0, versionCode 100 → workflow builds signed APK + GitHub release + delta patch + update-manifest.json.
- Verify: bootstrap reports 1.0; APK 200; install; walk through all 27 fixes.

### Phase 9 — On-VPS device testing (new)
- Emulator + system image installed on VPS: `sdkmanager "emulator" "system-images;android-35;google_apis;x86_64"`. KVM confirmed at /dev/kvm → hardware-accelerated x86_64 headless AVD.
- Create AVD, boot headless (`-no-window -no-audio -gpu swiftshader_indirect`), install debug APK via adb, grab `adb exec-out screencap` for the icon + each screen, run logcat + dumpsys to verify: launcher icon renders (not the robot), notification shows SeekBar/art/controls on top, playback starts, tabs swipe, downloads survive navigation, stats POSTs fire, no crashes.
- This replaces blind-release: only tag BETA-1.0 after the on-device pass is green. Keeps the manual test matrix (below) as the final human pass.

## Manual test matrix (post-install)
Play start time · download survives tab switches + progress · swipe tabs · active-tab no-op · long-press playlist menu · share · lyrics · media notification (seekbar/art/controls on top) · stats match desktop · playlist sync both directions · QR link from Google-OAuth desktop · theme/accent · profile edit · Discord button · memory stability.

## Risks / non-100%
- Media3 1.5.1 MediaSessionService manifest requirements (exported=true + `androidx.media3.session.MediaSessionService` intent-filter; foregroundServiceType mediaPlayback kept) — confirm vs library.
- stats table creation — follow existing ensureTables/migration pattern in server.js.
- On-device innertube resolution varies by network — proxy fallback covers.
- Cross-device queue stays session-local (intentional).
