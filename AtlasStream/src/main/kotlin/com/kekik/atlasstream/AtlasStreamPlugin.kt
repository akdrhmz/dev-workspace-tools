package com.kekik.atlasstream

import android.content.Context
import android.widget.EditText
import android.widget.LinearLayout
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.CloudStreamApp.Companion.setKey
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class AtlasStreamPlugin : Plugin() {
    companion object {
        const val PREF_KEY_ENABLED_SOURCES = "AtlasStream_enabled_sources"
        const val PREF_KEY_TMDB_API_KEY    = "AtlasStream_tmdb_api_key"

        val ALL_SOURCES = listOf(
            "DiziBox"          to "DiziBox (Yabanci Dizi)",
            "FilmMakinesi"     to "FilmMakinesi (1080p Film)",
            "HDFilmCehennemi"  to "HDFilmCehennemi (Film & Dizi)",
            "Dizilla"          to "Dizilla (Dublaj & Altyazi Dizi)",
            "SineWix"          to "SineWix (Genis Arsiv)",
            "JetFilmizle"      to "JetFilmizle (Yerli/Yabanci Film)",
            "DiziPal"          to "DiziPal (Geniþ Arþiv)",
            "FullHDFilmizlesene" to "FullHDFilmizlesene (Film Odaklý)",
            "FilmModu"         to "FilmModu (Film Odaklý)",
            "SezonlukDizi"     to "SezonlukDizi (Dizi Odaklý)",
            "CizgiMax"         to "CizgiMax (Çizgi Dizi/Film)",
            "DiziMom"          to "DiziMom (Dizi Odaklý)",
            "WebteIzle"        to "WebteIzle (Geniþ Arþiv)",
            "SetFilmIzle"      to "SetFilmIzle (Film Odaklý)",
            "KultFilmler"      to "KültFilmler (Nostalji/Film)",
            "TRanimaci"        to "TRanimaci (Çizgi Dizi)"
        )
    }

    override fun load(context: Context) {
        registerMainAPI(AtlasStreamProvider())

        this.openSettings = openSettings@{ ctx ->
            showSettingsDialog(ctx)
        }
    }

    private fun showSettingsDialog(context: Context) {
        val options = arrayOf(
            "\uD83D\uDD27 Kaynak Saglayicilari Sec (Aktif/Pasif)",
            "\uD83D\uDD11 Ozel TMDB API Anahtari Tanimla",
            "\u2139\uFE0F AtlasStream Universal Hakkinda"
        )

        AlertDialog.Builder(context)
            .setTitle("\u2699\uFE0F AtlasStream Universal Ayarlari")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> showSourceSelectionDialog(context)
                    1 -> showTmdbApiKeyDialog(context)
                    2 -> showAboutDialog(context)
                }
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun showSourceSelectionDialog(context: Context) {
        val currentEnabled = getKey<List<String>>(PREF_KEY_ENABLED_SOURCES)
            ?: ALL_SOURCES.map { it.first }

        val labels       = ALL_SOURCES.map { it.second }.toTypedArray()
        val checkedItems = ALL_SOURCES.map { currentEnabled.contains(it.first) }.toBooleanArray()
        val selectedKeys = currentEnabled.toMutableList()

        AlertDialog.Builder(context)
            .setTitle("\uD83D\uDD0D Aktif Arama Kaynaklari")
            .setMultiChoiceItems(labels, checkedItems) { _, index, isChecked ->
                val key = ALL_SOURCES[index].first
                if (isChecked) {
                    if (!selectedKeys.contains(key)) selectedKeys.add(key)
                } else {
                    selectedKeys.remove(key)
                }
            }
            .setPositiveButton("Kaydet") { _, _ ->
                setKey(PREF_KEY_ENABLED_SOURCES, selectedKeys.toList())
                Toast.makeText(context, "\u2705 Kaynak ayarlari guncellendi!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Iptal", null)
            .setNeutralButton("Tumunu Sec") { _, _ ->
                setKey(PREF_KEY_ENABLED_SOURCES, ALL_SOURCES.map { it.first })
                Toast.makeText(context, "\u2705 Tum kaynaklar etkinlestirildi!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showTmdbApiKeyDialog(context: Context) {
        val currentKey = getKey<String>(PREF_KEY_TMDB_API_KEY) ?: ""
        val input = EditText(context).apply {
            setText(currentKey)
            hint = "TMDB v3 API Key (Bos birakilirsa varsayilan kullanilir)"
            setSingleLine()
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
            addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle("\uD83D\uDD11 TMDB API Anahtari")
            .setView(container)
            .setPositiveButton("Kaydet") { _, _ ->
                val newKey = input.text.toString().trim()
                setKey(PREF_KEY_TMDB_API_KEY, newKey)
                Toast.makeText(context, "\u2705 TMDB API Anahtari kaydedildi!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("Iptal", null)
            .show()
    }

    private fun showAboutDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("\u2728 AtlasStream Universal")
            .setMessage(
                "AtlasStream Universal;\n\n" +
                "\u2022 Tekil TMDB Katalogu (Netflix, HBO, Disney+, Prime, Apple TV, BluTV vb.)\n" +
                "\u2022 6 Turkce kaynak motorunda paralel arama (DiziBox, HDFC, FilmMakinesi, Dizilla, SineWix, JetFilmizle)\n" +
                "\u2022 CloudStream icin sifir kirlilik, maksimum hiz ve 4K/1080p oynatma destegi sunar.\n\n" +
                "Surum: 3.0.0 (TMDB Meta-Engine + Fixed Resolvers)"
            )
            .setPositiveButton("Tamam", null)
            .show()
    }
}



