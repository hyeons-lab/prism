package com.hyeonslab.prism.demo

import com.hyeonslab.prism.renderer.Color
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Immutable UI state for the Compose demo scene. */
data class DemoUiState(
  val rotationSpeed: Float = 45f,
  val isPaused: Boolean = false,
  val cubeColor: Color = Color(0.3f, 0.5f, 0.9f),
  val fps: Float = 0f,
)

/** User actions (intents) that drive state changes in the demo. */
sealed interface DemoIntent {
  data class SetRotationSpeed(val speed: Float) : DemoIntent

  data object TogglePause : DemoIntent

  data class SetCubeColor(val color: Color) : DemoIntent

  data class UpdateFps(val fps: Float) : DemoIntent
}

/**
 * MVI store for the demo scene. Holds the current [DemoUiState] and processes [DemoIntent]s through
 * a pure reduce function.
 */
class DemoStore {
  private val _state = MutableStateFlow(DemoUiState())
  val state: StateFlow<DemoUiState> = _state.asStateFlow()

  fun dispatch(intent: DemoIntent) {
    _state.update { reduce(it, intent) }
  }

  private fun reduce(state: DemoUiState, intent: DemoIntent): DemoUiState =
    when (intent) {
      is DemoIntent.SetRotationSpeed -> state.copy(rotationSpeed = intent.speed)
      is DemoIntent.TogglePause -> state.copy(isPaused = !state.isPaused)
      is DemoIntent.SetCubeColor -> state.copy(cubeColor = intent.color)
      is DemoIntent.UpdateFps -> state.copy(fps = intent.fps)
    }
}
