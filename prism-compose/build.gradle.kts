plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.maven.publish)
}

kotlin {
  jvmToolchain(25)
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs { browser() }

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      api(project(":prism-core"))
      api(project(":prism-renderer"))
      api(project(":prism-native-widgets"))
      implementation(compose.runtime)
      implementation(compose.foundation)
      implementation(compose.ui)
      implementation(libs.kermit)
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.lifecycle.runtime.compose)
    }
    commonTest.dependencies { implementation(libs.kotlin.test) }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
      implementation(libs.kotlinx.coroutines.swing)
    }
  }

  compilerOptions { allWarningsAsErrors.set(true) }
}

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()
  pom { description.set("Jetpack Compose Multiplatform integration for embedding Prism rendering") }
}
