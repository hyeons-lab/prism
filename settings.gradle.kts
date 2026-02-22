pluginManagement {
    includeBuild("build-logic")
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}
plugins {
    id("org.gradle.toolchains.foojay-resolver-convention") version "1.0.0"
}

dependencyResolutionManagement {
    repositories {
        mavenLocal {
            content {
                includeGroup("io.ygdrasil")
                includeGroup("com.hyeons-lab")
            }
        }
        google()
        mavenCentral()
    }
}

rootProject.name = "prism"

include(":prism-js")
include(":prism-native")
include(":prism-math")
include(":prism-core")
include(":prism-renderer")
include(":prism-scene")
include(":prism-ecs")
include(":prism-input")
include(":prism-assets")
include(":prism-audio")
include(":prism-native-widgets")
include(":prism-compose")
include(":prism-flutter")
include(":prism-flutter-demo")
include(":prism-ios")
include(":prism-demo-core")
include(":prism-android-demo")
