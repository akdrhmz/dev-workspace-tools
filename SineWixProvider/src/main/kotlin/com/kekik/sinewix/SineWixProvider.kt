package com.kekik.sinewix

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class SineWixProvider : MainAPI() {
    override var mainUrl = "https://sinewix.org"
    override var name = "SineWix"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/filmler/page/" to "Filmler",
        "$mainUrl/diziler/page/" to "Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val targetUrl = request.data + page
        val doc = app.get(targetUrl).document

        val items = doc.select(".movie-item, .post, .film-item").mapNotNull { card ->
            val titleElem = card.selectFirst(".movie-title, .title, h2, h3") ?: return@mapNotNull null
            val linkElem = card.selectFirst("a") ?: return@mapNotNull null
            val href = linkElem.attr("href")
            val img = card.selectFirst("img")
            val poster = img?.let { it.attr("data-src").ifEmpty { it.attr("src") } }

            newMovieSearchResponse(titleElem.text().trim(), href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=$query").document

        return doc.select(".movie-item, .post, .search-result").mapNotNull { card ->
            val titleElem = card.selectFirst(".movie-title, .title, h2, h3") ?: return@mapNotNull null
            val linkElem = card.selectFirst("a") ?: return@mapNotNull null
            val href = linkElem.attr("href")
            val img = card.selectFirst("img")
            val poster = img?.let { it.attr("data-src").ifEmpty { it.attr("src") } }

            newMovieSearchResponse(titleElem.text().trim(), href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1")?.text()?.trim() ?: "Bilinmeyen Başlık"
        val poster = doc.selectFirst(".poster img")?.attr("src")
        val plot = doc.selectFirst(".content, .story, .film-content")?.text()?.trim()

        val episodeElements = doc.select(".episodes a, .bolumler a")
        if (episodeElements.isNotEmpty()) {
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

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
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
