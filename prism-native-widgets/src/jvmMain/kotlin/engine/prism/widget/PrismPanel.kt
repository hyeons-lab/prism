package engine.prism.widget

/**
 * JVM Desktop panel for embedding Prism engine rendering.
 * Wraps a java.awt.Canvas with wgpu surface attachment.
 */
class PrismPanel {
    val surface: PrismSurface = PrismSurface()
    // TODO: Extend java.awt.Canvas and create wgpu surface from HWND/NSView
}
