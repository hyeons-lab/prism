plugins {
  id("prism-quality")
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
  alias(libs.plugins.kotlin.serialization)
  alias(libs.plugins.maven.publish)
}

kotlin {
  jvm()
  android {
    namespace = "com.hyeonslab.prism.assets"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = libs.versions.minSdk.get().toInt()
  }
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  // linuxX64/mingwX64 removed — no platform code. Re-add when needed.

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs { browser() }

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      api(project(":prism-core"))
      api(project(":prism-renderer"))
      api(project(":prism-ecs"))
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.io.core)
      implementation(libs.kotlinx.serialization.json)
      implementation(libs.kermit)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotest.assertions.core)
      implementation(libs.kotlinx.coroutines.test)
    }
  }

  compilerOptions {
    allWarningsAsErrors.set(true)
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
}

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()
  pom { description.set("Asset management and resource loading for meshes, shaders, and textures") }
}
