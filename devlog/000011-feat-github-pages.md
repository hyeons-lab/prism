Agent: Claude Code (claude-opus-4-6) @ prism branch feat/github-pages

## Intent

Add a GitHub Pages landing site for the Prism engine. Fix CI regression where PrismDemo
XCFramework build was accidentally removed from the Apple CI job.

## What Changed

- 2026-02-16 `.github/workflows/ci.yml` — restored `:prism-demo:assemblePrismDemoDebugXCFramework` to the Apple CI Gradle command; xcodebuild needs it but only Prism.xcframework is uploaded as artifact

## Decisions

- 2026-02-16 Keep PrismDemo XCFramework build in CI but don't upload it — the Xcode project needs it to compile but it's not distributed separately

## Commits

(pending amend of cdb3c3b)
