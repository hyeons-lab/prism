package com.hyeonslab.prism.assets

import io.kotest.assertions.throwables.shouldThrow
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.test.Test

class GlbReaderTest {

  @Test
  fun isGlb_returnsTrueForGlbMagic() {
    val data = byteArrayOf(0x67, 0x6C, 0x54, 0x46, 0, 0, 0, 0) // "glTF"
    GlbReader.isGlb(data) shouldBe true
  }

  @Test
  fun isGlb_returnsFalseForJson() {
    val data = "{ \"asset\": {} }".encodeToByteArray()
    GlbReader.isGlb(data) shouldBe false
  }

  @Test
  fun read_parsesMinimalGlbWithJsonAndBin() {
    val glb = buildGlb(json = """{"asset":{"version":"2.0"}}""", bin = byteArrayOf(1, 2, 3, 4))
    val result = GlbReader.read(glb)
    result.json shouldBe """{"asset":{"version":"2.0"}}"""
    val bin = result.bin
    bin shouldNotBe null
    bin!![0] shouldBe 1
    bin[1] shouldBe 2
  }

  @Test
  fun read_parsesGlbWithJsonOnly() {
    val glb = buildGlb(json = """{"asset":{"version":"2.0"}}""", bin = null)
    val result = GlbReader.read(glb)
    result.json shouldBe """{"asset":{"version":"2.0"}}"""
    result.bin shouldBe null
  }

  @Test
  fun read_rejectsInvalidMagic() {
    val bad = byteArrayOf(0x00, 0x00, 0x00, 0x00, 2, 0, 0, 0, 12, 0, 0, 0)
    shouldThrow<IllegalArgumentException> { GlbReader.read(bad) }
  }

  @Test
  fun read_rejectsWrongVersion() {
    val glb = buildGlb(json = "{}", bin = null, version = 1)
    shouldThrow<IllegalArgumentException> { GlbReader.read(glb) }
  }

  @Test
  fun read_rejectsNegativeTotalLength() {
    // totalLength = 0x80000000 (MSB set) is negative as signed Int32 → below minimum of 12.
    val bad = ByteArray(12)
    bad[0] = 0x67.toByte()
    bad[1] = 0x6C.toByte()
    bad[2] = 0x54.toByte()
    bad[3] = 0x46.toByte() // magic "glTF"
    bad[4] = 2
    bad[5] = 0
    bad[6] = 0
    bad[7] = 0 // version 2
    // totalLength = 0x80000000 = -2147483648 as signed Int
    bad[8] = 0x00.toByte()
    bad[9] = 0x00.toByte()
    bad[10] = 0x00.toByte()
    bad[11] = 0x80.toByte()
    shouldThrow<IllegalArgumentException> { GlbReader.read(bad) }
  }

  @Test
  fun read_rejectsNegativeChunkLength() {
    // Build a minimal 20-byte buffer with a valid header but chunkLength = -1.
    val out = ByteArray(20)
    writeI32LE(out, 0, 0x46546C67) // magic
    writeI32LE(out, 4, 2) // version
    writeI32LE(out, 8, 20) // totalLength = 20 (valid)
    writeI32LE(out, 12, -1) // chunkLength = 0xFFFFFFFF = -1 (invalid)
    writeI32LE(out, 16, 0x4E4F534A) // chunkType = JSON
    shouldThrow<IllegalArgumentException> { GlbReader.read(out) }
  }

  @Test
  fun read_jsonChunkPaddingWithSpaces_trimmedCorrectly() {
    // JSON chunk padded with trailing spaces (0x20) per spec — must be trimmed exactly.
    val json = """{"asset":{"version":"2.0"}}"""
    val glb = buildGlb(json = json, bin = null)
    val result = GlbReader.read(glb)
    result.json shouldBe json // no trailing spaces in result
  }

  /**
   * Builds a minimal GLB byte array.
   *
   * JSON chunk is padded to 4-byte alignment with spaces. BIN chunk is padded with zeros.
   */
  private fun buildGlb(json: String, bin: ByteArray?, version: Int = 2): ByteArray {
    val jsonBytes = json.encodeToByteArray()
    val jsonPadded = padTo4(jsonBytes, 0x20) // spaces
    val binPadded = bin?.let { padTo4(it, 0x00) } // zeros

    val chunkCount = if (bin != null) 2 else 1
    val jsonChunkLen = 8 + jsonPadded.size
    val binChunkLen = if (binPadded != null) 8 + binPadded.size else 0
    val totalLength = 12 + jsonChunkLen + binChunkLen

    val out = ByteArray(totalLength)
    var offset = 0

    // Header
    writeI32LE(out, offset, 0x46546C67)
    offset += 4 // magic
    writeI32LE(out, offset, version)
    offset += 4
    writeI32LE(out, offset, totalLength)
    offset += 4

    // JSON chunk
    writeI32LE(out, offset, jsonPadded.size)
    offset += 4
    writeI32LE(out, offset, 0x4E4F534A)
    offset += 4 // "JSON"
    jsonPadded.copyInto(out, offset)
    offset += jsonPadded.size

    // BIN chunk
    if (binPadded != null) {
      writeI32LE(out, offset, binPadded.size)
      offset += 4
      writeI32LE(out, offset, 0x004E4942)
      offset += 4 // "BIN\0"
      binPadded.copyInto(out, offset)
    }

    return out
  }

  private fun padTo4(data: ByteArray, pad: Int): ByteArray {
    val rem = data.size % 4
    return if (rem == 0) data else data + ByteArray(4 - rem) { pad.toByte() }
  }

  private fun writeI32LE(buf: ByteArray, offset: Int, value: Int) {
    buf[offset + 0] = (value and 0xFF).toByte()
    buf[offset + 1] = ((value shr 8) and 0xFF).toByte()
    buf[offset + 2] = ((value shr 16) and 0xFF).toByte()
    buf[offset + 3] = ((value shr 24) and 0xFF).toByte()
  }
}
