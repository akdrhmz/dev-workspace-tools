package com.kekik.atlasstream

import android.util.Base64
import java.security.MessageDigest
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec
import kotlin.math.min

object SourceUtils {
    fun cleanTitle(title: String): String {
        return title
            .replace(":", "")
            .replace("!", "")
            .replace("?", "")
            .replace("'", "")
            .replace("\"", "")
            .replace("-", " ")
            .trim()
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
