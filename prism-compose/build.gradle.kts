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
  val isMac = System.getProperty("os.name").startsWith("Mac")
  iosArm64()
  iosSimulatorArm64()
  if (isMac) macosArm64()
  // No linuxX64()/mingwX64(): Compose Multiplatform has no linux/windows-native
  // target, so this module has no source for them. Declaring them produced
  // empty NO-SOURCE compilations whose publication failed (missing klib) during
  // publishToMavenCentral. JVM Desktop covers Linux/Windows for Compose.

  @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class) wasmJs { browser() }

  applyDefaultHierarchyTemplate()

  sourceSets {
    val commonMain by getting
    val jvmMain by getting
    val androidMain by getting
    val wasmJsMain by getting

    commonMain.dependencies {
      api(project(":prism-core"))
      api(project(":prism-renderer"))
      api(project(":prism-native-widgets"))
      implementation(libs.kermit)
      implementation(libs.kotlinx.coroutines.core)
    }

    val nonNativeMain by creating { dependsOn(commonMain) }

    jvmMain.dependsOn(nonNativeMain)
    androidMain.dependsOn(nonNativeMain)
    wasmJsMain.dependsOn(nonNativeMain)

    nonNativeMain.dependencies {
      implementation(libs.compose.runtime)
      implementation(libs.compose.foundation)
      implementation(libs.compose.ui)
      implementation(libs.lifecycle.runtime.compose)
    }

    wasmJsMain.dependencies {
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
    }

    val appleMain by getting { dependsOn(nonNativeMain) }
    commonTest.dependencies { implementation(libs.kotlin.test) }
    jvmMain.dependencies {
      implementation(compose.desktop.currentOs)
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
      implementation(libs.kotlinx.coroutines.swing)
    }
    androidMain.dependencies {
      implementation(libs.wgpu4k)
      implementation(libs.wgpu4k.toolkit)
      implementation(libs.kotlinx.coroutines.android)
    }
  }

  compilerOptions { allWarningsAsErrors.set(true) }
}

// Metadata compilation tasks see duplicate KLIBs (androidx.* vs org.jetbrains.compose.*) when
// both the Android KMP library and Compose Multiplatform plugins are applied. Suppress -Werror
// for these intermediate tasks only — actual platform compilations are unaffected.
afterEvaluate {
  tasks
    .matching { it.name.endsWith("KotlinMetadata") }
    .configureEach {
      if (this is org.jetbrains.kotlin.gradle.tasks.KotlinCompilationTask<*>) {
        compilerOptions {
          allWarningsAsErrors.set(false)
          freeCompilerArgs.add("-nowarn")
        }
      }
    }
}

mavenPublishing {
  publishToMavenCentral()
  signAllPublications()
  pom { description.set("Jetpack Compose Multiplatform integration for embedding Prism rendering") }
}
