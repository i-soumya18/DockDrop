package `in`.soumyaranjansahoo.dockdrop.core.crypto

import java.nio.charset.StandardCharsets
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * RFC 5869 HKDF using HMAC-SHA256.
 */
object HkdfSha256 {

    fun extract(salt: ByteArray, ikm: ByteArray): ByteArray {
        val effectiveSalt = if (salt.isEmpty()) ByteArray(32) else salt
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(effectiveSalt, "HmacSHA256"))
        return mac.doFinal(ikm)
    }

    fun expand(prk: ByteArray, info: ByteArray, length: Int): ByteArray {
        require(length <= 255 * 32) { "Requested HKDF length too large" }
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(prk, "HmacSHA256"))

        val n = (length + 31) / 32
        val okm = ByteArray(length)
        var t = ByteArray(0)
        var offset = 0

        for (i in 1..n) {
            mac.reset()
            mac.update(t)
            mac.update(info)
            mac.update(i.toByte())
            t = mac.doFinal()

            val toCopy = minOf(32, length - offset)
            System.arraycopy(t, 0, okm, offset, toCopy)
            offset += toCopy
        }

        return okm
    }

    fun deriveKey(ikm: ByteArray, salt: ByteArray, info: String, length: Int = 32): ByteArray {
        val prk = extract(salt, ikm)
        return expand(prk, info.toByteArray(StandardCharsets.UTF_8), length)
    }
}

/**
 * Subkeys derived from a Set's 256-bit Master Key (MK).
 */
class DerivedSetKeys(
    val payloadKey: ByteArray,
    val filenameKey: ByteArray
) {
    init {
        require(payloadKey.size == 32) { "Payload key must be 32 bytes" }
        require(filenameKey.size == 32) { "Filename key must be 32 bytes" }
    }

    /**
     * Computes the deterministic 64-character lowercase hex blobId from a logical path
     * (e.g. "Camera/IMG_001.jpg") using HMAC-SHA256 keyed with filenameKey.
     */
    fun computeBlobId(logicalPath: String): String {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(filenameKey, "HmacSHA256"))
        val digest = mac.doFinal(logicalPath.toByteArray(StandardCharsets.UTF_8))
        return digest.joinToString("") { "%02x".format(it) }
    }

    companion object {
        fun fromMasterKey(masterKey: ByteArray, setId: String): DerivedSetKeys {
            require(masterKey.size == 32) { "Master key must be 32 bytes" }
            val salt = setId.toByteArray(StandardCharsets.UTF_8)
            val prk = HkdfSha256.extract(salt, masterKey)
            val payloadKey = HkdfSha256.expand(
                prk,
                "DockDrop-v1-PayloadKey".toByteArray(StandardCharsets.UTF_8),
                32
            )
            val filenameKey = HkdfSha256.expand(
                prk,
                "DockDrop-v1-FilenameKey".toByteArray(StandardCharsets.UTF_8),
                32
            )
            return DerivedSetKeys(payloadKey, filenameKey)
        }
    }
}
