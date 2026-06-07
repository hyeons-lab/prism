package com.hyeonslab.prism.audio

import android.content.Context
import android.media.AudioAttributes
import android.media.MediaPlayer
import android.media.SoundPool
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time

/**
 * Android [AudioEngine] backed by [SoundPool] for short sound effects and [MediaPlayer] for music.
 *
 * Sounds and music are loaded from the app's `assets/` by their relative [path]. Sounds load
 * asynchronously, so preload them (e.g. at scene setup) before calling [playSound]. Suitable for
 * games: low-latency, polyphonic SFX plus a single looping music track per path.
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

    /** Asset path -> SoundPool sample id. */
    private val soundIds: MutableMap<String, Int> = mutableMapOf()

    /** Asset path -> most recent active stream id (for [stopSound]). */
    private val activeStreams: MutableMap<String, Int> = mutableMapOf()

    /** Asset path -> MediaPlayer for music. */
    private val musicPlayers: MutableMap<String, MediaPlayer> = mutableMapOf()

    private var masterVolume: Float = 1f

    override fun loadSound(path: String): Sound {
        if (!soundIds.containsKey(path)) {
            try {
                appContext.assets.openFd(path).use { afd ->
                    soundIds[path] = soundPool.load(afd, 1)
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
        val volume = (sound.volume * masterVolume).coerceIn(0f, 1f)
        val loop = if (sound.loop) -1 else 0
        val streamId = soundPool.play(sampleId, volume, volume, 1, loop, sound.pitch)
        if (streamId != 0) activeStreams[sound.path] = streamId
    }

    override fun stopSound(sound: Sound) {
        activeStreams.remove(sound.path)?.let { soundPool.stop(it) }
    }

    override fun loadMusic(path: String): Music = Music(id = path, path = path)

    override fun playMusic(music: Music) {
        val player =
            musicPlayers.getOrPut(music.path) {
                MediaPlayer().also { mp ->
                    try {
                        appContext.assets.openFd(music.path).use { afd ->
                            mp.setDataSource(afd.fileDescriptor, afd.startOffset, afd.length)
                        }
                        mp.isLooping = music.loop
                        mp.prepare()
                    } catch (e: Exception) {
                        logger.e(e) { "Failed to load music: ${music.path}" }
                    }
                }
            }
        val volume = (music.volume * masterVolume).coerceIn(0f, 1f)
        player.setVolume(volume, volume)
        runCatching { player.start() }
            .onFailure { logger.e(it) { "Failed to start music: ${music.path}" } }
    }

    override fun pauseMusic(music: Music) {
        musicPlayers[music.path]?.let { if (it.isPlaying) it.pause() }
    }

    override fun stopMusic(music: Music) {
        musicPlayers.remove(music.path)?.let { mp ->
            runCatching { mp.stop() }
            mp.release()
        }
    }

    override fun setMasterVolume(volume: Float) {
        masterVolume = volume.coerceIn(0f, 1f)
        for (mp in musicPlayers.values) mp.setVolume(masterVolume, masterVolume)
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
        activeStreams.clear()
        for (mp in musicPlayers.values) {
            runCatching { mp.stop() }
            mp.release()
        }
        musicPlayers.clear()
        logger.i { "AndroidAudioEngine shut down" }
    }
}
