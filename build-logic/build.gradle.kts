plugins { `kotlin-dsl` }

dependencies {
    implementation(libs.plugins.ktfmt.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
    implementation(libs.plugins.detekt.get().let { "${it.pluginId}:${it.pluginId}.gradle.plugin:${it.version}" })
}
