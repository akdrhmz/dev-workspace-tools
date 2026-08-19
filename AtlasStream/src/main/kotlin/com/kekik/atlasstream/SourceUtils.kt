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
        val normResult = normalizeTitle(resultTitle)
        if (normResult.isBlank()) return false

        val normTr = normalizeTitle(targetTitleTr)
        val normOrig = targetTitleOrig?.let { normalizeTitle(it) }

        // 1. Tam Eşleşme
        if (normTr.isNotEmpty() && normResult == normTr) return true
        if (normOrig != null && normOrig.isNotEmpty() && normResult == normOrig) return true

        // 2. Yıl Uyumu Kontrolü (Eğer her ikisinde de yıl varsa ve 1 yıldan fazla fark varsa eşleşmeyi reddet)
        if (targetYear != null && resultYear != null && Math.abs(targetYear - resultYear) > 1) {
            return false
        }

        // 3. Kelime Kümesi (Token) Kapsama Kontrolü (Örn: "Breaking Bad" -> "Breaking Bad 1. Sezon")
        val resultWords = normResult.split(" ").filter { it.length > 1 }.toSet()
        
        if (normTr.isNotEmpty()) {
            val trWords = normTr.split(" ").filter { it.length > 1 }.toSet()
            if (trWords.isNotEmpty() && (resultWords.containsAll(trWords) || trWords.containsAll(resultWords))) {
                return true
            }
        }

        if (normOrig != null && normOrig.isNotEmpty()) {
            val origWords = normOrig.split(" ").filter { it.length > 1 }.toSet()
            if (origWords.isNotEmpty() && (resultWords.containsAll(origWords) || origWords.containsAll(resultWords))) {
                return true
            }
        }

        // 4. Ön Ek / Başlangıç Eşleşmesi (Minimum 3 karakter)
        if (normTr.length >= 3 && (normResult.startsWith(normTr) || normTr.startsWith(normResult))) {
            return true
        }
        if (normOrig != null && normOrig.length >= 3 && (normResult.startsWith(normOrig) || normOrig.startsWith(normResult))) {
            return true
        }

        return false
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
