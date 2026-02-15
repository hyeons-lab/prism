plugins {
    alias(libs.plugins.kotlin.multiplatform) apply false
    alias(libs.plugins.kotlin.android) apply false
    alias(libs.plugins.kotlin.compose) apply false
    alias(libs.plugins.kotlin.serialization) apply false
    alias(libs.plugins.android.library) apply false
    alias(libs.plugins.android.application) apply false
    alias(libs.plugins.compose.multiplatform) apply false
    alias(libs.plugins.ktfmt) apply false
    alias(libs.plugins.detekt) apply false
    alias(libs.plugins.maven.publish) apply false
}

subprojects {
    apply(plugin = "com.ncorti.ktfmt.gradle")
    apply(plugin = "io.gitlab.arturbosch.detekt")

    configure<com.ncorti.ktfmt.gradle.KtfmtExtension> {
        googleStyle()
        maxWidth.set(100)
    }

    configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
        config.setFrom(rootProject.files("detekt.yml"))
        buildUponDefaultConfig = false
    }

    tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach {
        jvmTarget = "22"
    }

    afterEvaluate {
        // Only wire JVM and WASM detekt tasks into check — metadata tasks fail on JDK 25
        // because detekt's embedded Kotlin compiler doesn't support it as a host runtime.
        // detektJvmMain includes commonMain sources transitively, so coverage is preserved.
        val detektTasks =
            tasks.matching {
                (it.name.startsWith("detektJvm") || it.name.startsWith("detektWasmJs")) &&
                    it.name.endsWith("Main")
            }
        if (detektTasks.isNotEmpty()) {
            tasks.named("check") { dependsOn(detektTasks) }
        }
    }
}
