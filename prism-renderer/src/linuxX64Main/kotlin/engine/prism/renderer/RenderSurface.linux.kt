package engine.prism.renderer

/**
 * Linux Native implementation of [RenderSurface].
 *
 * Will wrap a native window / wgpu4k surface once the GPU backend is integrated.
 */
actual class RenderSurface {

    actual val width: Int
        get() = _width

    actual val height: Int
        get() = _height

    private var _width: Int = 0
    private var _height: Int = 0

    actual fun configure(width: Int, height: Int) {
        _width = width
        _height = height
        TODO("Not yet implemented — awaiting wgpu4k Linux surface integration")
    }
}
