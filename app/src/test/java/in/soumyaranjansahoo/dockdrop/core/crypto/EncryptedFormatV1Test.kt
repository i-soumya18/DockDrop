package `in`.soumyaranjansahoo.dockdrop.core.crypto

import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNotEquals
import org.junit.Assert.assertTrue
import org.junit.Assert.fail
import org.junit.Test
import java.io.ByteArrayInputStream
import java.io.ByteArrayOutputStream
import java.io.IOException
import java.security.GeneralSecurityException
import java.util.Random
import javax.crypto.AEADBadTagException

class EncryptedFormatV1Test {

    @Test
    fun testScrypt_rfc7914_vector2() {
        // RFC 7914 test vector 2:
        // P = "password", S = "NaCl", N = 1024, r = 8, p = 16, dkLen = 64
        val password = "password".toByteArray(Charsets.UTF_8)
        val salt = "NaCl".toByteArray(Charsets.UTF_8)
        val N = 1024
        val r = 8
        val p = 16
        val dkLen = 64

        val derived = ScryptKdf.scrypt(password, salt, N, r, p, dkLen)
        val expectedHex = "fdbabe1c9d3472007856e7190d01e9fe7c6ad7cbc8237830e77376634b3731622eaf30d92e22a3886ff109279d9830dac727afb94a83ee6d8360cbdfa2cc0640"
        val derivedHex = derived.joinToString("") { "%02x".format(it) }

        assertEquals(expectedHex, derivedHex)
    }

    @Test
    fun testHkdfAndHmac_derivesDistinctSubkeysAndDeterministicBlobId() {
        val masterKey = ByteArray(32) { (it + 1).toByte() }
        val keys = DerivedSetKeys.fromMasterKey(masterKey, "test-set-id")

        assertEquals(32, keys.payloadKey.size)
        assertEquals(32, keys.filenameKey.size)
        assertFalse(keys.payloadKey.contentEquals(keys.filenameKey))

        // Same path produces identical blob ID
        val blobId1 = keys.computeBlobId("DCIM/Camera/IMG_0001.jpg")
        val blobId2 = keys.computeBlobId("DCIM/Camera/IMG_0001.jpg")
        assertEquals(blobId1, blobId2)
        assertEquals(64, blobId1.length) // SHA-256 hex length

        // Different path produces different blob ID
        val blobId3 = keys.computeBlobId("DCIM/Camera/IMG_0002.jpg")
        assertNotEquals(blobId1, blobId3)
    }

    @Test
    fun testRecoveryKeyCodec_generationNormalizationAndVerification() {
        val recoveryKey = RecoveryKeyCodec.generate()
        // 8 groups of 4 separated by hyphens = 39 characters
        assertEquals(39, recoveryKey.length)
        assertTrue(recoveryKey.matches(Regex("^[0-9A-Z]{4}(-[0-9A-Z]{4}){7}$")))

        // Test normalization of confusables: 'O' -> '0', 'I' -> '1', 'L' -> '1', lower to upper
        val messy = "abcd-efgh-ijkl-mnop-qrst-uvwx-yzao-01IL"
        val normalized = RecoveryKeyCodec.normalize(messy)
        assertEquals("ABCDEFGH1JK1MN0PQRSTUVWXYZA00111", normalized)

        // Test last 4 chars verification
        val rawKey = "ABCD-EFGH-IJKL-MNOP-QRST-UVWX-YZ12-3456"
        assertTrue(RecoveryKeyCodec.verifyLastFourChars(rawKey, "3456"))
        assertFalse(RecoveryKeyCodec.verifyLastFourChars(rawKey, "9999"))

        // Scrypt derivation from recovery key
        val salt = ByteArray(16) { 0x11 }
        val derived1 = ScryptKdf.scrypt(RecoveryKeyCodec.normalize(rawKey).toByteArray(), salt, 16, 1, 1, 32)
        val derived2 = ScryptKdf.scrypt(RecoveryKeyCodec.normalize("abcd-efgh-ijkl-mnop-qrst-uvwx-yz12-3456").toByteArray(), salt, 16, 1, 1, 32)
        assertArrayEquals(derived1, derived2)
    }

    @Test
    fun testDdc1Stream_roundTripSmallPayload() {
        val payloadKey = ByteArray(32) { 0x42 }
        val originalData = "Hello, DockDrop encrypted world! 🔒".toByteArray(Charsets.UTF_8)

        val out = ByteArrayOutputStream()
        Ddc1OutputStream(out, payloadKey).use { stream ->
            stream.write(originalData)
        }

        val ciphertext = out.toByteArray()
        assertTrue(ciphertext.size > 24) // Must have at least 24-byte header

        val inStream = ByteArrayInputStream(ciphertext)
        val readBack = ByteArrayOutputStream()
        Ddc1InputStream(inStream, payloadKey).use { stream ->
            stream.copyTo(readBack)
        }

        assertArrayEquals(originalData, readBack.toByteArray())
    }

    @Test
    fun testDdc1Stream_multiChunkStream() {
        val payloadKey = ByteArray(32) { (it * 7).toByte() }
        // Create 2.2 MB data (across 3 chunks: chunk 0 (1MB), chunk 1 (1MB), chunk 2 final (200KB))
        val originalData = ByteArray(2 * 1024 * 1024 + 200 * 1024)
        Random(42).nextBytes(originalData)

        val out = ByteArrayOutputStream()
        Ddc1OutputStream(out, payloadKey).use { stream ->
            stream.write(originalData)
        }

        val ciphertext = out.toByteArray()
        val inStream = ByteArrayInputStream(ciphertext)
        val readBack = ByteArrayOutputStream()
        Ddc1InputStream(inStream, payloadKey).use { stream ->
            stream.copyTo(readBack)
        }

        assertArrayEquals(originalData, readBack.toByteArray())
    }

    @Test
    fun testDdc1Stream_detectsTampering() {
        val payloadKey = ByteArray(32) { 0x01 }
        val originalData = "Sensitive backup data".toByteArray()

        val out = ByteArrayOutputStream()
        Ddc1OutputStream(out, payloadKey).use { it.write(originalData) }

        val ciphertext = out.toByteArray()
        // Tamper with one byte in the ciphertext body
        ciphertext[35] = (ciphertext[35].toInt() xor 0xFF).toByte()

        try {
            val inStream = ByteArrayInputStream(ciphertext)
            Ddc1InputStream(inStream, payloadKey).use {
                it.readBytes()
            }
            fail("Expected AEADBadTagException or GeneralSecurityException when ciphertext is tampered")
        } catch (e: Exception) {
            assertTrue("Expected AEAD or security failure on tamper: ${e.message}",
                e is GeneralSecurityException || e.cause is GeneralSecurityException)
        }
    }

    @Test
    fun testDdc1Stream_detectsTruncation() {
        val payloadKey = ByteArray(32) { 0x01 }
        val originalData = ByteArray(1024 * 1024 + 100) // 2 chunks
        Random(123).nextBytes(originalData)

        val out = ByteArrayOutputStream()
        Ddc1OutputStream(out, payloadKey).use { it.write(originalData) }

        val ciphertext = out.toByteArray()
        // Truncate before chunk 1 starts (cut off after chunk 0)
        // Header (24) + Chunk 0 (4 len + 1048576 ciphertext + 16 tag = 1048596) = 1048620 bytes
        val truncatedCiphertext = ciphertext.copyOf(1048620)

        try {
            val inStream = ByteArrayInputStream(truncatedCiphertext)
            Ddc1InputStream(inStream, payloadKey).use {
                it.readBytes()
            }
            fail("Expected IOException on truncated stream without final chunk")
        } catch (e: IOException) {
            assertTrue(e.message?.contains("final chunk", ignoreCase = true) == true || e is java.io.EOFException)
        }
    }
}
