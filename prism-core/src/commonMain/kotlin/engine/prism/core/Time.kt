package engine.prism.core

data class Time(
    val deltaTime: Float = 0f,
    val totalTime: Float = 0f,
    val frameCount: Long = 0,
    val fixedDeltaTime: Float = 1f / 60f,
)
