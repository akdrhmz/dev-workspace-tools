package com.kekik.atlasstream

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.LoadResponse.Companion.addActors
import com.lagradost.cloudstream3.LoadResponse.Companion.addTrailer
import com.lagradost.cloudstream3.utils.AppUtils
import com.lagradost.cloudstream3.utils.AppUtils.toJson
import com.lagradost.cloudstream3.utils.ExtractorLink
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope

class AtlasStreamProvider : MainAPI() {
    override var mainUrl = "https://www.themoviedb.org"
    override var name = "AtlasStream Universal"
    override var lang = "tr"
    override val hasMainPage = true
    override val hasQuickSearch = true
    override val supportedTypes = setOf(
        TvType.Movie,
        TvType.TvSeries,
        TvType.Anime,
        TvType.Cartoon,
        TvType.Documentary
    )

    override val mainPage = mainPageOf(
        // Trendler ve Vizyon
        "trending/all/day"                                                  to "?? Günün Trendleri",
        "movie/now_playing"                                                 to "?? Vizyondaki Filmler",
        "movie/popular"                                                     to "? Popüler Filmler",
        "tv/popular"                                                        to "?? Popüler Diziler",

        // Platformlar
        "discover/tv?with_watch_providers=8&watch_region=TR"                to "?? Netflix Dizileri",
        "discover/movie?with_watch_providers=8&watch_region=TR"             to "?? Netflix Filmleri",
        "discover/tv?with_watch_providers=1899|384&watch_region=TR"         to "?? HBO Max Dizileri",
        "discover/tv?with_watch_providers=337&watch_region=TR"              to "?? Disney+ Ýçerikleri",
        "discover/tv?with_watch_providers=119&watch_region=TR"              to "?? Amazon Prime Dizileri",
        "discover/tv?with_watch_providers=350|2&watch_region=TR"            to "?? Apple TV+ Dizileri",
        "discover/tv?with_watch_providers=341&watch_region=TR"              to "?? BluTV Ýçerikleri",

        // Türler (Genres)
        "discover/movie?with_genres=28,12"                                  to "?? Aksiyon & Macera",
        "discover/movie?with_genres=16"                                     to "?? Animasyon",
        "discover/movie?with_genres=35"                                     to "?? Komedi",
        "discover/movie?with_genres=80,9648"                                to "?? Suç & Gizem",
        "discover/movie?with_genres=878,14"                                 to "?? Bilim Kurgu & Fantastik",
        "discover/movie?with_genres=27,53"                                  to "?? Korku & Gerilim",
        "discover/movie?with_genres=10749"                                  to "?? Romantik",
        "discover/movie?with_genres=99"                                     to "?? Belgesel",

        // Top Listeler
        "movie/top_rated"                                                   to "?? IMDb En Yüksek Puanlý Filmler",
        "tv/top_rated"                                                      to "?? En Yüksek Puanlý Diziler"
    )

    override suspend fun getMainPage(page: Int, request: MainPageRequest): HomePageResponse {
        val tmdbResponse = TmdbApi.getItemsFromEndpoint(request.data, page)
        val items = tmdbResponse.results.filter { it.posterPath != null }.map { item ->
            val isMovie = item.mediaType == "movie" || request.data.contains("movie")
            val payload = AtlasStreamMediaData(
                tmdbId = item.id,
                isMovie = isMovie,
                title = item.displayTitle,
                originalTitle = item.originalTitle ?: item.originalName,
                year = item.releaseYear
            )
            val jsonPayload = payload.toJson()

            if (isMovie) {
                newMovieSearchResponse(item.displayTitle, jsonPayload, TvType.Movie) {
                    this.posterUrl = item.fullPosterUrl
                    this.year = item.releaseYear
                }
            } else {
                newTvSeriesSearchResponse(item.displayTitle, jsonPayload, TvType.TvSeries) {
                    this.posterUrl = item.fullPosterUrl
                    this.year = item.releaseYear
                }
            }
        }
        return newHomePageResponse(request.name, items)
    }

    override suspend fun search(query: String): List<SearchResponse> {
        val tmdbResponse = TmdbApi.search(query)
        return tmdbResponse.results.filter { it.posterPath != null && (it.mediaType == "movie" || it.mediaType == "tv") }.map { item ->
            val isMovie = item.mediaType == "movie"
            val payload = AtlasStreamMediaData(
                tmdbId = item.id,
                isMovie = isMovie,
                title = item.displayTitle,
                originalTitle = item.originalTitle ?: item.originalName,
                year = item.releaseYear
            )
            val jsonPayload = payload.toJson()

            if (isMovie) {
                newMovieSearchResponse(item.displayTitle, jsonPayload, TvType.Movie) {
                    this.posterUrl = item.fullPosterUrl
                    this.year = item.releaseYear
                }
            } else {
                newTvSeriesSearchResponse(item.displayTitle, jsonPayload, TvType.TvSeries) {
                    this.posterUrl = item.fullPosterUrl
                    this.year = item.releaseYear
                }
            }
        }
    }

    override suspend fun load(url: String): LoadResponse {
        val media: AtlasStreamMediaData = try {
            AppUtils.parseJson<AtlasStreamMediaData>(url)
        } catch (e: Exception) {
            val id = url.filter { it.isDigit() }.toIntOrNull() ?: 550
            AtlasStreamMediaData(tmdbId = id, isMovie = true, title = "Ýçerik", originalTitle = null, year = null)
        }

        if (media.isMovie) {
            val detail = TmdbApi.getMovieDetail(media.tmdbId)
            val actors = detail.credits?.cast?.take(10)?.map { Actor(it.name, it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }) } ?: emptyList()
            val trailer = detail.videos?.results?.firstOrNull { it.site.equals("YouTube", true) && it.type.equals("Trailer", true) }?.let { "https://www.youtube.com/watch?v=${it.key}" }

            val playPayload = AtlasStreamMediaData(
                tmdbId = detail.id,
                isMovie = true,
                title = detail.displayTitle,
                originalTitle = detail.originalTitle ?: media.originalTitle,
                year = detail.releaseYear ?: media.year,
                imdbId = detail.externalIds?.imdbId
            )
            val jsonPlayPayload = playPayload.toJson()

            return newMovieLoadResponse(detail.displayTitle, jsonPlayPayload, TvType.Movie, jsonPlayPayload) {
                this.posterUrl = detail.fullPosterUrl
                this.backgroundPosterUrl = detail.fullBackdropUrl
                this.plot = detail.overview
                this.year = detail.releaseYear
                this.duration = detail.runtime
                this.tags = detail.genres?.map { it.name } ?: emptyList()
                addActors(actors)
                addTrailer(trailer)
            }
        } else {
            val detail = TmdbApi.getTvDetail(media.tmdbId)
            val actors = detail.credits?.cast?.take(10)?.map { Actor(it.name, it.profilePath?.let { p -> "https://image.tmdb.org/t/p/w185$p" }) } ?: emptyList()
            val trailer = detail.videos?.results?.firstOrNull { it.site.equals("YouTube", true) && it.type.equals("Trailer", true) }?.let { "https://www.youtube.com/watch?v=${it.key}" }

            val episodesList = mutableListOf<Episode>()
            val validSeasons = detail.seasons?.filter { it.seasonNumber > 0 } ?: emptyList()

            coroutineScope {
                val seasonTasks = validSeasons.map { seasonSummary ->
                    async {
                        try {
                            TmdbApi.getTvSeason(detail.id, seasonSummary.seasonNumber)
                        } catch (e: Exception) {
                            null
                        }
                    }
                }

                seasonTasks.forEach { task ->
                    val seasonDetail = task.await() ?: return@forEach
                    seasonDetail.episodes.forEach { ep ->
                        val epPayload = AtlasStreamMediaData(
                            tmdbId = detail.id,
                            isMovie = false,
                            title = detail.displayTitle,
                            originalTitle = detail.originalName ?: media.originalTitle,
                            year = detail.releaseYear ?: media.year,
                            season = ep.seasonNumber,
                            episode = ep.episodeNumber,
                            imdbId = detail.externalIds?.imdbId
                        )
                        episodesList.add(
                            newEpisode(epPayload.toJson()) {
                                this.name = ep.name ?: "${ep.seasonNumber}. Sezon ${ep.episodeNumber}. Bölüm"
                                this.season = ep.seasonNumber
                                this.episode = ep.episodeNumber
                                this.posterUrl = ep.fullStillUrl ?: detail.fullPosterUrl
                                this.description = ep.overview
                            }
                        )
                    }
                }
            }

            return newTvSeriesLoadResponse(detail.displayTitle, url, TvType.TvSeries, episodesList) {
                this.posterUrl = detail.fullPosterUrl
                this.backgroundPosterUrl = detail.fullBackdropUrl
                this.plot = detail.overview
                this.year = detail.releaseYear
                this.tags = detail.genres?.map { it.name } ?: emptyList()
                addActors(actors)
                addTrailer(trailer)
            }
        }
    }

    override suspend fun loadLinks(
        data: String,
        isCasting: Boolean,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ): Boolean {
        val media: AtlasStreamMediaData = try {
            AppUtils.parseJson<AtlasStreamMediaData>(data)
        } catch (e: Exception) {
            return false
        }

        SourceResolver.resolveLinks(media, subtitleCallback, callback)
        return true
    }
}
