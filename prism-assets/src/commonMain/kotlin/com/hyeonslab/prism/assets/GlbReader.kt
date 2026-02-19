package com.hyeonslab.prism.assets

internal data class GlbContent(val json: String, val bin: ByteArray?)

internal object GlbReader {
  private const val MAGIC = 0x46546C67 // "glTF" as little-endian uint32
  private const val CHUNK_JSON = 0x4E4F534A // "JSON"
  private const val CHUNK_BIN = 0x004E4942 // "BIN\0"
  private const val GLTF_VERSION = 2

  fun read(data: ByteArray): GlbContent {
    require(data.size >= 12) { "GLB too short: ${data.size} bytes" }

    val magic = readInt32LE(data, 0)
    require(magic == MAGIC) { "Not a GLB file (magic 0x${magic.toString(16)})" }

    val version = readInt32LE(data, 4)
    require(version == GLTF_VERSION) { "Unsupported GLB version: $version" }

    val totalLength = readInt32LE(data, 8)
    require(totalLength >= 12) { "GLB length field invalid: $totalLength" }
    require(totalLength <= data.size) { "GLB length $totalLength > data ${data.size}" }

    var offset = 12
    var json: String? = null
    var bin: ByteArray? = null

    while (offset + 8 <= totalLength) {
      val chunkLength = readInt32LE(data, offset)
      val chunkType = readInt32LE(data, offset + 4)
      offset += 8

      require(chunkLength >= 0) { "Negative chunk length $chunkLength at offset ${offset - 8}" }
      require(offset + chunkLength <= totalLength) {
        "Chunk extends past file end (offset=$offset, len=$chunkLength, total=$totalLength)"
      }

      when (chunkType) {
        // JSON chunk is padded with spaces (0x20 per spec) — trim only spaces, not all whitespace
        CHUNK_JSON -> json = data.decodeToString(offset, offset + chunkLength).trimEnd(' ')
        CHUNK_BIN -> bin = data.copyOfRange(offset, offset + chunkLength)
      // Unknown chunk types are silently skipped per spec
      }

      offset += chunkLength
    }

    return GlbContent(json = requireNotNull(json) { "GLB has no JSON chunk" }, bin = bin)
  }

  fun isGlb(data: ByteArray): Boolean =
    data.size >= 4 &&
      data[0] == 0x67.toByte() && // 'g'
      data[1] == 0x6C.toByte() && // 'l'
      data[2] == 0x54.toByte() && // 'T'
      data[3] == 0x46.toByte() // 'F'

  private fun readInt32LE(data: ByteArray, offset: Int): Int =
    (data[offset].toInt() and 0xFF) or
      ((data[offset + 1].toInt() and 0xFF) shl 8) or
      ((data[offset + 2].toInt() and 0xFF) shl 16) or
      ((data[offset + 3].toInt() and 0xFF) shl 24)
}
