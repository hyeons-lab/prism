package com.hyeonslab.prism.assets

import co.touchlab.kermit.Logger
import com.hyeonslab.prism.math.Mat4
import com.hyeonslab.prism.math.Quaternion
import com.hyeonslab.prism.math.Transform
import com.hyeonslab.prism.math.Vec3
import com.hyeonslab.prism.renderer.Color
import com.hyeonslab.prism.renderer.Material
import com.hyeonslab.prism.renderer.Mesh
import com.hyeonslab.prism.renderer.Texture
import com.hyeonslab.prism.renderer.TextureDescriptor
import com.hyeonslab.prism.renderer.TextureFilter
import com.hyeonslab.prism.renderer.TextureFormat
import com.hyeonslab.prism.renderer.TextureWrap
import com.hyeonslab.prism.renderer.VertexLayout
import kotlin.io.encoding.Base64
import kotlin.io.encoding.ExperimentalEncodingApi
import kotlin.math.sqrt
import kotlinx.serialization.json.Json

/** Loads glTF 2.0 (.gltf / .glb) files into [GltfAsset]. */
class GltfLoader : AssetLoader<GltfAsset> {
  override val supportedExtensions: List<String> = listOf("gltf", "glb")

  override suspend fun load(path: String, data: ByteArray): GltfAsset {
    val (json, bin) =
      if (GlbReader.isGlb(data)) {
        val glb = GlbReader.read(data)
        Pair(glb.json, glb.bin)
      } else {
        Pair(data.decodeToString(), null)
      }

    val doc = jsonParser.decodeFromString<GltfDocument>(json)
    val basePath = path.substringBeforeLast('/').takeIf { '/' in path } ?: ""
    val buffers = resolveBuffers(doc, basePath, bin)
    val imageDataList = decodeImages(doc, buffers, basePath)
    val textures = buildTextures(doc, imageDataList)
    val materials = buildMaterials(doc, textures)
    val renderableNodes = traverseDefaultScene(doc, buffers, materials)
    return GltfAsset(textures, imageDataList, renderableNodes)
  }

  // ===== Buffer resolution =====

  private suspend fun resolveBuffers(
    doc: GltfDocument,
    basePath: String,
    bin: ByteArray?,
  ): List<ByteArray?> =
    (doc.buffers ?: emptyList()).mapIndexed { index, buffer ->
      when {
        buffer.uri == null -> {
          // GLB BIN chunk is always buffer index 0
          if (index == 0) bin
          else {
            Logger.w(TAG) { "GLB buffer $index has no URI and no BIN chunk" }
            null
          }
        }
        buffer.uri.startsWith("data:") -> decodeDataUri(buffer.uri)
        else -> {
          val fullPath = if (basePath.isNotEmpty()) "$basePath/${buffer.uri}" else buffer.uri
          try {
            FileReader.readBytes(fullPath)
          } catch (e: Exception) {
            Logger.w(TAG) { "Failed to read buffer '$fullPath': ${e.message}" }
            null
          }
        }
      }
    }

  // ===== Image decoding =====

  private suspend fun decodeImages(
    doc: GltfDocument,
    buffers: List<ByteArray?>,
    basePath: String,
  ): List<ImageData?> =
    (doc.images ?: emptyList()).map { image ->
      val bytes: ByteArray? =
        when {
          image.uri != null && image.uri.startsWith("data:") -> decodeDataUri(image.uri)
          image.bufferView != null -> {
            val bv = doc.bufferViews?.getOrNull(image.bufferView)
            val buf = bv?.let { buffers.getOrNull(it.buffer) }
            buf?.copyOfRange(bv.byteOffset, (bv.byteOffset + bv.byteLength).coerceAtMost(buf.size))
          }
          image.uri != null -> {
            val fullPath = if (basePath.isNotEmpty()) "$basePath/${image.uri}" else image.uri
            try {
              FileReader.readBytes(fullPath)
            } catch (e: Exception) {
              Logger.w(TAG) { "Failed to read image '${image.uri}': ${e.message}" }
              null
            }
          }
          else -> null
        }
      bytes?.let {
        try {
          ImageDecoder.decode(it)
        } catch (e: Exception) {
          Logger.w(TAG) { "Failed to decode image: ${e.message}" }
          null
        }
      }
    }

  // ===== Texture building =====

  private fun buildTextures(doc: GltfDocument, imageDataList: List<ImageData?>): List<Texture> =
    (doc.textures ?: emptyList()).map { gltfTex ->
      val imgData = gltfTex.source?.let { imageDataList.getOrNull(it) }
      val sampler = gltfTex.sampler?.let { doc.samplers?.getOrNull(it) }
      val img = gltfTex.source?.let { doc.images?.getOrNull(it) }

      val filter =
        when (sampler?.magFilter) {
          FILTER_NEAREST -> TextureFilter.NEAREST
          else -> TextureFilter.LINEAR
        }
      val wrapU =
        when (sampler?.wrapS ?: WRAP_REPEAT) {
          WRAP_CLAMP -> TextureWrap.CLAMP_TO_EDGE
          WRAP_MIRRORED -> TextureWrap.MIRRORED_REPEAT
          else -> TextureWrap.REPEAT
        }
      val wrapV =
        when (sampler?.wrapT ?: WRAP_REPEAT) {
          WRAP_CLAMP -> TextureWrap.CLAMP_TO_EDGE
          WRAP_MIRRORED -> TextureWrap.MIRRORED_REPEAT
          else -> TextureWrap.REPEAT
        }

      Texture(
        TextureDescriptor(
          width = imgData?.width ?: 1,
          height = imgData?.height ?: 1,
          format = TextureFormat.RGBA8_SRGB,
          minFilter = filter,
          magFilter = filter,
          wrapU = wrapU,
          wrapV = wrapV,
          label = img?.name ?: gltfTex.name ?: "",
        )
      )
    }

  // ===== Material building =====

  private fun buildMaterials(doc: GltfDocument, textures: List<Texture>): List<Material> =
    (doc.materials ?: emptyList()).map { mat ->
      val pbr = mat.pbrMetallicRoughness
      val baseColor =
        pbr?.baseColorFactor?.let { f ->
          if (f.size >= 4) Color(f[0], f[1], f[2], f[3]) else Color.WHITE
        } ?: Color.WHITE
      val emissive =
        mat.emissiveFactor?.let { f -> if (f.size >= 3) Color(f[0], f[1], f[2]) else Color.BLACK }
          ?: Color.BLACK

      Material(
        baseColor = baseColor,
        metallic = pbr?.metallicFactor ?: 1.0f,
        roughness = pbr?.roughnessFactor ?: 1.0f,
        emissive = emissive,
        occlusionStrength = mat.occlusionTexture?.strength ?: 1.0f,
        albedoTexture = pbr?.baseColorTexture?.index?.let { textures.getOrNull(it) },
        normalTexture = mat.normalTexture?.index?.let { textures.getOrNull(it) },
        metallicRoughnessTexture =
          pbr?.metallicRoughnessTexture?.index?.let { textures.getOrNull(it) },
        occlusionTexture = mat.occlusionTexture?.index?.let { textures.getOrNull(it) },
        emissiveTexture = mat.emissiveTexture?.index?.let { textures.getOrNull(it) },
        label = mat.name ?: "",
      )
    }

  // ===== Scene traversal =====

  private fun traverseDefaultScene(
    doc: GltfDocument,
    buffers: List<ByteArray?>,
    materials: List<Material>,
  ): List<GltfNodeData> {
    val nodes = doc.nodes ?: return emptyList()
    val sceneIdx = doc.scene ?: 0
    val scene = doc.scenes?.getOrNull(sceneIdx) ?: return emptyList()
    val result = mutableListOf<GltfNodeData>()

    fun traverse(nodeIdx: Int, parentWorld: Mat4) {
      val node = nodes.getOrNull(nodeIdx) ?: return
      val local = nodeLocalTransform(node)
      val world = parentWorld * local

      node.mesh?.let { meshIdx ->
        val gltfMesh = doc.meshes?.getOrNull(meshIdx) ?: return@let
        for ((primIdx, prim) in gltfMesh.primitives.withIndex()) {
          val mesh =
            buildPrimitiveMesh(prim, doc, buffers, gltfMesh.name ?: "mesh${meshIdx}_prim$primIdx")
              ?: continue
          val material = prim.material?.let { materials.getOrNull(it) }
          result.add(GltfNodeData(node.name, decomposeMatrix(world), mesh, material))
        }
      }

      for (childIdx in node.children ?: emptyList()) {
        traverse(childIdx, world)
      }
    }

    for (rootIdx in scene.nodes ?: emptyList()) {
      traverse(rootIdx, Mat4.identity())
    }
    return result
  }

  // ===== Mesh building =====

  private fun buildPrimitiveMesh(
    prim: GltfPrimitive,
    doc: GltfDocument,
    buffers: List<ByteArray?>,
    label: String,
  ): Mesh? {
    val posIdx =
      prim.attributes["POSITION"]
        ?: run {
          Logger.w(TAG) { "Primitive '$label' has no POSITION attribute" }
          return null
        }
    val positions = readFloats(doc, buffers, posIdx) ?: return null
    val vertexCount = positions.size / 3

    val normals = prim.attributes["NORMAL"]?.let { readFloats(doc, buffers, it) }
    val uvs = prim.attributes["TEXCOORD_0"]?.let { readFloats(doc, buffers, it) }
    val tangents = prim.attributes["TANGENT"]?.let { readFloats(doc, buffers, it) }

    // positionNormalUvTangent: [px,py,pz, nx,ny,nz, u,v, tx,ty,tz,tw] = 12 floats/vertex
    val floatsPerVertex = FLOATS_PER_VERTEX
    val vertices = FloatArray(vertexCount * floatsPerVertex)

    for (i in 0 until vertexCount) {
      val base = i * floatsPerVertex
      // Position (3)
      vertices[base + 0] = positions[i * 3 + 0]
      vertices[base + 1] = positions[i * 3 + 1]
      vertices[base + 2] = positions[i * 3 + 2]
      // Normal (3) — zero if missing
      if (normals != null && normals.size > i * 3 + 2) {
        vertices[base + 3] = normals[i * 3 + 0]
        vertices[base + 4] = normals[i * 3 + 1]
        vertices[base + 5] = normals[i * 3 + 2]
      }
      // UV (2) — zero if missing
      if (uvs != null && uvs.size > i * 2 + 1) {
        vertices[base + 6] = uvs[i * 2 + 0]
        vertices[base + 7] = uvs[i * 2 + 1]
      }
      // Tangent (4) — (0,0,1,1) if missing
      if (tangents != null && tangents.size > i * 4 + 3) {
        vertices[base + 8] = tangents[i * 4 + 0]
        vertices[base + 9] = tangents[i * 4 + 1]
        vertices[base + 10] = tangents[i * 4 + 2]
        vertices[base + 11] = tangents[i * 4 + 3]
      } else {
        vertices[base + 10] = 1f // z
        vertices[base + 11] = 1f // w
      }
    }

    val indices = prim.indices?.let { readIndices(doc, buffers, it) } ?: IntArray(0)

    return Mesh(vertexLayout = VertexLayout.positionNormalUvTangent(), label = label).apply {
      this.vertices = vertices
      this.indices = indices
    }
  }

  // ===== Accessor reading =====

  private fun readFloats(
    doc: GltfDocument,
    buffers: List<ByteArray?>,
    accessorIdx: Int,
  ): FloatArray? {
    val acc = doc.accessors?.getOrNull(accessorIdx) ?: return null
    val componentCount = typeComponentCount(acc.type)

    val bvIdx = acc.bufferView
    if (bvIdx == null) {
      // Accessor with no buffer view: return zeros (sparse accessor not supported)
      return FloatArray(acc.count * componentCount)
    }

    val bv = doc.bufferViews?.getOrNull(bvIdx) ?: return null
    val buf = buffers.getOrNull(bv.buffer) ?: return null

    val tightStride = componentCount * BYTES_PER_FLOAT
    val stride = if ((bv.byteStride ?: 0) > 0) bv.byteStride!! else tightStride
    val baseOffset = bv.byteOffset + acc.byteOffset

    return FloatArray(acc.count * componentCount) { idx ->
      val element = idx / componentCount
      val component = idx % componentCount
      readF32LE(buf, baseOffset + element * stride + component * BYTES_PER_FLOAT)
    }
  }

  private fun readIndices(
    doc: GltfDocument,
    buffers: List<ByteArray?>,
    accessorIdx: Int,
  ): IntArray? {
    val acc = doc.accessors?.getOrNull(accessorIdx) ?: return null
    val bvIdx = acc.bufferView ?: return null
    val bv = doc.bufferViews?.getOrNull(bvIdx) ?: return null
    val buf = buffers.getOrNull(bv.buffer) ?: return null

    val componentBytes =
      when (acc.componentType) {
        COMP_UINT -> 4
        COMP_USHORT -> 2
        COMP_UBYTE -> 1
        else -> return null
      }
    val stride = if ((bv.byteStride ?: 0) > 0) bv.byteStride!! else componentBytes
    val baseOffset = bv.byteOffset + acc.byteOffset

    return IntArray(acc.count) { i ->
      val offset = baseOffset + i * stride
      when (acc.componentType) {
        COMP_UINT -> readI32LE(buf, offset)
        COMP_USHORT -> readU16LE(buf, offset)
        COMP_UBYTE -> buf[offset].toInt() and 0xFF
        else -> 0
      }
    }
  }

  // ===== Transform helpers =====

  private fun nodeLocalTransform(node: GltfNode): Mat4 {
    if (node.matrix != null && node.matrix.size == 16) {
      return Mat4(node.matrix.toFloatArray())
    }
    val t =
      node.translation?.let { v ->
        if (v.size >= 3) Mat4.translation(Vec3(v[0], v[1], v[2])) else Mat4.identity()
      } ?: Mat4.identity()
    val r =
      node.rotation?.let { q ->
        if (q.size >= 4) Quaternion(q[0], q[1], q[2], q[3]).toMat4() else Mat4.identity()
      } ?: Mat4.identity()
    val s =
      node.scale?.let { v ->
        if (v.size >= 3) Mat4.scale(Vec3(v[0], v[1], v[2])) else Mat4.identity()
      } ?: Mat4.identity()
    return t * r * s
  }

  /** Decomposes a column-major Mat4 into Translation + Rotation (quaternion) + Scale. */
  private fun decomposeMatrix(m: Mat4): Transform {
    val d = m.data
    // Translation (column 3)
    val tx = d[12]
    val ty = d[13]
    val tz = d[14]
    // Scale = magnitude of each column's upper-3 elements
    val sx = sqrt(d[0] * d[0] + d[1] * d[1] + d[2] * d[2])
    val sy = sqrt(d[4] * d[4] + d[5] * d[5] + d[6] * d[6])
    val sz = sqrt(d[8] * d[8] + d[9] * d[9] + d[10] * d[10])
    // Normalize columns to get pure rotation matrix
    val r00 = if (sx > 0f) d[0] / sx else 0f
    val r10 = if (sx > 0f) d[1] / sx else 0f
    val r20 = if (sx > 0f) d[2] / sx else 0f
    val r01 = if (sy > 0f) d[4] / sy else 0f
    val r11 = if (sy > 0f) d[5] / sy else 0f
    val r21 = if (sy > 0f) d[6] / sy else 0f
    val r02 = if (sz > 0f) d[8] / sz else 0f
    val r12 = if (sz > 0f) d[9] / sz else 0f
    val r22 = if (sz > 0f) d[10] / sz else 0f
    // Shepperd's method: rotation matrix → quaternion
    val trace = r00 + r11 + r22
    val quat =
      if (trace > 0f) {
        val s = 0.5f / sqrt(trace + 1f)
        Quaternion((r21 - r12) * s, (r02 - r20) * s, (r10 - r01) * s, 0.25f / s)
      } else if (r00 > r11 && r00 > r22) {
        val s = 2f * sqrt(1f + r00 - r11 - r22)
        Quaternion(0.25f * s, (r01 + r10) / s, (r02 + r20) / s, (r21 - r12) / s)
      } else if (r11 > r22) {
        val s = 2f * sqrt(1f + r11 - r00 - r22)
        Quaternion((r01 + r10) / s, 0.25f * s, (r12 + r21) / s, (r02 - r20) / s)
      } else {
        val s = 2f * sqrt(1f + r22 - r00 - r11)
        Quaternion((r02 + r20) / s, (r12 + r21) / s, 0.25f * s, (r10 - r01) / s)
      }
    return Transform(Vec3(tx, ty, tz), quat.normalize(), Vec3(sx, sy, sz))
  }

  // ===== Low-level byte utilities =====

  private fun readF32LE(data: ByteArray, offset: Int): Float {
    val bits =
      (data[offset].toInt() and 0xFF) or
        ((data[offset + 1].toInt() and 0xFF) shl 8) or
        ((data[offset + 2].toInt() and 0xFF) shl 16) or
        ((data[offset + 3].toInt() and 0xFF) shl 24)
    return Float.fromBits(bits)
  }

  private fun readI32LE(data: ByteArray, offset: Int): Int =
    (data[offset].toInt() and 0xFF) or
      ((data[offset + 1].toInt() and 0xFF) shl 8) or
      ((data[offset + 2].toInt() and 0xFF) shl 16) or
      ((data[offset + 3].toInt() and 0xFF) shl 24)

  private fun readU16LE(data: ByteArray, offset: Int): Int =
    (data[offset].toInt() and 0xFF) or ((data[offset + 1].toInt() and 0xFF) shl 8)

  @OptIn(ExperimentalEncodingApi::class)
  private fun decodeDataUri(uri: String): ByteArray {
    val comma = uri.indexOf(',')
    require(comma >= 0) { "Invalid data URI: missing comma" }
    return Base64.decode(uri.substring(comma + 1))
  }

  // ===== Helpers =====

  private fun typeComponentCount(type: String): Int =
    when (type) {
      "SCALAR" -> 1
      "VEC2" -> 2
      "VEC3" -> 3
      "VEC4" -> 4
      "MAT2" -> 4
      "MAT3" -> 9
      "MAT4" -> 16
      else -> 1
    }

  companion object {
    private const val TAG = "GltfLoader"
    private const val FLOATS_PER_VERTEX = 12 // positionNormalUvTangent
    private const val BYTES_PER_FLOAT = 4
    private const val COMP_UBYTE = 5121
    private const val COMP_USHORT = 5123
    private const val COMP_UINT = 5125
    private const val FILTER_NEAREST = 9728
    private const val WRAP_REPEAT = 10497
    private const val WRAP_CLAMP = 33071
    private const val WRAP_MIRRORED = 33648

    private val jsonParser = Json { ignoreUnknownKeys = true }
  }
}
