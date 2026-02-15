package engine.prism.input

sealed class InputEvent {
  data class KeyDown(val key: Key) : InputEvent()

  data class KeyUp(val key: Key) : InputEvent()

  data class MouseMove(val x: Float, val y: Float, val deltaX: Float, val deltaY: Float) :
    InputEvent()

  data class MouseButtonDown(val button: MouseButton, val x: Float, val y: Float) : InputEvent()

  data class MouseButtonUp(val button: MouseButton, val x: Float, val y: Float) : InputEvent()

  data class MouseScroll(val deltaX: Float, val deltaY: Float) : InputEvent()
}
