# 000013 — chore/ci-prebuilt-wgpu4k

## Agent
Claude Code (claude-opus-4-6) @ repository:prism branch:chore/ci-prebuilt-wgpu4k

## Intent
Eliminate the 12-15 minute CI penalty on cache misses by storing pre-built wgpu4k Maven local artifacts as GitHub Release assets. Cache misses should drop to ~30s (download + extract) instead of 12-15 min (build from source).

## What Changed
- 2026-02-18 `.github/workflows/build-wgpu4k-deps.yml` — New workflow to build wgpu4k deps and upload as GitHub Release assets
- 2026-02-18 `.github/actions/setup-wgpu4k/action.yml` — Added download-from-release step between cache check and build-from-source

## Decisions
- 2026-02-18 Use GitHub Releases (not artifacts) for storage — releases are durable (no 7-day eviction), free for public repos, and accessible via `gh release download`
- 2026-02-18 Use zstd compression for tarballs — better compression ratio and speed than gzip
- 2026-02-18 Keep existing cache + build-from-source as fallback — the release download is a new middle layer, not a replacement

## Progress
- [x] Create devlog and plan files
- [x] Create `build-wgpu4k-deps.yml` workflow
- [x] Modify `setup-wgpu4k/action.yml` with release download step
- [ ] Commit and push
- [ ] Create draft PR
- [ ] Test workflow manually
