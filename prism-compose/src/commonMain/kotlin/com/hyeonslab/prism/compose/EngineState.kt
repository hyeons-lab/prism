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
class EngineState internal constructor(config: EngineConfig) {
  val engine: Engine = Engine(config)
  var time: Time by mutableStateOf(Time())
    internal set

  var isInitialized: Boolean by mutableStateOf(false)
    internal set

  var fps: Float by mutableStateOf(0f)
    internal set
}

@Composable
fun rememberEngineState(config: EngineConfig = EngineConfig()): EngineState {
  val state = remember(config) { EngineState(config) }
  DisposableEffect(state) {
    state.engine.initialize()
    state.isInitialized = true
    onDispose {
      state.engine.shutdown()
      state.isInitialized = false
    }
  }
  return state
}
