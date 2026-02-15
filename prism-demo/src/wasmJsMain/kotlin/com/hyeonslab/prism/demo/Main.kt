package com.hyeonslab.prism.demo

import androidx.compose.ui.ExperimentalComposeUiApi
import androidx.compose.ui.window.CanvasBasedWindow

@OptIn(ExperimentalComposeUiApi::class)
fun main() {
  CanvasBasedWindow(canvasElementId = "prismCanvas", title = "Prism Demo") { ComposeDemoApp() }
}
