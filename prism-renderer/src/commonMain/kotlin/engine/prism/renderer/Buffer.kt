package engine.prism.renderer

/** Describes how a GPU buffer is intended to be used. */
enum class BufferUsage {
  /** Buffer holds vertex data. */
  VERTEX,

  /** Buffer holds index data. */
  INDEX,

  /** Buffer holds uniform/constant data for shaders. */
  UNIFORM,

  /** Buffer holds general-purpose data accessible from shaders. */
  STORAGE,
}

/**
 * Represents a GPU-allocated buffer.
 *
 * The [handle] property is set by the platform-specific renderer backend after allocation on the
 * GPU.
 *
 * @param usage Intended usage of this buffer.
 * @param sizeInBytes Size of the buffer in bytes.
 * @param label Optional debug label for graphics debuggers.
 */
class GpuBuffer(val usage: BufferUsage, val sizeInBytes: Long, val label: String = "") {
  /** Platform-specific GPU handle (e.g. WebGPU GPUBuffer, Vulkan VkBuffer). */
  var handle: Any? = null

  override fun toString(): String = "GpuBuffer(usage=$usage, size=$sizeInBytes, label='$label')"
}
