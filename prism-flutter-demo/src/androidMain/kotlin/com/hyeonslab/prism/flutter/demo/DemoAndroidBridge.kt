package com.hyeonslab.prism.flutter.demo

import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.DemoStore
import com.hyeonslab.prism.demo.createGltfDemoScene
import com.hyeonslab.prism.flutter.PrismAndroidBridge
import io.ygdrasil.webgpu.WGPUContext
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel

/**
 * Android demo bridge. Extends [PrismAndroidBridge] with the Damaged Helmet glTF scene,
 * mirroring [DemoMacosBridge] on macOS. All Android surface lifecycle is inherited;
 * this class only supplies the demo-specific scene factory and tick logic.
 *
 * @param glbLoader Loader that returns the raw bytes of the GLB asset. Must be non-null and must
 *   return non-null bytes; throws [IllegalStateException] otherwise.
 */
class DemoAndroidBridge(
    private val glbLoader: (() -> ByteArray?)? = null,
) : PrismAndroidBridge<DemoScene, DemoStore>(DemoStore()) {

    override val isPaused: Boolean get() = store.state.value.isPaused

    /**
     * Scope for progressive background work (texture uploads, IBL). Uses [Dispatchers.Main] so
     * coroutines interleave with the Choreographer render loop via [yield].
     */
    private val backgroundScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

    override fun onDimensionsChanged(width: Int, height: Int) {
        scene?.updateAspectRatio(width, height)
    }

    override fun shutdown() {
        backgroundScope.cancel()
        scene?.shutdown()
        super.shutdown()
    }

    override suspend fun createScene(wgpuContext: WGPUContext, width: Int, height: Int): DemoScene {
        val glbBytes = checkNotNull(glbLoader?.invoke()) {
            "DamagedHelmet.glb not available — provide a glbLoader that returns the asset bytes"
        }
        return createGltfDemoScene(wgpuContext, width, height, glbBytes,
            progressiveScope = backgroundScope)
    }

    override fun tickScene(scene: DemoScene, deltaTime: Float, elapsed: Float, frameCount: Long) {
        if (deltaTime > 0f) {
            val smoothedFps = store.state.value.fps * 0.9f + (1f / deltaTime) * 0.1f
            store.dispatch(DemoIntent.UpdateFps(smoothedFps))
        }
        scene.tick(deltaTime = deltaTime, elapsed = elapsed, frameCount = frameCount)
    }
}
