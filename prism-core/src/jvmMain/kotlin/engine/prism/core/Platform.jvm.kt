package engine.prism.core

actual object Platform {
  actual val name: String = "JVM"

  actual fun currentTimeMillis(): Long = System.currentTimeMillis()
}
