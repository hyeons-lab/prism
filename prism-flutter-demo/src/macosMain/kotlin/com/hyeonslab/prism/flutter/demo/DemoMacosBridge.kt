@file:OptIn(kotlinx.cinterop.ExperimentalForeignApi::class)

package com.hyeonslab.prism.flutter.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.demo.createGltfDemoScene
import com.hyeonslab.prism.flutter.PrismMetalBridge
import io.ygdrasil.webgpu.WGPUContext
import kotlinx.cinterop.addressOf
import kotlinx.cinterop.usePinned
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.runBlocking
import platform.Foundation.NSBundle
import platform.posix.SEEK_END
import platform.posix.SEEK_SET
import platform.posix.fclose
import platform.posix.fopen
import platform.posix.fread
import platform.posix.fseek
import platform.posix.ftell

private val log = Logger.withTag("DemoMacosBridge")

// Flutter bundles assets inside the App framework at this sub-path.
private const val GLB_ASSET_SUBPATH =
    "Contents/Frameworks/App.framework/Resources/flutter_assets/assets/DamagedHelmet.glb"

/**
 * macOS demo bridge. Extends [PrismMetalBridge] with the Damaged Helmet glTF scene.
 * Throws if the asset is not found in the app bundle.
 *
 * Swift usage:
 *   let bridge = DemoMacosBridge()
 *   bridge.attachMetalLayer(layerPtr: rawPtr, width: w, height: h)  // first draw
 *   bridge.renderFrame()  // each display-link tick
 *   bridge.detachSurface()  // on deinit
 */
class DemoMacosBridge : PrismMetalBridge<DemoScene, DemoStore>(DemoStore()) {

    /** True when the demo is paused — [PrismMetalBridge.renderFrame] reads this to skip ticking. */
    override val isPaused: Boolean get() = store.state.value.isPaused

    /**
     * Scope for progressive background work (texture uploads, IBL). Uses [Dispatchers.Main] so
     * coroutines run on the macOS main run loop, interleaving with the render loop via [yield].
     */
    private val backgroundScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    /** Orbit radius — initialised to match the glTF scene default (3.5); zoom adjusts this. */
    private var orbitRadius = 3.5f

    // --- Engine control methods exposed to Swift via the method channel ---

    /** Toggle the pause state. */
    fun togglePause() = store.dispatch(DemoIntent.TogglePause)

    /** Returns true when the demo is currently paused. */
    fun getPauseState(): Boolean = store.state.value.isPaused

    /** Returns the last smoothed FPS value. */
    fun getCurrentFps(): Double = store.state.value.fps.toDouble()

    /** Push a smoothed FPS reading into the store (called from tickScene). */
    fun dispatchFps(fps: Float) = store.dispatch(DemoIntent.UpdateFps(fps))

    // --- Camera control methods exposed to Swift via mouse/scroll events ---

    /**
     * Rotates the orbit camera by [dx] radians horizontally (azimuth) and [dy] radians
     * vertically (elevation). Called from Swift's `mouseDragged(with:)`.
     */
    fun orbitBy(dx: Double, dy: Double) {
        scene?.orbitBy(deltaAzimuth = dx.toFloat(), deltaElevation = dy.toFloat())
    }

    /**
     * Adjusts the orbit radius by [delta] units. Positive delta zooms in; negative zooms out.
     * Called from Swift's `scrollWheel(with:)`.
     */
    fun zoom(delta: Double) {
        orbitRadius = (orbitRadius - delta.toFloat()).coerceIn(2f, 40f)
        scene?.setOrbitRadius(orbitRadius)
    }

    /**
     * Returns the full demo state for the Flutter `getState` method channel call.
     * Overrides the base [PrismMetalBridge.getState] so the macOS response matches
     * the Kotlin [FlutterMethodHandler.getState] used by Android.
     */
    override fun getState(): Map<String, Any> {
        val state = store.state.value
        return mapOf(
            "initialized" to isInitialized,
            "isPaused" to state.isPaused,
            "fps" to state.fps.toDouble(),
            "rotationSpeed" to state.rotationSpeed.toDouble(),
            "metallic" to state.metallic.toDouble(),
            "roughness" to state.roughness.toDouble(),
            "envIntensity" to state.envIntensity.toDouble(),
        )
    }

    override fun onResize(width: Int, height: Int) {
        scene?.updateAspectRatio(width, height)
    }

    override fun shutdown() {
        runBlocking {
            backgroundScope.coroutineContext[Job]?.children?.forEach { it.join() }
        }
        backgroundScope.cancel()
        scene?.shutdown()
        super.shutdown()
    }

    override fun createScene(wgpuContext: WGPUContext, width: Int, height: Int): DemoScene {
        val glbBytes =
            checkNotNull(loadGlbFromFlutterAssets()) {
                "DamagedHelmet.glb not found in app bundle — ensure the asset is included in flutter_assets"
            }
        return runBlocking {
            createGltfDemoScene(
                wgpuContext, width, height, glbBytes,
                surfacePreConfigured = true,
                // Progressive: parse mesh structure immediately; decode and upload textures
                // one-per-frame via backgroundScope (Dispatchers.Main) so the render loop
                // starts right away with placeholder materials and textures fade in.
                progressiveScope = backgroundScope,
            )
        }
    }

    override fun tickScene(scene: DemoScene, deltaTime: Float, elapsed: Float, frameCount: Long) {
        if (deltaTime > 0f) {
            val smoothedFps = store.state.value.fps * 0.9f + (1f / deltaTime) * 0.1f
            store.dispatch(DemoIntent.UpdateFps(smoothedFps))
        }
        scene.tick(deltaTime = deltaTime, elapsed = elapsed, frameCount = frameCount)
    }

    // --- Private: asset loading ---

    private fun loadGlbFromFlutterAssets(): ByteArray? {
        val glbPath = "${NSBundle.mainBundle().bundlePath}/$GLB_ASSET_SUBPATH"
        val file = fopen(glbPath, "rb")
        if (file == null) {
            log.w { "GLB not found at: $glbPath" }
            return null
        }
        fseek(file, 0, SEEK_END)
        val size = ftell(file)
        fseek(file, 0, SEEK_SET)
        if (size <= 0L) {
            fclose(file)
            return null
        }
        val bytes = ByteArray(size.toInt())
        val bytesRead = bytes.usePinned { pinned ->
            fread(pinned.addressOf(0), 1uL, size.toULong(), file)
        }
        fclose(file)
        if (bytesRead.toLong() != size) {
            log.w { "Short read for DamagedHelmet.glb: expected $size bytes, got $bytesRead" }
            return null
        }
        log.i { "Loaded DamagedHelmet.glb ($size bytes) from app bundle" }
        return bytes
    }
}
