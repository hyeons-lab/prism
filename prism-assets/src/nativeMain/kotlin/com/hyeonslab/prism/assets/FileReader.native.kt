package com.hyeonslab.prism.assets

import kotlinx.io.buffered
import kotlinx.io.files.Path
import kotlinx.io.files.SystemFileSystem
import kotlinx.io.readByteArray

actual object FileReader {
  actual suspend fun readBytes(path: String): ByteArray {
    return SystemFileSystem.source(Path(path)).buffered().use { it.readByteArray() }
  }
}
