plugins {
  id("prism-quality")
  alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
  val isMac = System.getProperty("os.name").startsWith("Mac")
  val isAndroidBuild =
    System.getenv("ANDROID_NDK_HOME") != null || System.getenv("ANDROID_HOME") != null
  // Apple targets can only be compiled on macOS; Linux/Windows targets are built in Docker CI.
  val nativeTargets =
    if (isMac) {
      listOf(iosArm64(), iosSimulatorArm64(), macosArm64())
    } else {
      listOf(linuxX64(), mingwX64())
    }

  val androidNativeTargets =
    if (isAndroidBuild) {
      listOf(androidNativeArm64(), androidNativeX64())
    } else {
      emptyList()
    }

  (nativeTargets + androidNativeTargets).forEach { target ->
    target.binaries.sharedLib { baseName = "prism" }
  }

  if (isAndroidBuild) {
    androidNativeArm64 {
      compilations.getByName("main") {
        cinterops {
          create("androidNativeWindow") {
            definitionFile = file("src/androidNativeMain/cinterop/androidNativeWindow.def")
          }
        }
      }
    }
    androidNativeX64 {
      compilations.getByName("main") {
        cinterops {
          create("androidNativeWindow") {
            definitionFile = file("src/androidNativeMain/cinterop/androidNativeWindow.def")
          }
        }
      }
    }
  }

  sourceSets {
    val commonMain by getting
    val nativeMain by creating { dependsOn(commonMain) }

    (nativeTargets + androidNativeTargets).forEach { target ->
      target.compilations.getByName("main").defaultSourceSet.dependsOn(nativeMain)
    }

    nativeMain.dependencies {
      implementation(project(":prism-math"))
      implementation(project(":prism-core"))
      implementation(project(":prism-ecs"))
      implementation(project(":prism-renderer"))
      implementation(project(":prism-scene"))
      implementation(project(":prism-assets"))
      implementation(libs.kotlinx.atomicfu)
      implementation(libs.kotlinx.coroutines.core)
    }
    if (isMac) {
      macosMain.dependencies { implementation(libs.wgpu4k.toolkit) }
      iosMain.dependencies { implementation(libs.wgpu4k.toolkit) }
    }
    if (isAndroidBuild) {
      androidNativeArm64Main.dependencies {
        implementation(libs.wgpu4k.toolkit)
        implementation(libs.kotlinx.coroutines.core)
      }
      androidNativeX64Main.dependencies {
        implementation(libs.wgpu4k.toolkit)
        implementation(libs.kotlinx.coroutines.core)
      }
    }
  }

  compilerOptions { allWarningsAsErrors.set(true) }
}

// Gradle task to auto-generate Dart FFI bindings from the macosArm64 C header via ffigen.
// Run: ./gradlew :prism-native:generateFfiBindings
tasks.register<Exec>("generateFfiBindings") {
  val headerPath =
    layout.buildDirectory.file("bin/macosArm64/releaseShared/libprism_api.h").get().asFile
  dependsOn("linkReleaseSharedMacosArm64")
  workingDir(rootProject.projectDir.resolve("prism-flutter/flutter_plugin"))
  commandLine("dart", "run", "ffigen", "--config", "ffigen.yaml")
  doFirst {
    check(headerPath.exists()) {
      "C header not found at $headerPath — run linkReleaseSharedMacosArm64 first"
    }
  }
}
