package `in`.soumyaranjansahoo.dockdrop.core.crypto

import java.io.EOFException
import java.io.IOException
import java.io.InputStream
import java.io.OutputStream
import java.nio.ByteBuffer
import java.nio.ByteOrder
import java.security.SecureRandom
import javax.crypto.Cipher
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

/**
 * DDC1 Stream Constants
 */
object Ddc1Constants {
    val MAGIC = byteArrayOf(0x44, 0x44, 0x43, 0x31) // "DDC1"
    const val FORMAT_VERSION: Byte = 0x01
    const val DEFAULT_CHUNK_SIZE = 1_048_576 // 1 MB
    const val HEADER_SIZE = 24
    const val GCM_TAG_BYTES = 16

    const val FLAG_INTERMEDIATE: Byte = 0x00
    const val FLAG_FINAL: Byte = 0x01

    fun constructNonce(streamId: ByteArray, chunkIndex: Long): ByteArray {
        require(streamId.size == 12) { "streamId must be 12 bytes" }
        val nonce = streamId.clone()
        val idxBuffer = ByteBuffer.allocate(8).order(ByteOrder.BIG_ENDIAN).putLong(chunkIndex).array()
        for (i in 0 until 8) {
            nonce[4 + i] = (nonce[4 + i].toInt() xor idxBuffer[i].toInt()).toByte()
        }
        return nonce
    }

    fun constructAad(
        version: Byte,
        streamId: ByteArray,
        chunkIndex: Long,
        plaintextLen: Int,
        flags: Byte
    ): ByteArray {
        val bb = ByteBuffer.allocate(30).order(ByteOrder.BIG_ENDIAN)
        bb.put(MAGIC)
        bb.put(version)
        bb.put(streamId)
        bb.putLong(chunkIndex)
        bb.putInt(plaintextLen)
        bb.put(flags)
        return bb.array()
    }
}

/**
 * Output stream that encrypts data into the DDC1 chunked stream format.
 */
class Ddc1OutputStream(
    private val out: OutputStream,
    private val payloadKey: ByteArray,
    private val chunkSize: Int = Ddc1Constants.DEFAULT_CHUNK_SIZE,
    private val streamId: ByteArray = ByteArray(12).also { SecureRandom().nextBytes(it) }
) : OutputStream() {

    private val keySpec = SecretKeySpec(payloadKey, "AES")
    private val buffer = ByteArray(chunkSize)
    private var bufferPos = 0
    private var chunkIndex = 0L
    private var headerWritten = false
    private var closed = false

    private fun writeHeader() {
        if (headerWritten) return
        val hb = ByteBuffer.allocate(Ddc1Constants.HEADER_SIZE).order(ByteOrder.BIG_ENDIAN)
        hb.put(Ddc1Constants.MAGIC)
        hb.put(Ddc1Constants.FORMAT_VERSION)
        hb.put(0x00.toByte()) // flags
        hb.putInt(chunkSize)
        hb.putShort(0x0000.toShort()) // reserved
        hb.put(streamId)
        out.write(hb.array())
        headerWritten = true
    }

    private fun flushChunk(isFinal: Boolean) {
        writeHeader()

        val plaintextLen = bufferPos
        val flags = if (isFinal) Ddc1Constants.FLAG_FINAL else Ddc1Constants.FLAG_INTERMEDIATE

        val nonce = Ddc1Constants.constructNonce(streamId, chunkIndex)
        val aad = Ddc1Constants.constructAad(
            version = Ddc1Constants.FORMAT_VERSION,
            streamId = streamId,
            chunkIndex = chunkIndex,
            plaintextLen = plaintextLen,
            flags = flags
        )

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.ENCRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(aad)

        val ciphertextWithTag = cipher.doFinal(buffer, 0, plaintextLen)

        // Write frame header: plaintextLen (4B) + flags (1B)
        val fb = ByteBuffer.allocate(5).order(ByteOrder.BIG_ENDIAN)
        fb.putInt(plaintextLen)
        fb.put(flags)
        out.write(fb.array())

        // Write ciphertext + tag
        out.write(ciphertextWithTag)

        bufferPos = 0
        chunkIndex++
    }

    override fun write(b: Int) {
        val single = byteArrayOf(b.toByte())
        write(single, 0, 1)
    }

    override fun write(b: ByteArray, off: Int, len: Int) {
        if (closed) throw IOException("Stream is closed")
        var remaining = len
        var offset = off

        while (remaining > 0) {
            val space = chunkSize - bufferPos
            val toCopy = minOf(remaining, space)
            System.arraycopy(b, offset, buffer, bufferPos, toCopy)
            bufferPos += toCopy
            offset += toCopy
            remaining -= toCopy

            if (bufferPos == chunkSize) {
                flushChunk(isFinal = false)
            }
        }
    }

    override fun flush() {
        out.flush()
    }

    override fun close() {
        if (closed) return
        try {
            // Write final chunk (even if empty)
            flushChunk(isFinal = true)
            out.flush()
        } finally {
            closed = true
            out.close()
        }
    }
}

/**
 * Input stream that reads and decrypts a DDC1 chunked stream.
 */
class Ddc1InputStream(
    private val rawIn: InputStream,
    private val payloadKey: ByteArray
) : InputStream() {

    private val keySpec = SecretKeySpec(payloadKey, "AES")
    private var headerRead = false
    private var chunkSize = Ddc1Constants.DEFAULT_CHUNK_SIZE
    private var streamId = ByteArray(12)
    private var chunkIndex = 0L

    private var currentChunkBuffer: ByteArray? = null
    private var currentChunkPos = 0
    private var seenFinalChunk = false
    private var closed = false

    private fun readFully(b: ByteArray, off: Int, len: Int) {
        var n = 0
        while (n < len) {
            val count = rawIn.read(b, off + n, len - n)
            if (count < 0) throw EOFException("Unexpected EOF while reading DDC1 stream")
            n += count
        }
    }

    private fun readHeader() {
        if (headerRead) return

        val headerBytes = ByteArray(Ddc1Constants.HEADER_SIZE)
        try {
            readFully(headerBytes, 0, Ddc1Constants.HEADER_SIZE)
        } catch (e: EOFException) {
            throw IOException("File too short to contain a valid DDC1 header", e)
        }

        val hb = ByteBuffer.wrap(headerBytes).order(ByteOrder.BIG_ENDIAN)
        val magic = ByteArray(4)
        hb.get(magic)
        if (!magic.contentEquals(Ddc1Constants.MAGIC)) {
            throw IOException("Invalid DDC1 magic bytes: not a DockDrop encrypted stream")
        }

        val version = hb.get()
        if (version != Ddc1Constants.FORMAT_VERSION) {
            throw IOException("Unsupported DDC1 format version: $version")
        }

        val flags = hb.get()
        if (flags.toInt() != 0) {
            throw IOException("Invalid DDC1 header flags: $flags")
        }

        chunkSize = hb.getInt()
        if (chunkSize <= 0 || chunkSize > 32 * 1024 * 1024) {
            throw IOException("Invalid DDC1 chunk size: $chunkSize")
        }

        val reserved = hb.getShort()
        hb.get(streamId)

        headerRead = true
    }

    private fun loadNextChunk(): Boolean {
        readHeader()

        if (seenFinalChunk) {
            // Check for trailing bytes
            val nextByte = rawIn.read()
            if (nextByte != -1) {
                throw IOException("Corrupt DDC1 stream: trailing bytes after final chunk")
            }
            return false
        }

        val frameHeader = ByteArray(5)
        var readHeaderBytes = 0
        while (readHeaderBytes < 5) {
            val c = rawIn.read(frameHeader, readHeaderBytes, 5 - readHeaderBytes)
            if (c < 0) {
                if (readHeaderBytes == 0 && !seenFinalChunk) {
                    throw IOException("Truncated DDC1 stream: EOF reached without final chunk")
                }
                throw EOFException("Truncated chunk frame header in DDC1 stream")
            }
            readHeaderBytes += c
        }

        val fb = ByteBuffer.wrap(frameHeader).order(ByteOrder.BIG_ENDIAN)
        val plaintextLen = fb.getInt()
        val flags = fb.get()

        if (plaintextLen < 0 || plaintextLen > chunkSize) {
            throw IOException("Invalid plaintextLen in DDC1 chunk: $plaintextLen")
        }

        if (flags != Ddc1Constants.FLAG_INTERMEDIATE && flags != Ddc1Constants.FLAG_FINAL) {
            throw IOException("Invalid chunk flags in DDC1: $flags")
        }

        if (flags == Ddc1Constants.FLAG_INTERMEDIATE && plaintextLen == 0) {
            throw IOException("Invalid zero-length intermediate DDC1 chunk")
        }

        val ciphertextLen = plaintextLen + Ddc1Constants.GCM_TAG_BYTES
        val ciphertextWithTag = ByteArray(ciphertextLen)
        readFully(ciphertextWithTag, 0, ciphertextLen)

        val nonce = Ddc1Constants.constructNonce(streamId, chunkIndex)
        val aad = Ddc1Constants.constructAad(
            version = Ddc1Constants.FORMAT_VERSION,
            streamId = streamId,
            chunkIndex = chunkIndex,
            plaintextLen = plaintextLen,
            flags = flags
        )

        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        val gcmSpec = GCMParameterSpec(128, nonce)
        cipher.init(Cipher.DECRYPT_MODE, keySpec, gcmSpec)
        cipher.updateAAD(aad)

        val decrypted: ByteArray
        try {
            decrypted = cipher.doFinal(ciphertextWithTag)
        } catch (e: Exception) {
            throw IOException("AES-GCM authentication failed for chunk $chunkIndex: corrupt or tampered data", e)
        }

        currentChunkBuffer = decrypted
        currentChunkPos = 0
        chunkIndex++

        if (flags == Ddc1Constants.FLAG_FINAL) {
            seenFinalChunk = true
        }

        return true
    }

    override fun read(): Int {
        val single = ByteArray(1)
        val count = read(single, 0, 1)
        return if (count <= 0) -1 else (single[0].toInt() and 0xFF)
    }

    override fun read(b: ByteArray, off: Int, len: Int): Int {
        if (closed) throw IOException("Stream is closed")
        if (len == 0) return 0

        while (currentChunkBuffer == null || currentChunkPos >= currentChunkBuffer!!.size) {
            if (!loadNextChunk()) {
                return -1 // EOF
            }
            if (currentChunkBuffer!!.isEmpty() && seenFinalChunk) {
                return -1 // 0-length final chunk reached
            }
        }

        val available = currentChunkBuffer!!.size - currentChunkPos
        val toRead = minOf(len, available)
        System.arraycopy(currentChunkBuffer!!, currentChunkPos, b, off, toRead)
        currentChunkPos += toRead
        return toRead
    }

    override fun close() {
        closed = true
        rawIn.close()
    }
}
