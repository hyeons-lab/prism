plugins {
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.kotlin.compose)
    alias(libs.plugins.compose.multiplatform)
}

kotlin {
    jvm {
        mainRun {
            mainClass.set("engine.prism.demo.MainKt")
        }
    }
    iosArm64()
    iosSimulatorArm64()
    macosArm64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser {
            commonWebpackConfig {
                outputFileName = "prism-demo.js"
            }
        }
        binaries.executable()
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            implementation(project(":prism-math"))
            implementation(project(":prism-core"))
            implementation(project(":prism-renderer"))
            implementation(project(":prism-scene"))
            implementation(project(":prism-ecs"))
            implementation(project(":prism-input"))
            implementation(project(":prism-assets"))
            implementation(project(":prism-audio"))
            implementation(project(":prism-native-widgets"))
            implementation(project(":prism-compose"))
            implementation(compose.runtime)
            implementation(compose.foundation)
            implementation(compose.ui)
            implementation(compose.material3)
            implementation(libs.kermit)
        }
        jvmMain.dependencies {
            implementation(compose.desktop.currentOs)
        }
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
