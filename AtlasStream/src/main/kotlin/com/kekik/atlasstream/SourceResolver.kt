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

        if (media.isMovie) {
            if (isProviderEnabled("FilmMakinesi")) {
                jobs += async { resolveFilmMakinesi(cleanTr, cleanOrig, subtitleCallback, callback) }
            }
            if (isProviderEnabled("HDFilmCehennemi")) {
                jobs += async { resolveHDFC(cleanTr, cleanOrig, true, null, null, subtitleCallback, callback) }
            }
            if (isProviderEnabled("JetFilmizle")) {
                jobs += async { resolveJetFilmizle(cleanTr, cleanOrig, subtitleCallback, callback) }
            }
        } else {
            val s = media.season ?: 1
            val e = media.episode ?: 1

            if (isProviderEnabled("DiziBox")) {
                jobs += async { resolveDiziBox(cleanTr, cleanOrig, s, e, subtitleCallback, callback) }
            }
            if (isProviderEnabled("Dizilla")) {
                jobs += async { resolveDizilla(cleanTr, cleanOrig, s, e, subtitleCallback, callback) }
            }
            if (isProviderEnabled("HDFilmCehennemi")) {
                jobs += async { resolveHDFC(cleanTr, cleanOrig, false, s, e, subtitleCallback, callback) }
            }
        }

        // SineWix handles both (it has its own API)
        if (isProviderEnabled("SineWix")) {
            jobs += async { resolveSineWix(cleanTr, cleanOrig, media.isMovie, media.season, media.episode, subtitleCallback, callback) }
        }

        jobs.forEach {
            try { it.await() }
            catch (e: Exception) { Log.e(TAG, "Resolver error: ${e.message}") }
        }
    }

    // ============================================================
    // 1. DiziBox (adapted from DiziBox.kt — TV Series only)
    // ============================================================
    private val diziBoxCookies = mapOf(
        "LockUser"      to "true",
        "isTrustedUser" to "true",
        "dbxu"          to "1743289650198"
    )

    private suspend fun resolveDiziBox(
        title: String, origTitle: String?,
        season: Int, episode: Int,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val queries = listOfNotNull(origTitle, title).distinct()
        for (q in queries) {
            try {
                val searchUrl = "https://www.dizibox.live/?s=${URLEncoder.encode(q, "UTF-8")}"
                val doc = app.get(searchUrl, cookies = diziBoxCookies).document
                val showHref = doc.selectFirst("article.detailed-article a")?.attr("href") ?: continue

                // Try to construct episode URL
                val base = showHref.removeSuffix("/")
                val episodeUrl = "$base/${season}-sezon-${episode}-bolum-izle"

                val epDoc = app.get(episodeUrl, cookies = diziBoxCookies).document
                val iframe = epDoc.selectFirst("div#video-area iframe")?.attr("src") ?: continue
                Log.d(TAG, "DiziBox iframe: $iframe")

                diziBoxIframeDecode(episodeUrl, iframe, subtitleCallback, callback)

                // Also try alternative sources
                epDoc.select("div.video-toolbar option[value]").forEach { opt ->
                    val altLink = opt.attr("value")
                    try {
                        val altDoc = app.get(altLink, cookies = diziBoxCookies).document
                        val altIframe = altDoc.selectFirst("div#video-area iframe")?.attr("src") ?: return@forEach
                        diziBoxIframeDecode(altLink, altIframe, subtitleCallback, callback)
                    } catch (_: Exception) {}
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "DiziBox error: ${e.message}")
            }
        }
    }

    private suspend fun diziBoxIframeDecode(
        pageUrl: String, iframe: String,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        @Suppress("NAME_SHADOWING") var iframe = iframe

        if (iframe.contains("/player/king/king.php")) {
            iframe = iframe.replace("king.php?v=", "king.php?wmode=opaque&v=")
            val subDoc = app.get(iframe, referer = pageUrl, cookies = diziBoxCookies).document
            val subFrame = subDoc.selectFirst("div#Player iframe")?.attr("src") ?: return

            val iDoc      = app.get(subFrame, referer = "https://www.dizibox.live/").text
            val cryptData = Regex("""CryptoJS\.AES\.decrypt\("(.*)","""""").find(iDoc)?.groupValues?.get(1) ?: return
            val cryptPass = Regex(""","(.*)"\);""").find(iDoc)?.groupValues?.get(1) ?: return
            val decrypted = SourceUtils.decryptCryptoJS(cryptData, cryptPass)
            val vidUrl    = Regex("""file: '(.*)',""").find(decrypted)?.groupValues?.get(1) ?: return

            callback.invoke(
                newExtractorLink(
                    source = "DiziBox",
                    name   = "[DiziBox] King",
                    url    = vidUrl,
                    type   = ExtractorLinkType.M3U8
                ) {
                    this.headers = mapOf("Referer" to vidUrl)
                    this.quality = Qualities.Unknown.value
                }
            )

        } else if (iframe.contains("/player/moly/moly.php") || iframe.contains("/player/haydi")) {
            val phpName = if (iframe.contains("moly")) "moly.php" else "haydi.php"
            val paramSep = if (iframe.contains("moly")) "moly.php?h=" else "haydi.php?v="
            val replaceTo = paramSep.replace("?", "?wmode=opaque&").replace("=", "=")
            iframe = iframe.replace(paramSep, replaceTo)

            var subDoc = app.get(iframe, referer = pageUrl, cookies = diziBoxCookies).document
            val atobData = Regex("""unescape\("(.*)"\)""").find(subDoc.html())?.groupValues?.get(1)
            if (atobData != null) {
                val decoded = atobData.decodeUri()
                val strAtob = String(Base64.decode(decoded, Base64.DEFAULT), Charsets.UTF_8)
                subDoc = Jsoup.parse(strAtob)
            }

            val subFrame = subDoc.selectFirst("div#Player iframe")?.attr("src") ?: return
            loadExtractor(subFrame, "https://www.dizibox.live/", subtitleCallback, callback)
        }
    }

    // ============================================================
    // 2. Dizilla (adapted from Dizilla.kt — TV Series, AES encrypted API)
    // ============================================================
    private suspend fun resolveDizilla(
        title: String, origTitle: String?,
        season: Int, episode: Int,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val mainUrl = "https://dizillahd.com"
        val queries = listOfNotNull(title, origTitle).distinct()

        for (q in queries) {
            try {
                // Search via API
                val searchReq = app.post(
                    "$mainUrl/api/bg/searchContent?searchterm=${URLEncoder.encode(q, "UTF-8")}",
                    headers = mapOf(
                        "User-Agent"       to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0",
                        "Accept"           to "application/json, text/plain, */*",
                        "X-Requested-With" to "XMLHttpRequest",
                        "Referer"          to "$mainUrl/"
                    )
                )

                val searchJson = objectMapper.readTree(searchReq.text)
                val encryptedBlob = searchJson.get("response")?.asText() ?: continue
                val decryptedJson = SourceUtils.decryptDizillaResponse(encryptedBlob) ?: continue

                // Fix potential broken JSON
                val fixedJson = if (!decryptedJson.startsWith("{") && decryptedJson.contains("\"essage\"")) {
                    "{m\"$decryptedJson"
                } else {
                    decryptedJson
                }

                // Find slug from search results
                val contentJson = objectMapper.readTree(fixedJson)
                val results = contentJson.get("result") ?: continue
                val slug = results.firstOrNull()?.get("slug")?.asText() ?: continue
                Log.d(TAG, "Dizilla slug: $slug")

                // Navigate to show page and find season/episode
                val showDoc = app.get("$mainUrl/$slug").document

                // Find correct season link
                var episodeHref: String? = null
                for (sezonLink in showDoc.select("div.flex.items-center.flex-wrap.gap-2.mb-4 a")) {
                    val sezonHref = sezonLink.attr("href")
                    if (sezonHref.isBlank()) continue
                    val fullSezonUrl = if (sezonHref.startsWith("http")) sezonHref else "$mainUrl$sezonHref"

                    // Check if this is the right season
                    val splitParts = fullSezonUrl.split("-")
                    val seasonNum = splitParts.getOrNull(splitParts.size - 2)?.toIntOrNull()
                    if (seasonNum != null && seasonNum != season) continue

                    val sezonDoc = app.get(fullSezonUrl).document
                    for (bolum in sezonDoc.select("div.episodes div.cursor-pointer")) {
                        val epNum = bolum.selectFirst("a")?.text()?.trim()?.toIntOrNull()
                        if (epNum == episode) {
                            episodeHref = bolum.select("a").last()?.attr("href")
                            break
                        }
                    }
                    if (episodeHref != null) break
                }

                if (episodeHref == null) continue
                val fullEpUrl = if (episodeHref.startsWith("http")) episodeHref else "$mainUrl$episodeHref"
                Log.d(TAG, "Dizilla episode URL: $fullEpUrl")

                // Extract links from episode page
                val epPageDoc = app.get(fullEpUrl).document
                val nextData  = epPageDoc.selectFirst("script#__NEXT_DATA__")?.data() ?: continue
                val rootNode  = objectMapper.readTree(nextData)
                val secureData = rootNode.path("props").path("pageProps").path("secureData").asText()
                if (secureData.isEmpty()) continue

                val decodedData = SourceUtils.decryptDizillaResponse(secureData) ?: continue

                // Find iframe URLs in source_content
                val contentRegex = Regex(""""source_content"\s*:\s*"((?:[^"\\]|\\.)*)"""")
                contentRegex.findAll(decodedData).forEach { match ->
                    val rawHtml = match.groupValues[1]
                        .replace("\\\"", "\"")
                        .replace("\\/", "/")
                        .replace("\\\\", "\\")

                    if (rawHtml.contains("iframe", ignoreCase = true)) {
                        var iframeUrl = Jsoup.parse(rawHtml).select("iframe").attr("src")
                        if (iframeUrl.startsWith("//")) iframeUrl = "https:$iframeUrl"
                        if (iframeUrl.isNotBlank()) {
                            Log.d(TAG, "Dizilla iframe: $iframeUrl")
                            loadExtractor(iframeUrl, "$mainUrl/", subtitleCallback, callback)
                        }
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "Dizilla error: ${e.message}")
            }
        }
    }

    // ============================================================
    // 3. FilmMakinesi (adapted from FilmMakinesi.kt — Movies only)
    // ============================================================
    private suspend fun resolveFilmMakinesi(
        title: String, origTitle: String?,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val mainUrl = "https://filmmakinesi.to"
        val queries = listOfNotNull(origTitle, title).distinct()

        for (q in queries) {
            try {
                val searchUrl = "$mainUrl/arama/?s=${URLEncoder.encode(q, "UTF-8")}"
                val doc = app.get(searchUrl).document
                val movieHref = doc.selectFirst("div.film-list div.item-relative a.item")
                    ?.attr("href") ?: continue
                val fullUrl = if (movieHref.startsWith("http")) movieHref else "$mainUrl$movieHref"

                val movieDoc = app.get(fullUrl).document

                // Primary iframe (data-src)
                val iframeSrc = movieDoc.selectFirst("iframe")?.attr("data-src")
                if (!iframeSrc.isNullOrBlank()) {
                    Log.d(TAG, "FilmMakinesi iframe: $iframeSrc")
                    loadExtractor(iframeSrc, "$mainUrl/", subtitleCallback, callback)
                }

                // Additional video parts
                movieDoc.select(".video-parts a[data-video_url]").forEach { el ->
                    val url = el.attr("data-video_url")
                    if (url.isNotBlank()) {
                        Log.d(TAG, "FilmMakinesi video part: $url")
                        loadExtractor(url, "$mainUrl/", subtitleCallback, callback)
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "FilmMakinesi error: ${e.message}")
            }
        }
    }

    // ============================================================
    // 4. HDFilmCehennemi (adapted from HDFilmCehennemi.kt)
    // ============================================================
    private val hdfcHeaders = mapOf(
        "User-Agent"       to "Mozilla/5.0 (Windows NT 10.0; Win64; x64; rv:137.0) Gecko/20100101 Firefox/137.0",
        "X-Requested-With" to "fetch"
    )

    private suspend fun resolveHDFC(
        title: String, origTitle: String?,
        isMovie: Boolean, season: Int?, episode: Int?,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val mainUrl = "https://www.hdfilmcehennemi.nl"
        val queries = listOfNotNull(title, origTitle).distinct()

        for (q in queries) {
            try {
                // Search via JSON API
                val searchResp = app.get(
                    "$mainUrl/search?q=${URLEncoder.encode(q, "UTF-8")}",
                    headers = hdfcHeaders
                ).text

                val results: HDFCResults = objectMapper.readValue(searchResp)
                if (results.results.isEmpty()) continue

                // Parse first result
                val firstResultDoc = Jsoup.parse(results.results.first())
                val resultTitle = firstResultDoc.selectFirst("h4.title")?.text() ?: continue
                val resultHref  = firstResultDoc.selectFirst("a")?.attr("href") ?: continue
                Log.d(TAG, "HDFC found: $resultTitle -> $resultHref")

                // For TV series, navigate to specific episode
                val targetUrl = if (!isMovie && season != null && episode != null) {
                    val showDoc = app.get(resultHref).document
                    var epUrl: String? = null
                    showDoc.select("div.seasons-tab-content a").forEach { epLink ->
                        val epName = epLink.selectFirst("h4")?.text()?.trim() ?: return@forEach
                        val epS = Regex("""(\d+)\. ?Sezon""").find(epName)?.groupValues?.get(1)?.toIntOrNull() ?: 1
                        val epE = Regex("""(\d+)\. ?Bölüm""").find(epName)?.groupValues?.get(1)?.toIntOrNull()
                        if (epS == season && epE == episode) {
                            epUrl = epLink.attr("href")
                        }
                    }
                    epUrl ?: continue
                } else {
                    resultHref
                }

                // Load links from the page
                val document = app.get(targetUrl).document
                document.select("div.alternative-links").forEach { element ->
                    val langCode = element.attr("data-lang").uppercase()
                    element.select("button.alternative-link").forEach { button ->
                        val source  = button.text().replace("(HDrip Xbet)", "").trim() + " $langCode"
                        val videoID = button.attr("data-video")
                        if (videoID.isBlank()) return@forEach

                        try {
                            val apiResp = app.get(
                                "$mainUrl/video/$videoID/",
                                headers = mapOf(
                                    "Content-Type"     to "application/json",
                                    "X-Requested-With" to "fetch"
                                ),
                                referer = targetUrl
                            ).text

                            var iframeUrl = Regex("""data-src=\\"([^"]+)""").find(apiResp)?.groupValues?.get(1)
                                ?.replace("\\", "") ?: return@forEach

                            if (iframeUrl.contains("rapidrame")) {
                                iframeUrl = "$mainUrl/rplayer/" + iframeUrl.substringAfter("?rapidrame_id=")
                            }

                            Log.d(TAG, "HDFC source: $source -> $iframeUrl")
                            invokeHDFCLocalSource(source, mainUrl, iframeUrl, subtitleCallback, callback)
                        } catch (e: Exception) {
                            Log.e(TAG, "HDFC video API error: ${e.message}")
                        }
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "HDFC error: ${e.message}")
            }
        }
    }

    private suspend fun invokeHDFCLocalSource(
        source: String, mainUrl: String, url: String,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val script = app.get(url, referer = "$mainUrl/").document
            .select("script").find { it.data().contains("sources:") }?.data() ?: return

        val unpackedScript = getAndUnpack(script)
        val decryptedUrl   = SourceUtils.decryptLocalUrl(unpackedScript) ?: return
        val lastUrl        = decryptedUrl.substringAfter("https").let { "https$it" }

        // Subtitles
        val subData = script.substringAfter("tracks: [").substringBefore("]")
        AppUtils.tryParseJson<List<HDFCSubSource>>("[${subData}]")
            ?.filter { it.kind == "captions" }
            ?.forEach {
                val subtitleUrl = "$mainUrl${it.file}/"
                try {
                    val subResp = app.get(subtitleUrl, allowRedirects = true)
                    if (subResp.isSuccessful) {
                        subtitleCallback(newSubtitleFile(it.language.toString(), subtitleUrl))
                    }
                } catch (_: Exception) {}
            }

        callback.invoke(
            newExtractorLink(
                source = source,
                name   = source,
                url    = lastUrl,
                type   = ExtractorLinkType.M3U8
            ) {
                this.headers = mapOf(
                    "Referer"    to "$mainUrl/",
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36"
                )
                this.quality = Qualities.Unknown.value
            }
        )
    }

    // ============================================================
    // 5. SineWix (adapted from Sinewix.kt — API-based)
    // ============================================================
    private val sineWixApiToken = "9iQNC5HQwPlaFuJDkhncJ5XTJ8feGXOJatAA"
    private val sineWixHeaders = mapOf(
        "hash256"   to "711bff4afeb47f07ab08a0b07e85d3835e739295e8a6361db77eebd93d96306b",
        "signature" to "3082058830820370a00302010202145bbfbba9791db758ad12295636e094ab4b07dc24300d06092a864886f70d01010b05003074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f69643020170d3231313231353232303433335a180f32303531313231353232303433335a3074310b3009060355040613025553311330110603550408130a43616c69666f726e6961311630140603550407130d4d6f756e7461696e205669657731143012060355040a130b476f6f676c6520496e632e3110300e060355040b1307416e64726f69643110300e06035504031307416e64726f696430820222300d06092a864886f70d01010105000382020f003082020a0282020100a5106a24bb3f9c0aaf3a2b228f794b5eaf1757ba758b19736a39d1bdc73fc983a7237b8d5ca5156cfa999c1dab3418bbc2be0920e0ee001c8aa4812d1dae75d080f09e91e0abda83ff9a76e8384a4429f4849248069a59505b12ac2c14ba2e4d1a13afcdaf54e508697ff928a9f738e6f4a6fc27409c55329eb149b5ff89c5a2d7c06bf9e62086f955cad17d7be2623ee9d5ec56068eadc23cb0965a13ff97d49fe10ef41afc6eeca36b4ace9582097faff89f590bc831cdb3a69eec5d15b67c3f2cad49e37ed053733e3d2d400c47755b932bdbe15d749fd6ad1dce30ba5e66094dfb6ee6f64cafb807e11b19a990c5d078c6d6701cda0bdeb21e99404ff166074f4c89b04c418f4e7940db5c78647c475bcfb85d4c4e836ee7d7c1d53e9e736b5d96d4b4d8b98209064b729ac6a682d55a6a930e518d849898bb28329ca0aaa133b5e5270a9d5940cac6af4802a57fd971efda91abb602882dd6aa6ce2b236b57b52ee2481498f0cacbcc2c36c238bc84becad7eaaf1125b9a1ca9ded6c79f3f283a52050377809b2a9995d66e1636b0ed426fdd8685c47cb18e82077f4aefcc07887e1dc58b4d64be1632f0e7b4625da6f40c65a8512a6454a4b96963e7f876136e6c0069a519a79ad632078ed965aa12482458060c030ed50db706d854f88cb004630b49285d8af8b471ff8f6070687826412287b50049bcb7d1b6b62ef90203010001a310300e300c0603551d13040530030101ff300d06092a864886f70d01010b0500038202010051c0b7bd793181dc29ca777d3773f928a366c8469ecf2fa3cfb076e8831970d19bb2b96e44e8ccc647cf0696bb824ac61c23d958525d283cab26037b04d58aa79bf92192db843adf5c26a980f081d2f0e14f759fc5ff4c5bb3dce0860299bfe7b349a8155a2efaf731ba25ce796a80c1442c7bf80f8c1a7912ff0b6f6592264315337251a846460194fa594f81f38f9e5233a63201e931ad9cab5bf119f24025613f307194eaa6eb39a83f3c05a49ba34455b1aff7c6839bbb657d9392ffdf397432af6e56ba9534a8b07d7060fe09691c6cf07cb5324f67b3cc0871a8c621d81fe71d71085c55206a4f57e25f774fd4b979b299e8bb076b50fca42fa57da2d519fd35a4a7c0137babaed4345f8031b63b6a71f5e8268f709d658ccd7c2a58849379d25bfa598c3f4a2c3d9b7d89285fefeb7f0ec65137d38b08ce432a15688b624a179e6a4a505ebc3bcdfbc4d4330508ee2d8d0f016924dcec21a6838ef7d834c6f43bde4a5201ed0b3bb4e9bd377b470e36bcf5bc3d56169dbd8e39567aa7dce4d1a8a8a54a5e1aa6fb1a8aab0062669a966f96e15ccce6fe12ea5e6a8b8c8823bdc94988ca39759fd1cc8fd8ae5c3d74db50b174cf7d77655016c075c91d439ed01cc0a9f695c99fad3b5495fb6cb1e01a5fa020cc6022a85c07ec55f9eba89719f86e49d34ab5bd208c5f70cced2b7b7963c014f8404432979b506de29e",
        "User-Agent" to "EasyPlex (Android 14; SM-A546B; Samsung Galaxy A54 5G; tr)",
        "Accept"     to "application/json"
    )

    private suspend fun resolveSineWix(
        title: String, origTitle: String?,
        isMovie: Boolean, season: Int?, episode: Int?,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val mainUrl = "https://ydfvfdizipanel.ru"
        val queries = listOfNotNull(title, origTitle).distinct()

        for (q in queries) {
            try {
                val searchResp = app.get(
                    "$mainUrl/public/api/search/${URLEncoder.encode(q, "UTF-8")}/$sineWixApiToken",
                    headers = sineWixHeaders
                ).text

                val searchData = objectMapper.readTree(searchResp)
                val searchResults = searchData.get("search") ?: continue
                val firstResult = searchResults.firstOrNull() ?: continue
                val id   = firstResult.get("id")?.asInt() ?: continue
                val type = firstResult.get("type")?.asText() ?: "movie"

                if (isMovie && (type == "movie" || type == "anime_movie")) {
                    val infoResp = app.get(
                        "$mainUrl/public/api/media/movie/info/$id/$sineWixApiToken",
                        headers = sineWixHeaders
                    ).text
                    val infoData = objectMapper.readTree(infoResp)
                    val videoLink = infoData.get("videos")?.firstOrNull()?.get("link")?.asText()
                    if (!videoLink.isNullOrBlank()) {
                        loadExtractor(videoLink, subtitleCallback, callback)
                    }
                } else if (!isMovie) {
                    val infoResp = app.get(
                        "$mainUrl/public/api/series/show/$id/$sineWixApiToken",
                        headers = sineWixHeaders
                    ).text
                    val infoData = objectMapper.readTree(infoResp)
                    val seasons = infoData.get("seasons") ?: continue
                    for (s in seasons) {
                        val sNum = s.get("season_number")?.asInt() ?: continue
                        if (sNum != (season ?: 1)) continue
                        val episodes = s.get("episodes") ?: continue
                        for (ep in episodes) {
                            val eNum = ep.get("episode_number")?.asInt() ?: continue
                            if (eNum != (episode ?: 1)) continue
                            val videoLink = ep.get("videos")?.firstOrNull()?.get("link")?.asText()
                            if (!videoLink.isNullOrBlank()) {
                                loadExtractor(videoLink, subtitleCallback, callback)
                            }
                        }
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "SineWix error: ${e.message}")
            }
        }
    }

    // ============================================================
    // 6. JetFilmizle (adapted from JetFilmizle.kt — Movies)
    // ============================================================
    private suspend fun resolveJetFilmizle(
        title: String, origTitle: String?,
        subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit
    ) {
        val mainUrl = "https://jetfilmizle.now"
        val queries = listOfNotNull(origTitle, title).distinct()

        for (q in queries) {
            try {
                val doc = app.post(
                    "$mainUrl/arama?q=",
                    referer = "$mainUrl/",
                    data    = mapOf("s" to q)
                ).document

                val movieHref = doc.selectFirst("div.film-card a")?.attr("href") ?: continue
                val fullUrl = if (movieHref.startsWith("http")) movieHref else "$mainUrl$movieHref"

                val movieDoc = app.get(fullUrl).document

                // Primary player iframe
                val iframeElement = movieDoc.selectFirst("div#active-player iframe, div.player-container iframe")
                val iframeSrc = iframeElement?.attr("data-litespeed-src")?.takeIf { it.isNotBlank() }
                    ?: iframeElement?.attr("src")
                val mainIframe = if (iframeSrc?.startsWith("//") == true) "https:$iframeSrc" else iframeSrc

                if (!mainIframe.isNullOrBlank()) {
                    if (mainIframe.contains("d2rs")) {
                        // D2RS special handling
                        val apiUrl = mainIframe.replace("/?", "/get_video.php?")
                        try {
                            val resp = objectMapper.readTree(app.get(apiUrl).text)
                            if (resp.path("success").asBoolean()) {
                                val masterUrl   = resp.path("masterUrl").asText()
                                val referrerUrl = resp.path("referrerUrl").asText()
                                callback.invoke(
                                    newExtractorLink(
                                        source = "D2RS",
                                        name   = "[JetFilmizle] D2RS",
                                        url    = masterUrl,
                                        type   = ExtractorLinkType.M3U8
                                    ) {
                                        this.quality = Qualities.Unknown.value
                                        this.headers = mapOf("Referer" to referrerUrl)
                                    }
                                )
                            }
                        } catch (_: Exception) {}
                    } else if (mainIframe.contains("jetv.xyz")) {
                        val jetvDoc = app.get(mainIframe).document
                        val script = jetvDoc.select("script").find { it.data().contains("\"sources\": [") }?.data() ?: ""
                        if (script.isNotBlank()) {
                            val fileMatch = Regex("""file\s*:\s*['"]([^'"]+)['"]""").find(script)
                            val m3u8Url = fileMatch?.groupValues?.get(1)
                            if (!m3u8Url.isNullOrBlank()) {
                                callback.invoke(
                                    newExtractorLink(
                                        source = "Jetv",
                                        name   = "[JetFilmizle] Jetv",
                                        url    = m3u8Url,
                                        type   = ExtractorLinkType.M3U8
                                    ) {
                                        this.quality = Qualities.Unknown.value
                                    }
                                )
                            }
                        }
                    } else {
                        loadExtractor(mainIframe, "$mainUrl/", subtitleCallback, callback)
                    }
                }

                // Download links (Pixeldrain etc.)
                movieDoc.select("a.download-btn[href]").forEach { link ->
                    val href = link.attr("href")
                    if (href.contains("pixeldrain.com") && href.startsWith("http")) {
                        loadExtractor(href, "$mainUrl/", subtitleCallback, callback)
                    }
                }
                break
            } catch (e: Exception) {
                Log.e(TAG, "JetFilmizle error: ${e.message}")
            }
        }
    }

    // ============================================================
    // Helper Data Classes
    // ============================================================
    private data class HDFCResults(
        @JsonProperty("results") val results: List<String> = arrayListOf()
    )

    private data class HDFCSubSource(
        @JsonProperty("file")     val file: String?     = null,
        @JsonProperty("label")    val label: String?    = null,
        @JsonProperty("language") val language: String? = null,
        @JsonProperty("kind")     val kind: String?     = null
    )
}
