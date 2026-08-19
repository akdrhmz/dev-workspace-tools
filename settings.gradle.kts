pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

rootProject.name = "dev-workspace-tools"

// Sadece AtlasStream (Meta-Provider) derlenecek
include(":AtlasStream")
