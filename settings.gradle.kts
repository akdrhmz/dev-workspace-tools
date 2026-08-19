pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

rootProject.name = "dev-workspace-tools"

// Modüller

include(":AtlasStream")
include(":FilmMakinesiProvider")
include(":DiziBoxProvider")
include(":HDFilmCehennemiProvider")
include(":SineWixProvider")
include(":DizillaProvider")

include(":JetFilmizleProvider")


