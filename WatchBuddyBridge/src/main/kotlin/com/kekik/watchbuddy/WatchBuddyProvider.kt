package com.kekik.watchbuddy

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.AcraApplication.Companion.getKey
import com.lagradost.cloudstream3.utils.*

class WatchBuddyProvider : MainAPI() {
    override var mainUrl = "https://stream.watchbuddy.tv"
    override var name = "WatchBuddy Universal (170+ Site)"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.Cartoon,
        TvType.Documentary
    )

    private val apiUrl = "$mainUrl/api/v1"

    private fun getActivePlugins(): List<String> {
        return getKey<List<String>>(WatchBuddyPlugin.PREF_KEY_ENABLED_PLUGINS)
            ?: ProviderCategory.allProviders()
    }

    override val mainPage = mainPageOf(
        "$apiUrl/get_main_page?plugin=FilmMakinesi" to "FilmMakinesi - Son Filmler",
        "$apiUrl/get_main_page?plugin=DiziBox"      to "DiziBox - Son Diziler",
        "$apiUrl/get_main_page?plugin=HDFilmCehennemi" to "HDFilmCehennemi - Popüler",
        "$apiUrl/get_main_page?plugin=SineWix"      to "SineWix - Güncel",
        "$apiUrl/get_main_page?plugin=Dizilla"      to "Dizilla - Trend Diziler",
        "$apiUrl/get_main_page?plugin=Animecix"     to "Animecix - Güncel Animeler",
        "$apiUrl/get_main_page?plugin=JetFilmizle"  to "JetFilmizle - Yeni Filmler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val targetUrl = "${request.data}&page=$page"
        val response = app.get(targetUrl)
        val items = response.parsedSafe<List<WbMainPageItem>>() ?: emptyList()

        val searchResponses = items.map { item ->
            newMovieSearchResponse(item.title, item.url, TvType.Movie) {
                this.posterUrl = item.poster
            }
        }
        return newHomePageResponse(request.name, searchResponses)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val activePlugins = getActivePlugins()
        val searchUrl = "$apiUrl/search?q=$query"
        val response = app.get(searchUrl)
        val items = response.parsedSafe<List<WbSearchItem>>() ?: emptyList()

        // Kullanıcının eklenti ayarlarında seçtiği sitelere göre filtreleme yap
        val filteredItems = items.filter { item ->
            val pluginName = item.plugin
            pluginName == null || activePlugins.contains(pluginName)
        }

        return filteredItems.map { item ->
            val tvType = if (item.mediaType?.lowercase() == "series") TvType.TvSeries else TvType.Movie
            newMovieSearchResponse(
                item.title,
                item.url,
                tvType
            ) {
                this.posterUrl = item.poster
                this.year = item.year?.toIntOrNull()
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val loadUrl = "$apiUrl/load_item?url=$url"
        val response = app.get(loadUrl)
        val detail = response.parsed<WbItemDetail>()

        val ratingInt = detail.rating?.replace(",", ".")?.toDoubleOrNull()?.times(100)?.toInt()

        if (!detail.episodes.isNullOrEmpty()) {
            val episodes = detail.episodes.map { ep ->
                newEpisode(ep.url) {
                    this.name = ep.title
                    this.season = ep.season ?: 1
                    this.episode = ep.episode
                    this.posterUrl = ep.poster ?: detail.poster
                }
            }
            return newTvSeriesLoadResponse(detail.title, url, TvType.TvSeries, episodes) {
                this.posterUrl = detail.poster
                this.plot = detail.description
                this.tags = detail.genres ?: emptyList()
                this.rating = ratingInt
                this.year = detail.year?.toIntOrNull()
            }
        } else {
            return newMovieLoadResponse(detail.title, url, TvType.Movie, url) {
                this.posterUrl = detail.poster
                this.plot = detail.description
                this.tags = detail.genres ?: emptyList()
                this.rating = ratingInt
                this.year = detail.year?.toIntOrNull()
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val linksUrl = "$apiUrl/load_links?url=$data"
        val response = app.get(linksUrl)
        val extractResults = response.parsedSafe<List<WbExtractResult>>() ?: emptyList()

        for (res in extractResults) {
            // Subtitles
            res.subtitles?.forEach { sub ->
                subtitleCallback.invoke(
                    SubtitleFile(
                        lang = sub.language ?: "tr",
                        url = sub.url
                    )
                )
            }

            // Direct stream link
            if (res.url.startsWith("http")) {
                val isM3u8 = res.isM3u8 ?: res.url.contains(".m3u8")
                callback.invoke(
                    newExtractorLink(
                        source = res.name ?: name,
                        name = res.name ?: name,
                        url = res.url,
                        type = if (isM3u8) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        this.referer = res.referer ?: mainUrl
                        this.quality = if (res.quality == "1080p") Qualities.P1080.value else Qualities.Unknown.value
                        this.headers = res.headers ?: emptyMap()
                    }
                )
            } else {
                loadExtractor(res.url, data, subtitleCallback, callback)
            }
        }
        return true
    }
}
