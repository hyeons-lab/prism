package engine.prism.audio

import engine.prism.core.Engine
import engine.prism.core.Subsystem
import engine.prism.core.Time

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
