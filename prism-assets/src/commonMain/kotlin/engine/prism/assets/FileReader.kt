package engine.prism.assets

expect object FileReader {
    suspend fun readBytes(path: String): ByteArray
}
