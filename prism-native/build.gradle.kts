import org.jetbrains.kotlin.gradle.plugin.mpp.KotlinNativeTarget

plugins {
    id("prism-quality")
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    val nativeTargets =
        listOf(iosArm64(), iosSimulatorArm64(), macosArm64(), linuxX64(), mingwX64())

    nativeTargets.forEach { target -> target.binaries.sharedLib { baseName = "prism" } }

    sourceSets {
        nativeMain.dependencies {
            implementation(project(":prism-math"))
            implementation(project(":prism-core"))
        }
    }

    compilerOptions { allWarningsAsErrors.set(true) }
}

// Gradle task to auto-generate Dart FFI bindings from the macosArm64 C header via ffigen.
// Run: ./gradlew :prism-native:generateFfiBindings
tasks.register<Exec>("generateFfiBindings") {
    val headerPath =
        layout.buildDirectory
            .file("bin/macosArm64/releaseShared/libprism_api.h")
            .get()
            .asFile
    dependsOn("linkReleaseSharedMacosArm64")
    workingDir(rootProject.projectDir.resolve("prism-flutter/flutter_plugin"))
    commandLine("dart", "run", "ffigen", "--config", "ffigen.yaml")
    doFirst {
        check(headerPath.exists()) { "C header not found at $headerPath — run linkReleaseSharedMacosArm64 first" }
    }
}
