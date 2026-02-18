plugins {
  alias(libs.plugins.android.application)
  alias(libs.plugins.kotlin.android)
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
}

dependencies {
  implementation(project(":prism-demo-core"))
  implementation(project(":prism-native-widgets"))
  implementation(libs.kotlinx.coroutines.android)
  implementation(libs.kermit)
  implementation(libs.wgpu4k)
  implementation(libs.wgpu4k.toolkit)
}
