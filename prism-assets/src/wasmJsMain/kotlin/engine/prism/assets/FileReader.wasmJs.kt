package engine.prism.assets

actual object FileReader {
    actual suspend fun readBytes(path: String): ByteArray {
        TODO("WASM file reading not yet implemented")
    }
}
