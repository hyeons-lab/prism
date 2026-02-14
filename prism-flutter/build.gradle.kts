plugins {
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    jvm()
    iosArm64()
    iosSimulatorArm64()
    macosArm64()
    linuxX64()
    mingwX64()

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
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        commonTest.dependencies {
            implementation(libs.kotlin.test)
        }
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
    }
}
