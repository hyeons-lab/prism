pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        mavenLocal()
        google()
        mavenCentral()
    }
}

rootProject.name = "prism"

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
include(":prism-demo")
