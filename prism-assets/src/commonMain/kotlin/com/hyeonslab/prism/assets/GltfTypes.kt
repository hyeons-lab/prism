package com.hyeonslab.prism.assets

import kotlinx.serialization.SerialName
import kotlinx.serialization.Serializable
import kotlinx.serialization.json.JsonElement

@Serializable
internal data class GltfDocument(
  val asset: GltfAssetMetadata,
  val scene: Int? = null,
  val scenes: List<GltfScene>? = null,
  val nodes: List<GltfNode>? = null,
  val meshes: List<GltfMesh>? = null,
  val accessors: List<GltfAccessor>? = null,
  val bufferViews: List<GltfBufferView>? = null,
  val buffers: List<GltfBuffer>? = null,
  val materials: List<GltfMaterial>? = null,
  val textures: List<GltfTexture>? = null,
  val images: List<GltfImage>? = null,
  val samplers: List<GltfSampler>? = null,
  val extensionsUsed: List<String>? = null,
  val extensions: JsonElement? = null,
  val extras: JsonElement? = null,
)

@Serializable
internal data class GltfAssetMetadata(
  val version: String,
  val generator: String? = null,
  val copyright: String? = null,
  val minVersion: String? = null,
)

@Serializable internal data class GltfScene(val name: String? = null, val nodes: List<Int>? = null)

@Serializable
internal data class GltfNode(
  val name: String? = null,
  val mesh: Int? = null,
  val children: List<Int>? = null,
  // column-major 4x4 matrix; overrides TRS when present
  val matrix: List<Float>? = null,
  val translation: List<Float>? = null,
  val rotation: List<Float>? = null,
  val scale: List<Float>? = null,
  val skin: Int? = null,
  val extensions: JsonElement? = null,
  val extras: JsonElement? = null,
)

@Serializable
internal data class GltfMesh(val name: String? = null, val primitives: List<GltfPrimitive>)

@Serializable
internal data class GltfPrimitive(
  val attributes: Map<String, Int>,
  val indices: Int? = null,
  val material: Int? = null,
  // 4 = TRIANGLES (default)
  val mode: Int? = null,
  val extensions: JsonElement? = null,
  val extras: JsonElement? = null,
)

@Serializable
internal data class GltfAccessor(
  val bufferView: Int? = null,
  val byteOffset: Int = 0,
  // 5120=BYTE, 5121=UNSIGNED_BYTE, 5122=SHORT, 5123=UNSIGNED_SHORT, 5125=UNSIGNED_INT, 5126=FLOAT
  val componentType: Int,
  val count: Int,
  // "SCALAR", "VEC2", "VEC3", "VEC4", "MAT2", "MAT3", "MAT4"
  val type: String,
  val normalized: Boolean = false,
  val min: List<Float>? = null,
  val max: List<Float>? = null,
  val name: String? = null,
)

@Serializable
internal data class GltfBufferView(
  val buffer: Int,
  val byteOffset: Int = 0,
  val byteLength: Int,
  // null or 0 = tightly packed
  val byteStride: Int? = null,
  // 34962=ARRAY_BUFFER, 34963=ELEMENT_ARRAY_BUFFER
  val target: Int? = null,
  val name: String? = null,
)

@Serializable
internal data class GltfBuffer(
  val byteLength: Int,
  // null for GLB BIN chunk (buffer index 0)
  val uri: String? = null,
  val name: String? = null,
)

@Serializable
internal data class GltfMaterial(
  val name: String? = null,
  val pbrMetallicRoughness: GltfPbrMetallicRoughness? = null,
  val normalTexture: GltfNormalTextureInfo? = null,
  val occlusionTexture: GltfOcclusionTextureInfo? = null,
  val emissiveTexture: GltfTextureInfo? = null,
  // [R, G, B], default [0, 0, 0]
  val emissiveFactor: List<Float>? = null,
  val alphaMode: String? = null,
  val alphaCutoff: Float? = null,
  val doubleSided: Boolean = false,
  val extensions: JsonElement? = null,
  val extras: JsonElement? = null,
)

@Serializable
internal data class GltfPbrMetallicRoughness(
  // [R, G, B, A], default [1, 1, 1, 1]
  val baseColorFactor: List<Float>? = null,
  val baseColorTexture: GltfTextureInfo? = null,
  val metallicFactor: Float = 1.0f,
  val roughnessFactor: Float = 1.0f,
  val metallicRoughnessTexture: GltfTextureInfo? = null,
)

@Serializable
internal data class GltfTextureInfo(
  val index: Int,
  @SerialName("texCoord") val texCoord: Int = 0,
  val extensions: JsonElement? = null,
  val extras: JsonElement? = null,
)

@Serializable
internal data class GltfNormalTextureInfo(
  val index: Int,
  @SerialName("texCoord") val texCoord: Int = 0,
  val scale: Float = 1.0f,
  val extensions: JsonElement? = null,
  val extras: JsonElement? = null,
)

@Serializable
internal data class GltfOcclusionTextureInfo(
  val index: Int,
  @SerialName("texCoord") val texCoord: Int = 0,
  val strength: Float = 1.0f,
  val extensions: JsonElement? = null,
  val extras: JsonElement? = null,
)

@Serializable
internal data class GltfTexture(
  val sampler: Int? = null,
  val source: Int? = null,
  val name: String? = null,
  val extensions: JsonElement? = null,
  val extras: JsonElement? = null,
)

@Serializable
internal data class GltfImage(
  val uri: String? = null,
  val mimeType: String? = null,
  val bufferView: Int? = null,
  val name: String? = null,
)

@Serializable
internal data class GltfSampler(
  // 9728=NEAREST, 9729=LINEAR (and mipmap variants)
  val magFilter: Int? = null,
  val minFilter: Int? = null,
  // 10497=REPEAT, 33071=CLAMP_TO_EDGE, 33648=MIRRORED_REPEAT
  val wrapS: Int = 10497,
  val wrapT: Int = 10497,
  val name: String? = null,
)
