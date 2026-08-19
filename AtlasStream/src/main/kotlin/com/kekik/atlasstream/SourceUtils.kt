package com.kekik.atlasstream

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.PBEKeySpec
import javax.crypto.spec.SecretKeySpec
import javax.crypto.SecretKeyFactory
import kotlin.math.min

object SourceUtils {
    fun cleanTitle(title: String): String {
        return title
            .replace(":", " ")
            .replace("!", "")
            .replace("?", "")
            .replace("'", "")
            .replace("\"", "")
            .replace("-", " ")
            .replace(Regex("\\s+"), " ")
            .trim()
    }

    /**
     * TMDB başlıklarından ve IMDb ID'sinden site arama motorlarının bulabileceği tüm varyasyonları üretir.
     * Örn: "House of the Dragon", "Spartaküs: Kan ve Kum" -> ["tt11198330", "House of the Dragon", "Spartacus", "Spartaküs", ...]
     */
    fun getSearchQueries(titleTr: String, titleOrig: String?, imdbId: String? = null): List<String> {
        val queries = linkedSetOf<String>()

        // 1. IMDb ID: IMDb araması destekleyen siteler için doğrudan kesin sonuç verir
        if (!imdbId.isNullOrBlank() && imdbId.startsWith("tt")) {
            queries.add(imdbId)
        }

        fun addCandidates(raw: String) {
            val clean = cleanTitle(raw)
            if (clean.isNotBlank()) queries.add(clean)

            // İki nokta, tire, parantez veya slash öncesindeki ana başlık
            val parts = raw.split(":", "-", "(", "/", "|").map { it.trim() }.filter { it.length >= 2 }
            if (parts.isNotEmpty()) {
                val mainPart = cleanTitle(parts[0])
                if (mainPart.isNotBlank()) queries.add(mainPart)
            }

            // Eğer başlıkta "Sezon", "Season", "Part", "Bölüm" gibi ekler varsa temizle
            val noSeason = raw.replace(Regex("""(?i)\b(sezon|season|part|bölüm|bolum|\d+\.\s*sezon|\d+\.\s*bölüm)\b.*"""), "").trim()
            val cleanNoSeason = cleanTitle(noSeason)
            if (cleanNoSeason.isNotBlank()) queries.add(cleanNoSeason)
        }

        // 2. Orijinal / Uluslararası başlık (Örn: "House of the Dragon", "Spartacus")
        if (titleOrig != null) addCandidates(titleOrig)
        // 3. Türkçe başlık (Örn: "Spartaküs", "Örümcek Adam")
        addCandidates(titleTr)

        return queries.filter { it.length >= 2 }.toList()
    }

    /**
     * Başlıktaki parantez içi yıl, ek kelimeler ve gürültüleri temizler, Türkçe karakterleri normalize eder.
     */
    fun normalizeTitle(input: String): String {
        var s = input.lowercase().trim()
        
        // Parantez veya köşeli parantez içindeki yıl veya ekleri sil: (2024), [1080p], (Dizi), vb.
        s = s.replace("""\(\d{4}(?:-\d{4})?\)""".toRegex(), " ")
        s = s.replace("""\[\d{4}(?:-\d{4})?\]""".toRegex(), " ")
        s = s.replace("""\(\s*dizi\s*\)""".toRegex(RegexOption.IGNORE_CASE), " ")
        s = s.replace("""\(\s*film\s*\)""".toRegex(RegexOption.IGNORE_CASE), " ")
        s = s.replace("""\b(19\d\d|20\d\d)\b""".toRegex(), " ")
        
        // Türkçe streaming sitelerindeki yaygın ekleri ve gürültü kelimelerini temizle
        val stopWords = listOf(
            "turkce", "türkçe", "dublaj", "dublajli", "dublajlı", "altyazi", "altyazili", "altyazılı",
            "izle", "filmi", "dizisi", "dizi", "film", "full", "hd", "1080p", "720p", "4k", "uhd",
            "sinema", "tek parca", "tek parça", "parca", "parça", "fragman", "ozel", "özel",
            "sezon", "bolum", "bölüm", "series", "movie", "season", "episode"
        )
        for (sw in stopWords) {
            s = s.replace(Regex("""\b${Regex.escape(sw)}\b""", RegexOption.IGNORE_CASE), " ")
        }
        
        // Türkçe karakterleri standart Latin karakterlerine dönüştür
        val trMap = mapOf(
            'ç' to 'c', 'ğ' to 'g', 'ı' to 'i', 'i' to 'i', 'ö' to 'o', 'ş' to 's', 'ü' to 'u',
            'Ç' to 'c', 'Ğ' to 'g', 'İ' to 'i', 'I' to 'i', 'Ö' to 'o', 'Ş' to 's', 'Ü' to 'u'
        )
        val sb = StringBuilder()
        for (ch in s) {
            sb.append(trMap[ch] ?: ch)
        }
        s = sb.toString()

        // Noktalama ve özel karakterleri boşlukla değiştir
        s = s.replace(Regex("[^a-z0-9]"), " ")

        // Fazla boşlukları tek boşluğa indir
        return s.replace(Regex("\\s+"), " ").trim()
    }

    // Sitelerde sık görülen kısaltma / eş anlam farklarını eşitlemek için
    private val synonymMap = mapOf(
        "vol" to "volume", "pt" to "part", "ch" to "chapter",
        "and" to "ve", "the" to "", "a" to "", "an" to ""
    )

    private fun canonicalWords(norm: String): Set<String> {
        return norm.split(" ")
            .filter { it.length > 1 }
            .map { synonymMap[it] ?: it }
            .filter { it.isNotBlank() }
            .toSet()
    }

    /**
     * İki başlık kümesi arasındaki benzerliği 0.0 - 1.0 aralığında Jaccard skoru olarak döner.
     */
    private fun similarityScore(resultWords: Set<String>, targetWords: Set<String>): Double {
        if (resultWords.isEmpty() || targetWords.isEmpty()) return 0.0
        val intersection = resultWords.intersect(targetWords).size
        val union = resultWords.union(targetWords).size
        if (union == 0) return 0.0
        // Kapsama oranını da hesaba kat: küçük kümenin büyük kümede ne kadarının geçtiği
        val containmentRatio = intersection.toDouble() / minOf(resultWords.size, targetWords.size)
        val jaccard = intersection.toDouble() / union
        // İkisinin ağırlıklı ortalaması: kısa ek/başlık farklarına (alt başlık, "Bölüm" ekleri vb.) tolerans tanır
        return (jaccard * 0.5) + (containmentRatio * 0.5)
    }

    /**
     * Sağlayıcıdan dönen arama sonucu başlığı ile hedef içeriğin ne kadar eşleştiğini 0.0 - 1.0 arası skorlar.
     * 0.0 döndüğünde kesin uyumsuzluk (ör. yıl farkı), 1.0 tam eşleşme demektir.
     */
    fun titleMatchScore(
        resultTitle: String,
        targetTitleTr: String,
        targetTitleOrig: String? = null,
        targetYear: Int? = null,
        resultYear: Int? = null
    ): Double {
        val normResult = normalizeTitle(resultTitle)
        if (normResult.isBlank()) return 0.0

        val normTr = normalizeTitle(targetTitleTr)
        val normOrig = targetTitleOrig?.let { normalizeTitle(it) }

        // 1. Tam Eşleşme
        if (normTr.isNotEmpty() && normResult == normTr) return 1.0
        if (normOrig != null && normOrig.isNotEmpty() && normResult == normOrig) return 1.0

        // 2. Yıl Uyumu Kontrolü (Eğer her ikisinde de yıl varsa ve 1 yıldan fazla fark varsa kesin reddet)
        if (targetYear != null && resultYear != null && Math.abs(targetYear - resultYear) > 1) {
            return 0.0
        }
        // Yıl tam uyuyorsa küçük bir bonus ver (aynı isimli farklı yapımları ayırt etmeye yardımcı olur)
        val yearBonus = if (targetYear != null && resultYear != null && targetYear == resultYear) 0.1 else 0.0

        val resultWords = canonicalWords(normResult)

        var bestScore = 0.0
        if (normTr.isNotEmpty()) {
            bestScore = maxOf(bestScore, similarityScore(resultWords, canonicalWords(normTr)))
        }
        if (normOrig != null && normOrig.isNotEmpty()) {
            bestScore = maxOf(bestScore, similarityScore(resultWords, canonicalWords(normOrig)))
        }

        // İçerme / Ön ek eşleşmesi (ör. "House of the Dragon" -> "House of the Dragon 1. Sezon")
        if (normTr.length >= 3 && (normResult.contains(normTr) || normTr.contains(normResult))) {
            bestScore = maxOf(bestScore, 0.90)
        }
        if (normOrig != null && normOrig.length >= 3 && (normResult.contains(normOrig) || normOrig.contains(normResult))) {
            bestScore = maxOf(bestScore, 0.90)
        }

        if (bestScore <= 0.0) return 0.0
        return minOf(1.0, bestScore + yearBonus)
    }

    /** Eşik değeri: bu skorun altındaki sonuçlar eşleşme sayılmaz. */
    const val MATCH_THRESHOLD = 0.45

    /**
     * Sağlayıcıdan dönen arama sonucu başlığı ile hedef içeriğin eşleşip eşleşmediğini akıllıca kontrol eder.
     */
    fun isTitleMatch(
        resultTitle: String,
        targetTitleTr: String,
        targetTitleOrig: String? = null,
        targetYear: Int? = null,
        resultYear: Int? = null
    ): Boolean {
        return titleMatchScore(resultTitle, targetTitleTr, targetTitleOrig, targetYear, resultYear) >= MATCH_THRESHOLD
    }

    // --- FullHDFilmizlesene Decrypt ---
    fun atob(s: String): String {
        return String(Base64.decode(s, Base64.DEFAULT))
    }

    fun rot13(s: String): String {
        fun rot13Char(c: Char): Char {
            return when (c) {
                in 'a'..'z' -> ((c - 'a' + 13) % 26 + 'a'.code).toChar()
                in 'A'..'Z' -> ((c - 'A' + 13) % 26 + 'A'.code).toChar()
                else -> c
            }
        }
        return s.map { rot13Char(it) }.joinToString("")
    }

    // --- DiziPal AES-CBC PBKDF2 Decrypt ---
    private fun String.decodeHex(): ByteArray {
        check(length % 2 == 0) { "Hex string must have an even length" }
        return chunked(2).map { it.toInt(16).toByte() }.toByteArray()
    }

    fun decryptDizipalData(rawJsonText: String): String {
        return try {
            val passphrase = "3hPn4uCjTVtfYWcjIcoJQ4cL1WWk1qxXI39egLYOmNv6IblA7eKJz68uU3eLzux1biZLCms0quEjTYniGv5z1JcKbNIsDQFSeIZOBZJz4is6pD7UyWDggWWzTLBQbHcQFpBQdClnuQaMNUHtLHTpzCvZy33p6I7wFBvL4fnXBYH84aUIyWGTRvM2G5cfoNf4705tO2kv"
            val ctMatch = """"ciphertext"\s*:\s*"([^"]+)"""".toRegex().find(rawJsonText)?.groupValues?.get(1) ?: return ""
            val ivMatch = """"iv"\s*:\s*"([^"]+)"""".toRegex().find(rawJsonText)?.groupValues?.get(1) ?: return ""
            val saltMatch = """"salt"\s*:\s*"([^"]+)"""".toRegex().find(rawJsonText)?.groupValues?.get(1) ?: return ""

            val salt = saltMatch.decodeHex()
            val iv = ivMatch.decodeHex()
            val ciphertext = Base64.decode(ctMatch, Base64.DEFAULT)

            val factory = SecretKeyFactory.getInstance("PBKDF2WithHmacSHA512")
            val spec = PBEKeySpec(passphrase.toCharArray(), salt, 999, 256)
            val secretKey = factory.generateSecret(spec)
            val secret = SecretKeySpec(secretKey.encoded, "AES")

            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secret, IvParameterSpec(iv))

            val decryptedBytes = cipher.doFinal(ciphertext)
            var finalUrl = String(decryptedBytes, Charsets.UTF_8).replace("\\/", "/")
            if (finalUrl.startsWith("://")) finalUrl = "https$finalUrl"
            else if (finalUrl.startsWith("//")) finalUrl = "https:$finalUrl"
            else if (!finalUrl.startsWith("http")) finalUrl = "https://$finalUrl"
            finalUrl
        } catch (e: Exception) {
            ""
        }
    }

    // --- DiziBox CryptoJS AES Decrypt (from DiziBoxUtils.kt CryptoJS object) ---
    fun decryptCryptoJS(cipherText: String, password: String): String {
        return try {
            val ctBytes         = Base64.decode(cipherText.toByteArray(), Base64.DEFAULT)
            val saltBytes       = ctBytes.copyOfRange(8, 16)
            val cipherTextBytes = ctBytes.copyOfRange(16, ctBytes.size)

            val keySize   = 256
            val ivSize    = 128
            val key       = ByteArray(keySize / 8)
            val iv        = ByteArray(ivSize / 8)
            evpkdf(password.toByteArray(), keySize, ivSize, saltBytes, key, iv)

            val cipher = Cipher.getInstance("AES/CBC/PKCS7Padding")
            val keyS   = SecretKeySpec(key, "AES")
            cipher.init(Cipher.DECRYPT_MODE, keyS, IvParameterSpec(iv))

            String(cipher.doFinal(cipherTextBytes))
        } catch (e: Exception) {
            ""
        }
    }

    @Suppress("NAME_SHADOWING")
    private fun evpkdf(
        password: ByteArray, keySize: Int, ivSize: Int,
        salt: ByteArray, resultKey: ByteArray, resultIv: ByteArray
    ) {
        val keySize              = keySize / 32
        val ivSize               = ivSize / 32
        val targetKeySize        = keySize + ivSize
        val derivedBytes         = ByteArray(targetKeySize * 4)
        var numberOfDerivedWords = 0
        var block: ByteArray?    = null
        val hash                 = MessageDigest.getInstance("MD5")

        while (numberOfDerivedWords < targetKeySize) {
            if (block != null) hash.update(block)
            hash.update(password)
            block = hash.digest(salt)
            hash.reset()

            System.arraycopy(
                block!!, 0, derivedBytes, numberOfDerivedWords * 4,
                min(block.size, (targetKeySize - numberOfDerivedWords) * 4)
            )
            numberOfDerivedWords += block.size / 4
        }

        System.arraycopy(derivedBytes, 0, resultKey, 0, keySize * 4)
        System.arraycopy(derivedBytes, keySize * 4, resultIv, 0, ivSize * 4)
    }

    // --- Dizilla AES-CBC Decrypt (from Dizilla.kt decryptDizillaResponse) ---
    private const val DIZILLA_AES_KEY = "9bYMCNQiWsXIYFWYAu7EkdsSbmGBTyUI"

    fun decryptDizillaResponse(response: String): String? {
        return try {
            val fullData      = Base64.decode(response, Base64.DEFAULT)
            val ivSpec        = IvParameterSpec(ByteArray(16))
            val keySpec       = SecretKeySpec(DIZILLA_AES_KEY.toByteArray(Charsets.UTF_8), "AES")
            val cipher        = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, keySpec, ivSpec)
            String(cipher.doFinal(fullData), Charsets.UTF_8)
        } catch (e: Exception) {
            null
        }
    }

    // --- HDFilmCehennemi decryptLocalUrl (from HDFilmCehennemi.kt) ---
    fun decryptLocalUrl(unpackedScript: String): String? {
        try {
            val partsMatch = """\(\[\s*((?:['"][^'"]+['"]\s*,?\s*)+)\]\)""".toRegex().find(unpackedScript)
            val parts = partsMatch?.groupValues?.get(1)?.split(",")?.map {
                it.trim().trim('\'', '"').replace("\\/", "/")
            } ?: return null

            val moduloMatch = """(\d+)\s*%\s*\(i\s*\+\s*(\d+)\)""".toRegex().find(unpackedScript)
            val magicNum    = moduloMatch?.groupValues?.get(1)?.toLongOrNull() ?: 399756995L
            val magicOffset = moduloMatch?.groupValues?.get(2)?.toIntOrNull() ?: 5

            val funcBody = unpackedScript.substringAfter("function dc_").substringBefore("function d1x")

            val operations = mutableListOf<Pair<Int, Triple<String, Int, Int>>>()

            var index = funcBody.indexOf("atob(")
            while (index >= 0) {
                operations.add(Pair(index, Triple("atob", 0, 0)))
                index = funcBody.indexOf("atob(", index + 1)
            }

            index = funcBody.indexOf("reverse")
            while (index >= 0) {
                operations.add(Pair(index, Triple("reverse", 0, 0)))
                index = funcBody.indexOf("reverse", index + 1)
            }

            index = funcBody.indexOf("replace")
            while (index >= 0) {
                val block = funcBody.substring(index, minOf(index + 300, funcBody.length))
                var shift = 13
                val rotShiftMatch = """charCodeAt\(0\)\s*\+\s*(\d+)""".toRegex().find(block)
                if (rotShiftMatch != null) {
                    shift = rotShiftMatch.groupValues[1].toInt()
                } else {
                    val rotShiftMatch2 = """o\s*-\s*base\s*([+-])\s*(\d+)""".toRegex().find(block)
                    if (rotShiftMatch2 != null) {
                        val sign = rotShiftMatch2.groupValues[1]
                        val num  = rotShiftMatch2.groupValues[2].toInt()
                        shift = if (sign == "-") (26 - num) % 26 else num
                    }
                }
                operations.add(Pair(index, Triple("rot", shift, 0)))
                index = funcBody.indexOf("replace", index + 1)
            }

            operations.sortBy { it.first }

            var result = parts.joinToString("")

            for (op in operations) {
                val action = op.second
                when (action.first) {
                    "reverse" -> result = result.reversed()
                    "atob" -> {
                        var padded = result
                        while (padded.length % 4 != 0) padded += "="
                        result = String(Base64.decode(padded, Base64.NO_WRAP), Charsets.ISO_8859_1)
                    }
                    "rot" -> {
                        val rotShift = action.second
                        val rot = StringBuilder()
                        for (c in result) {
                            if (c in 'a'..'z') {
                                val shifted = c.code + rotShift
                                rot.append(if (shifted > 'z'.code) (shifted - 26).toChar() else shifted.toChar())
                            } else if (c in 'A'..'Z') {
                                val shifted = c.code + rotShift
                                rot.append(if (shifted > 'Z'.code) (shifted - 26).toChar() else shifted.toChar())
                            } else {
                                rot.append(c)
                            }
                        }
                        result = rot.toString()
                    }
                }
            }

            val unmix = StringBuilder()
            for (i in result.indices) {
                val charCode      = result[i].code.toLong()
                val decryptedCode = (charCode - (magicNum % (i + magicOffset)) + 256) % 256
                unmix.append(decryptedCode.toInt().toChar())
            }

            return unmix.toString()
        } catch (e: Exception) {
            return null
        }
    }
}
