## Context

Issue #11 requests glTF 2.0 asset loading for prism-assets. The prism-assets module already has a stub `MeshLoader` claiming `.gltf`/`.glb` extensions but returning an empty `Mesh`. This plan replaces that stub with a full parser.

The project is well-positioned for this:
- `Material` already has all 5 glTF PBR texture slots (`albedoTexture`, `normalTexture`, `metallicRoughnessTexture`, `occlusionTexture`, `emissiveTexture`) plus scalar fallbacks.
- `Mesh` uses `FloatArray` vertices + `IntArray` indices with a `positionNormalUvTangent` vertex layout (48 bytes/vertex: FLOAT3 pos + FLOAT3 normal + FLOAT2 uv + FLOAT4 tangent) that maps directly to glTF accessors.
- `kotlinx-serialization-json` is in the version catalog (1.9.0) but not yet applied to prism-assets.
- `FileReader` expect/actual works on JVM/Android/Native; WASM is a TODO.
- ECS has `TransformComponent`, `MeshComponent`, `MaterialComponent` for scene instantiation.

## Plan

### Step 1: Build setup
- Add `alias(libs.plugins.kotlin.serialization)` to `prism-assets/build.gradle.kts` plugins block.
- Add `implementation(libs.kotlinx.serialization.json)` to `commonMain.dependencies`.

### Step 2: `GltfTypes.kt` — internal serializable glTF JSON schema
All types are `internal` (implementation detail of the loader). Covers:
- `GltfDocument` — top-level JSON object (scenes, nodes, meshes, accessors, bufferViews, buffers, materials, textures, images, samplers)
- `GltfAssetMetadata` — the "asset" property (version, generator)
- `GltfScene` — scene node indices
- `GltfNode` — TRS transform or matrix, mesh index, children
- `GltfMesh` — name + list of primitives
- `GltfPrimitive` — attributes map, indices, material index, extensions
- `GltfAccessor` — bufferView, byteOffset, componentType, count, type, min/max
- `GltfBufferView` — buffer, byteOffset, byteLength, byteStride, target
- `GltfBuffer` — byteLength, uri (optional for GLB)
- `GltfMaterial` — pbrMetallicRoughness, normalTexture, occlusionTexture, emissiveTexture, emissiveFactor, extensions
- `GltfPbrMetallicRoughness` — baseColorFactor, baseColorTexture, metallicFactor, roughnessFactor, metallicRoughnessTexture
- `GltfTextureInfo` / `GltfOcclusionTextureInfo` — index, texCoord, scale/strength
- `GltfTexture` — sampler, source indices
- `GltfImage` — uri, bufferView, mimeType
- `GltfSampler` — magFilter, minFilter, wrapS, wrapT
- Use `JsonElement` for `extensions` and `extras` fields (forward-compatible)
- `@SerialName` where field names differ from camelCase

### Step 3: `GlbReader.kt` — GLB binary container parser
GLB format:
```
Header (12 bytes): magic=0x46546C67 ("glTF"), version=2, totalLength
JSON chunk: chunkLength, chunkType=0x4E4F534A ("JSON"), chunkData[chunkLength]
BIN chunk (optional): chunkLength, chunkType=0x004E4942 ("BIN\0"), chunkData[chunkLength]
```
- `GlbReader.read(data: ByteArray): GlbContent` — validates magic, reads chunks
- `data class GlbContent(val json: String, val bin: ByteArray?)` — internal
- All byte reads in little-endian using bit shifting (platform-agnostic, no `ByteBuffer`)

### Step 4: `ImageDecoder.kt` — expect/actual pixel decoder
```kotlin
// commonMain
data class ImageData(val width: Int, val height: Int, val pixels: ByteArray)  // RGBA8 row-major
expect object ImageDecoder {
    fun decode(bytes: ByteArray): ImageData?  // null = unsupported on this platform
}
```
Platform actuals:
- **JVM**: `javax.imageio.ImageIO` → `BufferedImage.getRGB()` → RGBA bytes (R/G/B from int, A from alpha)
- **Android**: `BitmapFactory.decodeByteArray()` → `Bitmap.copyPixelsToBuffer()` (returns RGBA8 by default on API 28+)
- **Native** (iOS/macOS/Linux/MinGW): stub returning `null` (TODO with CGImage/stb_image)
- **WASM**: stub returning `null` (TODO with `createImageBitmap`)

### Step 5: `GltfAsset.kt` — public Prism asset type
```kotlin
class GltfAsset(
    val meshes: List<Mesh>,
    val materials: List<Material>,
    val textures: List<Texture>,
    val imageData: List<ImageData?>,    // parallel to textures, null if decode failed
    val nodes: List<GltfNodeData>,
    val scenes: List<GltfSceneData>,
    val defaultScene: Int,
) {
    // Create ECS entities for all renderable nodes in defaultScene (flattened to world space)
    fun instantiateInWorld(world: World): List<Entity>
}
data class GltfNodeData(val name: String?, val worldTransform: Transform, val meshIndex: Int?, val materialIndex: Int?)
data class GltfSceneData(val name: String?, val rootNodes: List<Int>)
```
The `instantiateInWorld()` traverses the flattened node list and creates entities with `TransformComponent`, `MeshComponent`, `MaterialComponent`. Since Prism-ECS has no parent-child entity relationships, transforms are pre-multiplied to world space during loading.

### Step 6: `GltfLoader.kt` — main orchestrator
Implements `AssetLoader<GltfAsset>`. Key internal logic:

**Buffer resolution:**
- `resolveBuffer(idx)` → `ByteArray`: for GLB, returns the BIN chunk; for embedded data URIs, decodes base64 using `kotlin.io.encoding.Base64`; for external URIs, reads via `FileReader.readBytes(basePath + uri)`

**Accessor reading:**
- `readAccessorFloats(accessor)` → `FloatArray`: handles byteOffset, byteStride (0 = tight), componentType=FLOAT(5126), converts component bytes to Float using little-endian bit shifting
- `readAccessorIndices(accessor)` → `IntArray`: supports UNSIGNED_INT(5125), UNSIGNED_SHORT(5123), UNSIGNED_BYTE(5121)

**Vertex building** for `positionNormalUvTangent` (48 bytes/vertex):
```
offset  0: POSITION   FLOAT3  (12 bytes)
offset 12: NORMAL     FLOAT3  (12 bytes)
offset 24: TEXCOORD_0 FLOAT2  ( 8 bytes)
offset 32: TANGENT    FLOAT4  (16 bytes)
```
Missing attributes are zero-padded.

**Material mapping:**
```
pbrMetallicRoughness.baseColorFactor [RGBA] → baseColor
pbrMetallicRoughness.metallicFactor         → metallic
pbrMetallicRoughness.roughnessFactor        → roughness
emissiveFactor [RGB]                        → emissive (Color with alpha=1)
occlusionTexture.strength                   → occlusionStrength
pbrMetallicRoughness.baseColorTexture.index → albedoTexture
normalTexture.index                         → normalTexture
pbrMetallicRoughness.metallicRoughnessTexture.index → metallicRoughnessTexture
occlusionTexture.index                      → occlusionTexture
emissiveTexture.index                       → emissiveTexture
```

**Texture creation:**
- For each glTF texture: decode image via `ImageDecoder.decode()`, create `Texture(TextureDescriptor(...))`, leave `handle = null` (renderer uploads lazily when mesh is rendered)
- Sampler filters/wraps mapped from glTF filter constants (9728=NEAREST, 9729=LINEAR, 10497=REPEAT, 33071=CLAMP, 33648=MIRRORED)

**Scene hierarchy:** Traverse node tree recursively, accumulating parent transforms (Mat4 multiplication), producing a flat list of `GltfNodeData` with pre-multiplied world transforms.

**Node TRS:** `translation: [tx, ty, tz]`, `rotation: [x, y, z, w]`, `scale: [sx, sy, sz]` → `Transform`. Matrix nodes: decompose 4×4 column-major matrix into T/R/S (or store as Mat4 directly).

### Step 7: Update `MeshLoader` + `AssetManager`
- `MeshLoader.supportedExtensions`: remove `"gltf"`, `"glb"` (keep for future OBJ; or keep as no-op until OBJ is implemented)
- `AssetManager.initialize()`: add `registerLoader(GltfLoader())`

### Step 8: Unit tests (`prism-assets/src/commonTest/`)
- `GlbReaderTest`: build synthetic minimal GLB bytes (header + JSON chunk + BIN chunk), assert parsed correctly
- `GltfLoaderTest`:
  - Minimal glTF JSON with embedded buffer (data URI), single triangle mesh → assert `meshes.size==1`, `meshes[0].vertices` has correct floats
  - Material with `baseColorFactor=[1,0,0,1]` → `Material.baseColor == Color.RED`
  - Node with TRS → correct `Transform`

### Step 9: Demo integration
- Download `BoxTextured.glb` from Khronos glTF-Sample-Assets (MIT license) and add to `prism-demo-core/src/jvmMain/resources/`
- Update `DemoScene` to load the model via `AssetManager` + `GltfAsset.instantiateInWorld(world)` when the engine is initialized
- The existing `RenderSystem` handles rendering without any changes

### Out of scope (follow-up issues)
- KHR_materials_unlit extension
- KHR_texture_transform extension
- Skeletal animation import
- Morph target import
- WASM ImageDecoder (needs browser API interop)
- Native ImageDecoder (needs CGImage or bundled stb_image)
- Linux/Windows native builds (no platform code today)
