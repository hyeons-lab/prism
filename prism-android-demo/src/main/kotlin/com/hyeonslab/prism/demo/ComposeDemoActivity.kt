package com.hyeonslab.prism.demo

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge

/**
 * Android Compose demo activity. Displays a rotating lit cube rendered via wgpu4k/Vulkan with
 * Material3 UI controls overlaid. All logic lives in [AndroidComposeDemoContent] (KMP library).
 */
class ComposeDemoActivity : ComponentActivity() {
  override fun onCreate(savedInstanceState: Bundle?) {
    super.onCreate(savedInstanceState)
    enableEdgeToEdge()
    setContent { AndroidComposeDemoContent() }
  }
}
