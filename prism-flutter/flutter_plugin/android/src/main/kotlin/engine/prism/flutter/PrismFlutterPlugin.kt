package engine.prism.flutter

import android.content.Context
import com.hyeonslab.prism.flutter.AbstractFlutterMethodHandler
import com.hyeonslab.prism.flutter.MethodNotImplementedException
import com.hyeonslab.prism.flutter.PrismAndroidBridge
import io.flutter.embedding.engine.plugins.FlutterPlugin
import io.flutter.embedding.engine.plugins.activity.ActivityAware
import io.flutter.embedding.engine.plugins.activity.ActivityPluginBinding
import io.flutter.plugin.common.MethodCall
import io.flutter.plugin.common.MethodChannel

/**
 * Bundles the bridge and its method handler together so the host app creates both in
 * the same scope — eliminating the unchecked cast that would otherwise be required
 * when [AbstractFlutterMethodHandler] expects a typed bridge.
 */
data class PrismBridgeBundle(
    val bridge: PrismAndroidBridge<*, *>,
    val handler: AbstractFlutterMethodHandler,
)

/**
 * Flutter plugin entry point for Android. Registers the method channel, platform view factory,
 * and handles activity lifecycle (pause/resume) to stop/start the render loop.
 *
 * **Configuration required:** call [configure] before the Flutter engine attaches this plugin.
 * Demo apps provide `DemoAndroidBridge` and `FlutterMethodHandler` from `prism-flutter-demo`;
 * the plugin itself has no dependency on any demo module.
 *
 * Example (in Application.onCreate or MainActivity):
 * ```kotlin
 * PrismFlutterPlugin.configure { context ->
 *     val bridge = DemoAndroidBridge(glbLoader = {
 *         try { context.assets.open("flutter_assets/assets/DamagedHelmet.glb").use { it.readBytes() } }
 *         catch (e: Exception) { null }
 *     })
 *     PrismBridgeBundle(bridge, FlutterMethodHandler(bridge))
 * }
 * ```
 */
class PrismFlutterPlugin : FlutterPlugin, MethodChannel.MethodCallHandler, ActivityAware {

    companion object {
        private var bundleFactory: ((Context) -> PrismBridgeBundle)? = null

        /**
         * Configure the plugin with a factory that creates the bridge and handler bundle.
         * The factory receives the application [Context] so it can load assets.
         * Must be called before the Flutter engine starts.
         */
        fun configure(factory: (Context) -> PrismBridgeBundle) {
            bundleFactory = factory
        }
    }

    private lateinit var channel: MethodChannel
    private lateinit var bridge: PrismAndroidBridge<*, *>
    private lateinit var handler: AbstractFlutterMethodHandler
    private var platformView: PrismPlatformView? = null

    override fun onAttachedToEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        val bundle = checkNotNull(bundleFactory) {
            "PrismFlutterPlugin not configured — call PrismFlutterPlugin.configure() before engine attachment"
        }.invoke(binding.applicationContext)
        bridge = bundle.bridge
        handler = bundle.handler

        channel = MethodChannel(binding.binaryMessenger, "engine.prism.flutter/engine")
        channel.setMethodCallHandler(this)

        binding.platformViewRegistry.registerViewFactory(
            "engine.prism.flutter/render_view",
            PrismPlatformViewFactory(bridge) { platformView = it }
        )
    }

    override fun onDetachedFromEngine(binding: FlutterPlugin.FlutterPluginBinding) {
        channel.setMethodCallHandler(null)
        if (::bridge.isInitialized) bridge.shutdown()
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
