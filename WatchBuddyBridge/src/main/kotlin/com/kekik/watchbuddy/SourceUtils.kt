package com.kekik.watchbuddy

import android.util.Base64
import java.security.MessageDigest
import java.util.Arrays
import javax.crypto.Cipher
import javax.crypto.spec.IvParameterSpec
import javax.crypto.spec.SecretKeySpec

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

    fun decryptCryptoJS(encryptedText: String, passPhrase: String): String {
        return try {
            val cipherTextWithSalt = Base64.decode(encryptedText, Base64.DEFAULT)
            val salt = cipherTextWithSalt.copyOfRange(8, 16)
            val cipherText = cipherTextWithSalt.copyOfRange(16, cipherTextWithSalt.size)

            val keyAndIv = deriveKeyAndIv(passPhrase, salt)
            val key = Arrays.copyOfRange(keyAndIv, 0, 32)
            val iv = Arrays.copyOfRange(keyAndIv, 32, 48)

            val secretKey = SecretKeySpec(key, "AES")
            val ivSpec = IvParameterSpec(iv)
            val cipher = Cipher.getInstance("AES/CBC/PKCS5Padding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, ivSpec)

            String(cipher.doFinal(cipherText), Charsets.UTF_8)
        } catch (e: Exception) {
            ""
        }
    }

    private fun deriveKeyAndIv(passPhrase: String, salt: ByteArray): ByteArray {
        val md5 = MessageDigest.getInstance("MD5")
        val pass = passPhrase.toByteArray(Charsets.UTF_8)
        var dx = ByteArray(0)
        var keyAndIv = ByteArray(0)

        while (keyAndIv.size < 48) {
            md5.reset()
            md5.update(dx)
            md5.update(pass)
            md5.update(salt)
            dx = md5.digest()
            keyAndIv += dx
        }
        return keyAndIv
    }

    fun decryptCloseLoad(html: String): String? {
        return try {
            val scriptBlockMatch = """<script[^>]*>(.*?dc_[a-zA-Z0-9_]+\(.*?</script>)""".toRegex(RegexOption.DOT_MATCHES_ALL).find(html)
            val scriptContent = scriptBlockMatch?.groupValues?.get(1) ?: return null

            val arrayMatch = """\(\[((?:"[^"]+",?\s*)+)\]\)""".toRegex().find(scriptContent)
            val parts = arrayMatch?.groupValues?.get(1)?.split(",")?.map {
                it.trim().trim('"').replace("\\/", "/")
            } ?: return null

            val moduloMatch = """(\d+)\s*%\s*\(i\s*\+\s*(\d+)\)""".toRegex().find(scriptContent)
            val magicNum = moduloMatch?.groupValues?.get(1)?.toLongOrNull() ?: 399756995L
            val magicOffset = moduloMatch?.groupValues?.get(2)?.toIntOrNull() ?: 5

            val funcStartIdx = scriptContent.indexOf("function dc_")
            val funcEndIdx = scriptContent.indexOf("function d1x()", funcStartIdx).takeIf { it != -1 } ?: scriptContent.length
            val functionBody = if (funcStartIdx != -1) scriptContent.substring(funcStartIdx, funcEndIdx) else scriptContent

            var rotShift = 13
            val rotShiftMatch = """charCodeAt\(0\)\s*\+\s*(\d+)""".toRegex().find(functionBody)
            if (rotShiftMatch != null) {
                rotShift = rotShiftMatch.groupValues[1].toInt()
            } else {
                val rotShiftMatch2 = """o\s*-\s*base\s*([+-])\s*(\d+)""".toRegex().find(functionBody)
                if (rotShiftMatch2 != null) {
                    val sign = rotShiftMatch2.groupValues[1]
                    val num = rotShiftMatch2.groupValues[2].toInt()
                    rotShift = if (sign == "-") (26 - num) % 26 else num
                }
            }

            val operations = mutableListOf<Pair<Int, String>>()
            var index = functionBody.indexOf("atob(")
            while (index >= 0) {
                operations.add(Pair(index, "atob"))
                index = functionBody.indexOf("atob(", index + 1)
            }
            index = functionBody.indexOf("reverse")
            while (index >= 0) {
                operations.add(Pair(index, "reverse"))
                index = functionBody.indexOf("reverse", index + 1)
            }
            index = functionBody.indexOf("replace")
            while (index >= 0) {
                operations.add(Pair(index, "rot"))
                index = functionBody.indexOf("replace", index + 1)
            }
            operations.sortBy { it.first }

            var result = parts.joinToString("")
            for (op in operations) {
                when (op.second) {
                    "reverse" -> result = result.reversed()
                    "atob" -> {
                        var padded = result
                        while (padded.length % 4 != 0) padded += "="
                        result = String(Base64.decode(padded, Base64.NO_WRAP), Charsets.ISO_8859_1)
                    }
                    "rot" -> {
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
                val charCode = result[i].code.toLong()
                val decryptedCode = (charCode - (magicNum % (i + magicOffset)) + 256) % 256
                unmix.append(decryptedCode.toInt().toChar())
            }
            unmix.toString()
        } catch (e: Exception) {
            null
        }
    }
}