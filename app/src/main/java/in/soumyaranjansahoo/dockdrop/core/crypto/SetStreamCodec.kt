package `in`.soumyaranjansahoo.dockdrop.core.crypto

import java.io.InputStream
import java.io.OutputStream

/**
 * Encapsulates stream wrapping and on-disk path mapping per Set configuration.
 * Satisfies AGENTS.md Handoff Rule 5: Engine and Restore consume this codec without
 * branching on 'isEncrypted'.
 */
interface SetStreamCodec {
    val isEncrypted: Boolean
    fun wrapOutputStream(raw: OutputStream): OutputStream
    fun wrapInputStream(raw: InputStream): InputStream
    fun activeVaultPath(setName: String, logicalPath: String): String
    fun deletedVaultPath(setName: String, dateFolder: String, logicalPath: String): String
}

/**
 * Plain mirror stream codec for unencrypted Sets.
 */
object PlainSetStreamCodec : SetStreamCodec {
    override val isEncrypted: Boolean = false

    override fun wrapOutputStream(raw: OutputStream): OutputStream = raw

    override fun wrapInputStream(raw: InputStream): InputStream = raw

    override fun activeVaultPath(setName: String, logicalPath: String): String {
        return "$setName/$logicalPath"
    }

    override fun deletedVaultPath(setName: String, dateFolder: String, logicalPath: String): String {
        return "_deleted/$setName/$dateFolder/$logicalPath"
    }
}

/**
 * Encrypted stream codec using DDC1 chunked AES-256-GCM streams and HMAC-SHA256 blob names.
 */
class EncryptedSetStreamCodec(
    val derivedKeys: DerivedSetKeys
) : SetStreamCodec {
    override val isEncrypted: Boolean = true

    override fun wrapOutputStream(raw: OutputStream): OutputStream {
        return Ddc1OutputStream(raw, derivedKeys.payloadKey)
    }

    override fun wrapInputStream(raw: InputStream): InputStream {
        return Ddc1InputStream(raw, derivedKeys.payloadKey)
    }

    override fun activeVaultPath(setName: String, logicalPath: String): String {
        val blobId = derivedKeys.computeBlobId(logicalPath)
        val fanout = blobId.take(2)
        return "$setName.enc/blobs/$fanout/$blobId.enc"
    }

    override fun deletedVaultPath(setName: String, dateFolder: String, logicalPath: String): String {
        val blobId = derivedKeys.computeBlobId(logicalPath)
        val fanout = blobId.take(2)
        return "_deleted/$setName.enc/$dateFolder/$fanout/$blobId.enc"
    }
}
