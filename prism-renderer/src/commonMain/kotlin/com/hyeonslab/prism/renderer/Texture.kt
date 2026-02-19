package com.hyeonslab.prism.renderer

/** Pixel format for texture data. */
enum class TextureFormat {
  /** 8-bit RGBA, linear color space. */
  RGBA8_UNORM,

  /** 8-bit RGBA, sRGB color space. */
  RGBA8_SRGB,

  /** 8-bit BGRA, linear color space. */
  BGRA8_UNORM,

  /** 32-bit floating-point depth buffer. */
  DEPTH32_FLOAT,

  /** 24-bit depth + 8-bit stencil packed format. */
  DEPTH24_STENCIL8,

  /** 16-bit floating-point RGBA (HDR). */
  RGBA16_FLOAT,
}

/** Texture sampling filter mode. */
enum class TextureFilter {
  /** Nearest-neighbor (point) sampling. */
  NEAREST,

  /** Bilinear interpolation sampling. */
  LINEAR,
}

/** Texture coordinate wrapping mode. */
enum class TextureWrap {
  /** Tile the texture by repeating UV coordinates. */
  REPEAT,

  /** Clamp UV coordinates to the [0, 1] range. */
  CLAMP_TO_EDGE,

  /** Tile the texture, mirroring on each repeat. */
  MIRRORED_REPEAT,
}

/**
 * Describes the properties of a texture to be created.
 *
 * @param width Texture width in pixels.
 * @param height Texture height in pixels.
 * @param format Pixel format.
 * @param minFilter Minification filter.
 * @param magFilter Magnification filter.
 * @param wrapU Horizontal wrap mode.
 * @param wrapV Vertical wrap mode.
 * @param generateMipmaps Whether to automatically generate a mipmap chain.
 * @param label Optional debug label.
 */
data class TextureDescriptor(
  val width: Int,
  val height: Int,
  val format: TextureFormat = TextureFormat.RGBA8_SRGB,
  val minFilter: TextureFilter = TextureFilter.LINEAR,
  val magFilter: TextureFilter = TextureFilter.LINEAR,
  val wrapU: TextureWrap = TextureWrap.REPEAT,
  val wrapV: TextureWrap = TextureWrap.REPEAT,
  val generateMipmaps: Boolean = true,
  val label: String = "",
)

/**
 * Represents a GPU texture resource.
 *
 * The [handle] property is set by the platform-specific renderer backend after GPU allocation.
 *
 * @param descriptor The texture configuration used to create this texture.
 */
class Texture(val descriptor: TextureDescriptor) {
  /** Platform-specific GPU handle. */
  var handle: Any? = null

  override fun toString(): String {
    val d = descriptor
    return "Texture(${d.width}x${d.height}, format=${d.format}, label='${d.label}')"
  }
}
