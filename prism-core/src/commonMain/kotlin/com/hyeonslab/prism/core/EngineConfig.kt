package com.hyeonslab.prism.core

data class EngineConfig(
  val appName: String = "Prism App",
  val targetFps: Int = 60,
  val fixedTimeStep: Float = 1f / 60f,
  val enableDebug: Boolean = false,
)
