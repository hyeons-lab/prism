package engine.prism.assets

actual object FileReader {
  actual suspend fun readBytes(path: String): ByteArray {
    return Thread.currentThread().contextClassLoader?.getResourceAsStream(path)?.readBytes()
      ?: java.io.File(path).readBytes()
  }
}
