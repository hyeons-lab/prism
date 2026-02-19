package com.hyeonslab.prism.flutter

/** Thrown when a Flutter MethodChannel call references an unknown method name. */
class MethodNotImplementedException(method: String) : Exception("Unknown method: $method")

/**
 * Handles Flutter MethodChannel calls by dispatching to [PrismBridge]. Platform-specific
 * implementations connect this to the actual Flutter MethodChannel.
 *
 * Supported methods: setRotationSpeed, togglePause, setMetallic, setRoughness, setEnvIntensity,
 * isInitialized, getState, shutdown. Unknown methods throw [MethodNotImplementedException].
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
      "setMetallic" -> {
        val metallic = (args["metallic"] as? Number)?.toFloat() ?: 0f
        bridge.setMetallic(metallic)
        true
      }
      "setRoughness" -> {
        val roughness = (args["roughness"] as? Number)?.toFloat() ?: 0.5f
        bridge.setRoughness(roughness)
        true
      }
      "setEnvIntensity" -> {
        val intensity = (args["intensity"] as? Number)?.toFloat() ?: 1f
        bridge.setEnvIntensity(intensity)
        true
      }
      "isInitialized" -> bridge.isInitialized()
      "getState" -> {
        val state = bridge.state.value
        mapOf(
          "rotationSpeed" to state.rotationSpeed.toDouble(),
          "isPaused" to state.isPaused,
          "metallic" to state.metallic.toDouble(),
          "roughness" to state.roughness.toDouble(),
          "envIntensity" to state.envIntensity.toDouble(),
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
