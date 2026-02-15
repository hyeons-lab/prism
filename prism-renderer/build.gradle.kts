plugins { alias(libs.plugins.kotlin.multiplatform) }

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
      api(project(":prism-math"))
      api(project(":prism-core"))
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kermit)
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
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
