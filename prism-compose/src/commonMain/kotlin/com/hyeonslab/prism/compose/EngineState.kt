package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.EngineConfig
import com.hyeonslab.prism.core.Time

@Stable
class EngineState internal constructor(val engine: Engine, internal val ownsEngine: Boolean) {
  var time: Time by mutableStateOf(Time())
    internal set

  var isInitialized: Boolean by mutableStateOf(false)
    internal set

  var fps: Float by mutableStateOf(0f)
    internal set

  internal constructor(config: EngineConfig) : this(Engine(config), ownsEngine = true)
}

/** Creates and remembers an [EngineState] that owns its [Engine] lifecycle. */
@Composable
fun rememberEngineState(config: EngineConfig = EngineConfig()): EngineState {
  val state = remember(config) { EngineState(config) }
  DisposableEffect(state) {
    state.engine.initialize()
    state.isInitialized = true
    onDispose {
      if (state.ownsEngine) {
        state.engine.shutdown()
      }
      state.isInitialized = false
    }
  }
  return state
}

/**
 * Creates and remembers an [EngineState] wrapping an externally-created [Engine]. The engine
 * lifecycle is managed by the caller — this state will not shut down the engine on dispose.
 */
@Composable
fun rememberExternalEngineState(engine: Engine): EngineState {
  val state = remember(engine) { EngineState(engine, ownsEngine = false) }
  DisposableEffect(state) {
    state.isInitialized = true
    onDispose { state.isInitialized = false }
  }
  return state
}
