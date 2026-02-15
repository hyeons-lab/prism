package com.hyeonslab.prism.widget

import co.touchlab.kermit.Logger
import com.sun.jna.Native
import com.sun.jna.NativeLibrary
import com.sun.jna.Pointer
import darwin.CAMetalLayer
import darwin.NSView
import ffi.NativeAddress
import io.ygdrasil.webgpu.CompositeAlphaMode
import io.ygdrasil.webgpu.DeviceDescriptor
import io.ygdrasil.webgpu.GPUDevice
import io.ygdrasil.webgpu.GPUTextureFormat
import io.ygdrasil.webgpu.GPUTextureUsage
import io.ygdrasil.webgpu.GPUUncapturedErrorCallback
import io.ygdrasil.webgpu.NativeSurface
import io.ygdrasil.webgpu.RenderingContext
import io.ygdrasil.webgpu.Surface
import io.ygdrasil.webgpu.SurfaceConfiguration
import io.ygdrasil.webgpu.WGPU
import io.ygdrasil.webgpu.WGPUContext
import io.ygdrasil.webgpu.toNativeAddress
import java.awt.Canvas
import java.awt.event.ComponentAdapter
import java.awt.event.ComponentEvent
import java.lang.foreign.MemorySegment
import kotlinx.coroutines.runBlocking
import org.rococoa.ID
import org.rococoa.Rococoa

private val log = Logger.withTag("PrismPanel")

/**
 * JVM Desktop panel for embedding Prism engine rendering. Extends [java.awt.Canvas] (heavyweight
 * AWT component) which provides a real OS native view suitable for GPU rendering.
 *
 * On macOS, extracts the NSView from the Canvas, attaches a CAMetalLayer, and creates a wgpu
 * surface from it. This allows wgpu4k rendering directly into an AWT Canvas without requiring a
 * GLFW window.
 *
 * Usage: Embed this in a Compose Desktop window via `SwingPanel`.
 */
class PrismPanel : Canvas() {

  /** The wgpu4k context created from this Canvas's native handle. Available after [addNotify]. */
  var wgpuContext: WGPUContext? = null
    private set

  /** Whether the wgpu surface has been successfully initialized. */
  var isReady: Boolean = false
    private set

  /** Callback invoked when the surface is ready for rendering. */
  var onReady: (() -> Unit)? = null

  /** Callback invoked when the Canvas is resized with the new width and height in pixels. */
  var onResized: ((width: Int, height: Int) -> Unit)? = null

  /** Callback for uncaptured GPU errors. */
  var onUncapturedError: GPUUncapturedErrorCallback? = null

  private var wgpuInstance: WGPU? = null
  private var nativeSurface: NativeSurface? = null
  private var metalLayerPtr: Pointer? = null

  init {
    addComponentListener(
      object : ComponentAdapter() {
        override fun componentResized(e: ComponentEvent) {
          val ctx = wgpuContext ?: return
          val w = width
          val h = height
          if (w > 0 && h > 0) {
            log.d { "Canvas resized to ${w}x${h}" }
            updateMetalLayerFrame()
            reconfigureSurface(ctx, w, h)
            // If onReady was deferred (Canvas had 0x0 size during init), fire it now
            if (!isReady) {
              isReady = true
              log.i { "wgpu surface ready after resize (${w}x${h})" }
              onReady?.invoke()
            }
            onResized?.invoke(w, h)
          }
        }
      }
    )
  }

  override fun addNotify() {
    super.addNotify()
    log.i { "Canvas peer created, initializing wgpu surface..." }
    @Suppress("TooGenericExceptionCaught") // wgpu4k FFI can throw various exception types
    try {
      initializeWgpuSurface()
    } catch (e: Exception) {
      log.e(e) { "Failed to initialize wgpu surface" }
    }
  }

  override fun removeNotify() {
    log.i { "Canvas peer removed, cleaning up wgpu surface..." }
    isReady = false
    // Tear down in reverse order of construction: sublayer → wgpu context → wgpu instance
    metalLayerPtr?.let { ObjCBridge.removeFromSuperlayer(it) }
    metalLayerPtr = null
    wgpuContext?.close()
    wgpuContext = null
    nativeSurface = null
    wgpuInstance?.close()
    wgpuInstance = null
    super.removeNotify()
  }

  private fun initializeWgpuSurface() {
    val wgpu = WGPU.createInstance() ?: error("Failed to create WGPU instance")
    wgpuInstance = wgpu

    val surface = createNativeSurface(wgpu)
    nativeSurface = surface

    val adapter = runBlocking { wgpu.requestAdapter(surface) } ?: error("Failed to get GPU adapter")

    val device = runBlocking {
      adapter.requestDevice(DeviceDescriptor(onUncapturedError = onUncapturedError)).getOrThrow()
    }

    surface.computeSurfaceCapabilities(adapter)

    // Use actual dimensions, or 1x1 placeholder if not yet laid out (will be reconfigured on
    // first resize). Surface configuration requires non-zero dimensions.
    val w = width.coerceAtLeast(1)
    val h = height.coerceAtLeast(1)

    // Create Surface wrapper with windowHandler=0L (we don't use GLFW)
    val surfaceWrapper = Surface(surface, 0L)

    // Create a rendering context that returns our Canvas dimensions
    val textureFormat = surfaceWrapper.supportedFormats.first()
    val renderingContext = AwtRenderingContext(surfaceWrapper, textureFormat, w, h)

    // Configure the surface with explicit dimensions
    configureSurface(surface, device, textureFormat, surfaceWrapper.supportedAlphaMode, w, h)

    val ctx = WGPUContext(surfaceWrapper, adapter, device, renderingContext)
    wgpuContext = ctx

    // If the Canvas already has a valid size, signal ready immediately.
    // Otherwise, defer readiness until the first componentResized event.
    if (width > 0 && height > 0) {
      isReady = true
      log.i { "wgpu surface initialized (${w}x${h}, format=$textureFormat)" }
      onReady?.invoke()
    } else {
      log.i { "wgpu surface created (awaiting first resize for onReady)" }
    }
  }

  private fun createNativeSurface(wgpu: WGPU): NativeSurface {
    val os = System.getProperty("os.name").lowercase()
    return when {
      os.contains("mac") -> createMacOsSurface(wgpu)
      os.contains("win") -> createWindowsSurface(wgpu)
      os.contains("linux") || os.contains("nix") -> createLinuxSurface(wgpu)
      else -> error("Unsupported platform: $os")
    }
  }

  private fun createMacOsSurface(wgpu: WGPU): NativeSurface {
    val nsViewAddr = resolveNsViewPointer()
    log.d { "NSView address: 0x${nsViewAddr.toString(16)}" }

    // Make the content view layer-backed (preserves AWT's own rendering)
    val nsView = Rococoa.wrap(ID.fromLong(nsViewAddr), NSView::class.java)
    nsView.setWantsLayer(true)

    // The AWT reflection fallback returns the window's content NSView, not a
    // Canvas-specific view (macOS LW AWT shares one NSView for all components).
    // Adding the Metal layer as a SUBLAYER — positioned at Canvas bounds — instead
    // of replacing the content view's backing layer ensures the layer only covers
    // this Canvas, not the entire window.
    val contentLayer =
      ObjCBridge.getLayer(Pointer(nsViewAddr))
        ?: error("NSView has no backing layer after setWantsLayer(true)")

    val layer = CAMetalLayer.layer()
    val layerPtr = Pointer(layer.id().toLong())
    metalLayerPtr = layerPtr
    updateMetalLayerFrame()
    ObjCBridge.addSublayer(contentLayer, layerPtr)

    return wgpu.getSurfaceFromMetalLayer(layer.id().toLong().toNativeAddress())
      ?: error("Failed to create surface from Metal layer")
  }

  /** Updates the Metal sublayer's frame to match the Canvas position and size within the window. */
  private fun updateMetalLayerFrame() {
    val layerPtr = metalLayerPtr ?: return
    ObjCBridge.setFrame(
      layerPtr,
      x.toDouble(),
      y.toDouble(),
      width.coerceAtLeast(1).toDouble(),
      height.coerceAtLeast(1).toDouble(),
    )
  }

  /**
   * Resolves the native NSView pointer for this AWT Canvas. Tries JNA's
   * [Native.getComponentPointer] first (works with `-XstartOnFirstThread`). If that returns null
   * (common without the flag on macOS), falls back to reflection on AWT internals: `peer →
   * windowPeer → platformWindow.contentView → getAWTView()`.
   */
  private fun resolveNsViewPointer(): Long {
    // Try JNA first (fast path, works with -XstartOnFirstThread)
    val jnaPtr = Native.getComponentPointer(this)
    val jnaAddr = Pointer.nativeValue(jnaPtr)
    if (jnaAddr != 0L) {
      log.d { "NSView resolved via JNA: 0x${jnaAddr.toString(16)}" }
      return jnaAddr
    }

    // Fallback: reflection on AWT internals (works without -XstartOnFirstThread)
    log.d { "JNA returned null pointer, falling back to AWT reflection" }
    return resolveNsViewViaReflection()
  }

  /**
   * Extracts the NSView pointer from AWT's internal peer hierarchy via reflection: `Canvas.peer
   * (LWCanvasPeer) → windowPeer (LWWindowPeer) → getPlatformWindow() (CPlatformWindow) →
   * contentView (CPlatformView) → getAWTView() → long`.
   *
   * Requires: `--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED` and
   * `--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED`.
   */
  @Suppress("TooGenericExceptionCaught") // Reflection can throw many exception types
  private fun resolveNsViewViaReflection(): Long {
    try {
      // Step 1: Canvas → peer (LWCanvasPeer)
      val peerField = java.awt.Component::class.java.getDeclaredField("peer")
      peerField.isAccessible = true
      val peer = peerField.get(this) ?: error("Canvas has no AWT peer")

      // Step 2: LWCanvasPeer → windowPeer (LWWindowPeer)
      val lwComponentPeerClass = Class.forName("sun.lwawt.LWComponentPeer")
      val windowPeerField = lwComponentPeerClass.getDeclaredField("windowPeer")
      windowPeerField.isAccessible = true
      val windowPeer = windowPeerField.get(peer) ?: error("No window peer found")

      // Step 3: LWWindowPeer → getPlatformWindow() → CPlatformWindow
      val getPlatformWindow = windowPeer.javaClass.getMethod("getPlatformWindow")
      getPlatformWindow.isAccessible = true
      val platformWindow = getPlatformWindow.invoke(windowPeer) ?: error("No platform window")

      // Step 4: CPlatformWindow → contentView (CPlatformView)
      val cpwClass = Class.forName("sun.lwawt.macosx.CPlatformWindow")
      val contentViewField = cpwClass.getDeclaredField("contentView")
      contentViewField.isAccessible = true
      val contentView = contentViewField.get(platformWindow) ?: error("No content view")

      // Step 5: CPlatformView → getAWTView() → NSView pointer
      val getAWTView = contentView.javaClass.getMethod("getAWTView")
      getAWTView.isAccessible = true
      val nsViewPtr = getAWTView.invoke(contentView) as Long

      if (nsViewPtr == 0L) {
        error("CPlatformView.getAWTView() returned null pointer")
      }
      log.d { "NSView resolved via AWT reflection: 0x${nsViewPtr.toString(16)}" }
      return nsViewPtr
    } catch (e: Exception) {
      throw IllegalStateException(
        "Failed to extract NSView pointer via AWT reflection. " +
          "Ensure JVM is launched with: " +
          "--add-opens=java.desktop/java.awt=ALL-UNNAMED " +
          "--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED " +
          "--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED",
        e,
      )
    }
  }

  @Suppress("UnusedPrivateMember")
  private fun createWindowsSurface(wgpu: WGPU): NativeSurface {
    val hwndPtr =
      Native.getComponentPointer(this)
        ?: error(
          "Failed to obtain native HWND for this Canvas. " +
            "Ensure the component is displayable and peer is initialized."
        )
    val hwnd = Pointer.nativeValue(hwndPtr).toNativeAddress()
    val hinstance =
      com.sun.jna.platform.win32.Kernel32.INSTANCE.GetModuleHandle(null).pointer.toNativeAddress()
    return wgpu.getSurfaceFromWindows(hinstance, hwnd)
      ?: error("Failed to create surface from HWND")
  }

  @Suppress("UnusedPrivateMember")
  private fun createLinuxSurface(wgpu: WGPU): NativeSurface {
    val windowPtr =
      Native.getComponentPointer(this)
        ?: error(
          "Failed to obtain native X11 window handle for this Canvas. " +
            "Ensure the component is displayable and that X11 is being used " +
            "(Wayland is not yet supported)."
        )
    val windowId = Pointer.nativeValue(windowPtr).toULong()

    // Extract X11 Display pointer via reflection on AWT's internal Toolkit.
    // This only works on X11-based systems; Wayland is not yet supported.
    val displayPtr = extractX11DisplayPointer()

    val display = MemorySegment.ofAddress(displayPtr).let { NativeAddress(it) }
    return wgpu.getSurfaceFromX11Window(display, windowId)
      ?: error("Failed to create surface from X11 window (display=$displayPtr, window=$windowId)")
  }

  @Suppress("UnreachableCode") // False positive: detekt misanalyzes try-catch-throw flow
  private fun extractX11DisplayPointer(): Long {
    val toolkit = java.awt.Toolkit.getDefaultToolkit()
    val displayField =
      try {
        toolkit.javaClass.getDeclaredField("display")
      } catch (e: NoSuchFieldException) {
        throw IllegalStateException(
          "Cannot extract X11 display handle from AWT Toolkit (${toolkit.javaClass.name}). " +
            "Linux surface creation requires X11. Wayland is not yet supported.",
          e,
        )
      }
    try {
      displayField.isAccessible = true
    } catch (e: IllegalAccessException) {
      throw IllegalStateException(
        "Cannot access X11 display handle. " +
          "Ensure JVM is launched with --add-opens=java.desktop/sun.awt=ALL-UNNAMED",
        e,
      )
    }
    return displayField.getLong(toolkit)
  }

  private fun configureSurface(
    surface: NativeSurface,
    device: GPUDevice,
    format: GPUTextureFormat,
    supportedAlphaModes: Set<CompositeAlphaMode>,
    w: Int,
    h: Int,
  ) {
    val alphaMode =
      if (supportedAlphaModes.contains(CompositeAlphaMode.Inherit)) {
        CompositeAlphaMode.Inherit
      } else {
        CompositeAlphaMode.Opaque
      }
    surface.configure(
      SurfaceConfiguration(
        device = device,
        format = format,
        usage = GPUTextureUsage.RenderAttachment or GPUTextureUsage.CopySrc,
        alphaMode = alphaMode,
      ),
      w.toUInt(),
      h.toUInt(),
    )
  }

  /** Reconfigure the surface when the Canvas is resized. */
  fun reconfigureSurface(ctx: WGPUContext, w: Int, h: Int) {
    val surface = nativeSurface ?: return
    val renderingContext = ctx.renderingContext as? AwtRenderingContext ?: return
    renderingContext.updateSize(w, h)
    val format = renderingContext.textureFormat
    configureSurface(surface, ctx.device, format, ctx.surface.supportedAlphaMode, w, h)
  }
}

/**
 * Minimal Objective-C runtime bridge via JNA for CALayer frame manipulation.
 *
 * The macOS LW AWT architecture shares a single NSView (the window's content view) for all
 * components. To scope the Metal rendering layer to just the Canvas area, we add the CAMetalLayer
 * as a sublayer and set its frame to the Canvas bounds. This requires direct Objective-C runtime
 * calls since Rococoa 0.0.1 doesn't expose `layer`, `addSublayer:`, `setFrame:`, or
 * `removeFromSuperlayer`.
 *
 * On arm64, CGRect (4 doubles) is a Homogeneous Floating-point Aggregate passed in FP registers
 * d0–d3. JNA's [com.sun.jna.Function.invoke] places [Double] arguments in FP registers, matching
 * the arm64 calling convention for `objc_msgSend` dispatching to typed method implementations.
 */
private object ObjCBridge {
  private val runtime by lazy { NativeLibrary.getInstance("objc") }
  private val msgSend by lazy { runtime.getFunction("objc_msgSend") }
  private val selRegisterName by lazy { runtime.getFunction("sel_registerName") }
  private val objcGetClass by lazy { runtime.getFunction("objc_getClass") }

  // Cached selectors and class pointers — these are immutable runtime constants.
  // Caching avoids repeated JNA native calls on every resize (14 → 4 calls per frame).
  private val selLayer by lazy { sel("layer") }
  private val selAddSublayer by lazy { sel("addSublayer:") }
  private val selRemoveFromSuperlayer by lazy { sel("removeFromSuperlayer") }
  private val selSetFrame by lazy { sel("setFrame:") }
  private val selBegin by lazy { sel("begin") }
  private val selCommit by lazy { sel("commit") }
  private val selSetDisableActions by lazy { sel("setDisableActions:") }
  private val caTransaction by lazy { cls("CATransaction") }

  private fun sel(name: String): Pointer =
    selRegisterName.invoke(Pointer::class.java, arrayOf(name)) as Pointer

  private fun cls(name: String): Pointer =
    objcGetClass.invoke(Pointer::class.java, arrayOf(name)) as Pointer

  /** Returns `[view layer]`. */
  fun getLayer(view: Pointer): Pointer? =
    msgSend.invoke(Pointer::class.java, arrayOf(view, selLayer)) as? Pointer

  /** Calls `[parentLayer addSublayer:childLayer]`. */
  fun addSublayer(parentLayer: Pointer, childLayer: Pointer) {
    msgSend.invoke(Void::class.java, arrayOf(parentLayer, selAddSublayer, childLayer))
  }

  /** Calls `[layer removeFromSuperlayer]`. */
  fun removeFromSuperlayer(layer: Pointer) {
    msgSend.invoke(Void::class.java, arrayOf(layer, selRemoveFromSuperlayer))
  }

  /**
   * Calls `[layer setFrame:CGRect(x,y,w,h)]` inside a CATransaction that disables implicit
   * animations, so the layer repositions immediately during resize instead of animating over 0.25s.
   */
  @Suppress("ArrayPrimitive") // JNA Function.invoke requires Object[] — boxing is unavoidable
  fun setFrame(layer: Pointer, x: Double, y: Double, width: Double, height: Double) {
    msgSend.invoke(Void::class.java, arrayOf(caTransaction, selBegin))
    msgSend.invoke(Void::class.java, arrayOf(caTransaction, selSetDisableActions, true))
    msgSend.invoke(Void::class.java, arrayOf(layer, selSetFrame, x, y, width, height))
    msgSend.invoke(Void::class.java, arrayOf(caTransaction, selCommit))
  }
}

/**
 * A [RenderingContext] backed by AWT Canvas dimensions instead of GLFW window size. Avoids calling
 * `glfwGetWindowSize()` which would fail without a GLFW window.
 */
class AwtRenderingContext(
  private val surface: Surface,
  override val textureFormat: GPUTextureFormat,
  initialWidth: Int,
  initialHeight: Int,
) : RenderingContext {

  private var currentWidth: Int = initialWidth
  private var currentHeight: Int = initialHeight

  override val width: UInt
    get() = currentWidth.toUInt()

  override val height: UInt
    get() = currentHeight.toUInt()

  override fun getCurrentTexture() = surface.getCurrentTexture().texture

  override fun close() {
    // Surface is managed by WGPUContext
  }

  fun updateSize(w: Int, h: Int) {
    currentWidth = w
    currentHeight = h
  }
}
