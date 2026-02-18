package engine.prism.flutter

import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Flutter plugin entry point for Android. Registers the method channel and platform view factory.
 */
class PrismFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler {

    private lateinit var channel: MethodChannel
    private val bridge = com.hyeonslab.prism.flutter.PrismBridge()
    private val handler = com.hyeonslab.prism.flutter.FlutterMethodHandler(bridge)

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "engine.prism.flutter/engine")
        channel.setMethodCallHandler(this)

        binding.platformViewRegistry.registerViewFactory(
            "engine.prism.flutter/render_view",
            PrismPlatformViewFactory(bridge)
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        bridge.shutdown()
    }

    override fun onMethodCall(call: MethodCall, result: MethodChannel.Result) {
        try {
            @Suppress("UNCHECKED_CAST")
            val args = (call.arguments as? Map<String, Any?>) ?: emptyMap()
            val response = handler.handleMethodCall(call.method, args)
            result.success(response)
        } catch (e: IllegalStateException) {
            result.notImplemented()
        } catch (e: Exception) {
            result.error("PRISM_ERROR", e.message, null)
        }
    }
}
