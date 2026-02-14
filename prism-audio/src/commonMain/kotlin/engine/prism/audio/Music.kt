package engine.prism.audio

data class Music(
    val id: String,
    val path: String,
    val volume: Float = 1f,
    val loop: Boolean = true,
)
