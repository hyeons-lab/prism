package engine.prism.assets

import engine.prism.core.Engine
import engine.prism.core.Subsystem
import engine.prism.core.Time
import kotlinx.coroutines.sync.Mutex
import kotlinx.coroutines.sync.withLock

class AssetManager : Subsystem {
    override val name: String = "AssetManager"

    private val assets = mutableMapOf<String, AssetHandle<*>>()
    private val loaders = mutableMapOf<String, AssetLoader<*>>()
    private val mutex = Mutex()

    fun <T : Any> registerLoader(loader: AssetLoader<T>) {
        for (ext in loader.supportedExtensions) {
            loaders[ext.lowercase()] = loader
        }
    }

    @Suppress("UNCHECKED_CAST")
    fun <T : Any> getHandle(id: String, type: String): AssetHandle<T> {
        return assets.getOrPut(id) { AssetHandle<T>(id, type) } as AssetHandle<T>
    }

    @Suppress("UNCHECKED_CAST")
    suspend fun <T : Any> load(path: String, type: String): AssetHandle<T> {
        val handle = getHandle<T>(path, type)
        if (handle.isLoaded) return handle

        val ext = path.substringAfterLast('.').lowercase()
        val loader = loaders[ext] as? AssetLoader<T>
            ?: error("No loader registered for extension: $ext")

        val data = FileReader.readBytes(path)
        val asset = loader.load(path, data)
        mutex.withLock {
            handle.asset = asset
        }
        return handle
    }

    fun unload(id: String) {
        assets.remove(id)
    }

    fun unloadAll() {
        assets.clear()
    }

    override fun initialize(engine: Engine) {
        registerLoader(TextureLoader())
        registerLoader(MeshLoader())
        registerLoader(ShaderLoader())
    }

    override fun update(time: Time) {
        // Process async loading queue if needed
    }

    override fun shutdown() {
        unloadAll()
    }
}
