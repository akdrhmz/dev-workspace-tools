pluginManagement {
    repositories {
        google()
        mavenCentral()
        gradlePluginPortal()
        maven("https://jitpack.io")
    }
}

rootProject.name = "CSRepo"

// Modüller
include(":core-extractors")
include(":WatchBuddyBridge")
include(":FilmMakinesiProvider")
include(":DiziBoxProvider")
include(":HDFilmCehennemiProvider")
include(":SineWixProvider")
include(":DizillaProvider")
