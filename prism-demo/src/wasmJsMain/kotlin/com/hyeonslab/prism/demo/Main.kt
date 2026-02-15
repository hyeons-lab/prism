@file:OptIn(ExperimentalWasmJsInterop::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.core.Time
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.MaterialComponent
import com.hyeonslab.prism.ecs.components.MeshComponent
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.ecs.systems.RenderSystem
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Camera
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.renderer.WgpuRenderer
import io.ygdrasil.webgpu.canvasContextRenderer
import kotlin.math.PI
import kotlinx.coroutines.CoroutineExceptionHandler
import kotlinx.coroutines.DelicateCoroutinesApi
import kotlinx.coroutines.GlobalScope
import kotlinx.coroutines.launch
import web.html.HTMLCanvasElement

@JsFun(
  """(id) => {
  var el = document.getElementById(id);
  if (el && !(el instanceof HTMLCanvasElement)) {
    throw new Error('Element "' + id + '" is not a canvas element');
  }
  return el;
}"""
)
private external fun getCanvasById(id: String): HTMLCanvasElement?

@JsFun("(callback, interval) => setInterval(callback, interval)")
private external fun setInterval(callback: () -> Unit, interval: Int): JsAny

@JsFun("(id) => clearInterval(id)") private external fun clearInterval(id: JsAny)

@JsFun("() => performance.now()") private external fun performanceNow(): Double

@JsFun(
  """(msg) => {
  console.error('Prism: ' + msg);
  var canvas = document.getElementById('prismCanvas');
  if (canvas) canvas.style.display = 'none';
  var fallback = document.getElementById('fallback');
  if (fallback) {
    fallback.style.display = 'block';
    fallback.textContent = 'Error: ' + msg;
  }
}"""
)
private external fun showError(message: String)

@OptIn(DelicateCoroutinesApi::class)
fun main() {
  Logger.i("Prism") { "Starting Prism WebGPU Demo..." }

  val handler = CoroutineExceptionHandler { _, throwable ->
    Logger.e("Prism", throwable) { "Fatal error: ${throwable.message}" }
    showError(throwable.message ?: "Unknown error")
  }

  GlobalScope.launch(handler) {
    val canvas = getCanvasById("prismCanvas") ?: error("Canvas element 'prismCanvas' not found")
    val canvasContext = canvasContextRenderer(htmlCanvas = canvas, width = 800, height = 600)
    val wgpuContext = canvasContext.wgpuContext

    val renderer = WgpuRenderer(wgpuContext)
    val engine = Engine()
    engine.addSubsystem(renderer)
    engine.initialize()

    val world = World()
    world.addSystem(RenderSystem(renderer))

    // Camera entity
    val cameraEntity = world.createEntity()
    val camera = Camera()
    camera.position = Vec3(2f, 2f, 4f)
    camera.target = Vec3(0f, 0f, 0f)
    camera.fovY = 60f
    camera.aspectRatio = 800f / 600f
    camera.nearPlane = 0.1f
    camera.farPlane = 100f
    world.addComponent(cameraEntity, TransformComponent(position = camera.position))
    world.addComponent(cameraEntity, CameraComponent(camera))

    // Cube entity
    val cubeEntity = world.createEntity()
    world.addComponent(cubeEntity, TransformComponent())
    world.addComponent(cubeEntity, MeshComponent(mesh = Mesh.cube()))
    world.addComponent(
      cubeEntity,
      MaterialComponent(material = Material(baseColor = Color(0.3f, 0.5f, 0.9f))),
    )

    world.initialize()
    Logger.i("Prism") { "WebGPU initialized — starting render loop" }

    val startTime = performanceNow()
    val rotationSpeed = PI.toFloat() / 4f
    var frameCount = 0L
    var intervalId: JsAny? = null

    intervalId =
      setInterval(
        {
          try {
            val elapsed = ((performanceNow() - startTime) / 1000.0).toFloat()
            val angle = elapsed * rotationSpeed

            val cubeTransform = world.getComponent<TransformComponent>(cubeEntity)
            if (cubeTransform != null) {
              cubeTransform.rotation = Quaternion.fromAxisAngle(Vec3.UP, angle)
            }

            frameCount++
            val time = Time(deltaTime = 1f / 60f, totalTime = elapsed, frameCount = frameCount)
            world.update(time)
          } catch (e: Throwable) {
            intervalId?.let { clearInterval(it) }
            Logger.e("Prism", e) { "Render loop error: ${e.message}" }
            showError(e.message ?: "Render loop error")
          }
        },
        16,
      )
  }
}
