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
            "SineWix" to "SineWix (Film & Dizi API)",
            "SelcukFlix" to "SelcukFlix (Film & Dizi)",
            "RecTV" to "RecTV (Dizi, Film, Canlı)",
            "InatBox" to "InatBox (Film & Dizi)",
            "XPrime" to "XPrime (TMDB Motoru)",
            "HDFilmCehennemi" to "HDFilmCehennemi (Film)",
            "FilmMakinesi" to "FilmMakinesi (Film)",
            "FullHDFilmizlesene" to "FullHDFilmizlesene (Film)",
            "FilmModu" to "FilmModu (1080p/4K Film)",
            "FilmKovasi" to "FilmKovasi (Film)",
            "WebteIzle" to "WebteIzle (Film)",
            "WFilmİzle" to "WFilmİzle (Film)",
            "DiziBox" to "DiziBox (Yabancı Dizi)",
            "Dizilla" to "Dizilla (Yabancı Dizi)",
            "SezonlukDizi" to "SezonlukDizi (Yabancı Dizi)",
            "DiziMom" to "DiziMom (Yabancı Dizi)",
            "DiziGom" to "DiziGom (Yabancı Dizi)",
            "YabanciDizi" to "YabanciDizi (Yabancı Dizi)",
            "DDizi" to "DDizi (Yerli Dizi Arşivi)",
            "BelgeselX" to "BelgeselX (Belgesel)",
            "DMax" to "DMax (Discovery / Belgesel)",
            "TLC" to "TLC (TLC Belgesel & Reality)",
            "CanliTV" to "CanliTV (Canlı Kanallar)",
            "vavooSpor" to "vavooSpor (Canlı Spor)",
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
