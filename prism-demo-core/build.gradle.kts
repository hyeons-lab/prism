import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
  id("prism-quality")
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.skie)
}

kotlin {
  jvmToolchain(25)
  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalKotlinGradlePluginApi::class)
  jvm { mainRun { mainClass.set("com.hyeonslab.prism.demo.GlfwMainKt") } }
  android {
    namespace = "com.hyeonslab.prism.demo"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = libs.versions.minSdk.get().toInt()
  }
  macosArm64 {
    binaries {
      executable {
        entryPoint = "com.hyeonslab.prism.demo.main"
        // Run the macOS binary with assets/ as the working directory so
        // loadGlbBytes("DamagedHelmet.glb") resolves to the canonical location.
        runTask?.workingDir(project.file("assets"))
        runTask?.dependsOn(":downloadDemoAssets")
      }
    }
  }
  linuxX64()
  mingwX64()

  val xcf = XCFramework("PrismDemo")
  listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
      baseName = "PrismDemo"
      isStatic = true
      xcf.add(this)

      // Export engine modules so their types are accessible from Swift via PrismDemo.xcframework.
      export(project(":prism-math"))
      export(project(":prism-core"))
      export(project(":prism-renderer"))
      export(project(":prism-scene"))
      export(project(":prism-ecs"))
      export(project(":prism-input"))
      export(project(":prism-assets"))
      export(project(":prism-audio"))
      export(project(":prism-native-widgets"))
      export(project(":prism-compose"))
    }
  }

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser { commonWebpackConfig { outputFileName = "prism-demo-core.js" } }
    binaries.executable()
  }

  applyDefaultHierarchyTemplate()

  sourceSets {
    val commonMain by getting
    val jvmMain by getting
    val androidMain by getting
    val wasmJsMain by getting

    val nonNativeMain by creating { dependsOn(commonMain) }

    jvmMain.dependsOn(nonNativeMain)
    androidMain.dependsOn(nonNativeMain)
    wasmJsMain.dependsOn(nonNativeMain)

    val appleMain by getting { dependsOn(nonNativeMain) }

    nonNativeMain.dependencies {
      api(project(":prism-compose"))
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.ui)
      implementation(libs.compose.material3)
      implementation(libs.lifecycle.runtime.compose)
    }

    commonMain.dependencies {
      api(project(":prism-math"))
      api(project(":prism-core"))
      api(project(":prism-renderer"))
      api(project(":prism-scene"))
      api(project(":prism-ecs"))
      api(project(":prism-input"))
      api(project(":prism-assets"))
      api(project(":prism-audio"))
      api(project(":prism-native-widgets"))
      implementation(libs.kermit)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
    }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.kotlinx.coroutines.swing)
    }
    androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotest.assertions.core)
      implementation(libs.kotlinx.coroutines.test)
    }
  }

  compilerOptions { allWarningsAsErrors.set(true) }
}

// Common JVM args for all demo tasks
val isMacOs = providers.systemProperty("os.name").map { it.contains("Mac", ignoreCase = true) }

tasks.withType<JavaExec>().configureEach {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
  // Scope workingDir and asset download to demo entry-point tasks only, so other
  // JavaExec tasks (code-gen, formatters, etc.) are not affected.
  if (name == "jvmRun" || name == "runCompose") {
    workingDir = project.file("assets")
    dependsOn(":downloadDemoAssets")
  }
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
  jvmArgs("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
  jvmArgs("--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED")
  jvmArgs("--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  // -XstartOnFirstThread is required by GLFW on macOS but conflicts with Swing/Compose EDT.
  // Only apply it for GLFW-based tasks (the default `run` task), not `runCompose`.
  if (name != "runCompose") {
    jvmArgumentProviders.add(
      CommandLineArgumentProvider {
        if (isMacOs.get()) listOf("-XstartOnFirstThread") else emptyList()
      }
    )
  }
}

tasks.register<JavaExec>("runCompose") {
  group = "application"
  description = "Run the Compose Desktop demo with embedded 3D rendering"
  mainClass.set("com.hyeonslab.prism.demo.ComposeMainKt")
  classpath =
    files(
      kotlin.jvm().compilations.named("main").map { it.runtimeDependencyFiles },
      kotlin.jvm().compilations.named("main").map { it.output.allOutputs },
    )
}
