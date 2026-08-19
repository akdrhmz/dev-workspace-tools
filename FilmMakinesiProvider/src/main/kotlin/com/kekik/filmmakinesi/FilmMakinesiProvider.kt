package com.kekik.filmmakinesi

import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import java.net.URLEncoder

class FilmMakinesiProvider : MainAPI() {
    override var mainUrl = "https://filmmakinesi.pw"
    override var name = "FilmMakinesi"
    override var lang = "tr"
    override val hasMainPage = true
    override val supportedTypes = setOf(TvType.Movie)

    override val mainPage = mainPageOf(
        "$mainUrl/filmler/page/"              to "Son Eklenen Filmler",
        "$mainUrl/en-cok-izlenen-filmler/page/" to "En Çok İzlenenler",
        "$mainUrl/imdb-7-uzeri-filmler/page/"   to "IMDb 7+ Filmler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val targetUrl = request.data + page
        val doc = app.get(targetUrl).document

        val items = doc.select("div.film-list div.film-item, article.post").mapNotNull { card ->
            val titleElem = card.selectFirst("h2.title, a.title, .film-title") ?: return@mapNotNull null
            val href = card.selectFirst("a[href]")?.attr("href") ?: return@mapNotNull null
            val poster = card.selectFirst("img")?.let { img ->
                img.attr("src").ifEmpty { img.attr("data-src") }
            }

            newMovieSearchResponse(titleElem.text().trim(), href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val doc = app.get("$mainUrl/?s=${URLEncoder.encode(query, "UTF-8")}").document

        return doc.select("div.film-list div.film-item, article.post").mapNotNull { card ->
            val titleElem = card.selectFirst("h2.title, a.title, .film-title") ?: return@mapNotNull null
            val href = card.selectFirst("a[href]")?.attr("href") ?: return@mapNotNull null
            val poster = card.selectFirst("img")?.let { img ->
                img.attr("src").ifEmpty { img.attr("data-src") }
            }

            newMovieSearchResponse(titleElem.text().trim(), href, TvType.Movie) {
                this.posterUrl = poster
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val doc = app.get(url).document
        val title = doc.selectFirst("h1.title, .entry-title")?.text()?.trim() ?: "Bilinmeyen Film"
        val poster = doc.selectFirst(".poster img, .film-poster img")?.let {
            it.attr("src").ifEmpty { it.attr("data-src") }
        }
        val plot = doc.selectFirst(".story, .film-content, .entry-content")?.text()?.trim()
        val tags = doc.select(".film-genres a, .genre a").map { it.text().trim() }
        val ratingValue = doc.selectFirst(".imdb-score, .score")?.text()
            ?.replace(",", ".")?.toDoubleOrNull()

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
        doc.select("iframe[src]").mapNotNull { it.attr("src").ifEmpty { null } }
            .forEach { iframeUrl ->
                loadExtractor(iframeUrl, mainUrl, subtitleCallback, callback)
            }
        return true
    }
}
