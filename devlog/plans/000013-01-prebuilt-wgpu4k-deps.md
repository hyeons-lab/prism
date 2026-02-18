## Context

The "Setup wgpu4k" step in CI takes 12-15 minutes on cache misses because it clones and builds 3 repositories from source (webgpu-ktypes, wgpu4k-native, wgpu4k). The `actions/cache` layer helps on repeat runs, but caches evict after 7 days of inactivity or when the 10GB repo limit is hit. When that happens (or when version pins change), both the Linux and macOS jobs rebuild everything from scratch.

Goal: Store pre-built Maven local artifacts as durable GitHub Release assets so cache misses drop to ~30s (download + extract) instead of 12-15 min (build from source).

## Plan

### 1. New workflow: `.github/workflows/build-wgpu4k-deps.yml`

- Triggers: `workflow_dispatch` + `push` to `main` when `gradle/libs.versions.toml` changes
- Matrix: `[ubuntu-latest, macos-15]`
- Steps: read version pins, compute hash, check if release asset exists, build if not, tar+zstd, upload to GitHub Release
- Release tag: `wgpu4k-deps-v{hash}` where hash = `{ktypes:8}-{native:8}-{wgpu4k:8}`
- Asset names: `wgpu4k-deps-Linux.tar.zst`, `wgpu4k-deps-macOS.tar.zst`

### 2. Modify `.github/actions/setup-wgpu4k/action.yml`

Add download-from-release step between cache check and build-from-source:

1. Read version pins + compute cache key (unchanged)
2. `actions/cache` restore (unchanged)
3. **New: if miss, try `gh release download` from `wgpu4k-deps-v{hash}`**
4. If download fails, build from source (unchanged fallback)

### 3. Verification

- Trigger `build-wgpu4k-deps` manually, verify release created with both OS tarballs
- Delete `actions/cache` entries, trigger CI, verify download-from-release path used
- Verify existing cache path still works on subsequent runs
