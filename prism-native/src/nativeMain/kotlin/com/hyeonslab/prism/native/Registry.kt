package com.hyeonslab.prism.native

import co.touchlab.kermit.Logger
import kotlinx.atomicfu.atomic
import kotlinx.atomicfu.update

/**
 * Opaque-handle registry: maps Long IDs to Kotlin objects for the C API surface.
 *
 * NOTE: [get] and [remove] are not atomic with each other. A caller that checks [contains] and then
 * calls [get] or [remove] in a separate step may observe stale state if another thread concurrently
 * removes the same handle. The C API is currently single-threaded per engine, so this is not a
 * concern in practice, but callers must not rely on TOCTOU-safe semantics.
 */
internal object Registry {
  private val nextId = atomic(0L)
  private val map = atomic(mapOf<Long, Any>())

  fun <T : Any> put(obj: T): Long {
    val id = nextId.incrementAndGet()
    map.update { it + (id to obj) }
    return id
  }

  /** Returns true if [id] is currently registered. Does not log. */
  fun contains(id: Long): Boolean = map.value.containsKey(id)

  @Suppress("UNCHECKED_CAST")
  fun <T : Any> get(id: Long): T? {
    val obj = map.value[id] as? T
    // Handle 0 is the sentinel "not yet initialized" value used by C callers before the first
    // create call. Logging a warning for it would flood the console during normal startup.
    if (obj == null && id != 0L) {
      Logger.w("Registry") { "Object not found for handle: $id" }
    }
    return obj
  }

  fun remove(id: Long) {
    map.update { it - id }
  }
}
