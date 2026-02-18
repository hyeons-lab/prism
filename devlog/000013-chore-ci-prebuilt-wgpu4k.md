# 000013 — chore/ci-prebuilt-wgpu4k

## Agent
Claude Code (claude-opus-4-6) @ repository:prism branch:chore/ci-prebuilt-wgpu4k

## Intent
Eliminate the 12-15 minute CI penalty on cache misses by storing pre-built wgpu4k Maven local artifacts as GitHub Release assets. Cache misses should drop to ~30s (download + extract) instead of 12-15 min (build from source).

## What Changed
- 2026-02-18 `.github/workflows/build-wgpu4k-deps.yml` — New workflow to build wgpu4k deps and upload as GitHub Release assets
- 2026-02-18 `.github/actions/setup-wgpu4k/action.yml` — Added download-from-release step between cache check and build-from-source
- 2026-02-18 `.github/workflows/build-wgpu4k-deps.yml` — Fixed race condition in release creation, replaced eval tar with bash array
- 2026-02-18 `.github/actions/setup-wgpu4k/action.yml` — Improved error handling: capture gh release download errors, only silently fall back on not-found

## Decisions
- 2026-02-18 Use GitHub Releases (not artifacts) for storage — releases are durable (no 7-day eviction), free for public repos, and accessible via `gh release download`
- 2026-02-18 Use zstd compression for tarballs — better compression ratio and speed than gzip
- 2026-02-18 Keep existing cache + build-from-source as fallback — the release download is a new middle layer, not a replacement

## Issues
- 2026-02-18 `workflow_dispatch` can't trigger workflows that only exist on a non-default branch — used temporary push trigger on PR branch, removed after seeding
- 2026-02-18 First build run: macOS job cancelled mid-flight, re-ran with `gh run rerun --failed` to complete it

## Commits
- 64dd71b — chore: store pre-built wgpu4k deps as GitHub Release assets
- d0ee45b — chore: temp trigger for build-wgpu4k-deps on PR branch
- 5c41e5d — chore: remove temporary branch trigger from build-wgpu4k-deps
- 39cd014 — fix: address PR review feedback

## Progress
- [x] Create devlog and plan files
- [x] Create `build-wgpu4k-deps.yml` workflow
- [x] Modify `setup-wgpu4k/action.yml` with release download step
- [x] Commit and push
- [x] Create draft PR (#33)
- [x] Seed initial release (`wgpu4k-deps-v213ff3d2-1b1578a1-0ee61438` — Linux 244MB, macOS 357MB)
- [x] Address PR review feedback (race condition, eval tar, error handling)
- [ ] Verify CI uses release download path on cache miss
