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
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import java.net.URLEncoder

object SourceResolver {
    private const val TAG = "WB_Resolver"

    private val externalProviders: List<com.lagradost.cloudstream3.MainAPI> by lazy {
        listOf(
            com.kekik.atlasstream.providers.altiyuzaltmisaltifilmizle.AltiYuzAltmisAltiFilmIzle(),
            com.kekik.atlasstream.providers.animecix.AnimeciX(),
            com.kekik.atlasstream.providers.asyaanimeleri.AsyaAnimeleri(),
            com.kekik.atlasstream.providers.asyawatch.AsyaWatch(),
            com.kekik.atlasstream.providers.belgeselx.BelgeselX(),
            com.kekik.atlasstream.providers.canlitv.CanliTV(),
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
            com.kekik.atlasstream.providers.fullhdfilmizlede.FullHDFilmIzlede(),
            com.kekik.atlasstream.providers.hdfilmcehennemi.HDFilmCehennemi(),
            com.kekik.atlasstream.providers.hdfilmizle.HDFilmIzle(),
            com.kekik.atlasstream.providers.hdfilmsitesi.HDFilmSitesi(),
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
import kotlinx.coroutines.withTimeoutOrNull
import org.jsoup.Jsoup
import java.net.URLEncoder

object SourceResolver {
    private const val TAG = "WB_Resolver"

    private val externalProviders: List<com.lagradost.cloudstream3.MainAPI> by lazy {
        listOf(
            com.kekik.atlasstream.providers.altiyuzaltmisaltifilmizle.AltiYuzAltmisAltiFilmIzle(),
            com.kekik.atlasstream.providers.animecix.AnimeciX(),
            com.kekik.atlasstream.providers.asyaanimeleri.AsyaAnimeleri(),
            com.kekik.atlasstream.providers.asyawatch.AsyaWatch(),
            com.kekik.atlasstream.providers.belgeselx.BelgeselX(),
            com.kekik.atlasstream.providers.canlitv.CanliTV(),
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
            com.kekik.atlasstream.providers.fullhdfilmizlede.FullHDFilmIzlede(),
            com.kekik.atlasstream.providers.hdfilmcehennemi.HDFilmCehennemi(),
            com.kekik.atlasstream.providers.hdfilmizle.HDFilmIzle(),
            com.kekik.atlasstream.providers.hdfilmsitesi.HDFilmSitesi(),
            com.kekik.atlasstream.providers.inatbox.InatBox(),
            com.kekik.atlasstream.providers.jetfilmizle.JetFilmizle(),
            com.kekik.atlasstream.providers.koreanturk.KoreanTurk(),
            com.kekik.atlasstream.providers.kultfilmler.KultFilmler(),
            com.kekik.atlasstream.providers.powerdizi.powerDizi(),
            com.kekik.atlasstream.providers.powersinema.powerSinema(),
            com.kekik.atlasstream.providers.rarefilmm.RareFilmm(),
            com.kekik.atlasstream.providers.rectv.RecTV(),
            com.kekik.atlasstream.providers.roketdizi.RoketDizi(),
            com.kekik.atlasstream.providers.selcukflix.SelcukFlix(),
            com.kekik.atlasstream.providers.setfilmizle.SetFilmIzle(),
            com.kekik.atlasstream.providers.sezonlukdizi.SezonlukDizi(),
            com.kekik.atlasstream.providers.sinemacx.SinemaCX(),
            com.kekik.atlasstream.providers.sinewix.SineWix(),
            com.kekik.atlasstream.providers.superfilmgeldi.SuperFilmGeldi(),
            com.kekik.atlasstream.providers.tafdi.Tafdi(),
            com.kekik.atlasstream.providers.tlc.TLC(),
            com.kekik.atlasstream.providers.tlctr.Tlctr(),
            com.kekik.atlasstream.providers.tranimaci.TRanimaci(),
            com.kekik.atlasstream.providers.trasyalog.TRasyalog(),
            com.kekik.atlasstream.providers.turkanime.TurkAnime(),
            com.kekik.atlasstream.providers.tvdiziler.TvDiziler(),
            com.kekik.atlasstream.providers.ugurfilm.UgurFilm(),
            com.kekik.atlasstream.providers.vavoospor.vavooSpor(),
            com.kekik.atlasstream.providers.watch2movies.Watch2Movies(),
            com.kekik.atlasstream.providers.webteizle.WebteIzle(),
            com.kekik.atlasstream.providers.wfilmizle.WFilmIzle(),
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

    // TMDB'yi doğrudan backend olarak kullanan sağlayıcılar: arama/eşleştirme gerekmeden
    // media.tmdbId ile doğrudan yükleme yapılabilir (kesin eşleşme, fuzzy matching riski yok).
    private val tmdbDirectProviders = setOf("XPrime")

    // Tek bir sağlayıcının (arama + yükleme + link çözme dahil) alabileceği maksimum süre.
    // 57 sağlayıcı eşzamanlı çalıştığında ağ kuyruğu oluşabileceği için süre 35 saniyeye çıkarıldı.
    private const val PROVIDER_TIMEOUT_MS = 35_000L

    /** Verilen aday listesinden eşik değerini geçen en yüksek skorlu sonucu döner. */
    private fun bestMatch(
        results: List<SearchResponse>?,
        media: AtlasStreamMediaData
    ): SearchResponse? {
        return results
            ?.mapNotNull { item ->
                val resYear = (item as? MovieSearchResponse)?.year ?: (item as? TvSeriesSearchResponse)?.year ?: (item as? AnimeSearchResponse)?.year
                val score = SourceUtils.titleMatchScore(
                    resultTitle = item.name,
                    targetTitleTr = media.title,
                    targetTitleOrig = media.originalTitle,
                    targetYear = media.year,
                    resultYear = resYear
                )
                if (score >= SourceUtils.MATCH_THRESHOLD) item to score else null
            }
            ?.maxByOrNull { it.second }
            ?.first
    }

    suspend fun resolveLinks(
        media: AtlasStreamMediaData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) = coroutineScope {
        val searchQueries = SourceUtils.getSearchQueries(media.title, media.originalTitle)

        val jobs = mutableListOf<kotlinx.coroutines.Deferred<*>>()

        // Dinamik sağlayıcıları eşzamanlı çalıştır
        externalProviders.forEach { provider ->
            if (isProviderEnabled(provider.name)) {
                val matchesType = if (media.isMovie) {
                    provider.supportedTypes.contains(TvType.Movie)
                } else {
                    provider.supportedTypes.contains(TvType.TvSeries) ||
                    provider.supportedTypes.contains(TvType.Cartoon) ||
                    provider.supportedTypes.contains(TvType.AsianDrama) ||
                    provider.supportedTypes.contains(TvType.Anime)
                }

                if (matchesType) {
                    jobs.add(async {
                        val result = withTimeoutOrNull(PROVIDER_TIMEOUT_MS) {
                            try {
                                // 0. Kademe: TMDB-doğrudan sağlayıcılar (XPrime vb.) — arama/eşleştirme atlanır
                                if (media.isMovie && provider.name in tmdbDirectProviders) {
                                    val loadRes = try {
                                        provider.load(media.tmdbId.toString())
                                    } catch (e: Exception) { null }

                                    if (loadRes is MovieLoadResponse) {
                                        provider.loadLinks(loadRes.dataUrl, false, subtitleCallback, callback)
                                    }
                                    return@withTimeoutOrNull
                                }

                                // 1. Kademe: Aday Arama Sorgularını Sırayla/Akıllıca Tara
                                var matched: SearchResponse? = null

                                for (query in searchQueries) {
                                    val searchRes = try {
                                        provider.search(query)
                                    } catch (e: Exception) { null }

                                    matched = bestMatch(searchRes, media)
                                    if (matched != null) break
                                }

                                // 2. Kademe: IMDb ID ile Arama (Sağlayıcı IMDb ID aramasını destekliyorsa)
                                if (matched == null && !media.imdbId.isNullOrBlank()) {
                                    val imdbRes = try {
                                        provider.search(media.imdbId)
                                    } catch (e: Exception) { null }

                                    matched = imdbRes?.firstOrNull()
                                }

                                if (matched != null) {
                                    val loadRes = try {
                                        provider.load(matched.url)
                                    } catch (e: Exception) { null }

                                    if (media.isMovie) {
                                        if (loadRes is MovieLoadResponse) {
                                            provider.loadLinks(loadRes.dataUrl, false, subtitleCallback, callback)
                                        } else if (loadRes is TvSeriesLoadResponse) {
                                            // Film tek bölümlük dizi olarak eklenmişse
                                            val firstEp = loadRes.episodes.firstOrNull()
                                            if (firstEp != null) {
                                                provider.loadLinks(firstEp.data, false, subtitleCallback, callback)
                                            }
                                        }
                                    } else {
                                        if (loadRes is TvSeriesLoadResponse) {
                                            val targetSeason = media.season ?: 1
                                            val targetEpisode = media.episode ?: 1

                                            // 1. Standart Sezon x Bölüm eşleşmesi
                                            var ep = loadRes.episodes.firstOrNull {
                                                it.season == targetSeason && it.episode == targetEpisode
                                            }

                                            // 2. Sezon numarası 0 veya null olan düz bölümler (Örn: 1. Bölüm, 2. Bölüm)
                                            if (ep == null) {
                                                ep = loadRes.episodes.firstOrNull {
                                                    it.episode == targetEpisode && (it.season == null || it.season == 0 || it.season == 1)
                                                }
                                            }

                                            // 3. Mutlak Bölüm Fallback'i (Anime ve sezon ayrımı yapmayan diziler için)
                                            if (ep == null && media.absoluteEpisode != null) {
                                                ep = loadRes.episodes.firstOrNull {
                                                    it.episode == media.absoluteEpisode
                                                }
                                            }

                                            // 4. İsim bazlı bölüm eşleşmesi (Örn: "1. Bölüm" veya "Bölüm 1")
                                            if (ep == null) {
                                                ep = loadRes.episodes.firstOrNull {
                                                    val epName = it.name ?: ""
                                                    epName.contains("${targetEpisode}. Bölüm", ignoreCase = true) ||
                                                    epName.contains("Bölüm ${targetEpisode}", ignoreCase = true) ||
                                                    epName.contains("Episode ${targetEpisode}", ignoreCase = true)
                                                }
                                            }

                                            if (ep != null) {
                                                provider.loadLinks(ep.data, false, subtitleCallback, callback)
                                            }
                                        } else if (loadRes is MovieLoadResponse) {
                                            // Dizi tek parça film/özel bölüm olarak eklenmişse
                                            provider.loadLinks(loadRes.dataUrl, false, subtitleCallback, callback)
                                        }
                                    }
                                }
                            } catch (e: Exception) {
                                Log.e(TAG, "Provider Error: ${provider.name}", e)
                            }
                        }

                        if (result == null) {
                            Log.e(TAG, "Provider Timeout: ${provider.name}")
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
