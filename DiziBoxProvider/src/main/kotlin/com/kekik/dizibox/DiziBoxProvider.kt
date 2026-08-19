package com.kekik.dizibox

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class DiziBoxProvider : MainAPI() {
    override var mainUrl = "https://www.dizibox.tv"
    override var name = "DiziBox"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/diziler/page/"              to "Tüm Diziler",
        "$mainUrl/son-eklenen-bolumler/page/" to "Son Eklenen Bölümler",
        "$mainUrl/trend-diziler/page/"        to "Trend Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val targetUrl = request.data + page
        val doc = app.get(targetUrl).document

        val items = doc.select(".post-item, .tv-item, article.post").mapNotNull { card ->
            val titleElem = card.selectFirst(".entry-title, .title a, h2 a") ?: return@mapNotNull null
            val href = card.selectFirst("a[href]")?.attr("href") ?: return@mapNotNull null
            val poster = card.selectFirst("img")?.attr("src")

            newTvSeriesSearchResponse(titleElem.text().trim(), href, TvType.TvSeries) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=${URLEncoder.encode(query, "UTF-8")}").document

        return doc.select(".post-item, .search-item, article").mapNotNull { card ->
            val titleElem = card.selectFirst(".entry-title, .title a, h2 a") ?: return@mapNotNull null
            val href = card.selectFirst("a[href]")?.attr("href") ?: return@mapNotNull null
            val poster = card.selectFirst("img")?.attr("src")

            newTvSeriesSearchResponse(titleElem.text().trim(), href, TvType.TvSeries) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.entry-title, .series-title")?.text()?.trim() ?: "Bilinmeyen Dizi"
        val poster = doc.selectFirst(".poster img, .series-poster img")?.attr("src")
        val plot = doc.selectFirst(".overview, .story, .entry-content")?.text()?.trim()

        val episodes = doc.select(".episodes-list a, .bolumler a, .episode-item a").mapNotNull { epLink ->
            val epUrl = epLink.attr("href")
            val epTitle = epLink.text().trim()
            if (epUrl.isEmpty()) return@mapNotNull null

            newEpisode(epUrl) {
                this.name = epTitle
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
