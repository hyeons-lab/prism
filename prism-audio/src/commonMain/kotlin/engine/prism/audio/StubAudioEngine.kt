package engine.prism.audio

import co.touchlab.kermit.Logger
import engine.prism.core.Engine
import engine.prism.core.Time

class StubAudioEngine : AudioEngine {
  override val name: String = "Audio"
  private val logger = Logger.withTag("PrismAudio")

  override fun loadSound(path: String): Sound {
    logger.d { "Stub: loadSound($path)" }
    return Sound(id = path, path = path)
  }

  override fun playSound(sound: Sound) {
    logger.d { "Stub: playSound(${sound.id})" }
  }

  override fun stopSound(sound: Sound) {
    logger.d { "Stub: stopSound(${sound.id})" }
  }

  override fun loadMusic(path: String): Music {
    logger.d { "Stub: loadMusic($path)" }
    return Music(id = path, path = path)
  }

  override fun playMusic(music: Music) {
    logger.d { "Stub: playMusic(${music.id})" }
  }

  override fun pauseMusic(music: Music) {
    logger.d { "Stub: pauseMusic(${music.id})" }
  }

  override fun stopMusic(music: Music) {
    logger.d { "Stub: stopMusic(${music.id})" }
  }

  override fun setMasterVolume(volume: Float) {
    logger.d { "Stub: setMasterVolume($volume)" }
  }

  override fun initialize(engine: Engine) {
    logger.i { "StubAudioEngine initialized (no-op)" }
  }

  override fun update(time: Time) {
    // No-op
  }

  override fun shutdown() {
    logger.i { "StubAudioEngine shut down" }
  }
}
