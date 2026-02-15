package engine.prism.renderer

/** The programmable stage a shader targets. */
enum class ShaderStage {
  /** Vertex processing stage. */
  VERTEX,

  /** Fragment (pixel) processing stage. */
  FRAGMENT,

  /** General-purpose compute stage. */
  COMPUTE,
}

/**
 * Contains the source code for a single shader stage.
 *
 * @param code The shader source code text (WGSL, GLSL, MSL, etc.).
 * @param stage The pipeline stage this source targets.
 * @param entryPoint The name of the entry-point function within the source.
 */
data class ShaderSource(val code: String, val stage: ShaderStage, val entryPoint: String = "main")

/**
 * A compiled shader module containing vertex and fragment stages.
 *
 * The [handle] property is set by the platform-specific renderer backend after shader compilation
 * on the GPU.
 *
 * @param vertexSource Source code for the vertex stage.
 * @param fragmentSource Source code for the fragment stage.
 * @param label Optional debug label.
 */
class ShaderModule(
  val vertexSource: ShaderSource,
  val fragmentSource: ShaderSource,
  val label: String = "",
) {
  /** Platform-specific GPU handle (e.g. WebGPU GPUShaderModule). */
  var handle: Any? = null

  override fun toString(): String {
    val vs = vertexSource.entryPoint
    val fs = fragmentSource.entryPoint
    return "ShaderModule(vertex='$vs', fragment='$fs', label='$label')"
  }
}
