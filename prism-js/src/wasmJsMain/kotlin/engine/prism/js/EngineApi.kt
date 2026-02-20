@file:OptIn(ExperimentalJsExport::class)

package engine.prism.js

import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.EngineConfig
import kotlin.js.JsExport

/** Creates an Engine with the given config and returns its handle. */
@JsExport
fun prismCreateEngine(appName: String, targetFps: Int): String =
    Registry.put(Engine(EngineConfig(appName = appName, targetFps = targetFps)))

/** Initializes the engine. Must be called before the render loop. */
@JsExport
fun prismEngineInitialize(handle: String) {
    Registry.get<Engine>(handle)?.initialize()
}

/** Returns true if the engine handle is valid (was created and not destroyed). */
@JsExport
fun prismEngineIsAlive(handle: String): Boolean = Registry.get<Engine>(handle) != null

/** Returns the most recent frame delta time in seconds. */
@JsExport
fun prismEngineGetDeltaTime(handle: String): Float =
    Registry.get<Engine>(handle)?.time?.deltaTime ?: 0f

/** Returns the total elapsed time in seconds since the engine started. */
@JsExport
fun prismEngineGetTotalTime(handle: String): Float =
    Registry.get<Engine>(handle)?.time?.totalTime ?: 0f

/** Shuts down the engine and releases its handle. */
@JsExport
fun prismDestroyEngine(handle: String) {
    Registry.get<Engine>(handle)?.shutdown()
    Registry.remove(handle)
}
