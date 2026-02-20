package engine.prism.native

/** Opaque-handle registry: maps Long IDs to Kotlin objects for the C API surface. */
internal object Registry {
    private var nextId = 0L
    private val map = mutableMapOf<Long, Any>()

    fun <T : Any> put(obj: T): Long {
        val id = ++nextId
        map[id] = obj
        return id
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(id: Long): T? = map[id] as? T

    fun remove(id: Long) {
        map.remove(id)
    }
}
