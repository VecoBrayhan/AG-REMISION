pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AG-Remision"
enableFeaturePreview("TYPESAFE_PROJECT_ACCESSORS")

include(":composeApp")