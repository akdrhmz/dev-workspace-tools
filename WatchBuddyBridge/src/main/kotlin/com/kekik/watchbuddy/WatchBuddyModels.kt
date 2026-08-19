package com.kekik.watchbuddy

import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.annotation.JsonIgnoreProperties

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbPageResponse<T>(
    @JsonProperty("page") val page: Int = 1,
    @JsonProperty("results") val results: List<T> = emptyList(),
    @JsonProperty("total_pages") val totalPages: Int = 1,
    @JsonProperty("total_results") val totalResults: Int = 0
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbItem(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("original_title") val originalTitle: String? = null,
    @JsonProperty("original_name") val originalName: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("backdrop_path") val backdropPath: String? = null,
    @JsonProperty("media_type") val mediaType: String? = null,
    @JsonProperty("release_date") val releaseDate: String? = null,
    @JsonProperty("first_air_date") val firstAirDate: String? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
    @JsonProperty("vote_count") val voteCount: Int? = null,
    @JsonProperty("genre_ids") val genreIds: List<Int>? = null
) {
    val displayTitle: String get() = title ?: name ?: originalTitle ?: originalName ?: "Bilinmeyen"
    val releaseYear: Int? get() = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull()
    val fullPosterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val fullBackdropUrl: String? get() = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbDetail(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("title") val title: String? = null,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("original_title") val originalTitle: String? = null,
    @JsonProperty("original_name") val originalName: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("poster_path") val posterPath: String? = null,
    @JsonProperty("backdrop_path") val backdropPath: String? = null,
    @JsonProperty("release_date") val releaseDate: String? = null,
    @JsonProperty("first_air_date") val firstAirDate: String? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null,
    @JsonProperty("runtime") val runtime: Int? = null,
    @JsonProperty("number_of_seasons") val numberOfSeasons: Int? = null,
    @JsonProperty("number_of_episodes") val numberOfEpisodes: Int? = null,
    @JsonProperty("genres") val genres: List<TmdbGenre>? = null,
    @JsonProperty("credits") val credits: TmdbCredits? = null,
    @JsonProperty("videos") val videos: TmdbVideos? = null,
    @JsonProperty("seasons") val seasons: List<TmdbSeasonSummary>? = null,
    @JsonProperty("external_ids") val externalIds: TmdbExternalIds? = null
) {
    val displayTitle: String get() = title ?: name ?: originalTitle ?: originalName ?: "Bilinmeyen"
    val releaseYear: Int? get() = (releaseDate ?: firstAirDate)?.take(4)?.toIntOrNull()
    val fullPosterUrl: String? get() = posterPath?.let { "https://image.tmdb.org/t/p/w500$it" }
    val fullBackdropUrl: String? get() = backdropPath?.let { "https://image.tmdb.org/t/p/original$it" }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbGenre(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("name") val name: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbSeasonSummary(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("season_number") val seasonNumber: Int = 0,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("episode_count") val episodeCount: Int = 0,
    @JsonProperty("poster_path") val posterPath: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbSeasonDetail(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("season_number") val seasonNumber: Int = 0,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("episodes") val episodes: List<TmdbEpisode> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbEpisode(
    @JsonProperty("id") val id: Int = 0,
    @JsonProperty("episode_number") val episodeNumber: Int = 0,
    @JsonProperty("season_number") val seasonNumber: Int = 0,
    @JsonProperty("name") val name: String? = null,
    @JsonProperty("overview") val overview: String? = null,
    @JsonProperty("still_path") val stillPath: String? = null,
    @JsonProperty("air_date") val airDate: String? = null,
    @JsonProperty("vote_average") val voteAverage: Double? = null
) {
    val fullStillUrl: String? get() = stillPath?.let { "https://image.tmdb.org/t/p/w500$it" }
}

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbCredits(
    @JsonProperty("cast") val cast: List<TmdbCast> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbCast(
    @JsonProperty("name") val name: String = "",
    @JsonProperty("character") val character: String? = null,
    @JsonProperty("profile_path") val profilePath: String? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbVideos(
    @JsonProperty("results") val results: List<TmdbVideo> = emptyList()
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbVideo(
    @JsonProperty("key") val key: String = "",
    @JsonProperty("site") val site: String = "",
    @JsonProperty("type") val type: String = ""
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class TmdbExternalIds(
    @JsonProperty("imdb_id") val imdbId: String? = null,
    @JsonProperty("tvdb_id") val tvdbId: Int? = null
)

@JsonIgnoreProperties(ignoreUnknown = true)
data class WatchBuddyMediaData(
    @JsonProperty("tmdbId") val tmdbId: Int = 0,
    @JsonProperty("isMovie") val isMovie: Boolean = true,
    @JsonProperty("title") val title: String = "",
    @JsonProperty("originalTitle") val originalTitle: String? = null,
    @JsonProperty("year") val year: Int? = null,
    @JsonProperty("season") val season: Int? = null,
    @JsonProperty("episode") val episode: Int? = null,
    @JsonProperty("imdbId") val imdbId: String? = null
)