package com.kekik.watchbuddy

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
class WatchBuddyPlugin : Plugin() {
    companion object {
        const val PREF_KEY_ENABLED_SOURCES = "watchbuddy_enabled_sources"
        const val PREF_KEY_TMDB_API_KEY = "watchbuddy_tmdb_api_key"

        val ALL_SOURCES = listOf(
            "DiziBox"          to "DiziBox (Yabancı Dizi)",
            "FilmMakinesi"     to "FilmMakinesi (1080p Film)",
            "HDFilmCehennemi"  to "HDFilmCehennemi (Film & Dizi)",
            "Dizilla"          to "Dizilla (Dublaj & Altyazı Dizi)",
            "SineWix"          to "SineWix (Geniş Arşiv)",
            "JetFilmizle"      to "JetFilmizle (Yerli/Yabancı Film)"
        )
    }

    override fun load(context: Context) {
        registerMainAPI(WatchBuddyProvider())

        this.openSettings = openSettings@{ ctx ->
            showSettingsDialog(ctx)
        }
    }

    private fun showSettingsDialog(context: Context) {
        val options = arrayOf(
            "🔧 Kaynak Sağlayıcıları Seç (Aktif/Pasif)",
            "🔑 Özel TMDB API Anahtarı Tanımla",
            "ℹ️ WatchBuddy Universal Hakkında"
        )

        AlertDialog.Builder(context)
            .setTitle("⚙️ WatchBuddy Universal Ayarları")
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

        val labels = ALL_SOURCES.map { it.second }.toTypedArray()
        val checkedItems = ALL_SOURCES.map { currentEnabled.contains(it.first) }.toBooleanArray()
        val selectedKeys = currentEnabled.toMutableList()

        AlertDialog.Builder(context)
            .setTitle("🔍 Aktif Arama Kaynakları")
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
                Toast.makeText(context, "✅ Kaynak ayarları başarıyla güncellendi!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .setNeutralButton("Tümünü Seç") { _, _ ->
                setKey(PREF_KEY_ENABLED_SOURCES, ALL_SOURCES.map { it.first })
                Toast.makeText(context, "✅ Tüm kaynaklar etkinleştirildi!", Toast.LENGTH_SHORT).show()
            }
            .show()
    }

    private fun showTmdbApiKeyDialog(context: Context) {
        val currentKey = getKey<String>(PREF_KEY_TMDB_API_KEY) ?: ""
        val input = EditText(context).apply {
            setText(currentKey)
            hint = "TMDB v3 API Key (Boş bırakılırsa varsayılan kullanılır)"
            setSingleLine()
        }

        val container = LinearLayout(context).apply {
            orientation = LinearLayout.VERTICAL
            setPadding(50, 20, 50, 10)
            addView(input)
        }

        AlertDialog.Builder(context)
            .setTitle("🔑 TMDB API Anahtarı")
            .setView(container)
            .setPositiveButton("Kaydet") { _, _ ->
                val newKey = input.text.toString().trim()
                setKey(PREF_KEY_TMDB_API_KEY, newKey)
                Toast.makeText(context, "✅ TMDB API Anahtarı kaydedildi!", Toast.LENGTH_SHORT).show()
            }
            .setNegativeButton("İptal", null)
            .show()
    }

    private fun showAboutDialog(context: Context) {
        AlertDialog.Builder(context)
            .setTitle("✨ WatchBuddy Universal")
            .setMessage(
                "WatchBuddy Universal;\n\n" +
                "• Tekil TMDB Kataloğu (Netflix, HBO, Disney+, Prime, Apple TV, BluTV vb.)\n" +
                "• 170+ Türkçe kaynak arkasında paralel arama motoru (DiziBox, HDFC, FilmMakinesi, Dizilla vb.)\n" +
                "• CloudStream için sıfır kirlilik, maksimum hız ve 4K/1080p oynatma desteği sunar.\n\n" +
                "Sürüm: 2.0.0 (TMDB Meta-Engine)"
            )
            .setPositiveButton("Tamam", null)
            .show()
    }
}