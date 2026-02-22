plugins {
  id("prism-quality")
  alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser()
    binaries.executable()
    outputModuleName.set("prism")
    generateTypeScriptDefinitions()
  }

  sourceSets {
    wasmJsMain.dependencies {
      implementation(project(":prism-math"))
      implementation(project(":prism-core"))
      implementation(project(":prism-scene"))
      implementation(project(":prism-ecs"))
    }
  }

  compilerOptions { allWarningsAsErrors.set(true) }
}

// ── TypeScript SDK type generation ────────────────────────────────────────────
//
// Compiles prism-sdk.mts → build/sdk/prism-sdk.mjs + build/sdk/prism-sdk.d.mts
// Run: ./gradlew :prism-js:generateSdkTypes
//
// Uses Kotlin's managed Node.js to avoid a second Node.js download.
// tsc resolves `import ... from './prism.mjs'` via nodenext module resolution:
// it looks for prism.d.mts next to the source, which is copied from the Kotlin
// build output by copyPrismDmts before tsc runs.

/** Returns the Kotlin-managed Node.js installation root (parent of bin/). */
abstract class NodeDirValueSource : ValueSource<String, NodeDirValueSource.Parameters> {
  interface Parameters : ValueSourceParameters {
    val gradleUserHome: DirectoryProperty
  }

  override fun obtain(): String {
    val nodejsRoot = parameters.gradleUserHome.asFile.get().resolve("nodejs")
    val osName =
      when {
        System.getProperty("os.name").lowercase().contains("mac") -> "darwin"
        System.getProperty("os.name").lowercase().contains("win") -> "win"
        else -> "linux"
      }
    val arch =
      when (System.getProperty("os.arch")) {
        "aarch64" -> "arm64"
        else -> "x64"
      }
    return nodejsRoot
      .listFiles()
      ?.filter {
        it.isDirectory && it.name.startsWith("node-v") && it.name.endsWith("$osName-$arch")
      }
      ?.maxByOrNull { it.name }
      ?.absolutePath
      ?: error("Kotlin-managed Node.js not found in $nodejsRoot. Run a Kotlin WASM task first.")
  }
}

/**
 * Copies the Kotlin-generated prism.d.mts into the project directory so that tsc (nodenext
 * resolution) can find it as the type declarations for ./prism.mjs.
 *
 * Declares a specific @OutputFile (not the entire project directory) to avoid Gradle's
 * implicit-dependency validation error.
 */
abstract class CopyPrismDmtsTask
@javax.inject.Inject
constructor(private val fs: org.gradle.api.file.FileSystemOperations) : DefaultTask() {
  @get:InputFile abstract val sourceDmts: RegularFileProperty

  @get:OutputFile abstract val destDmts: RegularFileProperty

  @TaskAction
  fun run() {
    fs.copy {
      from(sourceDmts)
      into(destDmts.get().asFile.parentFile)
    }
  }
}

/** Runs `npm install` in the project directory using Kotlin's managed Node.js. */
abstract class SdkNpmInstallTask
@javax.inject.Inject
constructor(private val execOps: org.gradle.process.ExecOperations) : DefaultTask() {
  @get:Input abstract val nodeDir: Property<String>

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val packageJson: RegularFileProperty

  /** node_modules/.package-lock.json written by npm ≥ 7; used for up-to-date checking. */
  @get:OutputFile abstract val lockFile: RegularFileProperty

  @TaskAction
  fun install() {
    val dir = packageJson.get().asFile.parentFile
    // bin/npm is itself a Node.js script; run it via node to avoid PATH dependency.
    val node = File(nodeDir.get(), "bin/node")
    val npmScript = File(nodeDir.get(), "bin/npm")
    execOps.exec {
      workingDir(dir)
      executable(node.absolutePath)
      args(npmScript.absolutePath, "install")
    }
  }
}

/** Compiles prism-sdk.mts → build/sdk/ using tsc from node_modules. */
abstract class GenerateSdkTypesTask
@javax.inject.Inject
constructor(private val execOps: org.gradle.process.ExecOperations) : DefaultTask() {
  @get:Input abstract val nodeDir: Property<String>

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val sourceFile: RegularFileProperty

  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val tsconfigFile: RegularFileProperty

  /** prism.d.mts must be in place before tsc runs (copied by copyPrismDmts). */
  @get:InputFile
  @get:PathSensitive(PathSensitivity.RELATIVE)
  abstract val prismDmts: RegularFileProperty

  @get:OutputDirectory abstract val outputDir: DirectoryProperty

  @TaskAction
  fun compile() {
    val workDir = sourceFile.get().asFile.parentFile
    val node = File(nodeDir.get(), "bin/node")
    execOps.exec {
      workingDir(workDir)
      executable(node.absolutePath)
      args("node_modules/.bin/tsc", "--project", "tsconfig.sdk.json")
    }
  }
}

val nodeDirProvider =
  providers.of(NodeDirValueSource::class) {
    parameters.gradleUserHome.set(gradle.gradleUserHomeDir)
  }

// Step 1: copy prism.d.mts from the Kotlin build output next to prism-sdk.mts.
val copyPrismDmts by
  tasks.registering(CopyPrismDmtsTask::class) {
    dependsOn("compileProductionExecutableKotlinWasmJs")
    sourceDmts.set(
      layout.buildDirectory.file("compileSync/wasmJs/main/productionExecutable/kotlin/prism.d.mts")
    )
    destDmts.set(layout.projectDirectory.file("prism.d.mts"))
  }

// Step 2: npm install — installs typescript into prism-js/node_modules/
val sdkNpmInstall by
  tasks.registering(SdkNpmInstallTask::class) {
    dependsOn(":kotlinWasmNodeJsSetup")
    nodeDir.set(nodeDirProvider)
    packageJson.set(layout.projectDirectory.file("package.json"))
    lockFile.set(layout.projectDirectory.file("node_modules/.package-lock.json"))
  }

// Step 3: tsc — compiles prism-sdk.mts → build/sdk/
val generateSdkTypes by
  tasks.registering(GenerateSdkTypesTask::class) {
    dependsOn(copyPrismDmts, sdkNpmInstall)
    nodeDir.set(nodeDirProvider)
    sourceFile.set(layout.projectDirectory.file("prism-sdk.mts"))
    tsconfigFile.set(layout.projectDirectory.file("tsconfig.sdk.json"))
    prismDmts.set(layout.projectDirectory.file("prism.d.mts"))
    outputDir.set(layout.buildDirectory.dir("sdk"))
  }
