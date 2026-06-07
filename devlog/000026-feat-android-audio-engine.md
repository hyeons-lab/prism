# 000026 — feat/android-audio-engine

**Agent:** Claude (claude-opus-4-8) @ prism branch feat/android-audio-engine

## Intent

`prism-audio` defines the `AudioEngine` interface but only ships `StubAudioEngine` (no-op). Provide a
real Android implementation so apps get sound effects and music. Motivated by a downstream tablet
game (voice-driven kids' shooter) that needs pop/hit/chime SFX.

## What Changed

- 2026-06-07T14:11-07:00 prism-audio/src/androidMain/.../AndroidAudioEngine.kt — new Android
  `AudioEngine` actual: `SoundPool` (USAGE_GAME) for low-latency polyphonic SFX, `MediaPlayer` for
  music. Loads from app `assets/` by relative path; tracks sample ids and active stream ids (so
  `stopSound` works); per-path MediaPlayer map for play/pause/stop; master volume applied to both.
  This adds the module's first `androidMain` source set.

## Decisions

- 2026-06-07T14:11-07:00 SoundPool for SFX, MediaPlayer for music — the standard Android split:
  SoundPool is built for short, frequent, overlapping clips with minimal latency; MediaPlayer suits
  longer streamed music. Sounds load asynchronously, so callers preload (documented on the class).
- 2026-06-07T14:11-07:00 Constructor takes a `Context` (retains `applicationContext`) — required to
  read assets and build the players. `StubAudioEngine` remains the default for other platforms; no
  behavior change to existing demos.
- 2026-06-07T14:11-07:00 Devlog numbered 000026 (not 000025) to avoid colliding with the in-flight
  fix/android-surface-alpha-mode branch (PR #54) which already uses 000025. Renumber on merge if needed.

## Issues

- None. `:prism-audio:compileAndroidMain` is warning-clean (module sets allWarningsAsErrors); full
  gate `ktfmtCheck detektJvmMain jvmTest` passes.

## Commits

- HEAD — feat(audio): add Android AudioEngine (SoundPool SFX + MediaPlayer music)
