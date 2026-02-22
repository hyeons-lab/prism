package engine.prism.native

import co.touchlab.kermit.Logger
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/** Opaque-handle registry: maps Long IDs to Kotlin objects for the C API surface. */
internal object Registry {
  private val nextId = atomic(0L)
  private val map = atomic(mapOf<Long, Any>())

  fun <T : Any> put(obj: T): Long {
    val id = nextId.incrementAndGet()
    map.update { it + (id to obj) }
    return id
  }

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> get(id: Long): T? {
    val obj = map.value[id] as? T
    if (obj == null) {
      Logger.w("Registry") { "Object not found for handle: $id" }
    }
    return obj
  }

  fun remove(id: Long) {
    map.update { it - id }
  }
}
