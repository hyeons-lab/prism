# 000016-01 Create `prism-web` module

## Thinking

`prism-demo-core` currently owns the wasmJs executable target — but it's a cross-platform KMP
library. Mixing a library and a runnable binary in one module is an anti-pattern: it forces every
platform (iOS, Android, JVM) to carry webpack configuration and a web entry point that they don't
use. Separate modules keep concerns clean.

Three options were considered:
- **A)** Keep demo-core as-is — no change, perpetuates the anti-pattern
- **B)** Strip wasmJs entirely from demo-core and add it elsewhere — breaks web CI
- **C)** Create `prism-web` as a dedicated web app module — clean separation

Option C is chosen: `prism-web` is a standalone wasmJs application that calls engine APIs
directly (no dependency on `prism-demo-core`). This is intentionally duplicative for now; as the
engine matures, `prism-web` becomes the reference for direct engine usage.

Scene creation code is copied from `prism-demo-core` with the package renamed to
`com.hyeonslab.prism.web`. All demo scene logic uses only engine module APIs
(prism-renderer, prism-ecs, prism-assets, etc.), so the copy is a pure package rename.

## Plan

1. `settings.gradle.kts` — add `include(":prism-web")`

2. `prism-web/build.gradle.kts` — new file:
   - `id("prism-quality")` + `alias(libs.plugins.kotlin.multiplatform)`
   - `wasmJs { browser { outputFileName = "prism-web.js" } binaries.executable() }`
   - `wasmJsMain.dependencies`: engine modules (core, renderer, ecs, assets, math,
     native-widgets + wgpu4k, kermit, coroutines)
   - `compilerOptions { allWarningsAsErrors.set(true) }`
   - `tasks.register<Sync>("syncWasmDocs")` — depends on `wasmJsBrowserDistribution`,
     syncs `dist/wasmJs/productionExecutable` → `docs/wasm`, preserves `pbr.html`

3. `prism-web/src/wasmJsMain/kotlin/com/hyeonslab/prism/web/` — new source files:
   - `Main.kt` — copy of demo-core's wasmJs Main.kt, package renamed
   - `DemoScene.kt` — copy of demo-core's DemoScene.kt, package renamed
   - `CornellBoxScene.kt` — copy, package renamed
   - `MaterialPresetScene.kt` — copy, package renamed
   - `GltfDemoScene.kt` — copy, package renamed; calls `uploadDecodedImage` from same package
   - `TextureUploadHelper.kt` — wasmJs implementation as plain `internal fun` (no expect/actual)

4. `prism-web/src/wasmJsMain/resources/` — move from `prism-demo-core`:
   - `DamagedHelmet.glb` (git mv)
   - `index.html` (git mv, update script src to `prism-web.js`)

5. `prism-demo-core/build.gradle.kts` — change wasmJs block:
   - `@OptIn(...) wasmJs { browser() }` (library only, no executable, no webpack config)

6. `prism-demo-core/src/wasmJsMain/kotlin/.../Main.kt` — delete (moved to prism-web)

7. `docs/wasm/pbr.html` — update `<script src="prism-demo-core.js">` → `prism-web.js`

8. Build + sync: `./gradlew :prism-web:syncWasmDocs`

9. Format + validate: `./gradlew ktfmtFormat ktfmtCheck :prism-web:detektWasmJsMain jvmTest`

10. Commit, push, create PR
