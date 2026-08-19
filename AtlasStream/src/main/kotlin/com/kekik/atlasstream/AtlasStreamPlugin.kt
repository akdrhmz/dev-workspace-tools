package com.kekik.atlasstream

import android.app.AlertDialog
import android.content.Context
import android.graphics.drawable.Drawable
import android.widget.EditText
import android.widget.LinearLayout
import com.lagradost.cloudstream3.CloudStreamApp
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin
import com.lagradost.cloudstream3.plugins.Plugin.Manifest

@CloudstreamPlugin
class AtlasStreamPlugin : Plugin() {
    companion object {
        const val PREF_KEY_ENABLED_SOURCES = "atlasstream_enabled_sources"
        const val PREF_KEY_TMDB_API_KEY = "atlasstream_tmdb_api_key"
        const val PREF_KEY_USE_TURKISH = "atlasstream_use_turkish"

        val ALL_SOURCES = linkedMapOf(
            // --- ekirdek Kaynaklar ---
            "DiziBox"          to "DiziBox (Yabanc Dizi)",
            "Dizilla"          to "Dizilla (Yabanc Dizi)",
            "FilmMakinesi"     to "FilmMakinesi (Film Odakl)",
            "HDFilmCehennemi"  to "HDFilmCehennemi (Geni Ariv)",
            "SineWix"          to "SineWix (Dizi & Film)",
            "JetFilmizle"      to "JetFilmizle (Yerli/Yabanc Film)",
            "DiziPal"          to "DiziPal (Geni Ariv)",
            "FullHDFilmizlesene" to "FullHDFilmizlesene (Film Odakl)",
            "FilmModu"         to "FilmModu (Film Odakl)",
            "SezonlukDizi"     to "SezonlukDizi (Dizi Odakl)",
            "CizgiMax"         to "CizgiMax (izgi Dizi/Film)",
            
            // --- Dinamik Entegre Edilen Kaynaklar ---            "DiziMom" to "DiziMom (Dinamik Kaynak)",
            "SetFilmIzle" to "SetFilmIzle (Dinamik Kaynak)",
            "WebteIzle" to "WebteIzle (Dinamik Kaynak)",
            "KultFilmler" to "KultFilmler (Dinamik Kaynak)",
            "TRanimaci" to "TRanimaci (Dinamik Kaynak)",
            "YabanciDizi" to "YabanciDizi (Dinamik Kaynak)",
            "FilmKovasi" to "FilmKovasi (Dinamik Kaynak)",
            "HDFilmSitesi" to "HDFilmSitesi (Dinamik Kaynak)",
            "DiziGom" to "DiziGom (Dinamik Kaynak)",
            "DDizi" to "DDizi (Dinamik Kaynak)",
            "DiziYou" to "DiziYou (Dinamik Kaynak)",
            "SinemaCX" to "SinemaCX (Dinamik Kaynak)",
            "FullHDFilm" to "FullHDFilm (Dinamik Kaynak)",
            "HDFilmIzle" to "HDFilmIzle (Dinamik Kaynak)",
            "RoketDizi" to "RoketDizi (Dinamik Kaynak)",
            "DiziMag" to "DiziMag (Dinamik Kaynak)",
            "powerSinema" to "powerSinema (Dinamik Kaynak)",
            "powerDizi" to "powerDizi (Dinamik Kaynak)",
            "UgurFilm" to "UgurFilm (Dinamik Kaynak)",
            "SuperFilmGeldi" to "SuperFilmGeldi (Dinamik Kaynak)",
            "BelgeselX" to "BelgeselX (Dinamik Kaynak)",
            "DMax" to "DMax (Dinamik Kaynak)",
            "AltiYuzAltmisAltiFilmIzle" to "AltiYuzAltmisAltiFilmIzle (Dinamik Kaynak)",
            "RareFilmm" to "RareFilmm (Dinamik Kaynak)",
            "FilmIzleIlk" to "FilmIzleIlk (Dinamik Kaynak)",
            "Tafdi" to "Tafdi (Dinamik Kaynak)",
            "TvDiziler" to "TvDiziler (Dinamik Kaynak)",
            "XPrime" to "XPrime (Dinamik Kaynak)",
            "KoreanTurk" to "KoreanTurk (Dinamik Kaynak)",
            "AsyaWatch" to "AsyaWatch (Dinamik Kaynak)",
            "DiziKorea" to "DiziKorea (Dinamik Kaynak)",
            "Watch2Movies" to "Watch2Movies (Dinamik Kaynak)",
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
            "Kaynak Ynetimi (Siteleri A/Kapat)",
            "TMDB API Anahtar Yaplandr",
            "Hakknda & Durum"
        )

        AlertDialog.Builder(context)
            .setTitle("AtlasStream Ayarlar")
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
            .setTitle("Aktif Kaynaklar Sein")
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
            .setNeutralButton("Tmn Se") { _, _ ->
                CloudStreamApp.setKey(PREF_KEY_ENABLED_SOURCES, sourceKeys.toList())
            }
            .setNegativeButton("ptal", null)
            .show()
    }

    private fun showTmdbKeyDialog(context: Context) {
        val currentKey = CloudStreamApp.getKey<String>(PREF_KEY_TMDB_API_KEY) ?: ""
        val input = EditText(context).apply {
            hint = "TMDB API v3 Key girin (bo = varsaylan)"
            setText(currentKey)
        }

        val layout = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 40, 50, 10)
            addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle("TMDB API Anahtar")
            .setMessage("Kendi TMDB API anahtarnz kullanmak isterseniz buraya girin:")
            .setView(layout)
            .setPositiveButton("Kaydet") { _, _ ->
                val key = input.text.toString().trim()
                if (key.isNotEmpty()) {
                    CloudStreamApp.setKey(PREF_KEY_TMDB_API_KEY, key)
                } else {
                    CloudStreamApp.removeKey(PREF_KEY_TMDB_API_KEY)
                }
            }
            .setNeutralButton("Sfrla") { _, _ ->
                CloudStreamApp.removeKey(PREF_KEY_TMDB_API_KEY)
            }
            .setNegativeButton("ptal", null)
            .show()
    }

    private fun showAboutDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("AtlasStream v3.0")
            .setMessage("AtlasStream: Trkiye'nin en kapsaml Meta-Provider CloudStream eklentisi.\n\nToplam 40+ farkl film ve dizi kaynan tek bir arayzde birletirir. Anime ve Canl TV harici tm film/dizi arivini kapsar.")
            .setPositiveButton("Tamam", null)
            .show()
    }
}
