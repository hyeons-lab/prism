package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.demo.DemoUiState
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

  fun setMetallic(metallic: Float) {
    store.dispatch(DemoIntent.SetMetallic(metallic))
  }

  fun setRoughness(roughness: Float) {
    store.dispatch(DemoIntent.SetRoughness(roughness))
  }

  fun setEnvIntensity(intensity: Float) {
    store.dispatch(DemoIntent.SetEnvIntensity(intensity))
  }

  fun updateAspectRatio(width: Int, height: Int) {
    scene?.updateAspectRatio(width, height)
  }

  /** Detach the scene reference without shutting it down (caller owns the shutdown). */
  fun detachScene() {
    scene = null
  }

  fun shutdown() {
    scene?.shutdown()
    scene = null
  }

  fun isInitialized(): Boolean = scene != null
}
