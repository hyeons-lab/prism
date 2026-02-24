package com.hyeonslab.prism.native

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.assets.FileReader
import com.hyeonslab.prism.assets.GltfAsset
import com.hyeonslab.prism.assets.GltfLoader
import com.hyeonslab.prism.assets.ImageDecoder
import com.hyeonslab.prism.core.Engine
import com.hyeonslab.prism.ecs.Entity
import com.hyeonslab.prism.ecs.World
import com.hyeonslab.prism.ecs.components.CameraComponent
import com.hyeonslab.prism.ecs.components.LightComponent
import com.hyeonslab.prism.ecs.components.LightType
import com.hyeonslab.prism.ecs.components.TransformComponent
import com.hyeonslab.prism.ecs.systems.RenderSystem
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Camera
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Texture
import com.hyeonslab.prism.renderer.WgpuRenderer
import io.ygdrasil.webgpu.WGPUContext
import kotlin.math.PI
import kotlin.math.cos
import kotlin.math.sin
import kotlin.time.TimeSource
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import kotlinx.coroutines.runBlocking
import kotlinx.coroutines.withContext
import kotlinx.coroutines.yield

private val log = Logger.withTag("SceneState")

private const val GLTF_ORBIT_RADIUS = 3.5f
private val HALF_PI = (PI / 2.0).toFloat()

/**
 * Per-handle scene state: WgpuRenderer, ECS world, camera entity, orbit-camera parameters, pause
 * flag, and frame-timing accumulators.
 *
 * Created by [buildGltfScene] and stored in the platform-specific surfaces map keyed by engine
 * handle. Destroyed via [shutdown] when [prism_detach_surface] is called.
 *
 * [texToMaterials] maps each [Texture] to the [Material]s that reference it — used by progressive
 * texture loading to evict stale bind-group cache entries after a texture is uploaded to the GPU.
 */
internal class SceneState(
  val renderer: WgpuRenderer,
  val world: World,
  val cameraEntity: Entity,
  var width: Int,
  var height: Int,
  private val texToMaterials: Map<Texture, List<Material>> = emptyMap(),
) {
  private var orbitRadius = GLTF_ORBIT_RADIUS
  private var orbitAzimuth = 0f
  private var orbitElevation = 0f

  var isPaused = false

  // Frame timing — uses the monotonic clock so it is independent of wall-clock drift.
  private var start = TimeSource.Monotonic.markNow()
  private var lastMark = TimeSource.Monotonic.markNow()
  private var _frameCount = 0L
  private var _fps = 0.0

  val fps: Double
    get() = _fps

  // Progressive texture loading: coroutines run on Dispatchers.Main so GPU calls are always on
  // the render thread; image decode switches to Dispatchers.Default via withContext.
  private val textureScope = CoroutineScope(Dispatchers.Main + SupervisorJob())

  /**
   * Advances the frame timer and returns (deltaTime, elapsed) in seconds. Always called, even when
   * paused, so that resume doesn't compute a huge first-frame delta.
   */
  fun advanceTiming(): Pair<Float, Float> {
    val now = TimeSource.Monotonic.markNow()
    val delta = (now - lastMark).inWholeNanoseconds / 1_000_000_000f
    lastMark = now
    val elapsed = (now - start).inWholeNanoseconds / 1_000_000_000f
    // Exponential smoothing: weight recent FPS at 10% per frame.
    if (delta > 0f && delta < 1f) _fps = _fps * 0.9 + (1.0 / delta) * 0.1
    return delta to elapsed
  }

  /** Increments the rendered-frame counter and returns the new count. */
  fun nextFrame(): Long = ++_frameCount

  /**
   * Rotates the orbit camera by [deltaAzimuth] / [deltaElevation] radians. Elevation is clamped to
   * ±π/2 to avoid gimbal lock.
   */
  fun orbitBy(deltaAzimuth: Float, deltaElevation: Float) {
    orbitAzimuth += deltaAzimuth
    orbitElevation = (orbitElevation + deltaElevation).coerceIn(-HALF_PI + 0.05f, HALF_PI - 0.05f)
    val ca = cos(orbitElevation)
    val x = orbitRadius * ca * sin(orbitAzimuth)
    val y = orbitRadius * sin(orbitElevation)
    val z = orbitRadius * ca * cos(orbitAzimuth)
    val cam = world.getComponent<CameraComponent>(cameraEntity) ?: return
    cam.camera.position = Vec3(x, y, z)
  }

  /**
   * Adjusts orbit radius by [delta] (positive zooms in, negative zooms out) and immediately
   * re-positions the camera.
   */
  fun zoom(delta: Float) {
    orbitRadius = (orbitRadius - delta).coerceIn(2f, 40f)
    orbitBy(0f, 0f) // reposition without changing angles
  }

  /** Updates the camera aspect ratio. Call from [prism_resize] after a surface size change. */
  fun updateAspectRatio(newWidth: Int, newHeight: Int) {
    if (newWidth <= 0 || newHeight <= 0) return
    width = newWidth
    height = newHeight
    val cam = world.getComponent<CameraComponent>(cameraEntity) ?: return
    cam.camera.aspectRatio = newWidth.toFloat() / newHeight.toFloat()
  }

  /**
   * Launches progressive texture loading. Each image is decoded on [Dispatchers.Default] (off the
   * render thread) then uploaded to the GPU on [Dispatchers.Main] (the render / main thread). A
   * [yield] between textures lets the NSRunLoop process the next MTKView draw callback, so each
   * texture appears in its own frame rather than stalling for all textures at once.
   */
  internal fun startProgressiveDecode(rawBytes: List<ByteArray?>, textures: List<Texture>) {
    if (rawBytes.isEmpty()) return
    textureScope.launch {
      for (i in rawBytes.indices) {
        val bytes = rawBytes[i] ?: continue
        val texture = textures.getOrNull(i) ?: continue
        val imageData =
          withContext(Dispatchers.Default) {
            try {
              ImageDecoder.decode(bytes, unpremultiply = true)
            } catch (e: Exception) {
              log.w { "Progressive texture $i decode failed: ${e.message}" }
              null
            }
          } ?: continue
        // Back on Dispatchers.Main: GPU calls are safe on the render thread.
        texture.descriptor =
          texture.descriptor.copy(width = imageData.width, height = imageData.height)
        renderer.initializeTexture(texture)
        renderer.uploadTextureData(texture, imageData.pixels)
        // Evict cached bind groups so the next draw call rebuilds them with the real texture.
        texToMaterials[texture]?.forEach { renderer.invalidateMaterial(it) }
        // Yield so the NSRunLoop can run the next render frame before decoding the next texture.
        yield()
      }
      log.i { "Progressive texture loading complete (${textures.size} textures)" }
    }
  }

  /** Shuts down the ECS world and cancels any in-progress texture loading coroutines. */
  fun shutdown() {
    textureScope.cancel()
    world.shutdown()
  }
}

/**
 * Reads the GLB file at [glbPath], creates a [WgpuRenderer] on the pre-configured [wgpuContext],
 * loads glTF geometry into an ECS [World], and returns the fully initialized [SceneState].
 *
 * The Metal surface **must** already be configured (via [prism_attach_metal_layer]) before calling
 * this — [surfacePreConfigured] = true is passed to [WgpuRenderer] so it skips redundant
 * `surface.configure()` calls.
 *
 * Geometry (meshes) is uploaded synchronously so the scene renders immediately. Textures are
 * decoded in the background and streamed to the GPU via [SceneState.startProgressiveDecode] —
 * default 1×1 white/normal/black placeholders are used automatically until each real texture
 * arrives. The caller is responsible for calling [SceneState.shutdown] when done.
 */
internal fun buildGltfScene(
  engine: Engine,
  wgpuContext: WGPUContext,
  glbPath: String,
  width: Int,
  height: Int,
): SceneState {
  log.i { "Loading GLB: $glbPath (${width}x${height})" }

  val glbBytes = runBlocking { FileReader.readBytes(glbPath) }
  log.i { "Read ${glbBytes.size} bytes from $glbPath" }

  // Surface was already configured by prism_attach_metal_layer; skip redundant reconfiguration.
  // hdrEnabled must be set before initialize() so that initialize() auto-calls initializeIbl().
  val renderer = WgpuRenderer(wgpuContext, surfacePreConfigured = true)
  renderer.hdrEnabled = true
  renderer.initialize(engine)

  val world = World()
  world.addSystem(RenderSystem(renderer))

  // loadStructure() parses the GLB structure without decoding any images. Placeholder 1×1 textures
  // are created immediately so geometry can render while real textures load in the background.
  val result = runBlocking { GltfLoader().loadStructure("model.glb", glbBytes) }
  val asset = result.asset

  // Upload geometry (vertices + indices) to the GPU immediately so the first frame can draw.
  for (node in asset.renderableNodes) {
    renderer.uploadMesh(node.mesh)
  }
  asset.instantiateInWorld(world)

  // Camera: orbit distance 3.5 units, 45° FOV, clipping 0.1–50 m.
  val cameraEntity = world.createEntity()
  val camera =
    Camera().apply {
      position = Vec3(0f, 0f, GLTF_ORBIT_RADIUS)
      target = Vec3.ZERO
      fovY = 45f
      aspectRatio = if (height > 0) width.toFloat() / height.toFloat() else 1f
      nearPlane = 0.1f
      farPlane = 50f
    }
  world.addComponent(cameraEntity, TransformComponent(position = camera.position))
  world.addComponent(cameraEntity, CameraComponent(camera))

  // Directional light — warm white from upper-left.
  val dirLight = world.createEntity()
  world.addComponent(dirLight, TransformComponent())
  world.addComponent(
    dirLight,
    LightComponent(
      lightType = LightType.DIRECTIONAL,
      color = Color(1.0f, 0.95f, 0.8f),
      intensity = 2.0f,
      direction = Vec3(-0.5f, -1.0f, -0.5f),
    ),
  )

  // Point light — cool white for complementary specular highlights.
  val pointLight = world.createEntity()
  world.addComponent(pointLight, TransformComponent(position = Vec3(3f, 3f, 3f)))
  world.addComponent(
    pointLight,
    LightComponent(
      lightType = LightType.POINT,
      color = Color(0.8f, 0.9f, 1.0f),
      intensity = 20f,
      range = 15f,
    ),
  )

  world.initialize()

  val texToMaterials = buildTexToMaterials(asset)
  val scene = SceneState(renderer, world, cameraEntity, width, height, texToMaterials)
  scene.startProgressiveDecode(result.rawTextureImageBytes, asset.textures)

  log.i {
    "Scene ready: ${asset.renderableNodes.size} primitives, " +
      "${asset.textures.size} textures loading progressively"
  }
  return scene
}

/** Builds a reverse map from each [Texture] to the [Material]s that reference it. */
private fun buildTexToMaterials(asset: GltfAsset): Map<Texture, List<Material>> {
  val map = mutableMapOf<Texture, MutableList<Material>>()
  for (node in asset.renderableNodes) {
    val mat = node.material ?: continue
    listOfNotNull(
        mat.albedoTexture,
        mat.normalTexture,
        mat.metallicRoughnessTexture,
        mat.occlusionTexture,
        mat.emissiveTexture,
      )
      .forEach { tex -> map.getOrPut(tex) { mutableListOf() }.add(mat) }
  }
  return map
}
