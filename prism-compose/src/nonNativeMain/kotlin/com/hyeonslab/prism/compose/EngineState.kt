package com.hyeonslab.prism.compose

import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.Stable
import androidx.compose.runtime.remember
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.EngineConfig
import com.hyeonslab.prism.core.Store
import com.hyeonslab.prism.core.Time
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update

/** Events dispatched by Prism composables to update [EngineState]. */
sealed interface EngineStateEvent {
  /** Engine lifecycle initialized. */
  data object Initialized : EngineStateEvent

  /** Engine lifecycle disposed. */
  data object Disposed : EngineStateEvent

  /** Rendering surface resized. */
  data class SurfaceResized(val width: Int, val height: Int) : EngineStateEvent

  /** A frame was rendered with updated timing and FPS. */
  data class FrameTick(val time: Time, val fps: Float) : EngineStateEvent
}

/** Immutable state snapshot for a Prism [Engine]. */
data class EngineState(
  val time: Time = Time(),
  val isInitialized: Boolean = false,
  val fps: Float = 0f,
  val surfaceWidth: Int = 0,
  val surfaceHeight: Int = 0,
)

/**
 * MVI store for a Prism [Engine]. Holds the current [EngineState] in a [StateFlow] and processes
 * [EngineStateEvent]s through a pure reducer. Composables dispatch events via [dispatch] — they
 * never mutate state directly.
 */
@Stable
class EngineStore internal constructor(val engine: Engine, internal val ownsEngine: Boolean) :
  Store<EngineState, EngineStateEvent> {
  private val _state = MutableStateFlow(EngineState())
  override val state: StateFlow<EngineState> = _state.asStateFlow()

  override fun dispatch(event: EngineStateEvent) {
    _state.update { reduce(it, event) }
  }

  private fun reduce(state: EngineState, event: EngineStateEvent): EngineState =
    when (event) {
      is EngineStateEvent.Initialized -> state.copy(isInitialized = true)
      is EngineStateEvent.Disposed -> state.copy(isInitialized = false)
      is EngineStateEvent.SurfaceResized ->
        state.copy(surfaceWidth = event.width, surfaceHeight = event.height)
      is EngineStateEvent.FrameTick -> state.copy(time = event.time, fps = event.fps)
    }

  internal constructor(config: EngineConfig) : this(Engine(config), ownsEngine = true)
}

/** Creates and remembers an [EngineStore] that owns its [Engine] lifecycle. */
@Composable
fun rememberEngineStore(config: EngineConfig = EngineConfig()): EngineStore {
  val store = remember(config) { EngineStore(config) }
  DisposableEffect(store) {
    store.engine.initialize()
    store.dispatch(EngineStateEvent.Initialized)
    onDispose {
      if (store.ownsEngine) {
        store.engine.shutdown()
      }
      store.dispatch(EngineStateEvent.Disposed)
    }
  }
  return store
}

/**
 * Creates and remembers an [EngineStore] wrapping an externally-created [Engine]. The engine
 * lifecycle is managed by the caller — this store will not shut down the engine on dispose.
 */
@Composable
fun rememberExternalEngineStore(engine: Engine): EngineStore {
  val store = remember(engine) { EngineStore(engine, ownsEngine = false) }
  DisposableEffect(store) {
    store.dispatch(EngineStateEvent.Initialized)
    onDispose { store.dispatch(EngineStateEvent.Disposed) }
  }
  return store
}
