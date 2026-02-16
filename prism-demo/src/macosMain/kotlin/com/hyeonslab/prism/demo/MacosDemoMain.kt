@file:OptIn(ExperimentalForeignApi::class, BetaInteropApi::class)

package com.hyeonslab.prism.demo

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.math.MathUtils
import com.hyeonslab.prism.widget.createPrismSurface
import glfw.glfwPollEvents
import glfw.glfwShowWindow
import glfw.glfwWindowShouldClose
import kotlin.concurrent.Volatile
import kotlinx.cinterop.BetaInteropApi
import kotlinx.cinterop.ExperimentalForeignApi
import kotlinx.cinterop.ObjCAction
import kotlinx.coroutines.runBlocking
import platform.AppKit.NSBackingStoreBuffered
import platform.AppKit.NSBezelStyleRounded
import platform.AppKit.NSButton
import platform.AppKit.NSPanel
import platform.AppKit.NSSlider
import platform.AppKit.NSTextField
import platform.AppKit.NSWindowStyleMaskClosable
import platform.AppKit.NSWindowStyleMaskTitled
import platform.Foundation.NSMakeRect
import platform.Foundation.NSSelectorFromString
import platform.QuartzCore.CACurrentMediaTime
import platform.darwin.NSObject

private val log = Logger.withTag("PrismMacOS")

// Shared mutable state between GLFW render loop and AppKit controls.
// Both run on the main thread (glfwPollEvents dispatches AppKit events on macOS),
// but @Volatile is added defensively in case of future refactoring.
@Volatile private var rotationSpeedDegrees = 45f
@Volatile private var isPaused = false

fun main() = runBlocking {
  log.i { "Starting Prism macOS Native Demo..." }

  val surface = createPrismSurface(width = 800, height = 600, title = "Prism macOS Demo")
  val wgpuContext = checkNotNull(surface.wgpuContext) { "wgpu context not available" }
  val windowHandler = checkNotNull(surface.windowHandler) { "GLFW window not available" }
  val scene = createDemoScene(wgpuContext, width = 800, height = 600)

  val controlsHandler = ControlsHandler()
  val controlsPanel = createControlsPanel(controlsHandler)

  glfwShowWindow(windowHandler)
  controlsPanel.orderFront(null)
  log.i { "Window opened — entering render loop" }

  var lastFrameTime = CACurrentMediaTime()
  var frameCount = 0L
  var currentAngle = 0f
  var totalElapsed = 0f

  while (glfwWindowShouldClose(windowHandler) == 0) {
    glfwPollEvents()

    val now = CACurrentMediaTime()
    val deltaTime = (now - lastFrameTime).toFloat()
    lastFrameTime = now
    frameCount++

    if (!isPaused) {
      currentAngle += MathUtils.toRadians(rotationSpeedDegrees) * deltaTime
      totalElapsed += deltaTime
    }

    scene.tickWithAngle(
      deltaTime = if (isPaused) 0f else deltaTime,
      elapsed = totalElapsed,
      frameCount = frameCount,
      angle = currentAngle,
    )
  }

  log.i { "Shutting down..." }
  scene.shutdown()
  surface.detach()
}

private fun createControlsPanel(handler: ControlsHandler): NSPanel {
  val panel =
    NSPanel(
      contentRect = NSMakeRect(820.0, 200.0, 250.0, 120.0),
      styleMask = NSWindowStyleMaskTitled or NSWindowStyleMaskClosable,
      backing = NSBackingStoreBuffered,
      defer = false,
    )
  panel.title = "Controls"
  panel.setFloatingPanel(true)
  panel.setBecomesKeyOnlyIfNeeded(true)

  val speedLabel = NSTextField(frame = NSMakeRect(10.0, 80.0, 230.0, 20.0))
  speedLabel.stringValue = "Speed: 45\u00B0/s"
  speedLabel.setEditable(false)
  speedLabel.setBordered(false)
  speedLabel.setDrawsBackground(false)
  handler.speedLabel = speedLabel

  val slider = NSSlider(frame = NSMakeRect(10.0, 50.0, 230.0, 24.0))
  slider.minValue = 0.0
  slider.maxValue = 180.0
  slider.doubleValue = 45.0
  slider.setContinuous(true)
  slider.target = handler
  slider.action = NSSelectorFromString("sliderChanged:")

  val button = NSButton(frame = NSMakeRect(10.0, 10.0, 230.0, 32.0))
  button.title = "Pause"
  button.setBezelStyle(NSBezelStyleRounded)
  button.target = handler
  button.action = NSSelectorFromString("togglePause:")
  handler.pauseButton = button

  panel.contentView!!.addSubview(speedLabel)
  panel.contentView!!.addSubview(slider)
  panel.contentView!!.addSubview(button)

  return panel
}

private class ControlsHandler : NSObject() {
  var speedLabel: NSTextField? = null
  var pauseButton: NSButton? = null

  @ObjCAction
  fun sliderChanged(sender: NSSlider) {
    val speed = sender.doubleValue.toFloat()
    rotationSpeedDegrees = speed
    speedLabel?.stringValue = "Speed: ${speed.toInt()}\u00B0/s"
  }

  @ObjCAction
  fun togglePause(sender: NSButton) {
    isPaused = !isPaused
    pauseButton?.title = if (isPaused) "Resume" else "Pause"
  }
}
