package com.hyeonslab.prism.flutter.demo

import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.flutter.PrismBridge

/**
 * Concrete [PrismBridge] for the PBR demo. Creates and owns a [DemoStore] so callers (e.g. the
 * Android plugin) never need to import [DemoStore] or [DemoIntent]. Exposes [isPaused] and
 * [dispatchFps] for the platform render loop.
 */
class DemoBridge : PrismBridge<DemoScene, DemoStore>(DemoStore()) {
  /** True when the demo is paused — check this in the platform render loop. */
  val isPaused: Boolean
    get() = store.state.value.isPaused

  override fun shutdown() {
    scene?.shutdown()
    super.shutdown()
  }
}
