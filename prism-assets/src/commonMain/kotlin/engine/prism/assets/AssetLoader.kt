package engine.prism.assets

interface AssetLoader<T : Any> {
  val supportedExtensions: List<String>

  suspend fun load(path: String, data: ByteArray): T

  fun unload(asset: T) {}
}
