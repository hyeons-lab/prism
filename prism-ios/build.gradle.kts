import com.squareup.kotlinpoet.FileSpec
import com.squareup.kotlinpoet.KModifier
import com.squareup.kotlinpoet.PropertySpec
import com.squareup.kotlinpoet.TypeSpec
import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

buildscript {
  repositories { mavenCentral() }
  dependencies { classpath("com.squareup:kotlinpoet:2.2.0") }
}

plugins { alias(libs.plugins.kotlin.multiplatform) }

val generatedDir = layout.buildDirectory.dir("generated/src/iosMain/kotlin")

val generatePrismVersion by
  tasks.registering {
    val versionName = providers.gradleProperty("VERSION_NAME")
    val outputDir = generatedDir
    inputs.property("version", versionName)
    outputs.dir(outputDir)
    doLast {
      val fileSpec =
        FileSpec.builder("com.hyeonslab.prism", "Prism")
          .addType(
            TypeSpec.objectBuilder("Prism")
              .addKdoc("Prism engine version information for the iOS XCFramework distribution.")
              .addProperty(
                PropertySpec.builder("VERSION", String::class)
                  .addModifiers(KModifier.CONST)
                  .initializer("%S", versionName.get())
                  .build()
              )
              .build()
          )
          .build()
      fileSpec.writeTo(outputDir.get().asFile)
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
