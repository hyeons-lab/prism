plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
  alias(libs.plugins.kotlin.compose)
}

android {
  namespace = "com.hyeonslab.prism.demo"
  compileSdk = libs.versions.compileSdk.get().toInt()

  defaultConfig {
    applicationId = "com.hyeonslab.prism.demo"
    minSdk = libs.versions.minSdk.get().toInt()
    targetSdk = libs.versions.targetSdk.get().toInt()
    versionCode = 1
    versionName = "0.1.0"
  }

  kotlin { jvmToolchain(25) }

  buildFeatures { compose = true }

  sourceSets {
    getByName("main") {
      // Serve the shared demo asset (downloaded by :downloadDemoAssets) as an APK asset.
      assets.srcDirs(rootProject.file("prism-demo-core/assets"))
    }
  }
}

// Download the shared demo asset before any task that reads the assets source directory.
// This covers AGP merge tasks, all AGP lint tasks (lintAnalyze, lintVitalAnalyze,
// generate*LintReportModel, generate*LintVitalReportModel, etc.) and future consumers.
tasks.matching { it.name.startsWith("merge") && it.name.endsWith("Assets") || "lint" in it.name.lowercase() }
  .configureEach { dependsOn(":downloadDemoAssets") }

dependencies {
  implementation(project(":prism-demo-core"))
  implementation(project(":prism-native-widgets"))
  implementation(project(":prism-compose"))
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kermit)
  implementation(libs.wgpu4k)
  implementation(libs.wgpu4k.toolkit)
  implementation(libs.activity.compose)
}
