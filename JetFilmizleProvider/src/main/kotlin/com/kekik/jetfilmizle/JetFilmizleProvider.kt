package com.kekik.jetfilmizle

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class JetFilmizleProvider : MainAPI() {
    override var mainUrl = "https://jetfilmizle.mobi"
    override var name = "JetFilmizle"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "https://jetfilmizle.mobi/filmler/" to "Yeni Filmler",
        "https://jetfilmizle.mobi/en-cok-izlenenler/" to "Populer Filmler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val targetUrl = request.data + page
        val doc = app.get(targetUrl).document

        val items = doc.select(".movie-item, .post, .film-item, article").mapNotNull { card ->
            val titleElem = card.selectFirst("h2, h3, .title") ?: return@mapNotNull null
            val linkElem = card.selectFirst("a[href]") ?: card.selectFirst("a") ?: return@mapNotNull null
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

        return doc.select(".movie-item, .post, .film-item, article").mapNotNull { card ->
            val titleElem = card.selectFirst("h2, h3, .title") ?: return@mapNotNull null
            val linkElem = card.selectFirst("a[href]") ?: card.selectFirst("a") ?: return@mapNotNull null
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
        val title = doc.selectFirst("h1")?.text()?.trim() ?: "Bilinmeyen"
        val poster = doc.selectFirst(".poster img, img.poster")?.attr("src")
        val plot = doc.selectFirst(".story, .overview, .content, .description")?.text()?.trim()

        val episodeElements = doc.select(".episodes a, .bolumler a, .episodes-list a")
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
