package com.kekik.dizilla

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class DizillaProvider : MainAPI() {
    override var mainUrl = "https://dizilla.club"
    override var name = "Dizilla"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries, TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/tum-diziler/page/" to "Tüm Diziler",
        "$mainUrl/trend-diziler/page/" to "Trend Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val targetUrl = request.data + page
        val doc = app.get(targetUrl).document

        val items = doc.select(".poster-box, .media-item, .dizi-item").mapNotNull { card ->
            val titleElem = card.selectFirst(".title, h3, h2") ?: return@mapNotNull null
            val linkElem = card.selectFirst("a") ?: return@mapNotNull null
            val href = linkElem.attr("href")
            val img = card.selectFirst("img")
            val poster = img?.let { it.attr("data-src").ifEmpty { it.attr("src") } }

            newTvSeriesSearchResponse(titleElem.text().trim(), href, TvType.TvSeries) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/arama?q=${URLEncoder.encode(query, "UTF-8")}").document

        return doc.select(".search-result-item, .poster-box, .media-item").mapNotNull { card ->
            val titleElem = card.selectFirst(".title, h3, h2") ?: return@mapNotNull null
            val linkElem = card.selectFirst("a") ?: return@mapNotNull null
            val href = linkElem.attr("href")
            val img = card.selectFirst("img")
            val poster = img?.let { it.attr("data-src").ifEmpty { it.attr("src") } }

            newTvSeriesSearchResponse(titleElem.text().trim(), href, TvType.TvSeries) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: "Bilinmeyen Dizi"
        val poster = doc.selectFirst(".poster img")?.attr("src")
        val plot = doc.selectFirst(".summary, .story, .dizi-ozet")?.text()?.trim()

        val episodeElements = doc.select(".episodes-list a, .season-list a, .bolumler a")
        val episodes = episodeElements.mapNotNull { ep ->
            val epHref = ep.attr("href")
            if (epHref.isEmpty()) return@mapNotNull null
            newEpisode(epHref) {
                this.name = ep.text().trim()
                this.posterUrl = poster
            }
        }

        return newTvSeriesLoadResponse(title, url, TvType.TvSeries, episodes) {
            this.posterUrl = poster
            this.plot = plot
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document
        doc.select("iframe[src]").mapNotNull { it.attr("src").ifEmpty { null } }
            .forEach { iframeUrl ->
                loadExtractor(iframeUrl, mainUrl, subtitleCallback, callback)
            }
        return true
    }
}
