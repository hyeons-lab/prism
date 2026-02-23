package com.hyeonslab.prism.demo

import androidx.compose.runtime.Stable
import com.hyeonslab.prism.core.Store
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Immutable UI state for the Compose demo scene. */
data class DemoUiState(
  val rotationSpeed: Float = 45f,
  val isPaused: Boolean = false,
  val metallic: Float = 0.5f,
  val roughness: Float = 0.5f,
  val envIntensity: Float = 1.0f,
  val fps: Float = 0f,
)

/** User actions (intents) that drive state changes in the demo. */
sealed interface DemoIntent {
  data class SetRotationSpeed(val speed: Float) : DemoIntent

  data object TogglePause : DemoIntent

  data class SetMetallic(val metallic: Float) : DemoIntent

  data class SetRoughness(val roughness: Float) : DemoIntent

  data class SetEnvIntensity(val intensity: Float) : DemoIntent

  data class UpdateFps(val fps: Float) : DemoIntent
}

/**
 * MVI store for the demo scene. Holds the current [DemoUiState] and processes [DemoIntent]s through
 * a pure reduce function.
 */
@Stable
class DemoStore : Store<DemoUiState, DemoIntent> {
  private val _state = MutableStateFlow(DemoUiState())
  override val state: StateFlow<DemoUiState> = _state.asStateFlow()

  override fun dispatch(event: DemoIntent) {
    _state.update { reduce(it, event) }
  }

  private fun reduce(state: DemoUiState, event: DemoIntent): DemoUiState =
    when (event) {
      is DemoIntent.SetRotationSpeed -> state.copy(rotationSpeed = event.speed)
      is DemoIntent.TogglePause -> state.copy(isPaused = !state.isPaused)
      is DemoIntent.SetMetallic -> state.copy(metallic = event.metallic)
      is DemoIntent.SetRoughness -> state.copy(roughness = event.roughness)
      is DemoIntent.SetEnvIntensity -> state.copy(envIntensity = event.intensity)
      is DemoIntent.UpdateFps -> state.copy(fps = event.fps)
    }
}
