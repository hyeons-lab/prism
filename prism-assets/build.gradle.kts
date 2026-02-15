plugins {
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.maven.publish)
}

kotlin {
  jvm()
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  linuxX64()
  mingwX64()

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs { browser() }

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      api(project(":prism-core"))
      api(project(":prism-renderer"))
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kotlinx.io.core)
      implementation(libs.kermit)
    }
    commonTest.dependencies {
      implementation(libs.kotlin.test)
      implementation(libs.kotest.assertions.core)
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
