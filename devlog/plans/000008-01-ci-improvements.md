# Plan: Improve CI workflow for XCFramework and iOS builds

## Thinking

The `apple` job in `.github/workflows/ci.yml` builds the XCFramework and runs iOS simulator tests, but has several gaps that reduce confidence in the pipeline:

1. **Duplicated wgpu4k build logic** — The 6 steps for cloning, caching, building wgpu4k from source are copy-pasted between the `ci` and `apple` jobs. This is a maintenance burden and makes it easy for the two copies to drift.

2. **Unpinned runner** — `macos-latest` can shift between macOS versions (e.g., 14 to 15) without notice, causing subtle breakage in Xcode/Metal-dependent builds.

3. **No macOS native tests** — All 12 modules declare `macosArm64()` but only iOS simulator tests run in CI. macOS native tests are free to run on the same runner.

4. **Only one XCFramework slice verified** — Only `ios-arm64-simulator` header is checked. The `ios-arm64` device slice could be broken silently.

5. **No artifact upload** — The XCFramework is built and verified but not preserved. Developers can't download it from CI runs.

6. **No xcodebuild verification** — The XCFramework is built but never consumed by the actual Xcode project. A broken import or missing symbol wouldn't be caught.

The solution is straightforward: extract a composite action for wgpu4k, pin the runner, expand tests, verify both slices, upload artifacts, and add xcodegen+xcodebuild.

One consideration: `timeout-minutes` is not supported on composite action steps (GitHub Actions runner limitation), but the job-level timeouts (30min ci, 45min apple) provide the safety net.

For the Rust targets input, the composite action accepts an optional `rust-targets` input. When empty (ci job on Linux), `dtolnay/rust-toolchain` installs just the host target. When populated (apple job), it installs iOS cross-compilation targets.

---

## Plan

### 1. Create composite action `.github/actions/setup-wgpu4k/action.yml`

Encapsulates the duplicated wgpu4k build steps:
- Extract version/commit from `gradle/libs.versions.toml`
- Cache `~/.m2/repository/io/ygdrasil` keyed on commit
- Install Rust (if cache miss), with optional `rust-targets` input
- Cache Rust toolchain (if cache miss)
- Build wgpu4k from source (if cache miss)
- Write build metrics + cache diagnostics to job summary

### 2. Replace wgpu4k steps in both CI jobs

**`ci` job**: Replace 6 steps with single `uses: ./.github/actions/setup-wgpu4k`
**`apple` job**: Replace 6 steps with `uses: ./.github/actions/setup-wgpu4k` with `rust-targets: aarch64-apple-ios,aarch64-apple-ios-sim`

### 3. Pin macOS runner to `macos-15`

### 4. Add macOS native tests (`macosArm64Test`) alongside iOS simulator tests

### 5. Verify both XCFramework slices (`ios-arm64` and `ios-arm64-simulator`) with summary table

### 6. Upload XCFramework as artifact (14-day retention)

### 7. Add xcodegen + xcodebuild steps to verify end-to-end iOS app build

### Final `apple` job structure

```
apple:
  runs-on: macos-15
  steps:
    - Checkout
    - Set up JDK 25
    - Set up JDK 21
    - Setup Gradle
    - Get Kotlin version
    - Cache Kotlin/Native
    - Setup wgpu4k (composite action)
    - Apple native tests + XCFramework
    - Verify XCFramework exports (both slices)
    - Upload XCFramework
    - Generate Xcode project
    - Build iOS app
```
