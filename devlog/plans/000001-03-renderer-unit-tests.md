# Plan: Add Unit Tests for prism-renderer

> **Note:** This Thinking section was reconstructed from devlog `000001-initial-scaffolding.md` (Session 13) and the original plan artifact after the devlog plans convention was established. Future plans will have thinking captured in real time as the plan is developed.

## Thinking

The prism-renderer module had 13 source files (461+ lines in WgpuRenderer alone) with zero tests. Session 4's critical review flagged this as major issue #5. The question was: what's actually testable here?

**WgpuRenderer itself is NOT testable without a GPU.** It creates GPU resources, submits command buffers, and presents to a surface — all operations that require a live WebGPU device. So that's off the table for unit tests. It gets tested manually via the demo app.

**But most of the module is pure data classes and logic.** Looking at what's in prism-renderer:
- `Color` — data class with constants and `fromRgba8()` conversion (pure math)
- `Mesh` — factory methods (`triangle()`, `quad()`, `cube()`) that build vertex/index arrays (pure data)
- `VertexLayout` — factory methods for attribute layouts (pure data)
- `Camera` — view/projection matrix computation (pure math)
- `Shader` — data classes and constants (pure data)

All of these are testable in commonTest without any GPU.

**Test style decision:** The project already had 75 tests in prism-math using `kotlin.test.@Test` + Kotest matchers (`shouldBe`, `shouldContain`, `plusOrMinus`). I followed the same pattern for consistency.

**What NOT to test:** I initially included tests for data class equality/copy and enum entry counts, but these are testing Kotlin language guarantees, not application logic. Removed them as low-value.

**Tautological test pitfall:** The first draft of the Camera VP test was `viewProjectionMatrix() shouldBe projectionMatrix() * viewMatrix()` — but that's just re-deriving the implementation. If the implementation is wrong, the test passes anyway. Replaced with behavioral tests: VP differs from view-only and projection-only, moving the camera changes VP.

**Normal offset derivation:** The first draft of Mesh normal tests used hardcoded array indices (`vertices[base + 3]`), which assumed position-first layout. Changed to derive the normal offset from layout metadata: `attributes.first { it.name == "normal" }.offset`. More robust if layout order ever changes.

---

## Plan

### Context

The prism-renderer module has 13 source files with zero tests. Session 4's critical review flagged this. Most renderer types are pure data classes or contain pure logic testable without a GPU.

### Scope

**5 test files** in `prism-renderer/src/commonTest/kotlin/com/hyeonslab/prism/renderer/`:

#### 1. `ColorTest.kt`
- Constants have correct RGBA values (WHITE, BLACK, RED, GREEN, BLUE, YELLOW, CYAN, MAGENTA, TRANSPARENT, CORNFLOWER_BLUE)
- `fromRgba8()` conversion (0→0f, 255→1f, mid-range values, default alpha)
- Data class equality and copy

#### 2. `MeshTest.kt`
- `Mesh.triangle()`: 3 vertices, 3 indices, isIndexed=true, label="Triangle", layout is positionNormalUv (stride 32)
- `Mesh.quad()`: 4 vertices, 6 indices, isIndexed=true, label="Quad"
- `Mesh.cube()`: 24 vertices, 36 indices, isIndexed=true, label="Cube", 6 faces x 4 verts, 6 faces x 6 indices
- `vertexCount` calculation: vertices.size / (stride / 4)
- Empty mesh: vertexCount=0, indexCount=0, isIndexed=false
- Cube normals: each face's 4 vertices share the same unit normal pointing outward

#### 3. `VertexLayoutTest.kt`
- `positionOnly()`: stride=12, 1 attribute (FLOAT3 at offset 0)
- `positionColor()`: stride=28, 2 attributes (FLOAT3@0, FLOAT4@12)
- `positionNormalUv()`: stride=32, 3 attributes (FLOAT3@0, FLOAT3@12, FLOAT2@24)
- `positionNormalUvTangent()`: stride=48, 4 attributes
- Attribute offsets are contiguous (last attribute offset + size == stride)

#### 4. `CameraTest.kt`
- Default camera values
- `viewMatrix()` produces valid matrix (not identity, determinant != 0)
- `projectionMatrix()` perspective vs orthographic differ
- `viewProjectionMatrix()` equals `projectionMatrix() * viewMatrix()`
- Camera position change affects VP

#### 5. `ShaderTest.kt`
- `Shaders.UNIFORMS_SIZE` == 128L
- `Shaders.MATERIAL_UNIFORMS_SIZE` == 16L
- Vertex/fragment shader properties (stage, entryPoint, code content)
- ShaderSource default entryPoint, ShaderModule construction

### Conventions (matching prism-math tests)
- Framework: `kotlin.test.Test` + `io.kotest.matchers.shouldBe`
- Derive offsets from layout metadata, not hardcoded indices
- No GPU tests — all pure logic
- No tests for Kotlin language guarantees (data class equality, enum counts)

### Verification

```bash
./gradlew :prism-renderer:jvmTest    # run the new tests
./gradlew ktfmtFormat                # auto-format
./gradlew ktfmtCheck detektJvmMain   # lint check
```
