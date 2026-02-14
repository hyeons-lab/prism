package engine.prism.renderer

/**
 * Platform-specific GPU rendering surface.
 *
 * Each platform provides an actual implementation that wraps the native
 * window surface or swap chain (e.g. LWJGL surface on JVM, CAMetalLayer on iOS,
 * HTML canvas on WebAssembly).
 */
expect class RenderSurface {
    /**
     * Configures (or reconfigures) the surface for the given dimensions.
     * Called on initial setup and whenever the window is resized.
     *
     * @param width Surface width in pixels.
     * @param height Surface height in pixels.
     */
    fun configure(width: Int, height: Int)

    /** Current surface width in pixels. */
    val width: Int

    /** Current surface height in pixels. */
    val height: Int
}
