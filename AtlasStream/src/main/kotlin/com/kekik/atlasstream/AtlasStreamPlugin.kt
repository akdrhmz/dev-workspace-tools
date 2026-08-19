package com.kekik.atlasstream

import android.app.AlertDialog
import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AtlasStreamPlugin : Plugin() {
    companion object {
        const val PREF_KEY_ENABLED_SOURCES = "atlasstream_enabled_sources"
        const val PREF_KEY_TMDB_API_KEY = "atlasstream_tmdb_api_key"
        const val PREF_KEY_USE_TURKISH = "atlasstream_use_turkish"

        val ALL_SOURCES = linkedMapOf(
            "DiziBox"          to "DiziBox (Yabanci Dizi)",
            "Dizilla"          to "Dizilla (Yabanci Dizi)",
            "FilmMakinesi"     to "FilmMakinesi (Film Odakli)",
            "HDFilmCehennemi"  to "HDFilmCehennemi (Genis Arsiv)",
            "SineWix"          to "SineWix (Dizi & Film)",
            "JetFilmizle"      to "JetFilmizle (Yerli/Yabanci Film)",
            "DiziPal"          to "DiziPal (Genis Arsiv)",
            "FullHDFilmizlesene" to "FullHDFilmizlesene (Film Odakli)",
            "FilmModu"         to "FilmModu (Film Odakli)",
            "SezonlukDizi"     to "SezonlukDizi (Dizi Odakli)",
            "CizgiMax"         to "CizgiMax (Cizgi Dizi/Film)",
            "YabanciDizi"      to "YabanciDizi (Dizi Odakli)",
            "FilmKovasi"       to "FilmKovasi (Film Odakli)",
            "HDFilmSitesi"     to "HDFilmSitesi (Film Odakli)",
            "DiziGom"          to "DiziGom (Dizi Odakli)",
            "DDizi"            to "DDizi (Yerli/Yabanci Dizi)",
            "DiziYou"          to "DiziYou (Dizi Odakli)",
            "SinemaCX"         to "SinemaCX (Film Odakli)",
            "FullHDFilm"       to "FullHDFilm (Film Odakli)",
            "HDFilmIzle"       to "HDFilmIzle (Film Odakli)",
            "RoketDizi"        to "RoketDizi (Dizi Odakli)",
            "DiziMag"          to "DiziMag (Dizi Odakli)",
            "powerSinema"      to "powerSinema (Film Odakli)",
            "powerDizi"        to "powerDizi (Dizi Odakli)",
            "UgurFilm"         to "UgurFilm (Film Odakli)",
            "SuperFilmGeldi"   to "SuperFilmGeldi (Film Odakli)",
            "BelgeselX"        to "BelgeselX (Belgesel)",
            "DMax"             to "DMax (Belgesel/Program)",
            "AltiYuzAltmisAltiFilmIzle" to "AltiYuzAltmisAlti (Film)",
            "RareFilmm"        to "RareFilmm (Kult Film)",
            "FilmIzleIlk"      to "FilmIzleIlk (Film)",
            "Tafdi"            to "Tafdi (Film & Dizi)",
            "TvDiziler"        to "TvDiziler (Dizi)",
            "XPrime"           to "XPrime (Dizi & Film)",
            "KoreanTurk"       to "KoreanTurk (Asya/Kore)",
            "AsyaWatch"        to "AsyaWatch (Asya)",
            "DiziKorea"        to "DiziKorea (Kore)",
            "Watch2Movies"     to "Watch2Movies (Film/Dizi)",
            "DiziMom"          to "DiziMom (Dizi)",
            "SetFilmIzle"      to "SetFilmIzle (Film)",
            "WebteIzle"        to "WebteIzle (Film)",
            "KultFilmler"      to "KultFilmler (Nostalji/Film)",
            "TRanimaci"        to "TRanimaci (Cizgi Dizi)"
        )
    }

    override fun load(context: Context) {
        registerMainAPI(AtlasStreamProvider())

        openSettings = { ctx ->
            showSettingsDialog(ctx)
        }
    }

    private fun showSettingsDialog(context: Context) {
        val options = arrayOf(
            "Kaynak Yonetimi (Siteleri Ac/Kapat)",
            "TMDB API Anahtari Yapilandir",
            "Hakkinda & Durum"
        )

        AlertDialog.Builder(context)
            .setTitle("AtlasStream Ayarlari")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSourceSelectionDialog(context)
                    1 -> showTmdbKeyDialog(context)
                    2 -> showAboutDialog(context)
                }
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun showSourceSelectionDialog(context: Context) {
        val sourceKeys = ALL_SOURCES.keys.toTypedArray()
        val sourceNames = ALL_SOURCES.values.toTypedArray()

        val savedEnabled = CloudStreamApp.getKey<List<String>>(PREF_KEY_ENABLED_SOURCES)
        val checkedItems = BooleanArray(sourceKeys.size) { i ->
            savedEnabled == null || savedEnabled.contains(sourceKeys[i])
        }

        AlertDialog.Builder(context)
            .setTitle("Aktif Kaynaklari Secin")
            .setMultiChoiceItems(sourceNames, checkedItems) { _, which, isChecked ->
                checkedItems[which] = isChecked
            }
            .setPositiveButton("Kaydet") { _, _ ->
                val selected = mutableListOf<String>()
                for (i in sourceKeys.indices) {
                    if (checkedItems[i]) selected.add(sourceKeys[i])
                }
                CloudStreamApp.setKey(PREF_KEY_ENABLED_SOURCES, selected)
            }
            .setNeutralButton("Tumunu Sec") { _, _ ->
                CloudStreamApp.setKey(PREF_KEY_ENABLED_SOURCES, sourceKeys.toList())
            }
            .setNegativeButton("Iptal", null)
            .show()
    }

    private fun showTmdbKeyDialog(context: Context) {
        val currentKey = CloudStreamApp.getKey<String>(PREF_KEY_TMDB_API_KEY) ?: ""
        val input = EditText(context).apply {
            hint = "TMDB API v3 Key girin (bos = varsayilan)"
            setText(currentKey)
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle("TMDB API Anahtari")
            .setMessage("Kendi TMDB API anahtarinizi kullanmak isterseniz buraya girin:")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    CloudStreamApp.setKey(PREF_KEY_TMDB_API_KEY, key)
                } else {
                    CloudStreamApp.removeKey(PREF_KEY_TMDB_API_KEY)
                }
            }
            .setNeutralButton("Sifirla") { _, _ ->
                CloudStreamApp.removeKey(PREF_KEY_TMDB_API_KEY)
            }
            .setNegativeButton("Iptal", null)
            .show()
    }

    private fun showAboutDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("AtlasStream v3.0")
            .setMessage("AtlasStream: Turkiye'nin en kapsamli Meta-Provider CloudStream eklentisi.\n\nToplam 40+ farkli film ve dizi kaynagini tek bir arayuzde birlestirir.")
            .setPositiveButton("Tamam", null)
            .show()
    }
}
