plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
}

kotlin {
  jvmToolchain(25)
  jvm { mainRun { mainClass.set("com.hyeonslab.prism.demo.GlfwMainKt") } }
  iosArm64()
  iosSimulatorArm64()
  macosArm64()

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser { commonWebpackConfig { outputFileName = "prism-demo.js" } }
    binaries.executable()
  }

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      implementation(project(":prism-math"))
      implementation(project(":prism-core"))
      implementation(project(":prism-renderer"))
      implementation(project(":prism-scene"))
      implementation(project(":prism-ecs"))
      implementation(project(":prism-input"))
      implementation(project(":prism-assets"))
      implementation(project(":prism-audio"))
      implementation(project(":prism-native-widgets"))
      implementation(project(":prism-compose"))
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.ui)
      implementation(compose.material3)
      implementation(libs.kermit)
      implementation(libs.kotlinx.coroutines.core)
    }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotest.assertions.core)
      implementation(libs.kotlinx.coroutines.test)
    }
    wasmJsMain.dependencies {
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
    }
  }

  compilerOptions { allWarningsAsErrors.set(true) }
}

// Common JVM args for all demo tasks
tasks.withType<JavaExec> {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--add-opens=java.desktop/java.awt=ALL-UNNAMED")
  jvmArgs("--add-opens=java.desktop/sun.awt=ALL-UNNAMED")
  jvmArgs("--add-opens=java.desktop/sun.lwawt=ALL-UNNAMED")
  jvmArgs("--add-opens=java.desktop/sun.lwawt.macosx=ALL-UNNAMED")
  jvmArgs("--enable-native-access=ALL-UNNAMED")
  // -XstartOnFirstThread is required by GLFW on macOS but conflicts with Swing/Compose EDT.
  // Only apply it for GLFW-based tasks (the default `run` task), not `runCompose`.
  if (name != "runCompose" && org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
    jvmArgs("-XstartOnFirstThread")
  }
}

val javaToolchains = extensions.getByType<JavaToolchainService>()

tasks.register<JavaExec>("runCompose") {
  group = "application"
  description = "Run the Compose Desktop demo with embedded 3D rendering"
  mainClass.set("com.hyeonslab.prism.demo.ComposeMainKt")
  classpath =
    kotlin.jvm().compilations["main"].runtimeDependencyFiles +
      kotlin.jvm().compilations["main"].output.allOutputs
}
