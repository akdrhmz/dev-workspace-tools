package com.kekik.hdfilmcehennemi

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*

class HDFilmCehennemiProvider : MainAPI() {
    override var mainUrl = "https://www.hdfilmcehennemi.nl"
    override var name = "HDFilmCehennemi"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie, TvType.TvSeries)

    override val mainPage = mainPageOf(
        "$mainUrl/page/"                   to "Son Eklenenler",
        "$mainUrl/filmler/page/"           to "Filmler",
        "$mainUrl/en-cok-izlenenler/page/" to "En Çok İzlenenler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val targetUrl = request.data + page
        val doc = app.get(targetUrl).document

        val items = doc.select(".card-poster, .poster, .film-item").mapNotNull { card ->
            val linkElem = card.selectFirst("a") ?: return@mapNotNull null
            val titleElem = card.selectFirst(".title, h2, h3") ?: return@mapNotNull null
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
        val doc = app.get("$mainUrl/search?q=$query").document

        return doc.select(".card-poster, .search-result, .film-item").mapNotNull { card ->
            val linkElem = card.selectFirst("a") ?: return@mapNotNull null
            val titleElem = card.selectFirst(".title, h2, h3") ?: return@mapNotNull null
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
        val title = doc.selectFirst("h1.movie-title, .title")?.text()?.trim() ?: "Bilinmeyen Film"
        val poster = doc.selectFirst(".poster img")?.let {
            it.attr("data-src").ifEmpty { it.attr("src") }
        }
        val plot = doc.selectFirst(".movie-overview, .overview, .story")?.text()?.trim()
        val tags = doc.select(".genre a, .genres a").map { it.text().trim() }
        val ratingValue = doc.selectFirst(".rating, .imdb")?.text()
            ?.replace(",", ".")?.toDoubleOrNull()

        // Check if there are series episodes
        val episodeElements = doc.select(".episodes-list a, .season-list a")
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
                this.tags = tags
                this.score = Score.from10(ratingValue)
            }
        }

        return newMovieLoadResponse(title, url, TvType.Movie, url) {
            this.posterUrl = poster
            this.plot = plot
            this.tags = tags
            this.score = Score.from10(ratingValue)
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val doc = app.get(data).document

        // Player iframes
        doc.select("iframe[src]").mapNotNull { it.attr("src").ifEmpty { null } }
            .forEach { iframeUrl ->
                loadExtractor(iframeUrl, mainUrl, subtitleCallback, callback)
            }

        // Data-video player buttons
        doc.select("[data-video], .play-btn").mapNotNull { it.attr("data-video").ifEmpty { null } }
            .forEach { videoUrl ->
                loadExtractor(videoUrl, mainUrl, subtitleCallback, callback)
            }

        return true
    }
}
