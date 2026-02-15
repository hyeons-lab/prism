# Plan: Add Maven Central Publishing to Prism Modules

> **Note:** This Thinking section was reconstructed from devlog `000001-initial-scaffolding.md` (Session 11) and the original plan artifact after the devlog plans convention was established. Future plans will have thinking captured in real time as the plan is developed.

## Thinking

The project needed Maven Central publishing so consumers could depend on Prism modules without building from source. Several decisions needed to be made:

**Group ID: `com.hyeons-lab` vs keeping `engine.prism`.** The user had already verified the `com.hyeons-lab` namespace on Sonatype Central Portal, so this was settled. But there was a wrinkle: Maven group IDs allow hyphens (`com.hyeons-lab`) while Kotlin/Java package names don't. So the package becomes `com.hyeonslab.prism.*` (hyphen removed) while the Maven coordinate stays `com.hyeons-lab:prism-*`.

**Package rename scope was large** — 102 Kotlin files across all 12 modules needed `engine.prism.*` → `com.hyeonslab.prism.*` in both package declarations and imports, plus directory restructuring. This was the riskiest part of the change since a single missed rename would break compilation.

**Publishing plugin choice: vanniktech/gradle-maven-publish-plugin** was the obvious choice for KMP. It handles all the KMP publication variants automatically (JVM, JS, native targets) and integrates cleanly with the Sonatype Central Portal API. Version 0.36.0 was current.

**Convention plugin via buildSrc** — rather than repeating publishing config in 10 module build files, a `prism-publishing.gradle.kts` convention plugin in `buildSrc` centralizes everything. Each module just applies `id("prism-publishing")` and sets its description.

**Which modules to publish:** All 10 library modules. Excluded `prism-demo` (app, not library) and `prism-flutter` (not functional yet).

**Snapshot vs release routing** — The convention plugin needed to detect `-SNAPSHOT` in `VERSION_NAME` and route to the snapshot repository (`https://central.sonatype.com/repository/maven-snapshots/`) vs the standard Central Portal release path. This is handled by vanniktech's plugin when configured correctly.

**During implementation (Session 11),** the detekt version had been changed to `2.0.0-alpha.2` in the working tree by a previous session, which broke the build because that alpha version wasn't on the Gradle Plugin Portal. Had to revert to `1.23.8` before the package rename commit could go through.

---

## Plan

### Context

Prism has 10 library modules and 2 app modules with no publishing configuration. The user has verified the `com.hyeons-lab` namespace on Sonatype Central Portal and wants to publish all library modules as `com.hyeons-lab:prism-*` artifacts.

**Coordinate mapping:**
- Maven group ID: `com.hyeons-lab` (hyphen valid in Maven coordinates)
- Kotlin packages: `com.hyeonslab.prism.*` (hyphen removed — invalid in Kotlin/Java package names)
- Current packages: `engine.prism.*` → must be renamed to `com.hyeonslab.prism.*`

### Changes

#### 0. Rename packages from `engine.prism.*` to `com.hyeonslab.prism.*`

**Scope:** 102 Kotlin files across all 12 modules, plus build config and docs.

Per module: restructure directory trees from `engine/prism/<module>/` to `com/hyeonslab/prism/<module>/` for every source set. Update all `package` and `import` declarations.

#### 1. Create `buildSrc/build.gradle.kts`

Kotlin DSL build file with `gradle-maven-publish-plugin` dependency.

#### 2. Create `buildSrc/src/main/kotlin/prism-publishing.gradle.kts`

Convention plugin that all 10 library modules apply. Configures:
- `publishToMavenCentral()` for releases
- Snapshot repository for `-SNAPSHOT` versions
- `signAllPublications()`
- POM metadata from `gradle.properties`

#### 3. Update `gradle/libs.versions.toml`

Add maven-publish plugin version and entry.

#### 4. Update `gradle.properties`

Add GROUP, VERSION_NAME, POM metadata (URL, SCM, license, developer).

#### 5. Update 10 library module `build.gradle.kts` files

Each gets `id("prism-publishing")` plugin and module-specific description.

#### 6. Update `AGENTS.md`

Change group ID and package references throughout.

### Verification

1. `./gradlew assemble` — build works with convention plugin
2. `./gradlew publishToMavenLocal` — artifacts published locally
3. Inspect POM for correct group, license, developer, SCM
4. Snapshot and release routing work correctly
