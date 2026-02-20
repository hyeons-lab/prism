# 000016 — fix/ios-nsdata-read

## Agent
Claude Code (claude-sonnet-4-6) @ prism branch fix/ios-nsdata-read

## Intent
Fix two bugs that survived PR #41:
1. Apple Targets CI failure: `Unresolved reference 'dataWithContentsOfFile'` in `IosDemoController.kt`
2. Two-finger orbit fix + WASM rebuild were in commit 730f33f which was not included in the PR #41 squash merge, so the live site never got the two-finger drag-to-orbit behavior

## What Changed
- `2026-02-20T10:00-08:00` `prism-demo-core/src/iosMain/kotlin/com/hyeonslab/prism/demo/IosDemoController.kt`
  — Changed `loadBundleAssetBytes()` to use `NSFileManager.defaultManager.contentsAtPath(fullPath)`
    instead of `NSData.dataWithContentsOfFile(fullPath)`. Added `import platform.Foundation.NSFileManager`.

## Decisions
- `2026-02-20T10:00-08:00` Use `NSFileManager.defaultManager.contentsAtPath()` instead of any NSData
  factory method — reasoning below in Issues.

## Issues
- **Root cause:** In Kotlin/Native ObjC interop, ObjC class factory methods whose name starts with
  the lowercase class name (without NS prefix) are mapped as **constructors**, not companion object
  methods. `+ (NSData*)dataWithContentsOfFile:(NSString*)path` starts with "data" = lowercase "Data"
  from "NSData", so it maps to `NSData(contentsOfFile: ...)`, not `NSData.dataWithContentsOfFile(...)`.
  Using `NSData.dataWithContentsOfFile(...)` is always "Unresolved reference".
- **Previous attempted fix:** Changed original `NSData(contentsOfFile = fullPath)` to
  `NSData.dataWithContentsOfFile(fullPath)` — still wrong for the same reason above.
- **Correct fix:** `NSFileManager.defaultManager.contentsAtPath(fullPath)` — an instance method
  on NSFileManager that returns `NSData?` without any NSData constructor shenanigans. This is
  a well-established ObjC/Kotlin-Native pattern for reading file contents.

## Issues (continued)
- **Two-finger fix missing from live site:** PR #41 squash merge captured 4 of 5 commits. Commit
  730f33f (two-finger orbit rewrite + WASM rebuild + pbr.html `pan-x pan-y`) was created after the
  PR was merged, so the live site still has `touch-action: none` and the old single-finger WASM bundle.
  Fixed by adding the two-finger logic and WASM rebuild to this PR.

## Commits
- d0eec32 — fix: read bundle asset bytes via NSFileManager.defaultManager.contentsAtPath()
- HEAD — fix: two-finger orbit on mobile, rebuild WASM (missed from PR #41 squash)
