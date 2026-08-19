package com.kekik.watchbuddy

import com.lagradost.cloudstream3.CloudStreamApp.Companion.getKey
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.AppUtils
import java.net.URLEncoder

object TmdbApi {
    private const val DEFAULT_API_KEY = "8c598c9af9b0badc281e95b1890834bc"
    private const val BASE_URL = "https://api.themoviedb.org/3"

    private fun getApiKey(): String {
        return getKey<String>(WatchBuddyPlugin.PREF_KEY_TMDB_API_KEY)
            ?.takeIf { it.isNotBlank() } ?: DEFAULT_API_KEY
    }

    private fun buildUrl(path: String, params: Map<String, String> = emptyMap()): String {
        val queryParams = mutableMapOf(
            "api_key"  to getApiKey(),
            "language" to "tr-TR"
        )
        queryParams.putAll(params)
        val queryString = queryParams.map { "${it.key}=${URLEncoder.encode(it.value, "UTF-8")}" }.joinToString("&")
        val separator = if (path.contains("?")) "&" else "?"
        return if (path.startsWith("http")) "$path$separator$queryString" else "$BASE_URL/$path$separator$queryString"
    }

    suspend fun getItemsFromEndpoint(endpoint: String, page: Int = 1): TmdbPageResponse<TmdbItem> {
        val url = buildUrl(endpoint, mapOf("page" to page.toString()))
        val res = app.get(url, cacheTime = 60).text
        return AppUtils.parseJson<TmdbPageResponse<TmdbItem>>(res)
    }

    suspend fun search(query: String, page: Int = 1): TmdbPageResponse<TmdbItem> {
        val url = buildUrl("search/multi", mapOf("query" to query, "page" to page.toString()))
        val res = app.get(url, cacheTime = 30).text
        return AppUtils.parseJson<TmdbPageResponse<TmdbItem>>(res)
    }

    suspend fun getMovieDetail(id: Int): TmdbDetail {
        val url = buildUrl("movie/$id", mapOf("append_to_response" to "credits,videos,external_ids"))
        val res = app.get(url, cacheTime = 60).text
        return AppUtils.parseJson<TmdbDetail>(res)
    }

    suspend fun getTvDetail(id: Int): TmdbDetail {
        val url = buildUrl("tv/$id", mapOf("append_to_response" to "credits,videos,external_ids"))
        val res = app.get(url, cacheTime = 60).text
        return AppUtils.parseJson<TmdbDetail>(res)
    }

    suspend fun getTvSeason(tvId: Int, seasonNumber: Int): TmdbSeasonDetail {
        val url = buildUrl("tv/$tvId/season/$seasonNumber")
        val res = app.get(url, cacheTime = 60).text
        return AppUtils.parseJson<TmdbSeasonDetail>(res)
    }
}