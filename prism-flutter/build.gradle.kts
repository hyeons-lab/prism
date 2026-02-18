plugins {
  id("prism-quality")
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.android.kotlin.multiplatform.library)
}

kotlin {
  jvmToolchain(25)
  jvm()
  android {
    namespace = "com.hyeonslab.prism.flutter"
    compileSdk = libs.versions.compileSdk.get().toInt()
    minSdk = libs.versions.minSdk.get().toInt()
  }
  iosArm64()
  iosSimulatorArm64()
  macosArm64()
  linuxX64()
  mingwX64()

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
  wasmJs {
    browser()
    binaries.executable()
    outputModuleName.set("prism-flutter")
  }

  applyDefaultHierarchyTemplate()

  sourceSets {
    commonMain.dependencies {
      api(project(":prism-core"))
      api(project(":prism-renderer"))
      api(project(":prism-scene"))
      api(project(":prism-ecs"))
      api(project(":prism-input"))
      api(project(":prism-assets"))
      api(project(":prism-audio"))
      api(project(":prism-native-widgets"))
      api(project(":prism-demo-core"))
      implementation(libs.kotlinx.coroutines.core)
      implementation(libs.kermit)
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
    }
    androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }
    commonTest.dependencies { implementation(libs.kotlin.test) }
  }

  compilerOptions {
    allWarningsAsErrors.set(true)
    freeCompilerArgs.add("-Xexpect-actual-classes")
  }
}
