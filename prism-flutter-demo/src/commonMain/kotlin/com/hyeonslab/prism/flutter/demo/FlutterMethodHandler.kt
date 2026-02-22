package com.hyeonslab.prism.flutter.demo

import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.flutter.AbstractFlutterMethodHandler
import com.hyeonslab.prism.flutter.MethodNotImplementedException
import com.hyeonslab.prism.flutter.PrismBridge

/**
 * Flutter MethodChannel dispatcher for the demo. Extends [AbstractFlutterMethodHandler]
 * so the Android plugin can hold it by the abstract type without importing demo classes.
 */
class FlutterMethodHandler(private val demoBridge: PrismBridge<DemoScene, DemoStore>)
    : AbstractFlutterMethodHandler(demoBridge) {

    override fun getState(): Map<String, Any?> {
        val state = demoBridge.store.state.value
        return mapOf(
            "rotationSpeed" to state.rotationSpeed.toDouble(),
            "isPaused" to state.isPaused,
            "metallic" to state.metallic.toDouble(),
            "roughness" to state.roughness.toDouble(),
            "envIntensity" to state.envIntensity.toDouble(),
            "fps" to state.fps.toDouble(),
        )
    }

    override fun handleDomainCall(method: String, args: Map<String, Any?>): Any? = when (method) {
        "togglePause" -> { demoBridge.store.dispatch(DemoIntent.TogglePause); true }
        "setRotationSpeed" -> {
            val speed = (args["speed"] as? Number)?.toFloat()
                ?: throw IllegalArgumentException("setRotationSpeed requires a 'speed' argument")
            demoBridge.store.dispatch(DemoIntent.SetRotationSpeed(speed))
            true
        }
        "setMetallic" -> {
            demoBridge.store.dispatch(DemoIntent.SetMetallic((args["metallic"] as? Number)?.toFloat() ?: 0f))
            true
        }
        "setRoughness" -> {
            demoBridge.store.dispatch(DemoIntent.SetRoughness((args["roughness"] as? Number)?.toFloat() ?: 0.5f))
            true
        }
        "setEnvIntensity" -> {
            demoBridge.store.dispatch(DemoIntent.SetEnvIntensity((args["intensity"] as? Number)?.toFloat() ?: 1f))
            true
        }
        else -> throw MethodNotImplementedException(method)
    }
}
