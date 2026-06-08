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
- 2026-06-07T20:31-07:00 (PR review) Gate SFX playback on load completion. SoundPool.load is async,
  so `playSound` now skips (with a warning) until the sample id appears in a `readySamples` set
  populated by `setOnLoadCompleteListener`. `SoundPool.load`/`play` returning 0 are treated as
  failures (not stored / logged) rather than silently succeeding.
- 2026-06-07T20:31-07:00 (PR review) Never cache a half-initialized MediaPlayer: `playMusic` builds
  the player in `prepareMusicPlayer`, which only inserts into `musicPlayers` after `prepare()`
  succeeds and releases on failure. `isLooping` is set every play so a later call with a different
  `Music.loop` takes effect.
- 2026-06-07T20:31-07:00 (PR review) Preserve per-track music volume across master changes: store the
  last requested `Music.volume` per path in `musicVolumes` and apply `track * master` in both
  `playMusic` and `setMasterVolume` (previously master overwrote every player, losing per-track
  levels). `musicVolumes` is cleared in `stopMusic` and `shutdown` so it doesn't leak stale state.

## Issues

- None. `:prism-audio:compileAndroidMain` is warning-clean (module sets allWarningsAsErrors); full
  gate `ktfmtCheck detektJvmMain jvmTest` passes.

## Commits

- bfa9c45 — feat(audio): add Android AudioEngine (SoundPool SFX + MediaPlayer music)
- HEAD — fix(audio): gate SFX on load, harden MediaPlayer + per-track volume (PR review)
