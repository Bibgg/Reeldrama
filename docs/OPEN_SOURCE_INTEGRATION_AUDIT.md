# Open-source OTT / episode-player audit for ReelDrama

Date: 2026-11-10
Repository reviewed: https://github.com/Bibgg/Reeldrama

## Current ReelDrama status (checked on 2026-11-10)
- Android app is native Kotlin + Jetpack Compose.
- Existing app already has drama details, episode selection, vertical episode pager, progress callbacks and next-episode callback.
- Existing `VideoSurfacePlayer.kt` uses Android `MediaPlayer` + `TextureView`; it is not Media3/ExoPlayer.
- Backend has Drama and Episode MongoDB models and public published-drama/episode routes.
- APISHort integration currently exposes admin-only provider list, language list and popular metadata endpoints. It does not yet expose a verified per-drama episode-detail / authorized playback-source flow.
- Latest GitHub Actions debug APK workflow for commit `6317522fffb257676495c7d4025301f9cd094321` completed successfully. This confirms compilation/package workflow, not end-to-end video playback or a signed release APK.

## Candidate open-source projects reviewed

| Project | License / evidence | Useful parts | Decision |
|---|---|---|---|
| [open-ott-play/ottplay-android](https://github.com/open-ott-play/ottplay-android) | MIT; native Kotlin, Compose, Media3; README describes series, seasons, episodes and Xtream APIs | Player architecture, episode/catalog models, source error handling, Media3 integration | Best technical reference. Do not transplant its IPTV source/account screens into ReelDrama without a deliberate product decision. |
| [Ivorisnoob/OpenStream](https://github.com/Ivorisnoob/OpenStream) | MIT; Kotlin, Compose, Media3 ExoPlayer, HLS/MP4, Room, downloads and subtitles | Player engine patterns, quality/subtitle/download architecture | Useful secondary reference; metadata/source extension model is not the same as ReelDrama's admin-curated backend. |
| [flixquest/flixquest](https://github.com/flixquest/flixquest) | Public repo; README says scraper backend is closed-source and streaming API resolves movie/TV episode streams | API contract examples for season/episode lookup | Do not depend on its closed scraper backend or copy scraping logic. |
| [recloudstream/cloudstream](https://github.com/recloudstream/cloudstream) | GPL-3.0; plugin/source-based streaming app | General plugin architecture reference only | Do not copy code into this project without a GPL compatibility decision; do not use scraping to bypass access controls. |

## Integration decisions
1. Keep ReelDrama's existing native Compose app, MongoDB models and Render backend. Do not replace the app with an unrelated APK.
2. Port/adapt the Media3 playback approach from MIT-licensed references rather than importing a prebuilt APK. Keep attribution and retain each upstream license notice if code is copied.
3. Preserve the existing Drama -> Episode data relationship and ensure episode lists are ordered by episodeNumber.
4. Add provider-specific episode-detail/playback integration only after the provider documents the endpoint and the account grants permission to use the stream. Catalog metadata is not a video URL.
5. Never treat a publicly reachable stream URL, free-to-watch content, or a GitHub APK as permission to redistribute or monetize the content.
6. Add an end-to-end smoke test using a known authorized sample HLS/MP4 source: catalog -> details -> episode list -> play -> completion -> next episode. Do not mark all provider episodes as playable until each returned stream is validated.

## Next implementation tasks
- [ ] Replace the legacy Android MediaPlayer implementation with AndroidX Media3 ExoPlayer (HLS + progressive MP4), preserving ReelDrama overlays and progress callbacks.
- [ ] Add backend provider episode-detail endpoint only after confirming APISHort's documented route/response for a single drama and episode stream.
- [ ] Add safe import mapping from provider metadata to internal Drama/Episode records, with duplicate protection and unpublished-by-default state.
- [ ] Add an admin-only playback validation/import flow; never publish a record without an authorized, validated source.
- [ ] Add tests for empty episode lists, missing URLs, non-200 provider responses, ordering, duplicate episodes and next-episode behavior.
- [ ] Verify actual playback on a physical Android phone; a successful GitHub Actions build alone is not playback proof.

## Important distinction
A repository can provide player code, database patterns, or an API client. It does not automatically provide a lawful catalog of dramas or working authorized episode streams. ReelDrama's all-episodes promise depends on the upstream provider exposing every episode and a valid stream for each one, plus rights to show that content in ReelDrama.
