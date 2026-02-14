package engine.prism.assets

class AssetHandle<T : Any> internal constructor(
    val id: String,
    val type: String,
) {
    var asset: T? = null
        internal set
    val isLoaded: Boolean get() = asset != null

    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is AssetHandle<*>) return false
        return id == other.id && type == other.type
    }

    override fun hashCode(): Int = 31 * id.hashCode() + type.hashCode()
    override fun toString(): String = "AssetHandle(id=$id, type=$type, loaded=$isLoaded)"
}
