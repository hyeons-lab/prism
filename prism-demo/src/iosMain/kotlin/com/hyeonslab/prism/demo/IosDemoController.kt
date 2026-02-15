@file:OptIn(ExperimentalForeignApi::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import io.ygdrasil.webgpu.IosContext
import io.ygdrasil.webgpu.iosContextRenderer
import kotlin.math.PI
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.CValue
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.useContents
import platform.CoreGraphics.CGSize
import platform.MetalKit.MTKView
import platform.MetalKit.MTKViewDelegateProtocol
import platform.QuartzCore.CACurrentMediaTime
import platform.darwin.NSObject

private val log = Logger.withTag("PrismIOS")
private const val ROTATION_SPEED = PI.toFloat() / 4f

/**
 * Configures and returns the demo scene bound to the given [MTKView]. This is the main entry point
 * called from Swift via framework interop.
 *
 * After calling this, the [MTKView]'s delegate is set to a [DemoRenderDelegate] that drives the
 * render loop on each display-link callback.
 */
suspend fun configureDemo(view: MTKView): IosContext {
  val width = view.drawableSize.useContents { width.toInt() }
  val height = view.drawableSize.useContents { height.toInt() }
  log.i { "Configuring demo: ${width}x${height}" }

  val iosContext = iosContextRenderer(view, width, height)
  val scene = createDemoScene(iosContext.wgpuContext, width = width, height = height)

  view.delegate = DemoRenderDelegate(scene)
  log.i { "iOS demo configured — render delegate installed" }
  return iosContext
}

/**
 * MTKView render delegate that drives the Prism demo render loop. Each [drawInMTKView] call updates
 * the cube rotation and ticks the ECS world.
 */
@OptIn(BetaInteropApi::class)
class DemoRenderDelegate(private val scene: DemoScene) : NSObject(), MTKViewDelegateProtocol {

  private val startTime = CACurrentMediaTime()
  private var lastFrameTime = startTime
  private var frameCount = 0L

  override fun drawInMTKView(view: MTKView) {
    val now = CACurrentMediaTime()
    val deltaTime = (now - lastFrameTime).toFloat()
    lastFrameTime = now
    val elapsed = (now - startTime).toFloat()

    val angle = elapsed * ROTATION_SPEED
    val cubeTransform = scene.world.getComponent<TransformComponent>(scene.cubeEntity)
    cubeTransform?.rotation = Quaternion.fromAxisAngle(Vec3.UP, angle)

    frameCount++
    val time = Time(deltaTime = deltaTime, totalTime = elapsed, frameCount = frameCount)
    scene.world.update(time)
  }

  override fun mtkView(view: MTKView, drawableSizeWillChange: CValue<CGSize>) {
    drawableSizeWillChange.useContents {
      val w = width.toInt()
      val h = height.toInt()
      log.i { "Drawable size changed: ${w}x${h}" }
      scene.updateAspectRatio(w, h)
    }
  }
}
