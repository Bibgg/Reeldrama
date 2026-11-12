# K Player Android

A native Android video player project. This repository is being rebuilt from the former ReelDrama app into **K Player**.

## Product direction

Build an original, polished player experience by studying common interaction patterns in established products. We do not copy their branding, proprietary assets, or source code.

- VLC: media-library architecture, playback controls, media and subtitle handling, and filter concepts.
- Telegram: lightweight media-editing concepts such as crop, rotate, and visual adjustments. These are planned as our own editor tools, not copied implementations.
- YouTube: seeking, playback speed, quality-selection concepts, and accessible controls.
- MX Player: local media workflows, subtitle/audio track controls, and gesture ergonomics.
- Netflix / JioHotstar: episode-oriented viewing, resume progress, and OTT-style presentation.

## Current first milestone

- Native Kotlin + Jetpack Compose Android app.
- Pick a local video using Android's Storage Access Framework (no broad storage permission).
- Play local files with AndroidX Media3 / ExoPlayer.
- Custom controls: play/pause, seek, double-tap ±10 seconds, playback speed, and fit/zoom display mode.
- GitHub Actions debug APK build.

## Planned milestones

1. **Core player:** validate local playback, lifecycle handling, seeking, speed, orientation, and device compatibility.
2. **Advanced controls:** audio/subtitle track selection, brightness/volume gestures, lock controls, resume position, and playlist.
3. **Original video effects/editor:** research Media3 Transformer and frame-processing APIs; prototype crop, rotate, color adjustments, and presets. Do not present a filter as functional until it processes the actual video output.
4. **OTT experience:** episode lists, continue watching, and quality selection only when the source actually provides multiple renditions.
5. **Online playback:** only after the local player is stable, design the Video API, database, access control, and streaming delivery.

## Build

GitHub Actions builds the debug APK on pushes to `main` and on manual workflow runs. Download it from the workflow run's **Artifacts** section.

The workflow installs Gradle 8.13 and uses JDK 17. No API keys or backend credentials are required for this local-player milestone.

## Architecture

- UI: Jetpack Compose
- Playback: AndroidX Media3 ExoPlayer
- Local file access: Android Storage Access Framework
- Build: Gradle + GitHub Actions

Media3 is the playback foundation for this first milestone. It is not VLC, and this project does not bundle the VLC engine.

## Open-source learning references

- [Official VLC for Android source](https://github.com/videolan/vlc-android)
- [Android Media3 ExoPlayer guide](https://developer.android.com/media/media3/exoplayer/hello-world)
- [AndroidX Media3 project](https://github.com/androidx/media)

VLC for Android is GPL-licensed and its LibVLC engine has a separate LGPL license. This project studies publicly documented architecture and behavior; it does not copy VLC source code. Review licenses carefully before ever incorporating third-party code.

## Important

This is an initial development milestone, not a claim that the player is feature-complete or better than established services. Device testing and successful CI builds are required before calling a feature verified.
