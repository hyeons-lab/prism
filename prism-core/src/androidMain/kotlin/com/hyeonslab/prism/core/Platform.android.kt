package com.hyeonslab.prism.core

actual object Platform {
  actual val name: String = "Android"

  actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
