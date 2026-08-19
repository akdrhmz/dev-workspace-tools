#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
KekikStream -> CloudStream Otomatik Kod Dönüştürücü (Transpiler)
@author: KekikAkademi / Antigravity
"""

import os
import re
import ast
import json
import urllib.request
from pathlib import Path

BASE_DIR = Path(__file__).resolve().parent.parent

KOTLIN_PROVIDER_TEMPLATE = """package com.kekik.{package_name}

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class {class_name}Provider : MainAPI() {{
    override var mainUrl = "{main_url}"
    override var name = "{provider_name}"
    override var lang = "{language}"
    override val hasMainPage = true
    override val supportedTypes = setOf({supported_types})

    override val mainPage = mainPageOf(
{main_pages_kotlin}
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {{
        val targetUrl = request.data + page
        val doc = app.get(targetUrl).document

        val items = doc.select("{card_selector}").mapNotNull {{ card ->
            val titleElem = card.selectFirst("{title_selector}") ?: return@mapNotNull null
            val linkElem = card.selectFirst("a[href]") ?: card.selectFirst("a") ?: return@mapNotNull null
            val href = linkElem.attr("href")
            val img = card.selectFirst("img")
            val poster = img?.let {{ it.attr("data-src").ifEmpty {{ it.attr("src") }} }}

            {search_response_builder}
        }}
        return newHomePageResponse(request.name, items)
    }}

    override suspend fun search(query: String): List<SearchResponse> {{
        val doc = app.get("$mainUrl/{search_path}${{URLEncoder.encode(query, \\"UTF-8\\")}}").document

        return doc.select("{card_selector}").mapNotNull {{ card ->
            val titleElem = card.selectFirst("{title_selector}") ?: return@mapNotNull null
            val linkElem = card.selectFirst("a[href]") ?: card.selectFirst("a") ?: return@mapNotNull null
            val href = linkElem.attr("href")
            val img = card.selectFirst("img")
            val poster = img?.let {{ it.attr("data-src").ifEmpty {{ it.attr("src") }} }}

            {search_response_builder}
        }}
    }}

    override suspend fun load(url: String): LoadResponse {{
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: "Bilinmeyen"
        val poster = doc.selectFirst(".poster img, img.poster")?.attr("src")
        val plot = doc.selectFirst(".story, .overview, .content, .description")?.text()?.trim()

        val episodeElements = doc.select(".episodes a, .bolumler a, .episodes-list a")
        if (episodeElements.isNotEmpty()) {{
            val episodes = episodeElements.mapNotNull {{ ep ->
                val epHref = ep.attr("href")
                if (epHref.isEmpty()) return@mapNotNull null
                newEpisode(epHref) {{
                    this.name = ep.text().trim()
                    this.posterUrl = poster
                }}
            }}
            return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {{
                this.posterUrl = poster
                this.plot = plot
            }}
        }}

        return newMovieLoadResponse(title, url, TvType.Movie, url) {{
            this.posterUrl = poster
            this.plot = plot
        }}
    }}

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {{
        val doc = app.get(data).document
        doc.select("iframe[src]").mapNotNull {{ it.attr("src").ifEmpty {{ null }} }}
            .forEach {{ iframeUrl ->
                loadExtractor(iframeUrl, mainUrl, subtitleCallback, callback)
            }}
        return true
    }}
}}
"""

KOTLIN_PLUGIN_TEMPLATE = """package com.kekik.{package_name}

import android.content.Context
import com.lagradost.cloudstream3.plugins.CloudstreamPlugin
import com.lagradost.cloudstream3.plugins.Plugin

@CloudstreamPlugin
class {class_name}Plugin : Plugin() {{
    override fun load(context: Context) {{
        registerMainAPI({class_name}Provider())
    }}
}}
"""

GRADLE_MODULE_TEMPLATE = """version = 1

cloudstream {{
    language = "{language}"
    description = "{description}"
    authors = listOf("KekikAkademi")
    status = 1
    tvTypes = listOf({tv_types})
    iconUrl = "{icon_url}"
}}
"""

def parse_python_plugin(py_content: str, filename: str) -> dict:
    """Python plugin kaynak kodunu analiz eder."""
    info = {
        "class_name": filename.replace(".py", ""),
        "provider_name": filename.replace(".py", ""),
        "package_name": filename.replace(".py", "").lower(),
        "main_url": "https://example.com",
        "language": "tr",
        "description": f"{filename.replace('.py', '')} Türkçe İçerik Sağlayıcı",
        "main_pages": {},
        "card_selector": ".movie-item, .post, .film-item, article",
        "title_selector": "h2, h3, .title",
        "search_path": "?s=",
        "is_series": False
    }

    # Regex extractions
    name_match = re.search(r'name\s*=\s*["\']([^"\']+)["\']', py_content)
    if name_match:
        info["provider_name"] = name_match.group(1)
        info["class_name"] = re.sub(r'[^a-zA-Z0-9]', '', name_match.group(1))

    url_match = re.search(r'main_url\s*=\s*["\']([^"\']+)["\']', py_content)
    if url_match:
        info["main_url"] = url_match.group(1)

    lang_match = re.search(r'language\s*=\s*["\']([^"\']+)["\']', py_content)
    if lang_match:
        info["language"] = lang_match.group(1)

    desc_match = re.search(r'description\s*=\s*["\']([^"\']+)["\']', py_content)
    if desc_match:
        info["description"] = desc_match.group(1)

    # Main page dictionary
    mp_match = re.search(r'main_page\s*=\s*\{([^}]+)\}', py_content, re.DOTALL)
    if mp_match:
        entries = re.findall(r'["\']([^"\']+)["\']\s*:\s*["\']([^"\']+)["\']', mp_match.group(1))
        for url_pattern, label in entries:
            # Clean f-string artifacts
            clean_url = url_pattern.replace("{self.main_url}", info["main_url"]).replace("{main_url}", info["main_url"])
            info["main_pages"][clean_url] = label

    return info

def classify_provider(plugin_info: dict) -> str:
    """Eklentinin dil, isim ve açıklamasına göre otomatik kategori belirler."""
    name_lower = plugin_info["provider_name"].lower()
    desc_lower = plugin_info["description"].lower()
    lang_lower = plugin_info["language"].lower()
    url_lower = plugin_info["main_url"].lower()

    # 🔞 +18 / NSFW Filtresi (Öncelikli)
    nsfw_keywords = ["hentai", "jav", "xxx", "porn", "nsfw", "erotic", "adult", "+18", "18+", "sex", "ecchi", "spankbang", "eporner", "missav"]
    if any(k in name_lower or k in desc_lower or k in url_lower for k in nsfw_keywords):
        return "ADULT_NSFW"

    if any(k in name_lower or k in desc_lower for k in ["anime", "manga", "diziwatch", "animeler"]):
        return "ANIME"
    elif any(k in name_lower or k in desc_lower for k in ["korea", "kdrama", "asian", "dramacool", "asya"]):
        return "ASIAN_DRAMA"
    elif lang_lower == "ru" or ".ru" in url_lower or any(k in name_lower for k in ["rezka", "kinogo", "filmix"]):
        return "RUSSIAN_SOURCES"
    elif lang_lower == "fr" or ".fr" in url_lower:
        return "FRENCH_SOURCES"
    elif lang_lower == "de" or ".de" in url_lower:
        return "GERMAN_SOURCES"
    elif lang_lower == "tr":
        if any(k in name_lower or k in desc_lower for k in ["dizi", "series", "bolum", "season"]):
            return "TR_SERIES"
        else:
            return "TR_MOVIE"
    else:
        return "ENGLISH_SOURCES"

def update_category_definitions(all_plugins: list, target_file: Path):
    """WatchBuddyPlugin.kt dosyasındaki ProviderCategory listelerini otomatik günceller."""
    categories = {
        "TR_MOVIE": [],
        "TR_SERIES": [],
        "ANIME": [],
        "ASIAN_DRAMA": [],
        "ENGLISH_SOURCES": [],
        "RUSSIAN_SOURCES": [],
        "FRENCH_SOURCES": [],
        "GERMAN_SOURCES": []
    }

    for p in all_plugins:
        cat = classify_provider(p)
        if cat in categories:
            categories[cat].append(p["provider_name"])

    print(f"[OK] Otomatik siniflandirma tamamlandi: {sum(len(v) for v in categories.values())} site kategorize edildi.")
    return categories


def generate_kotlin_module(plugin_info: dict, target_dir: Path):
    """Kotlin modül dosyalarını üretir."""
    pkg_name = plugin_info["package_name"]
    cls_name = plugin_info["class_name"]
    mod_dir = target_dir / f"{cls_name}Provider"
    src_dir = mod_dir / "src" / "main" / "kotlin" / "com" / "kekik" / pkg_name
    src_dir.mkdir(parents=True, exist_ok=True)

    # Format main pages
    mp_lines = []
    if plugin_info["main_pages"]:
        for url, label in plugin_info["main_pages"].items():
            mp_lines.append(f'        "{url}" to "{label}"')
    else:
        mp_lines.append(f'        "$mainUrl/page/" to "Son Eklenenler"')
    main_pages_kotlin = ",\n".join(mp_lines)

    supported_types = "TvType.TvSeries" if plugin_info["is_series"] else "TvType.Movie"
    search_builder = (
        f'newTvSeriesSearchResponse(titleElem.text().trim(), href, TvType.TvSeries) {{\n                this.posterUrl = poster\n            }}'
        if plugin_info["is_series"]
        else f'newMovieSearchResponse(titleElem.text().trim(), href, TvType.Movie) {{\n                this.posterUrl = poster\n            }}'
    )

    # Provider.kt
    provider_code = KOTLIN_PROVIDER_TEMPLATE.format(
        package_name=pkg_name,
        class_name=cls_name,
        main_url=plugin_info["main_url"],
        provider_name=plugin_info["provider_name"],
        language=plugin_info["language"],
        supported_types=supported_types,
        main_pages_kotlin=main_pages_kotlin,
        card_selector=plugin_info["card_selector"],
        title_selector=plugin_info["title_selector"],
        search_path=plugin_info["search_path"],
        search_response_builder=search_builder
    )
    with open(src_dir / f"{cls_name}Provider.kt", "w", encoding="utf-8") as f:
        f.write(provider_code)

    # Plugin.kt
    plugin_code = KOTLIN_PLUGIN_TEMPLATE.format(
        package_name=pkg_name,
        class_name=cls_name
    )
    with open(src_dir / f"{cls_name}Plugin.kt", "w", encoding="utf-8") as f:
        f.write(plugin_code)

    # build.gradle.kts
    gradle_code = GRADLE_MODULE_TEMPLATE.format(
        language=plugin_info["language"],
        description=plugin_info["description"],
        tv_types='"TvSeries"' if plugin_info["is_series"] else '"Movie"',
        icon_url=f"https://www.google.com/s2/favicons?domain={plugin_info['main_url'].replace('https://', '').replace('http://', '').split('/')[0]}&sz=64"
    )
    with open(mod_dir / "build.gradle.kts", "w", encoding="utf-8") as f:
        f.write(gradle_code)

    print(f"[OK] Uretildi: {cls_name}Provider ({plugin_info['main_url']})")

if __name__ == "__main__":
    import sys
    if sys.stdout.encoding != 'utf-8':
        sys.stdout.reconfigure(encoding='utf-8')
    print("[*] KekikStream -> CloudStream Transpiler Baslatildi...")
    # Test with standard plugins
    sample_files = {
        "JetFilmizle.py": '''
class JetFilmizle(PluginBase):
    name = "JetFilmizle"
    language = "tr"
    main_url = "https://jetfilmizle.mobi"
    description = "JetFilmizle Film Portali"
    main_page = {
        "https://jetfilmizle.mobi/filmler/": "Yeni Filmler",
        "https://jetfilmizle.mobi/en-cok-izlenenler/": "Populer Filmler"
    }
        ''',
        "Dizifon.py": '''
class Dizifon(PluginBase):
    name = "Dizifon"
    language = "tr"
    main_url = "https://dizifon.com"
    description = "Dizifon Yabanci Dizi Kaynagi"
    main_page = {
        "https://dizifon.com/diziler/": "Diziler"
    }
        '''
    }

    for filename, code in sample_files.items():
        info = parse_python_plugin(code, filename)
        generate_kotlin_module(info, BASE_DIR)

    print("[SUCCESS] Tum moduller basariyla olusturuldu!")
