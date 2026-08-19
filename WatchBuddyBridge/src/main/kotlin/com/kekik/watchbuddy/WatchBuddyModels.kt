package com.kekik.watchbuddy

import com.fasterxml.jackson.annotation.JsonProperty

data class WbApiResponse<T>(
    @JsonProperty("success") val success: Boolean? = true,
    @JsonProperty("result") val result: T? = null,
    @JsonProperty("error") val error: String? = null,
    @JsonProperty("provider_error") val providerError: Map<String, Any?>? = null
)

data class WbPluginInfo(
    @JsonProperty("name") val name: String,
    @JsonProperty("language") val language: String? = "tr",
    @JsonProperty("main_url") val mainUrl: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("favicon") val favicon: String? = null,
    @JsonProperty("main_page") val mainPage: Map<String, String>? = emptyMap()
)

data class WbSearchItem(
    @JsonProperty("title") val title: String,
    @JsonProperty("url") val url: String,
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("media_type") val mediaType: String? = "movie",
    @JsonProperty("year") val year: String? = null,
    @JsonProperty("rating") val rating: String? = null,
    @JsonProperty("plugin") val plugin: String? = null
)

data class WbMainPageItem(
    @JsonProperty("title") val title: String,
    @JsonProperty("url") val url: String,
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("category") val category: String? = null,
    @JsonProperty("rating") val rating: String? = null,
    @JsonProperty("year") val year: String? = null
)

data class WbEpisode(
    @JsonProperty("title") val title: String,
    @JsonProperty("url") val url: String,
    @JsonProperty("season") val season: Int? = 1,
    @JsonProperty("episode") val episode: Int? = 1,
    @JsonProperty("poster") val poster: String? = null
)

data class WbItemDetail(
    @JsonProperty("title") val title: String,
    @JsonProperty("url") val url: String,
    @JsonProperty("poster") val poster: String? = null,
    @JsonProperty("description") val description: String? = null,
    @JsonProperty("rating") val rating: String? = null,
    @JsonProperty("year") val year: String? = null,
    @JsonProperty("genres") val genres: List<String>? = emptyList(),
    @JsonProperty("duration") val duration: String? = null,
    @JsonProperty("actors") val actors: List<String>? = emptyList(),
    @JsonProperty("sources") val sources: List<String>? = emptyList(),
    @JsonProperty("episodes") val episodes: List<WbEpisode>? = emptyList()
)

data class WbSubtitle(
    @JsonProperty("url") val url: String,
    @JsonProperty("language") val language: String? = "tr",
    @JsonProperty("format") val format: String? = "vtt"
)

data class WbExtractResult(
    @JsonProperty("name") val name: String? = "WatchBuddy Stream",
    @JsonProperty("url") val url: String,
    @JsonProperty("referer") val referer: String? = null,
    @JsonProperty("headers") val headers: Map<String, String>? = emptyMap(),
    @JsonProperty("subtitles") val subtitles: List<WbSubtitle>? = emptyList(),
    @JsonProperty("quality") val quality: String? = "1080p",
    @JsonProperty("is_m3u8") val isM3u8: Boolean? = false
)
