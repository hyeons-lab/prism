package com.hyeonslab.prism.audio

import com.hyeonslab.prism.core.Subsystem

interface AudioEngine : Subsystem {
  fun loadSound(path: String): Sound

  fun playSound(sound: Sound)

  fun stopSound(sound: Sound)

  fun loadMusic(path: String): Music

  fun playMusic(music: Music)

  fun pauseMusic(music: Music)

  fun stopMusic(music: Music)

  fun setMasterVolume(volume: Float)
}
