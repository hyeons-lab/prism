package engine.prism.core

expect object Platform {
  val name: String

  fun currentTimeMillis(): Long
}
