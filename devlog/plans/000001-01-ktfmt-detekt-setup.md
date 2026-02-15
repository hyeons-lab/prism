# Plan: Wire KtFmt + Detekt to All Subprojects

> **Note:** This Thinking section was reconstructed from devlog `000001-initial-scaffolding.md` (Sessions 7–9) after the devlog plans convention was established. Future plans will have thinking captured in real time as the plan is developed.

## Thinking

KtFmt and Detekt were both declared in the root `build.gradle.kts` with `apply false`, and AGENTS.md documented `./gradlew ktfmtFormat` and `./gradlew detekt` as if they worked — but running them did nothing. The plugins were never actually applied to any subproject. This is a common gotcha with Gradle convention plugins: declaring them in the root plugins block with `apply false` makes them available for subprojects to apply, but doesn't apply them.

The fix seemed simple at first: add a `subprojects {}` block that applies both plugins. But then I discovered a deeper issue with Detekt specifically.

**KtFmt was straightforward** — applying the plugin and configuring Google style + 100-char width in a `subprojects {}` block worked immediately. Running `./gradlew ktfmtFormat` auto-formatted 113 source files.

**Detekt in KMP was not straightforward.** After applying detekt, `./gradlew detekt` still reported `NO-SOURCE` for all 12 subprojects. Investigation (Session 8) revealed the root cause: the plain `detekt` task looks for sources at `src/main/kotlin` (standard JVM layout), but KMP projects have sources in `src/commonMain/kotlin`, `src/jvmMain/kotlin`, etc. There is no `src/main/kotlin`.

However, detekt auto-creates KMP-specific tasks when applied to a KMP project:
- `detektMetadataCommonMain` — analyzes `src/commonMain/kotlin` (~90% of code)
- `detektJvmMain` — analyzes `src/jvmMain/kotlin` (with type resolution)
- `detektWasmJsMain`, `detektIosArm64Main`, etc. — per-platform

The question became: which of these to wire into the Gradle `check` lifecycle? I decided on `detektMetadataCommonMain`, `detektJvmMain`, and `detektWasmJsMain` — these cover commonMain (~90% of code), JVM-specific code, and WASM-specific code. Platform-specific native tasks were excluded because they'd require native compilation toolchains in CI.

**Detekt config needed careful tuning.** Running `detektMetadataCommonMain` on prism-math found 226 MagicNumber issues — almost all from matrix indices and trig constants. This is a game engine math library; magic numbers are the norm, not the exception. I disabled MagicNumber globally rather than adding 226 suppressions. Generated the full default config with `detektGenerateConfig`, then customized thresholds for the codebase's patterns.

**Detekt jvmTarget was another gotcha.** The project uses JVM 25 for wgpu4k FFI, but detekt's embedded Kotlin compiler only supports up to JVM 22. Setting `jvmTarget = "22"` on all detekt tasks resolved this — detekt doesn't need FFI features, it just needs to parse the code.

---

## Plan

### Context

KtFmt and Detekt plugins are declared in the root `build.gradle.kts` with `apply false` but never applied to any subproject. Running `./gradlew ktfmtFormat` does nothing. AGENTS.md references these tasks as if they work. Detekt has the same problem, compounded by KMP-specific task naming.

### Approach

Add a `subprojects {}` block to the root `build.gradle.kts` that applies both KtFmt and Detekt to every subproject, with Google style / 100-char width configuration for KtFmt. For Detekt, wire KMP-specific tasks into the `check` lifecycle and create a tuned config file.

### Files

| File | Action |
|------|--------|
| `build.gradle.kts` | **EDIT** — add `subprojects {}` block applying ktfmt + detekt, configure detekt jvmTarget=22, wire KMP detekt tasks into `check` |
| `detekt.yml` | **NEW** — generated from defaults, then customized (MagicNumber disabled, thresholds raised) |
| Various source files | **EDIT** — fix detekt issues (MatchingDeclarationName renames, LongMethod suppressions, MaxLineLength fixes) |

### Changes

#### `build.gradle.kts`

After the `plugins {}` block, add:

```kotlin
subprojects {
    apply(plugin = "com.ncorti.ktfmt.gradle")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<com.ncorti.ktfmt.gradle.KtfmtExtension> {
        googleStyle()
        maxWidth.set(100)
    }

    // Wire KMP-specific detekt tasks into check lifecycle
    // Plain `detekt` task is NO-SOURCE in KMP projects
    tasks.matching { it.name in setOf("detektMetadataCommonMain", "detektJvmMain", "detektWasmJsMain") }
        .configureEach { tasks.named("check").configure { dependsOn(this@configureEach) } }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "22" // detekt's embedded compiler max; project uses JVM 25 for FFI
    }
}
```

#### `detekt.yml`

Generate from defaults, then customize:
- Disable MagicNumber globally (226 issues in math lib, too noisy for game engine)
- Raise thresholds: TooManyFunctions(25), LongParameterList(8/10), LongMethod(100), CyclomaticComplexMethod(20)
- Exclude compose/demo from FunctionNaming
- Disable ForbiddenComment
- Set MaxLineLength to 100 (matches ktfmt)

### Verification

1. `./gradlew ktfmtCheck` — runs across all subprojects, reports formatting issues
2. `./gradlew ktfmtFormat` — auto-formats all source files
3. `./gradlew detektJvmMain` — runs real static analysis (not NO-SOURCE)
4. `./gradlew build` — compiles successfully after formatting
