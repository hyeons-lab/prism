@file:OptIn(ExperimentalNativeApi::class, ExperimentalForeignApi::class)

package engine.prism.native

import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.EngineConfig
import kotlin.experimental.ExperimentalNativeApi
import kotlin.native.CName
import kotlinx.cinterop.ByteVar
import kotlinx.cinterop.CPointer
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.toKString

// ---------------------------------------------------------------------------
// Engine API
// ---------------------------------------------------------------------------

/**
 * Creates an Engine with [appName] and [targetFps] and returns an opaque handle.
 * [appName] is a null-terminated C string; pass NULL to use the default app name.
 */
@CName("prism_create_engine")
fun prismCreateEngine(appName: CPointer<ByteVar>?, targetFps: Int): Long =
    Registry.put(
        Engine(EngineConfig(appName = appName?.toKString() ?: "Prism", targetFps = targetFps))
    )

/** Initializes the engine identified by [handle]. Must be called before the render loop. */
@CName("prism_engine_initialize")
fun prismEngineInitialize(handle: Long) {
    Registry.get<Engine>(handle)?.initialize()
}

/** Returns 1 if [handle] refers to a live engine, 0 otherwise. */
@CName("prism_engine_is_alive")
fun prismEngineIsAlive(handle: Long): Int = if (Registry.get<Engine>(handle) != null) 1 else 0

/** Returns the most recent frame delta time in seconds, or 0 if handle is invalid. */
@CName("prism_engine_get_delta_time")
fun prismEngineGetDeltaTime(handle: Long): Float =
    Registry.get<Engine>(handle)?.time?.deltaTime ?: 0f

/** Returns total elapsed time in seconds since the engine started, or 0 if invalid. */
@CName("prism_engine_get_total_time")
fun prismEngineGetTotalTime(handle: Long): Float =
    Registry.get<Engine>(handle)?.time?.totalTime ?: 0f

/** Returns the total frame count, or 0 if the handle is invalid. */
@CName("prism_engine_get_frame_count")
fun prismEngineGetFrameCount(handle: Long): Long =
    Registry.get<Engine>(handle)?.time?.frameCount ?: 0L

/** Shuts down the engine and releases the handle. */
@CName("prism_destroy_engine")
fun prismDestroyEngine(handle: Long) {
    Registry.get<Engine>(handle)?.shutdown()
    Registry.remove(handle)
}
