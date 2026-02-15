package com.hyeonslab.prism.assets

actual object FileReader {
  actual suspend fun readBytes(path: String): ByteArray {
    TODO("iOS file reading not yet implemented")
  }
}
