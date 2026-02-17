plugins {
  id("prism-quality")
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.compose)
  alias(libs.plugins.compose.multiplatform)
  alias(libs.plugins.maven.publish)
}

kotlin {
  jvmToolchain(25)
  jvm()
  android {
    namespace = "com.hyeonslab.prism.compose"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = libs.versions.minSdk.get().toInt()
  }
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
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.ui)
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
