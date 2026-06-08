package com.hyeonslab.prism.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import java.util.concurrent.ConcurrentHashMap

/**
 * Android [AudioEngine] backed by [SoundPool] for short sound effects and [MediaPlayer] for music.
 *
 * Sounds and music are loaded from the app's `assets/` by their relative [path]. SoundPool loads
 * asynchronously, so preload sounds (e.g. at scene setup) before calling [playSound]: playback is
 * gated on the sample finishing load and is dropped (with a warning) until then. Suitable for games:
 * low-latency, polyphonic SFX plus a single looping music track per path.
 *
 * @param context any [Context] (the application context is retained).
 * @param maxStreams maximum number of simultaneous sound effects.
 */
class AndroidAudioEngine(context: Context, maxStreams: Int = 8) : AudioEngine {

  override val name: String = "Audio"

  private val appContext: Context = context.applicationContext
  private val logger = Logger.withTag("PrismAudio")

  private val soundPool: SoundPool =
    SoundPool.Builder()
      .setMaxStreams(maxStreams)
      .setAudioAttributes(
        AudioAttributes.Builder()
          .setUsage(AudioAttributes.USAGE_GAME)
          .setContentType(AudioAttributes.CONTENT_TYPE_SONIFICATION)
          .build()
      )
      .build()

  /** Asset path -> SoundPool sample id (only populated for successful [SoundPool.load] calls). */
  private val soundIds: MutableMap<String, Int> = mutableMapOf()

  /**
   * Sample ids that have finished loading successfully. Written from the SoundPool load-complete
   * callback (which may run on a different thread) and read from [playSound], so it is concurrent.
   */
  private val readySamples: MutableSet<Int> = ConcurrentHashMap.newKeySet()

  /** Asset path -> most recent active stream id (for [stopSound]). */
  private val activeStreams: MutableMap<String, Int> = mutableMapOf()

  /** Asset path -> MediaPlayer for music. Only holds successfully-prepared players. */
  private val musicPlayers: MutableMap<String, MediaPlayer> = mutableMapOf()

  /** Asset path -> last requested per-track music volume, so master changes can re-multiply it. */
  private val musicVolumes: MutableMap<String, Float> = mutableMapOf()

  private var masterVolume: Float = 1f

  init {
    soundPool.setOnLoadCompleteListener { _, sampleId, status ->
      if (status == 0) {
        readySamples.add(sampleId)
      } else {
        logger.e { "SoundPool failed to load sampleId=$sampleId (status=$status)" }
      }
    }
  }

  override fun loadSound(path: String): Sound {
    if (!soundIds.containsKey(path)) {
      try {
        appContext.assets.openFd(path).use { afd ->
          // load() returns 0 on failure; only record real sample ids so playSound can trust the map.
          val sampleId = soundPool.load(afd, 1)
          if (sampleId != 0) {
            soundIds[path] = sampleId
          } else {
            logger.e { "SoundPool.load returned 0 (failed) for: $path" }
          }
        }
      } catch (e: Exception) {
        logger.e(e) { "Failed to load sound: $path" }
      }
    }
    return Sound(id = path, path = path)
  }

  override fun playSound(sound: Sound) {
    val sampleId = soundIds[sound.path]
    if (sampleId == null) {
      logger.w { "playSound: sound not loaded: ${sound.path}" }
      return
    }
    if (sampleId !in readySamples) {
      logger.w { "playSound: sound still loading, skipping: ${sound.path}" }
      return
    }
    val volume = (sound.volume * masterVolume).coerceIn(0f, 1f)
    val loop = if (sound.loop) -1 else 0
    val streamId = soundPool.play(sampleId, volume, volume, 1, loop, sound.pitch)
    if (streamId != 0) {
      activeStreams[sound.path] = streamId
    } else {
      logger.w { "playSound: SoundPool.play returned 0 (not started): ${sound.path}" }
    }
  }

  override fun stopSound(sound: Sound) {
    activeStreams.remove(sound.path)?.let { soundPool.stop(it) }
  }

  override fun loadMusic(path: String): Music = Music(id = path, path = path)

  override fun playMusic(music: Music) {
    // Reuse an already-prepared player; otherwise build one and only cache it on success so a
    // failed load never leaves a half-initialized MediaPlayer in the map.
    val player = musicPlayers[music.path] ?: prepareMusicPlayer(music.path) ?: return
    // Apply looping every play so a later call with a different Music.loop takes effect.
    player.isLooping = music.loop
    musicVolumes[music.path] = music.volume
    val volume = (music.volume * masterVolume).coerceIn(0f, 1f)
    player.setVolume(volume, volume)
    runCatching { player.start() }
      .onFailure { logger.e(it) { "Failed to start music: ${music.path}" } }
  }

  /** Builds and prepares a [MediaPlayer], caching it on success. Returns null (and releases) on failure. */
  private fun prepareMusicPlayer(path: String): MediaPlayer? {
    val mp = MediaPlayer()
    return try {
      appContext.assets.openFd(path).use { afd ->
        mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
      }
      mp.prepare()
      musicPlayers[path] = mp
      mp
    } catch (e: Exception) {
      logger.e(e) { "Failed to load music: $path" }
      mp.release()
      null
    }
  }

  override fun pauseMusic(music: Music) {
    musicPlayers[music.path]?.let { if (it.isPlaying) it.pause() }
  }

  override fun stopMusic(music: Music) {
    musicVolumes.remove(music.path)
    musicPlayers.remove(music.path)?.let { mp ->
      runCatching { mp.stop() }
      mp.release()
    }
  }

  override fun setMasterVolume(volume: Float) {
    masterVolume = volume.coerceIn(0f, 1f)
    // Re-apply as a multiplier on each track's last requested volume, preserving per-track levels.
    for ((path, mp) in musicPlayers) {
      val trackVolume = ((musicVolumes[path] ?: 1f) * masterVolume).coerceIn(0f, 1f)
      mp.setVolume(trackVolume, trackVolume)
    }
  }

  override fun initialize(engine: Engine) {
    logger.i { "AndroidAudioEngine initialized" }
  }

  override fun update(time: Time) {
    // No per-frame work needed.
  }

  override fun shutdown() {
    soundPool.release()
    soundIds.clear()
    readySamples.clear()
    activeStreams.clear()
    for (mp in musicPlayers.values) {
      runCatching { mp.stop() }
      mp.release()
    }
    musicPlayers.clear()
    musicVolumes.clear()
    logger.i { "AndroidAudioEngine shut down" }
  }
}
