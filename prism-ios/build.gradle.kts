import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
  id("prism-quality")
  alias(libs.plugins.kotlin.multiplatform)
  alias(libs.plugins.skie)
}

val generatedDir = layout.buildDirectory.dir("generated/src/iosMain/kotlin")

val generatePrismVersion by
  tasks.registering {
    val versionName = providers.gradleProperty("VERSION_NAME")
    val outputDir = generatedDir
    inputs.property("version", versionName)
    outputs.dir(outputDir)
    doLast {
      val dir = outputDir.get().asFile.resolve("com/hyeonslab/prism")
      dir.mkdirs()
      dir
        .resolve("Prism.kt")
        .writeText(
          buildString {
            appendLine("package com.hyeonslab.prism")
            appendLine()
            appendLine(
              "/** Prism engine version information for the iOS XCFramework distribution. */"
            )
            appendLine("internal object Prism {")
            appendLine("  const val VERSION: String = \"${versionName.get()}\"")
            appendLine("}")
          }
        )
    }
  }

kotlin {
  val xcf = XCFramework("Prism")

  listOf(iosArm64(), iosSimulatorArm64()).forEach { target ->
    target.binaries.framework {
      baseName = "Prism"
      isStatic = true
      xcf.add(this)

      export(project(":prism-math"))
      export(project(":prism-core"))
      export(project(":prism-renderer"))
      export(project(":prism-scene"))
      export(project(":prism-ecs"))
      export(project(":prism-input"))
      export(project(":prism-assets"))
      export(project(":prism-audio"))
      export(project(":prism-native-widgets"))
      export(project(":prism-compose"))
    }
  }

  applyDefaultHierarchyTemplate()

  sourceSets {
    iosMain { kotlin.srcDir(generatePrismVersion) }
    commonMain.dependencies {
      api(project(":prism-math"))
      api(project(":prism-core"))
      api(project(":prism-renderer"))
      api(project(":prism-scene"))
      api(project(":prism-ecs"))
      api(project(":prism-input"))
      api(project(":prism-assets"))
      api(project(":prism-audio"))
      api(project(":prism-native-widgets"))
      api(project(":prism-compose"))
    }
  }

  compilerOptions { allWarningsAsErrors.set(true) }
}
