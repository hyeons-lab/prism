# 000016 — fix/ios-nsdata-read

## Agent
Claude Code (claude-sonnet-4-6) @ prism branch fix/ios-nsdata-read

## Intent
Fix the Apple Targets CI failure: `Unresolved reference 'dataWithContentsOfFile'` in
`IosDemoController.kt`. The previous fix attempt used `NSData.dataWithContentsOfFile()`
which is also wrong — it's not exposed as a companion object method in Kotlin/Native.

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

## Commits
- HEAD — fix: read bundle asset bytes via NSFileManager.defaultManager.contentsAtPath()
