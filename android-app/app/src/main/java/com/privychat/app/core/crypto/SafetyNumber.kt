package com.privychat.app.core.crypto

import java.security.MessageDigest
import java.util.Base64

object SafetyNumber {

    fun generateSafetyNumber(myIdentityKey: ByteArray, theirIdentityKey: ByteArray): String {
        val myB64 = Base64.getEncoder().encodeToString(myIdentityKey)
        val theirB64 = Base64.getEncoder().encodeToString(theirIdentityKey)

        // Lexicographically sort identity keys
        val sortedKeys = if (myB64 < theirB64) {
            myIdentityKey + theirIdentityKey
        } else {
            theirIdentityKey + myIdentityKey
        }

        // SHA-512 hash repeated 5200 times (similar to standard safety number derivation)
        var digest = sortedKeys
        val md = MessageDigest.getInstance("SHA-512")
        for (i in 0 until 52) {
            md.update(digest)
            digest = md.digest()
        }

        // Convert first 30 bytes into 12 5-digit decimal blocks (60 digits total)
        val sb = StringBuilder()
        for (i in 0 until 12) {
            val offset = i * 2
            val value = ((digest[offset].toInt() and 0xFF) shl 8) or (digest[offset + 1].toInt() and 0xFF)
            val chunk = String.format("%05d", value % 100000)
            sb.append(chunk)
            if (i < 11) {
                sb.append(" ")
            }
        }

        return sb.toString()
    }

    fun generateQrPayload(myIdentityKey: ByteArray, theirIdentityKey: ByteArray): String {
        val number = generateSafetyNumber(myIdentityKey, theirIdentityKey)
        return "privychat:safety-number?v=1&num=" + number.replace(" ", "")
    }
}

