package com.hyeonslab.prism.flutter

/** Thrown when a Flutter MethodChannel call references an unknown method name. */
class MethodNotImplementedException(method: String) : Exception("Unknown method: $method")

/**
 * Handles Flutter MethodChannel calls by dispatching to [PrismBridge]. Platform-specific
 * implementations connect this to the actual Flutter MethodChannel.
 *
 * The supported methods are: setRotationSpeed, togglePause, setCubeColor, isInitialized, getState,
 * shutdown. Unknown methods throw [MethodNotImplementedException] — the platform plugin should
 * catch this and call the appropriate "not implemented" response.
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
          "rotationSpeed" to state.rotationSpeed.toDouble(),
          "isPaused" to state.isPaused,
          "fps" to state.fps.toDouble(),
        )
      }
      "shutdown" -> {
        bridge.shutdown()
        true
      }
      else -> throw MethodNotImplementedException(method)
    }
  }
}
