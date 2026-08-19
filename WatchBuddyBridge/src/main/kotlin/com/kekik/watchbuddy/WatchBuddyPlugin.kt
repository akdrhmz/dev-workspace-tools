package com.kekik.watchbuddy

import android.content.Context
import android.widget.Toast
import androidx.appcompat.app.AlertDialog
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.AcraApplication.Companion.setKey
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

enum class ProviderCategory(
    val title: String,
    val icon: String,
    val isNsfw: Boolean,
    val providers: List<String>
) {
    TR_MOVIE(
        "Türkçe Film Siteleri",
        "🎬",
        false,
        listOf("FilmMakinesi", "HDFilmCehennemi", "JetFilmizle", "FullHDFilmizlesene", "FilmModu", "720pIzle", "Webteizle")
    ),
    TR_SERIES(
        "Türkçe Yabancı Dizi Siteleri",
        "📺",
        false,
        listOf("DiziBox", "Dizilla", "Dizipal", "Dizifon", "RoketDizi", "DiziYo", "YabanciDizi")
    ),
    ANIME(
        "Anime & Çizgi Dizi Siteleri",
        "🎌",
        false,
        listOf("Animecix", "Türkanime", "DiziWatch", "AnimeKisa", "Gogoanime", "ZoroAnime")
    ),
    ASIAN_DRAMA(
        "Uzakdoğu & Kore Dizileri (K-Drama)",
        "🎎",
        false,
        listOf("Dramacool", "KissAsian", "AsyaFanatikleri", "Koreanturk", "MyAsianTV")
    ),
    ENGLISH_SOURCES(
        "İngilizce / Global Kaynaklar",
        "🌍",
        false,
        listOf("FlixHQ", "SFlix", "Fmovies", "123Movies", "LookMovie", "Vidsrc", "SuperStream")
    ),
    RUSSIAN_SOURCES(
        "Rusça Kaynaklar (RU)",
        "🇷🇺",
        false,
        listOf("Rezka", "KinoGo", "HDRezka", "ZetFlix", "Filmix")
    ),
    FRENCH_SOURCES(
        "Fransızca Kaynaklar (FR)",
        "🇫🇷",
        false,
        listOf("FrenchStream", "Wiflix", "Cpasmieux", "EmpireStreaming")
    ),
    GERMAN_SOURCES(
        "Almanca Kaynaklar (DE)",
        "🇩🇪",
        false,
        listOf("Kinox", "Movie4k", "BsTo", "StreamKiste")
    ),
    ADULT_NSFW(
        "Yetişkin & +18 İçerikler (NSFW)",
        "🔞",
        true,
        listOf("HentaiHaven", "HentaiMama", "Hanime", "MissAV", "JavGuru", "Eporner", "SpankBang")
    );

    companion object {
        fun allSafeProviders(): List<String> = values().filter { !it.isNsfw }.flatMap { it.providers }.distinct()
        fun allProviders(): List<String> = values().flatMap { it.providers }.distinct()
    }
}

@CloudstreamPlugin
class WatchBuddyPlugin : Plugin() {
    companion object {
        const val PREF_KEY_ENABLED_CATEGORIES = "wb_enabled_categories"
        const val PREF_KEY_ENABLED_PLUGINS = "wb_enabled_plugins"
        const val PREF_KEY_SAFE_MODE = "wb_safe_mode_enabled"

        val DEFAULT_SAFE_CATEGORIES = listOf(
            ProviderCategory.TR_MOVIE.name,
            ProviderCategory.TR_SERIES.name,
            ProviderCategory.ANIME.name,
            ProviderCategory.ASIAN_DRAMA.name
        )
    }

    override fun load(context: Context) {
        registerMainAPI(WatchBuddyProvider())
        openSettings = { ctx ->
            showMainSettingsDialog(ctx)
        }
    }

    private fun showMainSettingsDialog(context: Context) {
        val isSafeMode = getKey<Boolean>(PREF_KEY_SAFE_MODE) ?: true
        val safeModeStatus = if (isSafeMode) "🟢 AÇIK (Korumalı)" else "🔴 KAPALI"

        val options = arrayOf(
            "🛡️ Aile & Güvenli Mod [+18 Filtresi]: $safeModeStatus",
            "📂 Kategori ve Dil Paketleri (Tek Tıkla Seçim)",
            "⚡ Hızlı Hazır Profiller (Sadece TR / Global vb.)",
            "🔍 Tekil Site Listesi (İnce Ayar - 170+ Site)"
        )

        AlertDialog.Builder(context)
            .setTitle("⚙️ WatchBuddy Gelişmiş Filtreleme")
            .setItems(options) { _, which ->
                when (which) {
                    0 -> toggleSafeMode(context)
                    1 -> showCategorySettings(context)
                    2 -> showQuickProfiles(context)
                    3 -> showIndividualSiteSettings(context)
                }
            }
            .setNegativeButton("Kapat", null)
            .show()
    }

    private fun toggleSafeMode(context: Context) {
        val current = getKey<Boolean>(PREF_KEY_SAFE_MODE) ?: true
        val newStatus = !current
        setKey(PREF_KEY_SAFE_MODE, newStatus)

        if (newStatus) {
            // Safe mode açıldı -> +18 kategorisini ve sitelerini anında kapat
            val savedCats = getKey<List<String>>(PREF_KEY_ENABLED_CATEGORIES) ?: DEFAULT_SAFE_CATEGORIES
            val cleanCats = savedCats.filter { it != ProviderCategory.ADULT_NSFW.name }
            setKey(PREF_KEY_ENABLED_CATEGORIES, cleanCats)

            val cleanProviders = ProviderCategory.values()
                .filter { cleanCats.contains(it.name) && !it.isNsfw }
                .flatMap { it.providers }
                .distinct()
            setKey(PREF_KEY_ENABLED_PLUGINS, cleanProviders)

            Toast.makeText(context, "🛡️ Güvenli Mod Açıldı: Tüm +18/Yetişkin siteler kilitlendi!", Toast.LENGTH_LONG).show()
        } else {
            Toast.makeText(context, "⚠️ Güvenli Mod Kapatıldı: +18 kategoriler seçilebilir hale geldi.", Toast.LENGTH_LONG).show()
        }
    }

    private fun showCategorySettings(context: Context) {
        val isSafeMode = getKey<Boolean>(PREF_KEY_SAFE_MODE) ?: true
        val categories = if (isSafeMode) {
            ProviderCategory.values().filter { !it.isNsfw }.toTypedArray()
        } else {
            ProviderCategory.values()
        }

        val savedCats = getKey<List<String>>(PREF_KEY_ENABLED_CATEGORIES) ?: DEFAULT_SAFE_CATEGORIES
        val checkedItems = BooleanArray(categories.size) { i ->
            savedCats.contains(categories[i].name)
        }

        val selectedCats = savedCats.toMutableList()
        val labels = categories.map { "${it.icon} ${it.title} (${it.providers.size} Site)" }.toTypedArray()

        AlertDialog.Builder(context)
            .setTitle("📂 Kategori ve Dil Paketleri")
            .setMultiChoiceItems(labels, checkedItems) { _, which, isChecked ->
                val catName = categories[which].name
                if (isChecked) {
                    if (!selectedCats.contains(catName)) selectedCats.add(catName)
                } else {
                    selectedCats.remove(catName)
                }
            }
            .setPositiveButton("Kaydet & Uygula") { dialog, _ ->
                setKey(PREF_KEY_ENABLED_CATEGORIES, selectedCats)
                val enabledProviders = categories
                    .filter { selectedCats.contains(it.name) }
                    .flatMap { it.providers }
                    .distinct()
                setKey(PREF_KEY_ENABLED_PLUGINS, enabledProviders)

                Toast.makeText(
                    context,
                    "✅ ${selectedCats.size} kategori (${enabledProviders.size} site) aktif edildi!",
                    Toast.LENGTH_SHORT
                ).show()
                dialog.dismiss()
            }
            .setNegativeButton("Geri", null)
            .setNeutralButton("Sadece Türkçe") { dialog, _ ->
                val trCats = listOf(ProviderCategory.TR_MOVIE.name, ProviderCategory.TR_SERIES.name, ProviderCategory.ANIME.name)
                setKey(PREF_KEY_ENABLED_CATEGORIES, trCats)
                val enabled = ProviderCategory.values().filter { trCats.contains(it.name) }.flatMap { it.providers }.distinct()
                setKey(PREF_KEY_ENABLED_PLUGINS, enabled)
                Toast.makeText(context, "🇹🇷 Sadece Türkçe paketler etkinleştirildi", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }

    private fun showQuickProfiles(context: Context) {
        val profiles = arrayOf(
            "🇹🇷 Sadece Türkçe (Film + Dizi + TR Anime) [Güvenli]",
            "🎌 Anime & Uzakdoğu Odaklı (K-Drama + Anime + TR)",
            "🌍 Global & Yabancı Odaklı (EN + FR + DE + TR)",
            "🧹 Minimum & Hızlı Paket (En Popüler 5 Türkçe Site)",
            "⚡ Tüm Güvenli Siteler (+18 Kapalı)",
            "🔞 +18 / Yetişkin Paketi Dahil Et"
        )

        AlertDialog.Builder(context)
            .setTitle("⚡ Hızlı Hazır Profiller")
            .setItems(profiles) { dialog, which ->
                when (which) {
                    0 -> {
                        val cats = listOf(ProviderCategory.TR_MOVIE.name, ProviderCategory.TR_SERIES.name, ProviderCategory.ANIME.name)
                        setKey(PREF_KEY_SAFE_MODE, true)
                        applyCategories(context, cats, "Sadece Türkçe Profili (Güvenli)")
                    }
                    1 -> {
                        val cats = listOf(ProviderCategory.ANIME.name, ProviderCategory.ASIAN_DRAMA.name, ProviderCategory.TR_SERIES.name)
                        applyCategories(context, cats, "Anime & K-Drama Profili")
                    }
                    2 -> {
                        val cats = listOf(ProviderCategory.TR_MOVIE.name, ProviderCategory.TR_SERIES.name, ProviderCategory.ENGLISH_SOURCES.name, ProviderCategory.FRENCH_SOURCES.name)
                        applyCategories(context, cats, "Global Profil")
                    }
                    3 -> {
                        val cats = listOf(ProviderCategory.TR_MOVIE.name, ProviderCategory.TR_SERIES.name)
                        setKey(PREF_KEY_SAFE_MODE, true)
                        setKey(PREF_KEY_ENABLED_CATEGORIES, cats)
                        setKey(PREF_KEY_ENABLED_PLUGINS, listOf("FilmMakinesi", "DiziBox", "HDFilmCehennemi", "SineWix", "Dizilla"))
                        Toast.makeText(context, "⚡ Minimum 5 site aktif edildi", Toast.LENGTH_SHORT).show()
                    }
                    4 -> {
                        val safeCats = ProviderCategory.values().filter { !it.isNsfw }.map { it.name }
                        setKey(PREF_KEY_SAFE_MODE, true)
                        applyCategories(context, safeCats, "Tüm Güvenli Siteler")
                    }
                    5 -> {
                        setKey(PREF_KEY_SAFE_MODE, false)
                        val allCats = ProviderCategory.values().map { it.name }
                        applyCategories(context, allCats, "Yetişkin Dahil Tüm Paket")
                    }
                }
                dialog.dismiss()
            }
            .setNegativeButton("Geri", null)
            .show()
    }

    private fun applyCategories(context: Context, catNames: List<String>, profileName: String) {
        setKey(PREF_KEY_ENABLED_CATEGORIES, catNames)
        val enabled = ProviderCategory.values()
            .filter { catNames.contains(it.name) }
            .flatMap { it.providers }
            .distinct()
        setKey(PREF_KEY_ENABLED_PLUGINS, enabled)
        Toast.makeText(context, "✅ $profileName uygulandı (${enabled.size} site aktif)", Toast.LENGTH_SHORT).show()
    }

    private fun showIndividualSiteSettings(context: Context) {
        val isSafeMode = getKey<Boolean>(PREF_KEY_SAFE_MODE) ?: true
        val allSources = if (isSafeMode) {
            ProviderCategory.allSafeProviders().sorted().toTypedArray()
        } else {
            ProviderCategory.allProviders().sorted().toTypedArray()
        }

        val savedEnabled = getKey<List<String>>(PREF_KEY_ENABLED_PLUGINS) ?: allSources.toList()
        val checkedItems = BooleanArray(allSources.size) { i ->
            savedEnabled.contains(allSources[i])
        }

        val selectedList = savedEnabled.toMutableList()

        AlertDialog.Builder(context)
            .setTitle("🔍 Tekil Site Seçimi (${allSources.size} Site)")
            .setMultiChoiceItems(allSources, checkedItems) { _, which, isChecked ->
                val sourceName = allSources[which]
                if (isChecked) {
                    if (!selectedList.contains(sourceName)) selectedList.add(sourceName)
                } else {
                    selectedList.remove(sourceName)
                }
            }
            .setPositiveButton("Kaydet") { dialog, _ ->
                setKey(PREF_KEY_ENABLED_PLUGINS, selectedList)
                Toast.makeText(context, "✅ Kaydedildi (${selectedList.size} site aktif)", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .setNegativeButton("Geri", null)
            .setNeutralButton("Tümünü Aç") { dialog, _ ->
                setKey(PREF_KEY_ENABLED_PLUGINS, allSources.toList())
                Toast.makeText(context, "Tüm siteler açıldı", Toast.LENGTH_SHORT).show()
                dialog.dismiss()
            }
            .show()
    }
}
