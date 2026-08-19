package com.kekik.atlasstream.providers.dizilla

// ! Bu araÃ§ @keyiflerolsun tarafÄ±ndan | @KekikAkademi iÃ§in yazÄ±lmÄ±ÅŸtÄ±r.



import android.util.Log
import com.lagradost.cloudstream3.ErrorLoadingException
import com.lagradost.cloudstream3.SubtitleFile
import com.lagradost.cloudstream3.app
import com.lagradost.cloudstream3.utils.*

open class ContentX : ExtractorApi() {
    override val name            = "ContentX"
    override val mainUrl         = "https://contentx.me"
    override val requiresReferer = true

    override suspend fun getUrl(url: String, referer: String?, subtitleCallback: (SubtitleFile) -> Unit, callback: (ExtractorLink) -> Unit) {
        val extRef   = referer ?: ""
        Log.d("Dizilla", "url Â» $url")

        val iSource = app.get(url, referer = extRef).text
        val iExtract = Regex("""window\.openPlayer\('([^']+)'""").find(iSource)!!.groups[1]?.value ?: throw ErrorLoadingException("iExtract is null")

        val subUrls = mutableSetOf<String>()

// DÃœZELTÄ°LMÄ°Å REGEX (Unicode ve Ã¶zel karakterleri dÃ¼zgÃ¼n yakalÄ±yor)
        Regex(""""file":"((?:\\\\\"|[^"])+)","label":"((?:\\\\\"|[^"])+)"""").findAll(iSource).forEach {
            val (subUrlRaw, subLangRaw) = it.destructured

            // URL ve Dil iÃ§in escape karakterleri temizleme
            val subUrl = subUrlRaw.replace("\\/", "/").replace("\\u0026", "&").replace("\\", "")
            val subLang = subLangRaw
                .replace("\\u0131", "Ä±")
                .replace("\\u0130", "Ä°")
                .replace("\\u00fc", "Ã¼")
                .replace("\\u00e7", "Ã§")
                .replace("\\u011f", "ÄŸ")
                .replace("\\u015f", "ÅŸ")

            if (subUrl in subUrls) return@forEach
            subUrls.add(subUrl)

            subtitleCallback.invoke(
                SubtitleFile(
                    lang = subLang,
                    url = fixUrl(subUrl)
                )
            )
        }

        Log.d("Dizilla", "subtitle Â» $subUrls -- subtitle diger $subtitleCallback")

        val vidSource  = app.get("${mainUrl}/source2.php?v=${iExtract}", referer=extRef).text
        val vidExtract = Regex("""file":"([^"]+)""").find(vidSource)?.groups?.get(1)?.value ?: throw ErrorLoadingException("vidExtract is null")
        val m3uLink    = vidExtract.replace("\\", "")

        callback.invoke(
            newExtractorLink(
                source  = this.name,
                name    = this.name,
                url     = m3uLink,
                type = ExtractorLinkType.M3U8

            ) {
                headers = mapOf("Referer" to url,
                    "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Norton/124.0.0.0") // "Referer" ayarÄ± burada yapÄ±labilir
                quality = Qualities.Unknown.value
            }
        )

        val iDublaj = Regex(""","([^']+)","TÃ¼rkÃ§e""").find(iSource)?.groups?.get(1)?.value
        if (iDublaj != null) {
            val dublajSource  = app.get("${mainUrl}/source2.php?v=${iDublaj}", referer=extRef).text
            val dublajExtract = Regex("""file":"([^"]+)""").find(dublajSource)!!.groups[1]?.value ?: throw ErrorLoadingException("dublajExtract is null")
            val dublajLink    = dublajExtract.replace("\\", "")

            callback.invoke(
                newExtractorLink(
                    source = this.name,
                    name = this.name,
                    url = dublajLink,
                    type = ExtractorLinkType.M3U8
                ) {
                    headers = mapOf("Referer" to url,
                        "User-Agent" to "Mozilla/5.0 (Windows NT 10.0; Win64; x64) AppleWebKit/537.36 (KHTML, like Gecko) Chrome/124.0.0.0 Safari/537.36 Norton/124.0.0.0") // "Referer" ayarÄ± burada yapÄ±labilir
                    quality = Qualities.Unknown.value
                }
            )
        }
    }
}
