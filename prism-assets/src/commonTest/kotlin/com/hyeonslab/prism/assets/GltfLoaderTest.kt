package com.hyeonslab.prism.assets

import io.kotest.matchers.floats.plusOrMinus
import io.kotest.matchers.shouldBe
import io.kotest.matchers.shouldNotBe
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.test.Test
import kotlinx.coroutines.test.runTest

class GltfLoaderTest {

  private val loader = GltfLoader()

  // ===== Triangle mesh (3 vertices, no indices, POSITION only) =====

  @Test
  fun load_triangleGltf_producesOneMeshWithThreeVertices() = runTest {
    val gltf = triangleGltf()
    val asset = loader.load("test.gltf", gltf.encodeToByteArray())
    asset.renderableNodes.size shouldBe 1
    val mesh = asset.renderableNodes[0].mesh
    // 3 vertices × 12 floats (positionNormalUvTangent)
    mesh.vertices.size shouldBe 36
    // positions
    mesh.vertices[0] shouldBe (0f plusOrMinus 1e-6f)
    mesh.vertices[1] shouldBe (0f plusOrMinus 1e-6f)
    mesh.vertices[2] shouldBe (0f plusOrMinus 1e-6f)
    mesh.vertices[12] shouldBe (1f plusOrMinus 1e-6f) // vertex 1 x
    mesh.vertices[24] shouldBe (0.5f plusOrMinus 1e-6f) // vertex 2 x
  }

  @Test
  fun load_triangleGltf_hasNoIndices() = runTest {
    val asset = loader.load("test.gltf", triangleGltf().encodeToByteArray())
    asset.renderableNodes[0].mesh.indices.size shouldBe 0
  }

  // ===== Material import =====

  @Test
  fun load_materialWithBaseColor_mapsToMaterialBaseColor() = runTest {
    val gltf = triangleGltfWithMaterial(baseColorFactor = "[1.0, 0.0, 0.0, 1.0]")
    val asset = loader.load("test.gltf", gltf.encodeToByteArray())
    val mat = asset.renderableNodes[0].material
    mat shouldNotBe null
    mat!!.baseColor.r shouldBe (1f plusOrMinus 1e-6f)
    mat.baseColor.g shouldBe (0f plusOrMinus 1e-6f)
    mat.baseColor.b shouldBe (0f plusOrMinus 1e-6f)
  }

  @Test
  fun load_materialWithMetallicRoughness_mapsCorrectly() = runTest {
    val gltf = triangleGltfWithMaterial(metallic = 0.8f, roughness = 0.3f)
    val asset = loader.load("test.gltf", gltf.encodeToByteArray())
    val mat = asset.renderableNodes[0].material
    mat shouldNotBe null
    mat!!.metallic shouldBe (0.8f plusOrMinus 1e-6f)
    mat.roughness shouldBe (0.3f plusOrMinus 1e-6f)
  }

  @Test
  fun load_noMaterial_nodeHasNullMaterial() = runTest {
    val asset = loader.load("test.gltf", triangleGltf().encodeToByteArray())
    // triangleGltf() has no material on the primitive
    asset.renderableNodes[0].material shouldBe null
  }

  // ===== Transform =====

  @Test
  fun load_nodeWithTranslation_appliesTranslation() = runTest {
    val gltf = triangleGltfWithTranslation(tx = 1f, ty = 2f, tz = 3f)
    val asset = loader.load("test.gltf", gltf.encodeToByteArray())
    val transform = asset.renderableNodes[0].worldTransform
    transform.position.x shouldBe (1f plusOrMinus 1e-5f)
    transform.position.y shouldBe (2f plusOrMinus 1e-5f)
    transform.position.z shouldBe (3f plusOrMinus 1e-5f)
  }

  // ===== GLB =====

  @Test
  fun load_glbWithEmbeddedBuffer_parsesSuccessfully() = runTest {
    val glb = buildGlb(triangleGltfGlbJson(), trianglePositionBuffer())
    val asset = loader.load("test.glb", glb)
    asset.renderableNodes.size shouldBe 1
    asset.renderableNodes[0].mesh.vertices.size shouldBe 36
  }

  // ===== Helpers =====

  /** Triangle with 3 positions encoded as a data URI in a .gltf file. */
  @OptIn(ExperimentalEncodingApi::class)
  private fun triangleGltf(): String {
    val positions = trianglePositionBuffer()
    val b64 = Base64.encode(positions)
    return """
      {
        "asset": {"version": "2.0"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"mesh": 0}],
        "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
        "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"}],
        "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": 36}],
        "buffers": [{"uri": "data:application/octet-stream;base64,$b64", "byteLength": 36}]
      }
    """
      .trimIndent()
  }

  @OptIn(ExperimentalEncodingApi::class)
  private fun triangleGltfWithMaterial(
    baseColorFactor: String = "[1.0, 1.0, 1.0, 1.0]",
    metallic: Float = 0.0f,
    roughness: Float = 0.5f,
  ): String {
    val positions = trianglePositionBuffer()
    val b64 = Base64.encode(positions)
    return """
      {
        "asset": {"version": "2.0"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"mesh": 0}],
        "meshes": [{"primitives": [{"attributes": {"POSITION": 0}, "material": 0}]}],
        "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"}],
        "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": 36}],
        "buffers": [{"uri": "data:application/octet-stream;base64,$b64", "byteLength": 36}],
        "materials": [{
          "pbrMetallicRoughness": {
            "baseColorFactor": $baseColorFactor,
            "metallicFactor": $metallic,
            "roughnessFactor": $roughness
          }
        }]
      }
    """
      .trimIndent()
  }

  @OptIn(ExperimentalEncodingApi::class)
  private fun triangleGltfWithTranslation(tx: Float, ty: Float, tz: Float): String {
    val positions = trianglePositionBuffer()
    val b64 = Base64.encode(positions)
    return """
      {
        "asset": {"version": "2.0"},
        "scene": 0,
        "scenes": [{"nodes": [0]}],
        "nodes": [{"mesh": 0, "translation": [$tx, $ty, $tz]}],
        "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
        "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"}],
        "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": 36}],
        "buffers": [{"uri": "data:application/octet-stream;base64,$b64", "byteLength": 36}]
      }
    """
      .trimIndent()
  }

  /** JSON for use as GLB JSON chunk (buffer has no URI — uses BIN chunk). */
  private fun triangleGltfGlbJson(): String =
    """
    {
      "asset": {"version": "2.0"},
      "scene": 0,
      "scenes": [{"nodes": [0]}],
      "nodes": [{"mesh": 0}],
      "meshes": [{"primitives": [{"attributes": {"POSITION": 0}}]}],
      "accessors": [{"bufferView": 0, "componentType": 5126, "count": 3, "type": "VEC3"}],
      "bufferViews": [{"buffer": 0, "byteOffset": 0, "byteLength": 36}],
      "buffers": [{"byteLength": 36}]
    }
    """
      .trimIndent()

  /** 3 positions as floats (little-endian): (0,0,0), (1,0,0), (0.5,1,0). 3 × 3 × 4 = 36 bytes. */
  private fun trianglePositionBuffer(): ByteArray {
    val floats = floatArrayOf(0f, 0f, 0f, 1f, 0f, 0f, 0.5f, 1f, 0f)
    val bytes = ByteArray(floats.size * 4)
    for ((i, f) in floats.withIndex()) {
      val bits = f.toBits()
      bytes[i * 4 + 0] = (bits and 0xFF).toByte()
      bytes[i * 4 + 1] = ((bits shr 8) and 0xFF).toByte()
      bytes[i * 4 + 2] = ((bits shr 16) and 0xFF).toByte()
      bytes[i * 4 + 3] = ((bits shr 24) and 0xFF).toByte()
    }
    return bytes
  }

  private fun buildGlb(json: String, bin: ByteArray): ByteArray {
    val jsonBytes = json.encodeToByteArray()
    val jsonPadded = padTo4(jsonBytes, 0x20)
    val binPadded = padTo4(bin, 0x00)
    val total = 12 + 8 + jsonPadded.size + 8 + binPadded.size
    val out = ByteArray(total)
    var off = 0
    writeI32LE(out, off, 0x46546C67)
    off += 4 // magic
    writeI32LE(out, off, 2)
    off += 4 // version
    writeI32LE(out, off, total)
    off += 4 // length
    writeI32LE(out, off, jsonPadded.size)
    off += 4
    writeI32LE(out, off, 0x4E4F534A)
    off += 4 // JSON
    jsonPadded.copyInto(out, off)
    off += jsonPadded.size
    writeI32LE(out, off, binPadded.size)
    off += 4
    writeI32LE(out, off, 0x004E4942)
    off += 4 // BIN
    binPadded.copyInto(out, off)
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
