# SpatialDJ project guidance

This project is the imagegen-v1 visual branch of the PICO OS 6 spatial DJ migration. The original `../frontend/` project and sibling `../pico-spatial-dj/` project are read-only references; do not overwrite or move them.

## Architecture

- The app runs in Share Space through one volumetric `DefaultWindowContainer`.
- `Main.kt` only wires the container, `PicoTheme`, and `DjConsoleScreen`.
- `ui/console/` owns immutable UI state, events, ViewModel wiring, and region components.
- `domain/model/` contains DJ domain state without Android or Compose dependencies.
- `domain/usecase/` contains track-targeting and crossfade rules.
- `data/repository/` provides demo tracks plus persistent Storage Access Framework URI metadata.
- `audio/` owns the 48 kHz low-latency `AudioTrack` mixer, demo synthesis, local `MediaCodec` decoding, EQ, pitch, cue, and scratch preview.
- `platform/` contains `SpatialApplication` and `LaunchActivity` only.

## Spatial rules

- All 2D UI must use SpatialUI (`com.pico.spatial.ui.*`) inside `PicoTheme`.
- Do not add Material or Material3 imports or dependencies.
- The default container has system glass disabled through `materialbackground=0`; the documented opaque root is intentional because the full-frame image represents a physical console body.
- `dj_console_topdown_base_v2.png` is the active decorative chassis layer with empty platter wells and blank display zones. `vinyl_platter_neutral.png` is a decorative live-rotation layer. Neither image may become a click target. Keep `dj_console_topdown_base.png` only as the v1 comparison/reference asset.
- `ConsoleCalibration.kt` is the single source of truth for image-relative overlay placement. Tune its named platter, channel-bay, transport, and mixer values when the chassis artwork changes; do not scatter coordinate offsets through composables.
- Built-in SpatialUI controls provide hover, audio, and haptic behavior. Custom clickable containers must use `Modifier.spatialHoverEffect`, `LocalIndication`, and shared controller haptics.
- Keep this app in Share Space. Do not add Stage-only anchors, environment mesh, or Full Space APIs without an explicit architecture decision.
- Local audio import must continue to use `OpenMultipleDocuments` with persisted URI grants. Do not add broad media-library permissions for this workflow.
- Audio UI events must update both immutable UI state and `DjAudioEngine`; a visual-only playback toggle is a regression.
- User-facing console labels and dynamic status messages route through `DjLocalization.kt`; keep Chinese/English switching state-only so it never resets audio or loaded tracks.

## Build and run

```bash
./gradlew testDebugUnitTest assembleDebug
./gradlew installDebug
adb shell am start -W -n com.haisnap.spatialdj/.platform.LaunchActivity
```

Run the project workflow and SpatialUI design verifiers before handoff. Android Studio must also run **Sync Project with Gradle Files** after dependency or module changes.

## Audio acceptance

1. Load a bundled demo on each deck and verify simultaneous playback.
2. Verify per-deck level, pitch, LOW/HIGH EQ, crossfader, pause, CUE, STOP, progress, and platter scratch preview.
3. Import at least one local MP3 or WAV through `IMPORT AUDIO`, load it, relaunch the app, and verify the persisted entry still decodes.
4. Evaluate whether the image-backed platters should graduate to optimized GLB/USD assets inside the existing volumetric window.
