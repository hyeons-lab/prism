# 000001-initial-scaffolding

**Agent:** Claude Code (claude-sonnet-4-5-20250929) @ `prism` branch `main`
**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `main` (from Session 2 onward)
**Agent:** Claude Code (claude-opus-4-6) @ `prism` branch `chore/ktfmt-detekt-subprojects` (Sessions 10-11)

## Intent

Establish project scaffolding (AGENTS.md, devlog system), integrate wgpu4k for GPU rendering, implement a rotating lit cube demo on JVM Desktop via GLFW, wire up ECS-driven rendering, set up CI with GitHub Actions, add code quality tooling (KtFmt + Detekt), and add unit tests for prism-renderer.

## What Changed

- **2026-02-14T10:05-08:00** `AGENTS.md` — Created comprehensive project guidance document covering architecture, build commands, KMP conventions, code quality standards, tech stack, and contribution guidelines. Later updated with wgpu4k setup section, devlog requirements, detekt commands, and CI quality check command.
- **2026-02-14T10:15-08:00** `CLAUDE.md` — Created top-level instructions file referencing AGENTS.md.
- **2026-02-14T10:36-08:00** `devlog/README.md` — Created devlog conventions. Later rewritten with branch-scoped naming.
- **2026-02-14T10:50-08:00** `gradle.properties`, `settings.gradle.kts`, `gradle/libs.versions.toml` — Build system setup: added KMP properties, `mavenLocal()` for wgpu4k, wgpu4k 0.2.0-SNAPSHOT deps. Later added `wgpu4kCommit` for pinning, `kotlin.incremental.native=true`.
- **2026-02-14T10:53-08:00** `prism-renderer/build.gradle.kts`, `prism-core/build.gradle.kts`, `prism-{ecs,assets,native-widgets}/build.gradle.kts` — Added wgpu4k deps, `-Xexpect-actual-classes` compiler flag.
- **2026-02-14T10:58-08:00** `prism-core/.../Engine.kt`, `prism-ecs/.../World.kt` — Changed `subsystems`/`entities` from `private` to `@PublishedApi internal` for inline function access.
- **2026-02-14T11:00-08:00** `prism-core/.../Platform.{wasmJs,macos,linux,mingw}.kt` — Created native Platform actual implementations. Fixed `js()` call, added error handling for Linux/MinGW.
- **2026-02-14T11:03-08:00** `prism-renderer/.../RenderSurface.{macos,linux,mingw}.kt` — Created native RenderSurface stubs.
- **2026-02-14T11:10-08:00** `prism-renderer/.../WgpuRenderer.kt` — Created full Renderer implementation using wgpu4k. Later added uniform buffers, bind groups, surface.present(), per-frame AutoClosableContext, `surfacePreConfigured` param, `onResize` callback.
- **2026-02-14T11:30-08:00** `.gitignore` — Added build artifacts. Later refined `*.dylib` to specific `**/libWGPU*.{dylib,so,dll}` patterns.
- **2026-02-14T14:10-08:00** `prism-renderer/.../Shaders.kt` — WGSL shader sources with vertex + fragment shaders, uniform layout.
- **2026-02-14T14:25-08:00** `prism-demo/build.gradle.kts` — Added mainClass `GlfwMainKt`, wgpu4k deps, JVM args, `jvmToolchain(25)`.
- **2026-02-14T14:30-08:00** `prism-demo/.../GlfwMain.kt` — GLFW windowed demo. Initially raw wgpu4k API, then rewrote to Engine + ECS with camera entity, cube entity, rotation via TransformComponent.
- **2026-02-14T17:09-08:00** `prism-ecs/.../CameraComponent.kt` — New data class wrapping Camera for ECS queries.
- **2026-02-14T17:09-08:00** `prism-ecs/.../RenderSystem.kt` — Implemented `initialize()` (shader module + pipeline) and `update()` (camera query, mesh iteration, draw).
- **2026-02-14T17:24-08:00** `README.md`, `LICENSE` — Public-facing README + Apache 2.0 license. Later added PBR + glTF milestones.
- **2026-02-14T17:47-08:00** `build.gradle.kts` — Added `subprojects {}` block applying KtFmt (Google style, 100-char) and Detekt to all subprojects. Configured detekt with `detekt.yml`, jvmTarget 22, wired KMP tasks into `check`.
- **2026-02-14T18:10-08:00** `detekt.yml` — Generated from defaults, disabled MagicNumber, tuned thresholds for TooManyFunctions, LongParameterList, LongMethod.
- **2026-02-14T18:12-08:00** `RenderPass.kt` → `RenderPassDescriptor.kt`, `Surface.kt` → `RenderSurface.kt` — Renamed to match MatchingDeclarationName rule.
- **2026-02-14T19:30-08:00** `.github/workflows/ci.yml` — CI workflow with JDK 25, wgpu4k Maven cache, ktfmtCheck, detekt, jvmTest. Later improved with shallow fetch, conditional Rust install, combined Gradle invocations, commit-based cache key.
- **2026-02-14T20:30-08:00** `gradle/libs.versions.toml` — Reverted detekt from `2.0.0-alpha.2` to `1.23.8`.
- **2026-02-14T20:39-08:00** Package rename `engine.prism` → `com.hyeonslab.prism` across 121 files.
- **2026-02-14T21:30-08:00** `prism-renderer/src/commonTest/kotlin/.../{ColorTest,MeshTest,VertexLayoutTest,CameraTest,ShaderTest}.kt` — 95 unit tests for renderer data classes and factories.

## Decisions

- **2026-02-14T10:30-08:00** **Use `devlog/` not `.devlog/`** — Visible and version-controlled, not hidden.
- **2026-02-14T10:52-08:00** **wgpu4k from source** — Built 0.2.0-SNAPSHOT from source to Maven local; 0.1.1 had different API.
- **2026-02-14T11:10-08:00** **Import aliases for name collisions** — `import ... as WGPUColor` etc. to avoid clashes with Prism types.
- **2026-02-14T14:00-08:00** **Direct GLFW for initial demo** — Simpler than Compose for M2.
- **2026-02-14T14:15-08:00** **Uniform buffer layout** — 128 bytes: VP mat4 at offset 0, model mat4 at offset 64. Material color in separate 16-byte buffer.
- **2026-02-14T16:30-08:00** **JVM toolchain 25 required** — wgpu4k inline functions compiled at JVM target 25.
- **2026-02-14T17:09-08:00** **RenderSystem owns pipeline creation** — Shader module and pipeline created in `RenderSystem.initialize()`.
- **2026-02-14T17:24-08:00** **Apache 2.0 license** — Consistent with wgpu4k. Copyright: Hyeons' Lab.
- **2026-02-14T17:47-08:00** **Apply KtFmt + Detekt via `subprojects {}` block** — Single block applies both plugins to all 12 subprojects.
- **2026-02-14T18:10-08:00** **Disable MagicNumber globally** — 226/248 detekt issues were MagicNumber in math library. Too noisy for a game engine.
- **2026-02-14T18:10-08:00** **jvmTarget 22 for detekt** — Detekt's embedded compiler max is 22; project uses JVM 25 for wgpu4k FFI.
- **2026-02-14T19:30-08:00** **Single CI job** — All checks in one job to avoid duplicating wgpu4k setup. JVM-only to avoid native compilation issues.
- **2026-02-14T21:10-08:00** **Pass CI step outputs via env, not `${{ }}`** — Avoids shell injection risk.
- **2026-02-14T21:30-08:00** **Kotest matchers throughout tests** — `shouldBe`, `shouldContain`, `plusOrMinus`. No `kotlin.test` assertions (only `@Test`).
- **2026-02-14T21:30-08:00** **No GPU tests** — All tests are pure logic. WgpuRenderer tested manually via demo.

## Research & Discoveries

- **wgpu4k**: Kotlin Multiplatform WebGPU bindings. Package is `io.ygdrasil.webgpu` (not `io.ygdrasil.wgpu`). Key patterns: `WGPUContext`, `.bind()` within `AutoClosableContext`, `ArrayBuffer.of(floatArray)` for GPU buffer uploads.
- **PanamaPort**: enables Project Panama FFI on Android 8.0+ — https://github.com/AnomalousEngine/PanamaPort
- **wgpu4k JVM toolchain requirement**: compiled with JVM target 25; modules using inline functions must match.
- **`ffi.LibraryLoader.load()`** must be called explicitly before any wgpu API usage on JVM.
- **wgpu4k render loop**: `autoClosableContext { }` per frame, `.bind()` on ephemeral resources, `surface.present()` after the context block.
- **`glfwContextRenderer()`** does NOT configure the surface — must call `surface.configure()` separately.
- **Detekt + KMP**: plain `detekt` task is NO-SOURCE in KMP. Use `detektMetadataCommonMain` and `detektJvmMain` explicitly.
- **wgpu4k repo**: moved from `AskiaAI/wgpu4k` to `wgpu4k/wgpu4k`.

## Issues

- **wgpu4k 0.2.0.b1 not available** — resolved by building from source.
- **`invalid texture` panic** — JVM target mismatch. Fix: `jvmToolchain(25)`.
- **`UnsatisfiedLinkError: wgpuCreateInstance`** — Native library not auto-loaded. Fix: `LibraryLoader.load()`.
- **`UnsupportedClassVersionError`** — Gradle JavaExec using JDK 21. Fix: Java 25 toolchain launcher.
- **Detekt effectively disabled** — Plain `detekt` task is NO-SOURCE in KMP. Fixed by wiring `detektMetadata*Main` and `detektJvmMain` into `check`.
- **`Invalid value (25) passed to --jvm-target`** — Detekt doesn't support JVM 25. Fixed: `jvmTarget = "22"`.
- **BUILD_STATUS.md was stale** — Listed 8 modules as broken when all compiled fine.
- **Build failed: detekt `2.0.0-alpha.2` not found** — Reverted to `1.23.8`.

## Lessons Learned

- `@PublishedApi internal` pattern needed for collections accessed by inline reified functions.
- Detekt rule categories matter: `FunctionNaming` is under `naming`, not `style`.
- Detekt jvmTarget max is 22 as of detekt 1.23.x.
- `shouldContain` from `io.kotest.matchers.string` gives better failure messages than `(x.contains(y)) shouldBe true`.
- Tests that re-derive the implementation are tautological — test observable behavior instead.

## Commits

- `634b171` — docs: add project documentation and session tracking system
- `13af90f` — chore: add build artifacts to .gitignore and update devlog
- `ab2f818` — docs: emphasize proactive devlog updates in AGENTS.md
- `8b873e1` — feat: implement WgpuRenderer with native platform support
- `a3b2fb4` — build: add JVM toolchain 25 to prism-demo module
- `59619b4` — fix: address critical review issues
- `fb339ba` — docs: add README and Apache 2.0 LICENSE
- `f28a84f` — docs: add PBR and glTF milestones to project plan
- `47bcc46` — chore: wire KtFmt and Detekt plugins to all subprojects
- `47cc7d0` — ci: add GitHub Actions workflow for CI checks
- `8b7c355` — fix(ci): update wgpu4k repo URL to wgpu4k/wgpu4k
- `97dbdb1` — refactor: rename package from engine.prism to com.hyeonslab.prism
