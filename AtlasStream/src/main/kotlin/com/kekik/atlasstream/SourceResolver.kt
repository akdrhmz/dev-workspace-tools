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

    private val externalProviders: List<com.lagradost.cloudstream3.MainAPI> by lazy {
        listOf(
            com.kekik.atlasstream.providers.altiyuzaltmisaltifilmizle.AltiYuzAltmisAltiFilmIzle(),
            com.kekik.atlasstream.providers.asyawatch.AsyaWatch(),
            com.kekik.atlasstream.providers.belgeselx.BelgeselX(),
            com.kekik.atlasstream.providers.cizgimax.CizgiMax(),
            com.kekik.atlasstream.providers.ddizi.DDiziProvider(),
            com.kekik.atlasstream.providers.dizibox.DiziBox(),
            com.kekik.atlasstream.providers.dizigom.DiziGom(),
            com.kekik.atlasstream.providers.dizikorea.DiziKorea(),
            com.kekik.atlasstream.providers.dizilla.Dizilla(),
            com.kekik.atlasstream.providers.dizimag.DiziMag(),
            com.kekik.atlasstream.providers.dizimom.DiziMom(),
            com.kekik.atlasstream.providers.dizipal.DiziPal(),
            com.kekik.atlasstream.providers.diziyou.DiziYou(),
            com.kekik.atlasstream.providers.dmax.DMax(),
            com.kekik.atlasstream.providers.filmizleilk.FilmIzleIlk(),
            com.kekik.atlasstream.providers.filmizlesene.FilmIzlesene(),
            com.kekik.atlasstream.providers.filmkovasi.FilmKovasi(),
            com.kekik.atlasstream.providers.filmmakinesi.FilmMakinesi(),
            com.kekik.atlasstream.providers.filmmodu.FilmModu(),
            com.kekik.atlasstream.providers.fullhdfilm.FullHDFilm(),
            com.kekik.atlasstream.providers.fullhdfilmizlesene.FullHDFilmizlesene(),
            com.kekik.atlasstream.providers.hdfilmcehennemi.HDFilmCehennemi(),
            com.kekik.atlasstream.providers.hdfilmizle.HDFilmIzle(),
            com.kekik.atlasstream.providers.hdfilmsitesi.HDFilmSitesi(),
            com.kekik.atlasstream.providers.jetfilmizle.JetFilmizle(),
            com.kekik.atlasstream.providers.koreanturk.KoreanTurk(),
            com.kekik.atlasstream.providers.kultfilmler.KultFilmler(),
            com.kekik.atlasstream.providers.powerdizi.powerDizi(),
            com.kekik.atlasstream.providers.powersinema.powerSinema(),
            com.kekik.atlasstream.providers.rarefilmm.RareFilmm(),
            com.kekik.atlasstream.providers.roketdizi.RoketDizi(),
            com.kekik.atlasstream.providers.setfilmizle.SetFilmIzle(),
            com.kekik.atlasstream.providers.sezonlukdizi.SezonlukDizi(),
            com.kekik.atlasstream.providers.sinemacx.SinemaCX(),
            com.kekik.atlasstream.providers.sinewix.SineWix(),
            com.kekik.atlasstream.providers.superfilmgeldi.SuperFilmGeldi(),
            com.kekik.atlasstream.providers.tafdi.Tafdi(),
            com.kekik.atlasstream.providers.tranimaci.TRanimaci(),
            com.kekik.atlasstream.providers.tvdiziler.TvDiziler(),
            com.kekik.atlasstream.providers.ugurfilm.UgurFilm(),
            com.kekik.atlasstream.providers.watch2movies.Watch2Movies(),
            com.kekik.atlasstream.providers.webteizle.WebteIzle(),
            com.kekik.atlasstream.providers.xprime.XPrime(),
            com.kekik.atlasstream.providers.yabancidizi.YabanciDizi()
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
