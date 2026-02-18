package engine.prism.flutter

import android.content.Context
import android.view.Choreographer
import android.view.SurfaceHolder
import android.view.SurfaceView
import android.view.View
import co.touchlab.kermit.Logger
import com.hyeonslab.prism.demo.DemoIntent
import com.hyeonslab.prism.demo.DemoScene
import com.hyeonslab.prism.demo.createDemoScene
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.flutter.PrismBridge
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.widget.createPrismSurface
import io.flutter.plugin.platform.PlatformView
import io.flutter.plugin.platform.PlatformViewFactory
import io.flutter.plugin.common.StandardMessageCodec
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch

private val log = Logger.withTag("PrismPlatformView")

/**
 * Factory for creating [PrismPlatformView] instances from Flutter's platform view registry.
 */
class PrismPlatformViewFactory(
    private val bridge: PrismBridge
) : PlatformViewFactory(StandardMessageCodec.INSTANCE) {

    override fun create(context: Context, viewId: Int, args: Any?): PlatformView {
        return PrismPlatformView(context, bridge)
    }
}

/**
 * Android platform view that hosts a [SurfaceView] with wgpu4k rendering.
 * Mirrors the pattern from PrismDemoActivity: SurfaceHolder.Callback + Choreographer render loop.
 */
class PrismPlatformView(
    context: Context,
    private val bridge: PrismBridge,
) : PlatformView, SurfaceHolder.Callback, Choreographer.FrameCallback {

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    private val surfaceView = SurfaceView(context)

    private var scene: DemoScene? = null
    private var surface: com.hyeonslab.prism.widget.PrismSurface? = null
    private var initJob: Job? = null
    private var running = false

    private var startTimeNanos = 0L
    private var lastFrameTimeNanos = 0L
    private var frameCount = 0L
    private var accumulatedAngle = 0f

    init {
        surfaceView.holder.addCallback(this)
    }

    override fun getView(): View = surfaceView

    // -- SurfaceHolder.Callback --

    override fun surfaceCreated(holder: SurfaceHolder) {
        log.i { "surfaceCreated" }
    }

    override fun surfaceChanged(holder: SurfaceHolder, format: Int, width: Int, height: Int) {
        log.i { "surfaceChanged: ${width}x${height}" }

        if (scene != null) {
            scene?.renderer?.resize(width, height)
            scene?.updateAspectRatio(width, height)
            return
        }
        if (initJob?.isActive == true) return

        initJob = scope.launch {
            val prismSurface = createPrismSurface(holder, width, height)
            surface = prismSurface

            val wgpuContext = checkNotNull(prismSurface.wgpuContext) {
                "wgpu context not available on Android"
            }
            val demoScene = createDemoScene(
                wgpuContext, width, height,
                initialColor = bridge.store.state.value.cubeColor,
            )
            scene = demoScene
            bridge.attachScene(demoScene)

            startTimeNanos = System.nanoTime()
            lastFrameTimeNanos = startTimeNanos
            frameCount = 0L
            accumulatedAngle = 0f
            running = true

            log.i { "Render loop starting" }
            Choreographer.getInstance().postFrameCallback(this@PrismPlatformView)
        }
    }

    override fun surfaceDestroyed(holder: SurfaceHolder) {
        log.i { "surfaceDestroyed" }
        stopRendering()
    }

    // -- Choreographer.FrameCallback --

    override fun doFrame(frameTimeNanos: Long) {
        if (!running) return

        val deltaTime = (frameTimeNanos - lastFrameTimeNanos) / 1_000_000_000f
        lastFrameTimeNanos = frameTimeNanos
        val elapsed = (frameTimeNanos - startTimeNanos) / 1_000_000_000f
        frameCount++

        val currentScene = scene ?: return
        val state = bridge.state.value

        // Update FPS (smoothed EMA)
        if (deltaTime > 0f) {
            val smoothedFps = state.fps * 0.9f + (1f / deltaTime) * 0.1f
            bridge.store.dispatch(DemoIntent.UpdateFps(smoothedFps))
        }

        // Update rotation angle
        if (!state.isPaused) {
            val speedRadians = Math.toRadians(state.rotationSpeed.toDouble()).toFloat()
            accumulatedAngle += speedRadians * deltaTime
        }

        try {
            // Update material color when it changes
            val cubeMaterial =
                currentScene.world.getComponent<MaterialComponent>(currentScene.cubeEntity)
            if (cubeMaterial != null && cubeMaterial.material?.baseColor != state.cubeColor) {
                cubeMaterial.material = Material(baseColor = state.cubeColor)
            }

            currentScene.tickWithAngle(
                deltaTime = if (state.isPaused) 0f else deltaTime,
                elapsed = elapsed,
                frameCount = frameCount,
                angle = accumulatedAngle,
            )
        } catch (e: Exception) {
            log.e(e) { "Render error on frame $frameCount" }
            running = false
            return
        }

        Choreographer.getInstance().postFrameCallback(this)
    }

    // -- PlatformView lifecycle --

    override fun dispose() {
        log.i { "dispose" }
        stopRendering()
        scope.cancel()
    }

    private fun stopRendering() {
        running = false
        initJob?.cancel()
        initJob = null
        Choreographer.getInstance().removeFrameCallback(this)
        scene?.shutdown()
        scene = null
        bridge.detachScene()
        surface?.detach()
        surface = null
    }
}
