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
include(":core-extractors")
include(":WatchBuddyBridge")
include(":FilmMakinesiProvider")
include(":DiziBoxProvider")
include(":HDFilmCehennemiProvider")
include(":SineWixProvider")
include(":DizillaProvider")
include(":DizifonProvider")
include(":JetFilmizleProvider")
