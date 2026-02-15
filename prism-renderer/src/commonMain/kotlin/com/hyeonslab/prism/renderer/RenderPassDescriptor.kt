package com.hyeonslab.prism.renderer

/**
 * Describes configuration for a render pass.
 *
 * @param clearColor Color to clear the color attachment to at the start of the pass.
 * @param clearDepth Value to clear the depth buffer to (typically 1.0 for far plane).
 * @param clearStencil Value to clear the stencil buffer to.
 * @param label Optional debug label for graphics debuggers.
 */
data class RenderPassDescriptor(
  val clearColor: Color = Color.CORNFLOWER_BLUE,
  val clearDepth: Float = 1f,
  val clearStencil: Int = 0,
  val label: String = "",
)
