## Thinking

PR #27 (`feat/github-pages`) removed `:prism-demo:assemblePrismDemoDebugXCFramework` from the
Apple CI job. The `prism-ios-demo` Xcode project depends on it (`prism-ios-demo/project.yml`
references `prism-demo/build/XCFrameworks/debug/PrismDemo.xcframework`), so xcodebuild fails.
The intent was to stop uploading the artifact, not stop building it.

## Plan

1. Add `:prism-demo:assemblePrismDemoDebugXCFramework` back to the Gradle command in
   `.github/workflows/ci.yml` (line 122), alongside `:prism-ios:assemblePrismDebugXCFramework`.
2. Amend existing commit and force-push to let CI re-run on PR #27.
3. Verify CI Apple Targets job passes (xcodebuild finds PrismDemo.xcframework).
4. Confirm PrismDemo XCFramework is NOT in uploaded artifacts (only Prism is uploaded).
