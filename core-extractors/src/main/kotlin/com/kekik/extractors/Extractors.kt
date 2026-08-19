package com.kekik.extractors

import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.ExtractorApi
import com.lagradost.cloudstream3.utils.ExtractorLink
import com.lagradost.cloudstream3.utils.ExtractorLinkType
import com.lagradost.cloudstream3.utils.Qualities
import com.lagradost.cloudstream3.utils.newExtractorLink

// ─── RapidVid Extractor ──────────────────────────────────────────────
open class RapidVid : ExtractorApi() {
    override val name = "RapidVid"
    override val mainUrl = "https://rapidvid.net"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val response = app.get(url, referer = referer ?: mainUrl)
        val html = response.text

        // M3U8 Regex
        Regex("""(?:file|source|src):\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""")
            .findAll(html).forEach { match ->
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name HLS",
                        url = match.groupValues[1],
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = url
                        this.quality = Qualities.Unknown.value
                    }
                )
            }

        // MP4 Regex
        Regex("""(?:file|source|src):\s*['"](https?://[^'"]+\.mp4[^'"]*)['"]""")
            .findAll(html).forEach { match ->
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name MP4",
                        url = match.groupValues[1],
                        type = ExtractorLinkType.VIDEO
                    ) {
                        this.referer = url
                        this.quality = Qualities.P1080.value
                    }
                )
            }
    }
}

// ─── VidMoxy Extractor ───────────────────────────────────────────────
open class VidMoxy : ExtractorApi() {
    override val name = "VidMoxy"
    override val mainUrl = "https://vidmoxy.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val response = app.get(url, referer = referer ?: mainUrl)
        val html = response.text

        Regex("""sources:\s*\[\{\s*file:\s*['"](https?://[^'"]+)['"]""")
            .findAll(html).forEach { match ->
                val streamUrl = match.groupValues[1]
                val isM3u8 = streamUrl.contains(".m3u8")
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name Stream",
                        url = streamUrl,
                        type = if (isM3u8) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        this.referer = url
                        this.quality = if (isM3u8) Qualities.Unknown.value else Qualities.P1080.value
                    }
                )
            }

        // Subtitle extraction
        val trackBlock = Regex("""tracks:\s*\[(.*?)\]""", RegexOption.DOT_MATCHES_ALL).find(html)
        if (trackBlock != null) {
            Regex("""file:\s*['"](https?://[^'"]+)['"].*?label:\s*['"]([^'"]+)['"]""")
                .findAll(trackBlock.groupValues[1]).forEach { subMatch ->
                    subtitleCallback.invoke(
                        com.lagradost.cloudstream3.newSubtitleFile(
                            lang = subMatch.groupValues[2],
                            url = subMatch.groupValues[1]
                        )
                    )
                }
        }
    }
}

// ─── Tortuga Extractor ───────────────────────────────────────────────
open class Tortuga : ExtractorApi() {
    override val name = "Tortuga"
    override val mainUrl = "https://tortuga.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val headers = mapOf("Referer" to (referer ?: mainUrl))
        val response = app.get(url, headers = headers)
        val html = response.text

        Regex("""https?://[^\s"'<>]+\.m3u8[^\s"'<>]*""")
            .findAll(html).forEach { match ->
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name HLS",
                        url = match.value,
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = url
                        this.quality = Qualities.Unknown.value
                        this.headers = mapOf("Referer" to url)
                    }
                )
            }
    }
}

// ─── CloseLoad Extractor ─────────────────────────────────────────────
open class CloseLoad : ExtractorApi() {
    override val name = "CloseLoad"
    override val mainUrl = "https://closeload.com"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val response = app.get(url, referer = referer ?: mainUrl)
        val html = response.text

        Regex("""(?:file|src):\s*['"](https?://[^'"]+)['"]""")
            .findAll(html).forEach { match ->
                val streamUrl = match.groupValues[1]
                val isM3u8 = streamUrl.contains(".m3u8")
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name Stream",
                        url = streamUrl,
                        type = if (isM3u8) ExtractorLinkType.M3U8 else ExtractorLinkType.VIDEO
                    ) {
                        this.referer = url
                        this.quality = Qualities.P1080.value
                    }
                )
            }
    }
}

// ─── Vidmoly Extractor ───────────────────────────────────────────────
open class Vidmoly : ExtractorApi() {
    override val name = "Vidmoly"
    override val mainUrl = "https://vidmoly.to"
    override val requiresReferer = true

    override suspend fun getUrl(
        url: String,
        referer: String?,
        subtitleCallback: (SubtitleFile) -> Unit,
        callback: (ExtractorLink) -> Unit
    ) {
        val response = app.get(url, referer = referer ?: mainUrl)
        val html = response.text

        Regex("""file:\s*['"](https?://[^'"]+\.m3u8[^'"]*)['"]""")
            .find(html)?.let { match ->
                callback.invoke(
                    newExtractorLink(
                        source = name,
                        name = "$name HLS",
                        url = match.groupValues[1],
                        type = ExtractorLinkType.M3U8
                    ) {
                        this.referer = url
                        this.quality = Qualities.Unknown.value
                    }
                )
            }
    }
}
