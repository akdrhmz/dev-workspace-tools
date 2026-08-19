package com.kekik.watchbuddy

import android.util.Log
import com.lagradost.cloudstream3.*
import com.lagradost.cloudstream3.utils.*
import kotlinx.coroutines.async
import kotlinx.coroutines.coroutineScope
import java.net.URLEncoder

object SourceResolver {
    private const val TAG = "Workspace_Resolver"

    suspend fun resolveLinks(
        media: WatchBuddyMediaData,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) = coroutineScope {
        val cleanTr = SourceUtils.cleanTitle(media.title)
        val cleanOrig = media.originalTitle?.let { SourceUtils.cleanTitle(it) }

        val jobs = mutableListOf<kotlinx.coroutines.Deferred<Unit>>()

        if (media.isMovie) {
            jobs += async { resolveFilmMakinesi(cleanTr, cleanOrig, subtitleCallback, callback) }
            jobs += async { resolveHDFC(cleanTr, cleanOrig, true, null, null, subtitleCallback, callback) }
            jobs += async { resolveSineWix(cleanTr, cleanOrig, true, null, null, subtitleCallback, callback) }
            jobs += async { resolveJetFilmizle(cleanTr, cleanOrig, subtitleCallback, callback) }
        } else {
            val s = media.season ?: 1
            val e = media.episode ?: 1

            jobs += async { resolveDiziBox(cleanTr, cleanOrig, s, e, subtitleCallback, callback) }
            jobs += async { resolveDizilla(cleanTr, cleanOrig, s, e, subtitleCallback, callback) }
            jobs += async { resolveHDFC(cleanTr, cleanOrig, false, s, e, subtitleCallback, callback) }
            jobs += async { resolveSineWix(cleanTr, cleanOrig, false, s, e, subtitleCallback, callback) }
        }

        jobs.forEach {
            try {
                it.await()
            } catch (e: Exception) {
                Log.e(TAG, "Resolver error: ${e.message}")
            }
        }
    }

    // --- 1. DiziBox Çözücü ---
    private suspend fun resolveDiziBox(
        title: String,
        origTitle: String?,
        season: Int,
        episode: Int,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val queries = listOfNotNull(origTitle, title).distinct()
        val cookies = mapOf(
            "LockUser" to "true",
            "isTrustedUser" to "true",
            "dbxu" to "1743289650198"
        )

        for (q in queries) {
            try {
                val searchUrl = "https://www.dizibox.live/?s=${URLEncoder.encode(q, "UTF-8")}"
                val doc = app.get(searchUrl, cookies = cookies, timeout = 10L).document
                val showHref = doc.selectFirst("article.detailed-article h3 a, article a[href*='/dizi/']")?.attr("href")
                    ?: doc.selectFirst("a[href*='dizibox.live/']")?.attr("href") ?: continue

                val episodeUrl = if (showHref.endsWith("/")) {
                    "${showHref}${season}-sezon-${episode}-bolum-izle"
                } else {
                    "${showHref}/${season}-sezon-${episode}-bolum-izle"
                }

                val epDoc = app.get(episodeUrl, cookies = cookies, timeout = 10L).document
                val iframes = epDoc.select("iframe").map { it.attr("src") }.filter { it.isNotBlank() }

                for (iframe in iframes) {
                    if (iframe.contains("/player/king/king.php") || iframe.contains("/player/moly/moly.php") || iframe.contains("/player/haydi/haydi.php")) {
                        val playerDoc = app.get(iframe, referer = episodeUrl, cookies = cookies).document
                        val encryptedData = playerDoc.selectFirst("input[name='data']")?.attr("value") ?: continue
                        val decryptedHtml = SourceUtils.decryptCryptoJS(encryptedData, "21042017")
                        val streamUrl = Regex("""file:\s*['"](.*?)['"]""").find(decryptedHtml)?.groupValues?.get(1) ?: continue

                        callback.invoke(
                            newExtractorLink(
                                source = "DiziBox",
                                name = "[DiziBox] 1080p HLS",
                                url = streamUrl,
                                type = ExtractorLinkType.M3U8
                            ) {
                                this.referer = iframe
                                this.quality = Qualities.Unknown.value
                            }
                        )
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "DiziBox fetch error: ${e.message}")
            }
        }
    }

    // --- 2. FilmMakinesi Çözücü ---
    private suspend fun resolveFilmMakinesi(
        title: String,
        origTitle: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val queries = listOfNotNull(origTitle, title).distinct()
        for (q in queries) {
            try {
                val searchUrl = "https://filmmakinesi.pw/?s=${URLEncoder.encode(q, "UTF-8")}"
                val doc = app.get(searchUrl, timeout = 10L).document
                val movieHref = doc.selectFirst("div.movie-poster a, div.poster a, article a")?.attr("href") ?: continue

                val movieDoc = app.get(movieHref, timeout = 10L).document
                val iframes = movieDoc.select("iframe").map { it.attr("src") }

                for (iframe in iframes) {
                    if (iframe.contains("closeload") || iframe.contains("closeload.com")) {
                        val closeLoadHtml = app.get(iframe, referer = movieHref).text
                        val realStreamUrl = SourceUtils.decryptCloseLoad(closeLoadHtml)
                        if (!realStreamUrl.isNullOrBlank() && realStreamUrl.startsWith("http")) {
                            callback.invoke(
                                newExtractorLink(
                                    source = "FilmMakinesi",
                                    name = "[FilmMakinesi] CloseLoad 1080p",
                                    url = realStreamUrl,
                                    type = ExtractorLinkType.M3U8
                                ) {
                                    this.referer = "https://closeload.com/"
                                    this.quality = Qualities.Unknown.value
                                }
                            )
                        }
                    } else if (iframe.startsWith("http")) {
                        loadExtractor(iframe, movieHref, subtitleCallback, callback)
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "FilmMakinesi fetch error: ${e.message}")
            }
        }
    }

    // --- 3. HDFilmCehennemi Çözücü ---
    private suspend fun resolveHDFC(
        title: String,
        origTitle: String?,
        isMovie: Boolean,
        season: Int?,
        episode: Int?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val queries = listOfNotNull(title, origTitle).distinct()
        for (q in queries) {
            try {
                val searchUrl = "https://www.hdfilmcehennemi.nl/search/${URLEncoder.encode(q, "UTF-8")}/"
                val doc = app.get(searchUrl, timeout = 10L).document
                val targetHref = doc.selectFirst("div.poster a, div.card-body a")?.attr("href") ?: continue

                val targetUrl = if (!isMovie && season != null && episode != null) {
                    val base = targetHref.removeSuffix("/")
                    "$base-sezon-$season-bolum-$episode"
                } else {
                    targetHref
                }

                val itemDoc = app.get(targetUrl, timeout = 10L).document
                val iframes = itemDoc.select("iframe").map { it.attr("src") }

                for (iframe in iframes) {
                    if (iframe.startsWith("http")) {
                        loadExtractor(iframe, targetUrl, subtitleCallback, callback)
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "HDFC fetch error: ${e.message}")
            }
        }
    }

    // --- 4. Dizilla Çözücü ---
    private suspend fun resolveDizilla(
        title: String,
        origTitle: String?,
        season: Int,
        episode: Int,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val queries = listOfNotNull(title, origTitle).distinct()
        for (q in queries) {
            try {
                val searchUrl = "https://dizilla.one/arama?q=${URLEncoder.encode(q, "UTF-8")}"
                val doc = app.get(searchUrl, timeout = 10L).document
                val showHref = doc.selectFirst("div.poster a, div.series-card a")?.attr("href") ?: continue

                val epUrl = "${showHref.removeSuffix("/")}/sezon-$season/bolum-$episode"
                val epDoc = app.get(epUrl, timeout = 10L).document
                val iframes = epDoc.select("iframe").map { it.attr("src") }

                for (iframe in iframes) {
                    if (iframe.startsWith("http")) {
                        loadExtractor(iframe, epUrl, subtitleCallback, callback)
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "Dizilla fetch error: ${e.message}")
            }
        }
    }

    // --- 5. SineWix Çözücü ---
    private suspend fun resolveSineWix(
        title: String,
        origTitle: String?,
        isMovie: Boolean,
        season: Int?,
        episode: Int?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val queries = listOfNotNull(title, origTitle).distinct()
        for (q in queries) {
            try {
                val searchUrl = "https://sinewix.org/search?q=${URLEncoder.encode(q, "UTF-8")}"
                val doc = app.get(searchUrl, timeout = 10L).document
                val itemHref = doc.selectFirst("a[href*='/film/'], a[href*='/dizi/']")?.attr("href") ?: continue

                val pageDoc = app.get(itemHref, timeout = 10L).document
                val iframes = pageDoc.select("iframe").map { it.attr("src") }

                for (iframe in iframes) {
                    if (iframe.startsWith("http")) {
                        loadExtractor(iframe, itemHref, subtitleCallback, callback)
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "SineWix fetch error: ${e.message}")
            }
        }
    }

    // --- 6. JetFilmizle Çözücü ---
    private suspend fun resolveJetFilmizle(
        title: String,
        origTitle: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val queries = listOfNotNull(title, origTitle).distinct()
        for (q in queries) {
            try {
                val searchUrl = "https://jetfilmizle.mobi/?s=${URLEncoder.encode(q, "UTF-8")}"
                val doc = app.get(searchUrl, timeout = 10L).document
                val movieHref = doc.selectFirst("article.movie a, div.movie-poster a")?.attr("href") ?: continue

                val movieDoc = app.get(movieHref, timeout = 10L).document
                val iframes = movieDoc.select("iframe").map { it.attr("src") }

                for (iframe in iframes) {
                    if (iframe.startsWith("http")) {
                        loadExtractor(iframe, movieHref, subtitleCallback, callback)
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "JetFilmizle fetch error: ${e.message}")
            }
        }
    }
}