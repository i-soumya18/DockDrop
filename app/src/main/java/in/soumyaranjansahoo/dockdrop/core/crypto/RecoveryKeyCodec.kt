package `in`.soumyaranjansahoo.dockdrop.core.crypto

import java.security.SecureRandom
import java.util.Locale

/**
 * Generates and normalizes 160-bit Recovery Keys using Crockford Base32.
 */
object RecoveryKeyCodec {

    private const val ALPHABET = "0123456789ABCDEFGHJKMNPQRSTVWXYZ"

    /**
     * Generates a 160-bit (20-byte) recovery key formatted as 8 groups of 4 characters:
     * e.g. "7QKM-2D9F-8V3P-4NXR-9T6W-1C5H-J8YB-G24K"
     */
    fun generate(random: SecureRandom = SecureRandom()): String {
        val bytes = ByteArray(20)
        random.nextBytes(bytes)
        return encode(bytes)
    }

    /**
     * Encodes 20 bytes (160 bits) into 32 Crockford Base32 characters formatted in 8 groups of 4.
     */
    fun encode(bytes: ByteArray): String {
        require(bytes.size == 20) { "Recovery key source must be exactly 20 bytes (160 bits)" }

        val sb = StringBuilder(32)
        var buffer = 0
        var bitsLeft = 0

        for (b in bytes) {
            buffer = (buffer shl 8) or (b.toInt() and 0xFF)
            bitsLeft += 8
            while (bitsLeft >= 5) {
                val index = (buffer shr (bitsLeft - 5)) and 0x1F
                sb.append(ALPHABET[index])
                bitsLeft -= 5
            }
        }
        if (bitsLeft > 0) {
            val index = (buffer shl (5 - bitsLeft)) and 0x1F
            sb.append(ALPHABET[index])
        }

        val raw32 = sb.toString()
        // Format as 8 groups of 4 characters
        return raw32.chunked(4).joinToString("-")
    }

    /**
     * Normalizes user-entered recovery key:
     * - Strips hyphens, spaces, and formatting characters
     * - Uppercases
     * - Maps confusable characters: 'O' -> '0', 'I' -> '1', 'L' -> '1'
     */
    fun normalize(input: String): String {
        val cleaned = input.uppercase(Locale.US)
            .replace("-", "")
            .replace(" ", "")
            .replace("O", "0")
            .replace("I", "1")
            .replace("L", "1")
        return cleaned
    }

    /**
     * Verifies that the normalized user-entered last four characters match
     * the last four characters of the normalized recovery key.
     */
    fun verifyLastFourChars(recoveryKey: String, userLastFour: String): Boolean {
        val normKey = normalize(recoveryKey)
        val normUser = normalize(userLastFour)
        if (normKey.length != 32 || normUser.length != 4) return false
        return normKey.takeLast(4) == normUser
    }
}
