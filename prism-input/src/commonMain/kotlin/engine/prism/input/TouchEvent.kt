package engine.prism.input

sealed class TouchEvent {
    data class TouchDown(
        val pointerId: Int,
        val x: Float,
        val y: Float,
    ) : TouchEvent()
    data class TouchMove(
        val pointerId: Int,
        val x: Float,
        val y: Float,
    ) : TouchEvent()
    data class TouchUp(
        val pointerId: Int,
        val x: Float,
        val y: Float,
    ) : TouchEvent()
    data class TouchCancel(
        val pointerId: Int,
    ) : TouchEvent()
}
