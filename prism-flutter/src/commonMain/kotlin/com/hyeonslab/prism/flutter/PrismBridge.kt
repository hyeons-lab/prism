package com.hyeonslab.prism.flutter

import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.EngineConfig

/**
 * Bridge between the Prism engine and Flutter platform channels. Provides a simplified API surface
 * that Flutter's Dart code can call through platform channels (MethodChannel) or FFI.
 */
@Suppress("UnusedParameter")
class PrismBridge {
  private var engine: Engine? = null

  fun initialize(appName: String, targetFps: Int = 60): Boolean {
    if (engine != null) return false
    val config = EngineConfig(appName = appName, targetFps = targetFps)
    engine = Engine(config).also { it.initialize() }
    return true
  }

  fun shutdown() {
    engine?.shutdown()
    engine = null
  }

  fun isInitialized(): Boolean = engine != null

  fun getEngine(): Engine? = engine

  fun resize(width: Int, height: Int) {
    // Forward to renderer subsystem when connected
  }

  fun setClearColor(r: Float, g: Float, b: Float, a: Float) {
    // Will be forwarded to renderer
  }

  fun frame(deltaTimeMs: Long) {
    // Tick one frame of the engine
  }
}
