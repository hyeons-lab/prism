package engine.prism.audio

data class Sound(
    val id: String,
    val path: String,
    val volume: Float = 1f,
    val pitch: Float = 1f,
    val loop: Boolean = false,
)
