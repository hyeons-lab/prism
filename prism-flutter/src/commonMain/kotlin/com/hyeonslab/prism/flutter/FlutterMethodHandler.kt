package com.hyeonslab.prism.flutter

/**
 * Handles Flutter MethodChannel calls by dispatching to [PrismBridge]. Platform-specific
 * implementations connect this to the actual Flutter MethodChannel.
 */
class FlutterMethodHandler(private val bridge: PrismBridge) {

  fun handleMethodCall(method: String, args: Map<String, Any?>): Any? {
    return when (method) {
      "setRotationSpeed" -> {
        val speed = (args["speed"] as? Number)?.toFloat() ?: 45f
        bridge.setRotationSpeed(speed)
        true
      }
      "togglePause" -> {
        bridge.togglePause()
        true
      }
      "setCubeColor" -> {
        val r = (args["r"] as? Number)?.toFloat() ?: 0.3f
        val g = (args["g"] as? Number)?.toFloat() ?: 0.5f
        val b = (args["b"] as? Number)?.toFloat() ?: 0.9f
        bridge.setCubeColor(r, g, b)
        true
      }
      "isInitialized" -> bridge.isInitialized()
      "getState" -> {
        val state = bridge.state.value
        mapOf(
          "rotationSpeed" to state.rotationSpeed,
          "isPaused" to state.isPaused,
          "fps" to state.fps,
        )
      }
      "shutdown" -> {
        bridge.shutdown()
        true
      }
      else -> error("Unknown method: $method")
    }
  }
}
