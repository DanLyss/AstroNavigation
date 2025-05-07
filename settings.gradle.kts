pluginManagement {
    repositories {
        gradlePluginPortal()
        google()
        mavenCentral()
    }
    plugins {
        id("com.android.application") version "8.2.0"
        id("org.jetbrains.kotlin.android") version "1.9.0"
    }
}

dependencyResolutionManagement {
    repositories {
        google()
        mavenCentral()
    }
}

rootProject.name = "AstroNavigation"

// register your modules
include(
    ":StarApp",
    ":StarApp:app",
    ":KotlinTranslation"
)
// map modules to their directories
project(":StarApp").projectDir           = file("StarApp")
project(":StarApp:app").projectDir       = file("StarApp/app")
project(":KotlinTranslation").projectDir = file("backend/KotlinTranslation")