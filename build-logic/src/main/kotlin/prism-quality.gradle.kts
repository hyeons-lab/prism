plugins {
    id("com.ncorti.ktfmt.gradle")
    id("io.gitlab.arturbosch.detekt")
}

configure<com.ncorti.ktfmt.gradle.KtfmtExtension> {
    googleStyle()
    maxWidth.set(100)
}

configure<io.gitlab.arturbosch.detekt.extensions.DetektExtension> {
    config.setFrom(rootProject.files("detekt.yml"))
    buildUponDefaultConfig = false
}

tasks.withType<io.gitlab.arturbosch.detekt.Detekt>().configureEach { jvmTarget = "22" }

// Wire JVM and WASM detekt tasks into check — metadata tasks fail on JDK 25
// because detekt's embedded Kotlin compiler doesn't support it as a host runtime.
// detektJvmMain includes commonMain sources transitively, so coverage is preserved.
afterEvaluate {
    val detektTasks =
        tasks.matching {
            (it.name.startsWith("detektJvm") || it.name.startsWith("detektWasmJs")) &&
                it.name.endsWith("Main")
        }
    if (detektTasks.isNotEmpty()) {
        tasks.named("check") { dependsOn(detektTasks) }
    }
}
