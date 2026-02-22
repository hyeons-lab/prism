plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.kotlin.multiplatform.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ktfmt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.maven.publish) apply false
    alias(libs.plugins.skie) apply false
}

// ---------------------------------------------------------------------------
// Demo asset setup
//
// DamagedHelmet.glb (Khronos glTF 2.0 sample model, CC BY 4.0) is not
// committed to the repository (it is 3.7 MB). Run this task once after
// cloning to place the file in the canonical location; all demo targets
// (JVM, macOS, Android, iOS) read from there via workingDir / srcDirs.
//
// Run: ./gradlew downloadDemoAssets
// ---------------------------------------------------------------------------
tasks.register("downloadDemoAssets") {
    group = "demo"
    description = "Downloads DamagedHelmet.glb from Khronos glTF-Sample-Assets into prism-demo-core/assets/."

    // Single canonical location — consuming modules reference it from here.
    // (Flutter example and docs/ carry their own committed copies.)
    val dest = rootProject.file("prism-demo-core/assets/DamagedHelmet.glb")
    outputs.file(dest)

    doLast {
        if (dest.exists()) {
            logger.lifecycle("Demo asset already present — nothing to do.")
            return@doLast
        }
        val url = "https://github.com/KhronosGroup/glTF-Sample-Assets/raw/main/Models/DamagedHelmet/glTF-Binary/DamagedHelmet.glb"
        logger.lifecycle("Downloading DamagedHelmet.glb …")
        val bytes = java.net.URI(url).toURL().openStream().use { it.readBytes() }
        logger.lifecycle("  ${bytes.size / 1024} KB downloaded.")
        dest.parentFile.mkdirs()
        dest.writeBytes(bytes)
        logger.lifecycle("  → $dest")
    }
}
