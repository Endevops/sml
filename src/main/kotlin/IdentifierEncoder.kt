package be.endevops

import java.security.MessageDigest

const val BASE32_ALPHABET = "abcdefghijklmnopqrstuvwxyz234567"
private fun base32Encode(input: ByteArray): String {
    var result = ""
    var bits = 0
    var value = 0
    for (byte in input) {
        value = (value shl 8) or (byte.toInt() and 0xFF)
        bits += 8
        while (bits >= 5) {
            result += BASE32_ALPHABET[(value shr (bits - 5)) and 0x1F]
            bits -= 5
        }
    }
    if (bits > 0) {
        result += BASE32_ALPHABET[(value shl (5 - bits)) and 0x1F]
    }
    return result
}

/**
 * Perform the naptr encoding of an identifier
 */
fun naptrIdentifierEncode(identifier: String): String {
    val digest = MessageDigest.getInstance("SHA-256")
    val hashBytes = digest.digest(identifier.lowercase().toByteArray(Charsets.UTF_8))
    return base32Encode(hashBytes)
}

fun cnameIdentifierEncode(identifier: String): String {
    val digest = MessageDigest.getInstance("MD5")
    return digest.digest(identifier.lowercase().toByteArray(Charsets.UTF_8))
        .joinToString("") { String.format("%02x", it) }
}
