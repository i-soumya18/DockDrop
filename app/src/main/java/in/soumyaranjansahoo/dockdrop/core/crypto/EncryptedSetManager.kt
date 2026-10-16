package `in`.soumyaranjansahoo.dockdrop.core.crypto

import android.net.Uri
import `in`.soumyaranjansahoo.dockdrop.core.storage.StorageGateway
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import kotlinx.serialization.Serializable
import kotlinx.serialization.encodeToString
import kotlinx.serialization.json.Json
import java.nio.charset.StandardCharsets
import java.security.KeyStore
import java.security.SecureRandom
import java.text.Normalizer
import java.util.concurrent.ConcurrentHashMap
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec
import javax.crypto.spec.SecretKeySpec

@Serializable
data class EncryptionSlot(
    val saltHex: String,
    val nonceHex: String,
    val wrappedKeyHex: String
)

@Serializable
data class EncryptionMetadata(
    val formatVersion: Int = 1,
    val cipher: String = "AES-256-GCM-DDC1",
    val kdf: String = "scrypt",
    val scryptN: Int = 16384,
    val scryptR: Int = 8,
    val scryptP: Int = 1,
    val passphraseSlot: EncryptionSlot,
    val recoverySlot: EncryptionSlot
)

data class ProvisionedEncryption(
    val metadata: EncryptionMetadata,
    val recoveryKeyFormatted: String,
    val masterKey: ByteArray,
    val derivedKeys: DerivedSetKeys,
    val keystoreWrappedMkHex: String? = null
)

class SetLockedException(val setId: String) :
    IllegalStateException("Backup set '$setId' is encrypted and must be unlocked before proceeding.")

/**
 * Manages encryption metadata, key slot derivation, wrapping, and active codecs.
 */
class EncryptedSetManager(
    private val storageGateway: StorageGateway,
    private val json: Json = Json { ignoreUnknownKeys = true; encodeDefaults = true }
) {
    // In-memory unlocked keys cache
    private val unlockedKeysCache = ConcurrentHashMap<String, DerivedSetKeys>()

    fun isSetUnlocked(setId: String): Boolean = unlockedKeysCache.containsKey(setId)

    fun cacheUnlockedKeys(setId: String, keys: DerivedSetKeys) {
        unlockedKeysCache[setId] = keys
    }

    fun clearUnlockedKeys(setId: String) {
        unlockedKeysCache.remove(setId)
    }

    fun clearAllUnlockedKeys() {
        unlockedKeysCache.clear()
    }

    /**
     * Provisions new encryption credentials at Set creation (S18).
     */
    fun provisionNewSetEncryption(
        setId: String,
        passphrase: String,
        random: SecureRandom = SecureRandom()
    ): ProvisionedEncryption {
        require(passphrase.length >= 8) { "Passphrase must be at least 8 characters" }

        // 1. Generate 256-bit Master Key
        val masterKey = ByteArray(32)
        random.nextBytes(masterKey)

        val derivedKeys = DerivedSetKeys.fromMasterKey(masterKey, setId)

        // 2. Wrap via Passphrase
        val normalizedPassphrase = Normalizer.normalize(passphrase, Normalizer.Form.NFC)
        val passSalt = ByteArray(16).also { random.nextBytes(it) }
        val kekPass = ScryptKdf.scrypt(
            password = normalizedPassphrase.toByteArray(StandardCharsets.UTF_8),
            salt = passSalt,
            N = 16384,
            r = 8,
            p = 1,
            dkLen = 32
        )
        val passNonce = ByteArray(12).also { random.nextBytes(it) }
        val passAad = "DockDrop-v1-KeyWrap-Passphrase|$setId".toByteArray(StandardCharsets.UTF_8)
        val wrappedPassMk = wrapKeyAesGcm(masterKey, kekPass, passNonce, passAad)

        // 3. Wrap via Recovery Key
        val recoveryKeyFormatted = RecoveryKeyCodec.generate(random)
        val normalizedRecovery = RecoveryKeyCodec.normalize(recoveryKeyFormatted)
        val recoverySalt = ByteArray(16).also { random.nextBytes(it) }
        val kekRecovery = ScryptKdf.scrypt(
            password = normalizedRecovery.toByteArray(StandardCharsets.UTF_8),
            salt = recoverySalt,
            N = 16384,
            r = 8,
            p = 1,
            dkLen = 32
        )
        val recoveryNonce = ByteArray(12).also { random.nextBytes(it) }
        val recoveryAad = "DockDrop-v1-KeyWrap-Recovery|$setId".toByteArray(StandardCharsets.UTF_8)
        val wrappedRecoveryMk = wrapKeyAesGcm(masterKey, kekRecovery, recoveryNonce, recoveryAad)

        val metadata = EncryptionMetadata(
            passphraseSlot = EncryptionSlot(
                saltHex = passSalt.toHex(),
                nonceHex = passNonce.toHex(),
                wrappedKeyHex = wrappedPassMk.toHex()
            ),
            recoverySlot = EncryptionSlot(
                saltHex = recoverySalt.toHex(),
                nonceHex = recoveryNonce.toHex(),
                wrappedKeyHex = wrappedRecoveryMk.toHex()
            )
        )

        // 4. Android Keystore convenience wrap (if available)
        val keystoreWrapped = tryWrapWithAndroidKeystore(setId, masterKey)

        // Cache in memory for immediate use in current session
        cacheUnlockedKeys(setId, derivedKeys)

        return ProvisionedEncryption(
            metadata = metadata,
            recoveryKeyFormatted = recoveryKeyFormatted,
            masterKey = masterKey,
            derivedKeys = derivedKeys,
            keystoreWrappedMkHex = keystoreWrapped
        )
    }

    suspend fun writeEncryptionMetadata(
        vaultTreeUri: Uri,
        setId: String,
        metadata: EncryptionMetadata
    ): Boolean = withContext(Dispatchers.IO) {
        val relPath = ".dockdrop/sets/$setId/encryption.json"
        val text = json.encodeToString(metadata)
        storageGateway.writeTextAtomic(vaultTreeUri, relPath, text)
    }

    suspend fun readEncryptionMetadata(
        vaultTreeUri: Uri,
        setId: String
    ): EncryptionMetadata? = withContext(Dispatchers.IO) {
        val relPath = ".dockdrop/sets/$setId/encryption.json"
        val text = storageGateway.readText(vaultTreeUri, relPath) ?: return@withContext null
        try {
            json.decodeFromString<EncryptionMetadata>(text)
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Unlocks an encrypted Set using the user passphrase.
     * Returns true if successful, false if the passphrase is wrong or decryption fails.
     */
    suspend fun unlockWithPassphrase(
        vaultTreeUri: Uri,
        setId: String,
        passphrase: String
    ): Boolean = withContext(Dispatchers.Default) {
        val metadata = readEncryptionMetadata(vaultTreeUri, setId) ?: return@withContext false
        val slot = metadata.passphraseSlot

        val salt = hexToBytes(slot.saltHex)
        val nonce = hexToBytes(slot.nonceHex)
        val wrappedKey = hexToBytes(slot.wrappedKeyHex)
        val aad = "DockDrop-v1-KeyWrap-Passphrase|$setId".toByteArray(StandardCharsets.UTF_8)

        val normalized = Normalizer.normalize(passphrase, Normalizer.Form.NFC)
        val kek = ScryptKdf.scrypt(
            password = normalized.toByteArray(StandardCharsets.UTF_8),
            salt = salt,
            N = metadata.scryptN,
            r = metadata.scryptR,
            p = metadata.scryptP,
            dkLen = 32
        )

        val masterKey = unwrapKeyAesGcm(wrappedKey, kek, nonce, aad) ?: return@withContext false
        val derivedKeys = DerivedSetKeys.fromMasterKey(masterKey, setId)
        cacheUnlockedKeys(setId, derivedKeys)
        true
    }

    /**
     * Unlocks an encrypted Set using the recovery key.
     * Returns true if successful, false if the recovery key is wrong or decryption fails.
     */
    suspend fun unlockWithRecoveryKey(
        vaultTreeUri: Uri,
        setId: String,
        recoveryKeyInput: String
    ): Boolean = withContext(Dispatchers.Default) {
        val metadata = readEncryptionMetadata(vaultTreeUri, setId) ?: return@withContext false
        val slot = metadata.recoverySlot

        val salt = hexToBytes(slot.saltHex)
        val nonce = hexToBytes(slot.nonceHex)
        val wrappedKey = hexToBytes(slot.wrappedKeyHex)
        val aad = "DockDrop-v1-KeyWrap-Recovery|$setId".toByteArray(StandardCharsets.UTF_8)

        val normalized = RecoveryKeyCodec.normalize(recoveryKeyInput)
        if (normalized.length != 32) return@withContext false

        val kek = ScryptKdf.scrypt(
            password = normalized.toByteArray(StandardCharsets.UTF_8),
            salt = salt,
            N = metadata.scryptN,
            r = metadata.scryptR,
            p = metadata.scryptP,
            dkLen = 32
        )

        val masterKey = unwrapKeyAesGcm(wrappedKey, kek, nonce, aad) ?: return@withContext false
        val derivedKeys = DerivedSetKeys.fromMasterKey(masterKey, setId)
        cacheUnlockedKeys(setId, derivedKeys)
        true
    }

    /**
     * Resolves the SetStreamCodec for a Set.
     * Complies with Handoff Rule 5: Engine and Restore consume this codec without
     * branching on 'isEncrypted'.
     */
    fun resolveCodecForSet(
        vaultTreeUri: Uri?,
        setId: String,
        isEncrypted: Boolean,
        keystoreWrappedMkHex: String? = null
    ): SetStreamCodec {
        if (!isEncrypted) {
            return PlainSetStreamCodec
        }

        // 1. Check in-memory unlocked keys
        val cached = unlockedKeysCache[setId]
        if (cached != null) {
            return EncryptedSetStreamCodec(cached)
        }

        // 2. Try Android Keystore unwrapping if available
        if (keystoreWrappedMkHex != null) {
            val unwrappedMk = tryUnwrapWithAndroidKeystore(setId, keystoreWrappedMkHex)
            if (unwrappedMk != null) {
                val derived = DerivedSetKeys.fromMasterKey(unwrappedMk, setId)
                cacheUnlockedKeys(setId, derived)
                return EncryptedSetStreamCodec(derived)
            }
        }

        throw SetLockedException(setId)
    }

    private fun wrapKeyAesGcm(
        masterKey: ByteArray,
        kek: ByteArray,
        nonce: ByteArray,
        aad: ByteArray
    ): ByteArray {
        val cipher = Cipher.getInstance("AES/GCM/NoPadding")
        cipher.init(Cipher.ENCRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(128, nonce))
        cipher.updateAAD(aad)
        return cipher.doFinal(masterKey)
    }

    private fun unwrapKeyAesGcm(
        wrappedKey: ByteArray,
        kek: ByteArray,
        nonce: ByteArray,
        aad: ByteArray
    ): ByteArray? {
        return try {
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, SecretKeySpec(kek, "AES"), GCMParameterSpec(128, nonce))
            cipher.updateAAD(aad)
            cipher.doFinal(wrappedKey)
        } catch (e: Exception) {
            null
        }
    }

    private fun tryWrapWithAndroidKeystore(setId: String, masterKey: ByteArray): String? {
        return try {
            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val alias = "dockdrop_mk_wrap_$setId"

            if (!ks.containsAlias(alias)) {
                val kgen = KeyGenerator.getInstance("AES", "AndroidKeyStore")
                val specClass = Class.forName("android.security.keystore.KeyGenParameterSpec\$Builder")
                val builder = specClass.getConstructor(String::class.java, Int::class.java)
                    .newInstance(alias, 1 or 2) // PURPOSE_ENCRYPT | PURPOSE_DECRYPT

                val setBlockModes = specClass.getMethod("setBlockModes", Array<String>::class.java)
                setBlockModes.invoke(builder, arrayOf("GCM"))

                val setEncryptionPaddings = specClass.getMethod("setEncryptionPaddings", Array<String>::class.java)
                setEncryptionPaddings.invoke(builder, arrayOf("NoPadding"))

                val setKeySize = specClass.getMethod("setKeySize", Int::class.java)
                setKeySize.invoke(builder, 256)

                val buildMethod = specClass.getMethod("build")
                val spec = buildMethod.invoke(builder)

                val initMethod = KeyGenerator::class.java.getMethod("init", java.security.spec.AlgorithmParameterSpec::class.java)
                initMethod.invoke(kgen, spec)
                kgen.generateKey()
            }

            val secretKey = ks.getKey(alias, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.ENCRYPT_MODE, secretKey)
            val iv = cipher.iv
            val ct = cipher.doFinal(masterKey)

            // Pack: iv (12 bytes) + ct (48 bytes: 32 ct + 16 tag)
            val combined = ByteArray(iv.size + ct.size)
            System.arraycopy(iv, 0, combined, 0, iv.size)
            System.arraycopy(ct, 0, combined, iv.size, ct.size)
            combined.toHex()
        } catch (e: Throwable) {
            // AndroidKeyStore unavailable in pure JVM unit tests or if keystore error
            null
        }
    }

    private fun tryUnwrapWithAndroidKeystore(setId: String, wrappedHex: String): ByteArray? {
        return try {
            val bytes = hexToBytes(wrappedHex)
            if (bytes.size < 12 + 16) return null
            val iv = bytes.copyOfRange(0, 12)
            val ct = bytes.copyOfRange(12, bytes.size)

            val ks = KeyStore.getInstance("AndroidKeyStore").apply { load(null) }
            val alias = "dockdrop_mk_wrap_$setId"
            if (!ks.containsAlias(alias)) return null

            val secretKey = ks.getKey(alias, null) as SecretKey
            val cipher = Cipher.getInstance("AES/GCM/NoPadding")
            cipher.init(Cipher.DECRYPT_MODE, secretKey, GCMParameterSpec(128, iv))
            cipher.doFinal(ct)
        } catch (e: Throwable) {
            null
        }
    }

    private fun ByteArray.toHex(): String = joinToString("") { "%02x".format(it) }

    private fun hexToBytes(hex: String): ByteArray {
        val len = hex.length
        val data = ByteArray(len / 2)
        var i = 0
        while (i < len) {
            data[i / 2] = ((Character.digit(hex[i], 16) shl 4) + Character.digit(hex[i + 1], 16)).toByte()
            i += 2
        }
        return data
    }
}
