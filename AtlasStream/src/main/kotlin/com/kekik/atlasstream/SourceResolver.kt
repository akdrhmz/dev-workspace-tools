package com.kekik.atlasstream

import android.util.Base64
import android.util.Log
import com.fasterxml.jackson.annotation.JsonProperty
import com.fasterxml.jackson.databind.DeserializationFeature
import com.fasterxml.jackson.databind.ObjectMapper
import com.fasterxml.jackson.module.kotlin.KotlinModule
import com.fasterxml.jackson.module.kotlin.readValue
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import com.lagradost.cloudstream3.utils.StringUtils.decodeUri
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import org.jsoup.Jsoup
import java.net.URLEncoder

object SourceResolver {
    private const val TAG = "WB_Resolver"

    private val externalProviders by lazy {
        listOf(

        )
    }

    private val objectMapper by lazy {
        ObjectMapper().registerModule(KotlinModule.Builder().build()).apply {
            configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
        }
    }

    private fun isProviderEnabled(providerKey: String): Boolean {
        val enabledList = CloudStreamApp.getKey<List<String>>(AtlasStreamPlugin.PREF_KEY_ENABLED_SOURCES)
        return enabledList == null || enabledList.contains(providerKey)
    }

    suspend fun resolveLinks(
        media: AtlasStreamMediaData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) = coroutineScope {
        val cleanTr   = SourceUtils.cleanTitle(media.title)
        val cleanOrig = media.originalTitle?.let { SourceUtils.cleanTitle(it) }

        val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()

        // Dinamik sağlayıcıları eşzamanlı çalıştır
        externalProviders.forEach { provider ->
            if (isProviderEnabled(provider.name)) {
                val matchesType = if (media.isMovie) {
                    provider.supportedTypes.contains(TvType.Movie)
                } else {
                    provider.supportedTypes.contains(TvType.TvSeries) ||
                    provider.supportedTypes.contains(TvType.Cartoon) ||
                    provider.supportedTypes.contains(TvType.AsianDrama)
                }

                if (matchesType) {
                    jobs.add(async {
                        try {
                            val searchRes = provider.search(cleanTr)
                            val matched = searchRes?.firstOrNull { 
                                SourceUtils.cleanTitle(it.name).equals(cleanTr, ignoreCase = true) || 
                                (cleanOrig != null && SourceUtils.cleanTitle(it.name).equals(cleanOrig, ignoreCase = true)) 
                            }
                            if (matched != null) {
                                val loadRes = provider.load(matched.url)
                                if (media.isMovie && loadRes is MovieLoadResponse) {
                                    provider.loadLinks(loadRes.dataUrl, false, subtitleCallback, callback)
                                } else if (!media.isMovie && loadRes is TvSeriesLoadResponse) {
                                    val targetSeason = media.season ?: 1
                                    val targetEpisode = media.episode ?: 1
                                    val ep = loadRes.episodes.firstOrNull { it.season == targetSeason && it.episode == targetEpisode }
                                    if (ep != null) {
                                        provider.loadLinks(ep.data, false, subtitleCallback, callback)
                                    }
                                }
                            }
                        } catch (e: Exception) {
                            Log.e(TAG, "Provider Error: ${provider.name}", e)
                        }
                    })
                }
            }
        }

        jobs.forEach {
            try { it.await() }
            catch (e: Exception) { Log.e(TAG, "Resolver error: ${e.message}") }
        }
    }
}
