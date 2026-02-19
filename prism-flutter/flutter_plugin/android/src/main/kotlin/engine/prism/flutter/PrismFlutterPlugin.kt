package engine.prism.flutter

import com.hyeonslab.prism.flutter.FlutterMethodHandler
import com.hyeonslab.prism.flutter.MethodNotImplementedException
import com.hyeonslab.prism.flutter.PrismBridge
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Flutter plugin entry point for Android. Registers the method channel, platform view factory,
 * and handles activity lifecycle (pause/resume) to stop/start the render loop.
 */
class PrismFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    private lateinit var channel: MethodChannel
    private val bridge = PrismBridge()
    private val handler = FlutterMethodHandler(bridge)
    private var platformView: PrismPlatformView? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel = MethodChannel(binding.binaryMessenger, "engine.prism.flutter/engine")
        channel.setMethodCallHandler(this)

        binding.platformViewRegistry.registerViewFactory(
            "engine.prism.flutter/render_view",
            PrismPlatformViewFactory(bridge) { platformView = it }
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
        } catch (@Suppress("SwallowedException") e: MethodNotImplementedException) {
            result.notImplemented()
        } catch (e: Exception) {
            result.error("PRISM_ERROR", e.message, null)
        }
    }

    // -- ActivityAware: pause/resume the render loop with the host activity --

    override fun onAttachedToActivity(binding: ActivityPluginBinding) {
        platformView?.resumeRendering()
    }

    override fun onReattachedToActivityForConfigChanges(binding: ActivityPluginBinding) {
        platformView?.resumeRendering()
    }

    override fun onDetachedFromActivity() {
        platformView?.pauseRendering()
    }

    override fun onDetachedFromActivityForConfigChanges() {
        platformView?.pauseRendering()
    }
}
