package com.hyeonslab.prism.flutter

/**
 * Handles Flutter MethodChannel calls by dispatching to PrismBridge. Platform-specific
 * implementations will connect this to the actual Flutter MethodChannel or FFI.
 */
class FlutterMethodHandler(private val bridge: PrismBridge) {

  fun handleMethodCall(method: String, args: Map<String, Any?>): Any? {
    return when (method) {
      "initialize" -> {
        val appName = args["appName"] as? String ?: "Prism App"
        val targetFps = (args["targetFps"] as? Number)?.toInt() ?: 60
        bridge.initialize(appName, targetFps)
      }
      "shutdown" -> {
        bridge.shutdown()
        true
      }
      "isInitialized" -> bridge.isInitialized()
      "resize" -> {
        val width = (args["width"] as? Number)?.toInt() ?: 0
        val height = (args["height"] as? Number)?.toInt() ?: 0
        bridge.resize(width, height)
        true
      }
      "setClearColor" -> {
        val r = (args["r"] as? Number)?.toFloat() ?: 0f
        val g = (args["g"] as? Number)?.toFloat() ?: 0f
        val b = (args["b"] as? Number)?.toFloat() ?: 0f
        val a = (args["a"] as? Number)?.toFloat() ?: 1f
        bridge.setClearColor(r, g, b, a)
        true
      }
      "frame" -> {
        val deltaTimeMs = (args["deltaTimeMs"] as? Number)?.toLong() ?: 16L
        bridge.frame(deltaTimeMs)
        true
      }
      else -> error("Unknown method: $method")
    }
  }
}
