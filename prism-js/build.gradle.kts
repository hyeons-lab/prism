plugins {
    id("prism-quality")
    alias(libs.plugins.kotlin.multiplatform)
}

kotlin {
    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
        outputModuleName.set("prism")
        generateTypeScriptDefinitions()
    }

    sourceSets {
        wasmJsMain.dependencies {
            implementation(project(":prism-math"))
            implementation(project(":prism-core"))
            implementation(project(":prism-scene"))
            implementation(project(":prism-ecs"))
        }
    }

    compilerOptions { allWarningsAsErrors.set(true) }
}
