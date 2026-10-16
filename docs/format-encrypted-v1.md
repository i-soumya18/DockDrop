# DockDrop Encrypted Format Specification — v1 (`DDC1`)

> **Status:** Approved (Phase 4)
> **Format Version:** `1` (`DDC1`)
> **Owner:** A6 (Crypto Agent) · **Consumed by:** A3 (Engine), A4 (Manifest), A5 (Restore)

## 1. Design Goals & Threat Model

1. **Confidentiality & Integrity at Rest:** If an external SSD is lost or stolen, an attacker without the passphrase or recovery key cannot read file contents, original filenames, directory structures, or the manifest index.
2. **Streaming & Constant Memory:** Files of any size (including multi-gigabyte 4K videos) are encrypted and decrypted on the fly in `1 MB` (`1,048,576`-byte) chunks matching DockDrop's `ADR-002` streaming buffer, with flat RAM usage.
3. **Truncation, Reordering & Splice Resistance:** Each chunk is independently authenticated with AES-256-GCM using a stream-unique 96-bit base nonce XORed with the 64-bit chunk index, and Associated Authenticated Data (AAD) binding the stream ID, chunk index, chunk length, and final-chunk flag.
4. **Zero Lock-In & Self-Contained Restore:** All metadata required to unlock and restore an encrypted Set on a clean phone or third-party decryptor lives on the SSD under `.dockdrop/sets/<setId>/encryption.json`. Passphrases are never stored anywhere.

---

## 2. On-SSD Directory Layout (Encrypted Set)

Unencrypted Sets use `<Vault>/<SetName>/<SourceLabel>/...` per `CLAUDE.md §7`. Encrypted Sets isolate their payload under `<Vault>/<SetName>.enc/` with opaque HMAC filenames:

```text
<Vault>/
  .dockdrop/
    drive.json                         (plaintext DriveInfo)
    sets/<setId>/
      set.json                         {"setId":"...","setName":"My phone","sourceLabels":[],"updatedAtMs":...,"isEncrypted":true}
      encryption.json                  (key slots & KDF parameters; see §4)
      HEAD                             text: current generation number (e.g. "4\n")
      manifest.<gen>.ndjson            DDC1-encrypted NDJSON manifest stream
      manifest.<gen>.sha256            SHA-256 hex digest of the decrypted plaintext manifest.<gen>.ndjson
  <SetName>.enc/
    blobs/
      <xx>/                            first 2 hex chars of <blobId> (256-way fan-out for exFAT speed)
        <blobId>.enc                   DDC1-encrypted file stream
  _deleted/
    <SetName>.enc/
      <yyyy-MM-dd>/
        <xx>/
          <blobId>.enc                 DDC1-encrypted kept-deleted file stream
```

---

## 3. Key Hierarchy & Subkey Derivation

### 3.1 Master Key (`MK`)
At Set creation, a 256-bit (32-byte) **Set Master Key (`MK`)** is generated from `java.security.SecureRandom`.

### 3.2 Subkey Derivation (`HKDF-SHA256`, RFC 5869)
From `MK` (32 bytes) and `salt = setId.encodeToByteArray(UTF_8)`, two independent 256-bit subkeys are derived via `HKDF-SHA256`:

1. **Payload Encryption Key (`K_payload`, 32 bytes):**
   ```text
   PRK       = HMAC-SHA256(key = setId_utf8, message = MK)
   K_payload = HMAC-SHA256(key = PRK, message = "DockDrop-v1-PayloadKey" || 0x01)
   ```
   Used as the AES-256-GCM key for all `DDC1` file streams and `manifest.<gen>.ndjson` streams in the Set.

2. **Filename Obfuscation Key (`K_filename`, 32 bytes):**
   ```text
   K_filename = HMAC-SHA256(key = PRK, message = "DockDrop-v1-FilenameKey" || 0x01)
   ```
   Used to compute deterministic opaque blob identifiers (`blobId`) from logical Set-relative paths (`<SourceLabel>/<relativePath>`):
   ```text
   blobId = LowercaseHex(HMAC-SHA256(key = K_filename, message = logicalPath.encodeToByteArray(UTF_8)))
   ```
   - `blobId` is 64 lowercase hexadecimal characters (256 bits).
   - Active Vault path: `<SetName>.enc/blobs/${blobId.substring(0, 2)}/${blobId}.enc`
   - Deleted Vault path: `_deleted/<SetName>.enc/${deletedDate}/${blobId.substring(0, 2)}/${blobId}.enc`

---

## 4. Key Derivation (`scrypt`) & Key Wrapping (`encryption.json`)

### 4.1 KDF Parameters (RFC 7914 `scrypt`)
Both the user Passphrase and the Recovery Key derive a 256-bit Key Encryption Key (`KEK`) using RFC 7914 `scrypt`:
- **`N` (CPU/Memory cost):** `16384` ($2^{14}$, requires `16 MB` working memory: $128 \times N \times r$)
- **`r` (Block size):** `8`
- **`p` (Parallelization):** `1`
- **`dkLen` (Derived key length):** `32` bytes (256 bits)
- **Salt:** `16` random bytes (`SecureRandom`), distinct for the Passphrase slot and Recovery Key slot.

### 4.2 Passphrase Normalization
- Converted to UTF-8 bytes after Unicode NFC normalization (`Normalizer.normalize(passphrase, Normalizer.Form.NFC)`).
- Minimum length enforced at creation (`S18`): 8 characters.

### 4.3 Recovery Key Generation & Normalization
- Generated from `20` random bytes (`160 bits`) via `SecureRandom`.
- Encoded with **Crockford Base32** alphabet (`0123456789ABCDEFGHJKMNPQRSTVWXYZ`, 5 bits per character = 32 characters).
- Displayed to the user in **8 groups of 4 characters** separated by hyphens:
  ```text
  7QKM-2D9F-8V3P-4NXR-9T6W-1C5H-J8YB-G24K
  ```
- **Input Normalization before KDF:** Uppercase, strip all hyphens (`-`) and whitespace, map confusable characters (`O`→`0`, `I`→`1`, `L`→`1`), and encode the resulting 32-character ASCII string as UTF-8 bytes into `scrypt`.

### 4.4 Key Slot Wrapping (`AES-256-GCM`)
Each slot wraps the 32-byte `MK` using `AES/GCM/NoPadding` (128-bit / 16-byte tag):
- **Key:** `KEK_pass` or `KEK_recovery` (32 bytes from `scrypt`)
- **Nonce:** `12` random bytes from `SecureRandom`
- **AAD (Associated Authenticated Data):**
  - Passphrase slot: `"DockDrop-v1-KeyWrap-Passphrase|$setId"` (UTF-8 bytes)
  - Recovery slot: `"DockDrop-v1-KeyWrap-Recovery|$setId"` (UTF-8 bytes)
- **Output (`wrappedKey`):** `48` bytes (`32` bytes ciphertext `||` `16` bytes GCM authentication tag).

### 4.5 Fail-Fast Verification Rule
When unlocking a Set with a passphrase or recovery key:
1. Derive `KEK` via `scrypt`.
2. Attempt `AES-256-GCM` decryption of `wrappedKey` with the slot's `nonce` and `AAD`.
3. If the GCM authentication tag fails (`AEADBadTagException`), immediately abort with `InvalidKeyException` **before** opening or writing any file or manifest. Zero partial writes occur.

### 4.6 `.dockdrop/sets/<setId>/encryption.json` Schema
All binary fields in `encryption.json` are lowercase hexadecimal strings:
```json
{
  "formatVersion": 1,
  "cipher": "AES-256-GCM-DDC1",
  "kdf": "scrypt",
  "scryptN": 16384,
  "scryptR": 8,
  "scryptP": 1,
  "passphraseSlot": {
    "saltHex": "a1b2c3d4e5f60718293a4b5c6d7e8f90",
    "nonceHex": "0102030405060708090a0b0c",
    "wrappedKeyHex": "<96 hex chars = 48 bytes>"
  },
  "recoverySlot": {
    "saltHex": "f0e1d2c3b4a5968778695a4b3c2d1e0f",
    "nonceHex": "0c0b0a090807060504030201",
    "wrappedKeyHex": "<96 hex chars = 48 bytes>"
  }
}
```

### 4.7 Android Keystore Convenience Unlock (Originating Device Only)
On the device where the Set is created or unlocked, DockDrop generates an Android Keystore hardware-backed AES-256-GCM key (`alias = "dockdrop_mk_wrap_$setId"`) and stores the Keystore-wrapped `MK` (`iv || ciphertext || tag`) in the local Room `sets` table (`keystoreWrappedMk`).
- During normal one-tap Drops on the originating phone, `MK` is unwrapped silently via Android Keystore.
- On a clean install or new phone, `keystoreWrappedMk` is absent; the user unlocks once via `S19` (Passphrase or Recovery Key), which unwraps `MK` from `encryption.json` and optionally re-seals it into the new phone's Android Keystore.

---

## 5. `DDC1` Chunked Stream Format (Files & Manifests)

Every encrypted file (`<blobId>.enc`) and encrypted manifest (`manifest.<gen>.ndjson`) is a `DDC1` stream encrypted with `K_payload`.

### 5.1 Stream Header (24 bytes)
| Byte Offset | Length | Type | Value / Description |
|---|---|---|---|
| `0..3` | 4 B | ASCII | Magic bytes: `"DDC1"` (`0x44, 0x44, 0x43, 0x31`) |
| `4` | 1 B | `UInt8` | Format version: `0x01` |
| `5` | 1 B | `UInt8` | Stream flags: `0x00` (reserved, must be `0x00`) |
| `6..9` | 4 B | `UInt32 BE` | Max plaintext bytes per chunk (`0x00100000` = `1,048,576` = `1 MB`) |
| `10..11` | 2 B | `UInt16 BE` | Reserved (`0x0000`) |
| `12..23` | 12 B | Bytes | Random 96-bit `streamId` generated via `SecureRandom` per stream write |

### 5.2 Chunk Framing (Sequential chunks `chunkIndex = 0, 1, 2, ...`)
Following the 24-byte header is a sequence of 1 or more authenticated chunks:

| Field | Length | Type | Description |
|---|---|---|---|
| `plaintextLen` | 4 B | `UInt32 BE` | Number of plaintext bytes in this chunk (`0 <= plaintextLen <= chunkSize`) |
| `chunkFlags` | 1 B | `UInt8` | `0x00` = non-final chunk (`plaintextLen > 0`); `0x01` = final chunk (`FLAG_FINAL`) |
| `ciphertextAndTag` | `plaintextLen + 16` B | Bytes | `AES-256-GCM` ciphertext (`plaintextLen` bytes) + 128-bit GCM tag (`16` bytes) |

### 5.3 Per-Chunk Nonce Construction (12 bytes)
For chunk index `i` (`UInt64`, 0-indexed):
- `nonce[0..3] = streamId[0..3]`
- `nonce[4..11] = streamId[4..11] XOR BigEndianUInt64(i)`

Because `streamId` is freshly generated (`96 bits`) for every stream write and `i` strictly increments per chunk, `(K_payload, nonce)` is never reused.

### 5.4 Per-Chunk AAD Construction (30 bytes)
Each chunk's GCM authentication tag covers `30` bytes of AAD:
```text
AAD = "DDC1" (4B) || version (1B) || streamId (12B) || BigEndianUInt64(chunkIndex) (8B) || BigEndianUInt32(plaintextLen) (4B) || chunkFlags (1B)
```

### 5.5 Empty File & Truncation Rules
1. **Empty stream (`0` plaintext bytes):** Consists of the 24-byte header followed by a single final chunk with `plaintextLen = 0`, `chunkFlags = 0x01`, and `16`-byte GCM tag (`24 + 5 + 16 = 45 bytes` total).
2. **Final-chunk enforcement:** Decryption MUST verify that the stream ends immediately after a chunk with `chunkFlags == 0x01`.
   - If EOF is reached before seeing a chunk with `chunkFlags == 0x01`, the decryptor MUST throw `IOException("Truncated DDC1 stream: missing final chunk")`.
   - If extra bytes exist after the `chunkFlags == 0x01` chunk, the decryptor MUST throw `IOException("Corrupt DDC1 stream: trailing bytes after final chunk")`.
   - Any non-final chunk (`chunkFlags == 0x00`) with `plaintextLen == 0` is invalid and MUST be rejected.

---

## 6. Reference Decryptor Pseudocode (Third-Party Interop)

```python
def decrypt_ddc1_stream(enc_bytes: bytes, k_payload: bytes) -> bytes:
    assert len(enc_bytes) >= 45 and enc_bytes[0:4] == b"DDC1"
    version = enc_bytes[4]
    assert version == 1 and enc_bytes[5] == 0
    chunk_size = int.from_bytes(enc_bytes[6:10], "big")
    stream_id = enc_bytes[12:24]

    offset = 24
    chunk_index = 0
    out = bytearray()
    seen_final = False

    while offset < len(enc_bytes):
        assert not seen_final, "Trailing bytes after final chunk"
        pt_len = int.from_bytes(enc_bytes[offset:offset+4], "big")
        flags = enc_bytes[offset+4]
        assert pt_len <= chunk_size
        assert flags in (0, 1)
        if flags == 0:
            assert pt_len > 0

        ct_and_tag = enc_bytes[offset+5 : offset+5+pt_len+16]
        assert len(ct_and_tag) == pt_len + 16

        nonce = bytearray(stream_id)
        idx_bytes = chunk_index.to_bytes(8, "big")
        for b in range(8):
            nonce[4 + b] ^= idx_bytes[b]

        aad = (
            b"DDC1"
            + bytes([version])
            + stream_id
            + idx_bytes
            + pt_len.to_bytes(4, "big")
            + bytes([flags])
        )
        pt = aes_256_gcm_decrypt(key=k_payload, nonce=bytes(nonce), aad=aad, data=ct_and_tag)
        out.extend(pt)

        if flags == 1:
            seen_final = True
        chunk_index += 1
        offset += 5 + pt_len + 16

    assert seen_final, "Missing final chunk"
    return bytes(out)
```
