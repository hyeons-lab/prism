package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.demo.DemoUiState
import com.hyeonslab.prism.renderer.Color
import kotlinx.coroutines.flow.StateFlow

/**
 * Bridge between the Prism engine and Flutter platform channels. Holds a [DemoScene] (rendering)
 * and [DemoStore] (UI state via MVI). The render loop is driven natively (Choreographer on Android,
 * MTKView delegate on iOS) — Flutter only sends control intents through the method channel.
 */
class PrismBridge {
  var scene: DemoScene? = null
    private set

  val store: DemoStore = DemoStore()

  val state: StateFlow<DemoUiState>
    get() = store.state

  fun attachScene(scene: DemoScene) {
    this.scene = scene
  }

  fun setRotationSpeed(degreesPerSecond: Float) {
    store.dispatch(DemoIntent.SetRotationSpeed(degreesPerSecond))
  }

  fun togglePause() {
    store.dispatch(DemoIntent.TogglePause)
  }

  fun setCubeColor(r: Float, g: Float, b: Float) {
    store.dispatch(DemoIntent.SetCubeColor(Color(r, g, b)))
  }

  fun updateAspectRatio(width: Int, height: Int) {
    scene?.updateAspectRatio(width, height)
  }

  fun shutdown() {
    scene?.shutdown()
    scene = null
  }

  fun isInitialized(): Boolean = scene != null
}
