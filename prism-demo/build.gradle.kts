plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
}

kotlin {
  jvmToolchain(25)
  jvm { mainRun { mainClass.set("engine.prism.demo.GlfwMainKt") } }
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
    }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
      implementation(libs.kotlinx.coroutines.core)
    }
  }

  compilerOptions { allWarningsAsErrors.set(true) }
}

tasks.withType<JavaExec> {
  javaLauncher.set(javaToolchains.launcherFor { languageVersion.set(JavaLanguageVersion.of(25)) })
  if (org.gradle.internal.os.OperatingSystem.current().isMacOsX) {
    jvmArgs("-XstartOnFirstThread")
  }
  jvmArgs("--add-opens=java.base/java.lang=ALL-UNNAMED")
  jvmArgs("--enable-native-access=ALL-UNNAMED")
}

val javaToolchains = extensions.getByType<JavaToolchainService>()
