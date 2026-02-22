import org.jetbrains.kotlin.gradle.plugin.mpp.apple.XCFramework

plugins {
    id("prism-quality")
    alias(libs.plugins.kotlin.multiplatform)
    alias(libs.plugins.android.kotlin.multiplatform.library)
    alias(libs.plugins.skie)
}

kotlin {
    jvmToolchain(25)
    android {
        namespace = "com.hyeonslab.prism.flutter.demo"
        compileSdk = libs.versions.compileSdk.get().toInt()
        minSdk = libs.versions.minSdk.get().toInt()
    }
    iosArm64()
    iosSimulatorArm64()
    val xcf = XCFramework("PrismFlutterDemo")
    macosArm64 {
        binaries.framework {
            baseName = "PrismFlutterDemo"
            isStatic = false
            xcf.add(this)
        }
    }
    linuxX64()
    mingwX64()

    @OptIn(org.jetbrains.kotlin.gradle.ExperimentalWasmDsl::class)
    wasmJs {
        browser()
        binaries.executable()
        // Keep "prism-flutter" as the output name for Dart/web loader compatibility.
        outputModuleName.set("prism-flutter")
    }

    applyDefaultHierarchyTemplate()

    sourceSets {
        commonMain.dependencies {
            api(project(":prism-flutter"))
            api(project(":prism-demo-core"))
            implementation(libs.kotlinx.coroutines.core)
            implementation(libs.kermit)
        }
        macosMain.dependencies {
            implementation(libs.wgpu4k.toolkit)
        }
        androidMain.dependencies { implementation(libs.kotlinx.coroutines.android) }
        commonTest.dependencies { implementation(libs.kotlin.test) }
    }

    compilerOptions {
        allWarningsAsErrors.set(true)
        freeCompilerArgs.add("-Xexpect-actual-classes")
    }
}

// Assemble PrismFlutterDemo.xcframework and copy into the Flutter example app's Frameworks.
// The example Runner links it via a local SPM package (example/macos/Packages/PrismFlutterDemo).
// The generic prism-flutter plugin has no dependency on this framework.
// Run: ./gradlew :prism-flutter-demo:bundleFlutterDemoMacOS
tasks.register<Copy>("bundleFlutterDemoMacOS") {
    dependsOn("assemblePrismFlutterDemoReleaseXCFramework")
    val xcfDir = layout.buildDirectory
        .dir("XCFrameworks/release/PrismFlutterDemo.xcframework")
    from(xcfDir)
    into(layout.projectDirectory.dir("example/macos/Frameworks/PrismFlutterDemo.xcframework"))
}

// Copy Kotlin/WASM build artifacts to the Flutter example web directory so that
// `flutter run -d chrome` can serve them alongside the Dart-compiled output.
// Run: ./gradlew :prism-flutter-demo:copyWasmToFlutterWeb
tasks.register<Copy>("copyWasmToFlutterWeb") {
    dependsOn(
        ":prism-flutter-demo:compileProductionExecutableKotlinWasmJsOptimize",
        ":prism-js:compileProductionExecutableKotlinWasmJsOptimize",
        ":prism-js:generateSdkTypes",
        ":kotlinWasmNpmInstall",
    )
    val wasmOutput = layout.buildDirectory
        .dir("compileSync/wasmJs/main/productionExecutable/optimized")
    from(wasmOutput) {
        include("prism-flutter.mjs")
        include("prism-flutter.uninstantiated.mjs")
        include("prism-flutter.wasm")
    }
    // prism-js: generated WASM module + hand-written OO SDK façade
    val prismJsBuild =
        project(":prism-js").layout.buildDirectory
            .dir("compileSync/wasmJs/main/productionExecutable/optimized")
    from(prismJsBuild) {
        include("prism.mjs")
        include("prism.uninstantiated.mjs")
        include("prism.wasm")
    }
    from(project(":prism-js").layout.buildDirectory.dir("sdk")) {
        include("prism-sdk.mjs")
    }
    // Skiko runtime (Compose/Skiko transitive dependency for WASM)
    from(rootProject.layout.buildDirectory.dir("wasm/packages_imported/skiko-js-wasm-runtime")) {
        include("**/skiko.mjs")
        include("**/skiko.wasm")
        eachFile { relativePath = RelativePath(true, name) }
    }
    into(layout.projectDirectory.dir("example/web"))
    includeEmptyDirs = false
}
