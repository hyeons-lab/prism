package engine.prism.js

/** Opaque-handle registry: maps String IDs to Kotlin objects. Thread-hostile; wasmJs is single-threaded. */
internal object Registry {
    private var nextId = 0L
    private val map = mutableMapOf<String, Any>()

    fun <T : Any> put(obj: T): String {
        val id = (++nextId).toString()
        map[id] = obj
        return id
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> get(id: String): T? = map[id] as? T

    fun remove(id: String) {
        map.remove(id)
    }
}
