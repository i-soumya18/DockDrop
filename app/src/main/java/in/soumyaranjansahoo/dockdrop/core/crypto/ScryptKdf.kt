package `in`.soumyaranjansahoo.dockdrop.core.crypto

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

/**
 * Pure Kotlin implementation of RFC 7914 scrypt.
 * Zero external native dependencies; operates in constant flat memory.
 */
object ScryptKdf {

    /**
     * Standard RFC 7914 scrypt KDF.
     *
     * @param password UTF-8 bytes of passphrase or normalized recovery key
     * @param salt Random salt bytes (e.g. 16 bytes)
     * @param N CPU/memory cost parameter (must be power of 2, e.g. 16384)
     * @param r Block size parameter (e.g. 8)
     * @param p Parallelization parameter (e.g. 1)
     * @param dkLen Length of derived key in bytes (e.g. 32)
     */
    fun scrypt(
        password: ByteArray,
        salt: ByteArray,
        N: Int = 16384,
        r: Int = 8,
        p: Int = 1,
        dkLen: Int = 32
    ): ByteArray {
        require(N > 1 && (N and (N - 1)) == 0) { "N must be a power of 2 greater than 1" }
        require(r >= 1) { "r must be >= 1" }
        require(p >= 1) { "p must be >= 1" }
        require(dkLen >= 1) { "dkLen must be >= 1" }

        val blockLen = 128 * r
        // Step 1: B = PBKDF2-HMAC-SHA256(password, salt, 1, p * 128 * r)
        val b = pbkdf2HmacSha256(password, salt, 1, p * blockLen)

        // Step 2: For each block i, B[i] = ROMix(B[i], N, r)
        val bInts = IntArray(p * blockLen / 4)
        ByteBuffer.wrap(b).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().get(bInts)

        val blockSizeInts = blockLen / 4
        val singleBlock = IntArray(blockSizeInts)

        for (i in 0 until p) {
            System.arraycopy(bInts, i * blockSizeInts, singleBlock, 0, blockSizeInts)
            roMix(singleBlock, N, r)
            System.arraycopy(singleBlock, 0, bInts, i * blockSizeInts, blockSizeInts)
        }

        val bOut = ByteArray(b.size)
        ByteBuffer.wrap(bOut).order(ByteOrder.LITTLE_ENDIAN).asIntBuffer().put(bInts)

        // Step 3: DK = PBKDF2-HMAC-SHA256(password, B, 1, dkLen)
        return pbkdf2HmacSha256(password, bOut, 1, dkLen)
    }

    private fun roMix(b: IntArray, n: Int, r: Int) {
        val blockLenInts = 32 * r
        val x = b.clone()
        // Memory cost: N * 128 * r bytes (e.g. 16384 * 1024 = 16 MB)
        val v = Array(n) { IntArray(blockLenInts) }

        for (i in 0 until n) {
            System.arraycopy(x, 0, v[i], 0, blockLenInts)
            blockMix(x, r)
        }

        val mask = n - 1
        for (i in 0 until n) {
            // integerify(X): first 4 bytes of block (2*r - 1) as unsigned int modulo N
            val offsetLastBlock = (2 * r - 1) * 16
            val j = x[offsetLastBlock] and mask
            val vj = v[j]
            for (k in 0 until blockLenInts) {
                x[k] = x[k] xor vj[k]
            }
            blockMix(x, r)
        }

        System.arraycopy(x, 0, b, 0, blockLenInts)
    }

    private fun blockMix(b: IntArray, r: Int) {
        val blockLenInts = 32 * r
        val x = IntArray(16)
        // X = B[2*r - 1]
        System.arraycopy(b, (2 * r - 1) * 16, x, 0, 16)

        val y = IntArray(blockLenInts)
        val t = IntArray(16)

        for (i in 0 until 2 * r) {
            val biOffset = i * 16
            for (k in 0 until 16) {
                t[k] = x[k] xor b[biOffset + k]
            }
            salsa208(t, x)

            // Y[i] alternate layout: even blocks first, then odd blocks
            val destOffset = if (i % 2 == 0) {
                (i / 2) * 16
            } else {
                (r + i / 2) * 16
            }
            System.arraycopy(x, 0, y, destOffset, 16)
        }

        System.arraycopy(y, 0, b, 0, blockLenInts)
    }

    /**
     * Salsa20/8 core permutation (8 rounds / 4 double-rounds).
     */
    private fun salsa208(input: IntArray, output: IntArray) {
        var x0 = input[0];  var x1 = input[1];  var x2 = input[2];  var x3 = input[3]
        var x4 = input[4];  var x5 = input[5];  var x6 = input[6];  var x7 = input[7]
        var x8 = input[8];  var x9 = input[9];  var x10 = input[10]; var x11 = input[11]
        var x12 = input[12]; var x13 = input[13]; var x14 = input[14]; var x15 = input[15]

        for (round in 0 until 4) {
            // Column round
            x4 = x4 xor Integer.rotateLeft(x0 + x12, 7)
            x8 = x8 xor Integer.rotateLeft(x4 + x0, 9)
            x12 = x12 xor Integer.rotateLeft(x8 + x4, 13)
            x0 = x0 xor Integer.rotateLeft(x12 + x8, 18)

            x9 = x9 xor Integer.rotateLeft(x5 + x1, 7)
            x13 = x13 xor Integer.rotateLeft(x9 + x5, 9)
            x1 = x1 xor Integer.rotateLeft(x13 + x9, 13)
            x5 = x5 xor Integer.rotateLeft(x1 + x13, 18)

            x14 = x14 xor Integer.rotateLeft(x10 + x6, 7)
            x2 = x2 xor Integer.rotateLeft(x14 + x10, 9)
            x6 = x6 xor Integer.rotateLeft(x2 + x14, 13)
            x10 = x10 xor Integer.rotateLeft(x6 + x2, 18)

            x3 = x3 xor Integer.rotateLeft(x15 + x11, 7)
            x7 = x7 xor Integer.rotateLeft(x3 + x15, 9)
            x11 = x11 xor Integer.rotateLeft(x7 + x3, 13)
            x15 = x15 xor Integer.rotateLeft(x11 + x7, 18)

            // Row round
            x1 = x1 xor Integer.rotateLeft(x0 + x3, 7)
            x2 = x2 xor Integer.rotateLeft(x1 + x0, 9)
            x3 = x3 xor Integer.rotateLeft(x2 + x1, 13)
            x0 = x0 xor Integer.rotateLeft(x3 + x2, 18)

            x6 = x6 xor Integer.rotateLeft(x5 + x4, 7)
            x7 = x7 xor Integer.rotateLeft(x6 + x5, 9)
            x4 = x4 xor Integer.rotateLeft(x7 + x6, 13)
            x5 = x5 xor Integer.rotateLeft(x4 + x7, 18)

            x11 = x11 xor Integer.rotateLeft(x10 + x9, 7)
            x8 = x8 xor Integer.rotateLeft(x11 + x10, 9)
            x9 = x9 xor Integer.rotateLeft(x8 + x11, 13)
            x10 = x10 xor Integer.rotateLeft(x9 + x8, 18)

            x12 = x12 xor Integer.rotateLeft(x15 + x14, 7)
            x13 = x13 xor Integer.rotateLeft(x12 + x15, 9)
            x14 = x14 xor Integer.rotateLeft(x13 + x12, 13)
            x15 = x15 xor Integer.rotateLeft(x14 + x13, 18)
        }

        output[0] = input[0] + x0
        output[1] = input[1] + x1
        output[2] = input[2] + x2
        output[3] = input[3] + x3
        output[4] = input[4] + x4
        output[5] = input[5] + x5
        output[6] = input[6] + x6
        output[7] = input[7] + x7
        output[8] = input[8] + x8
        output[9] = input[9] + x9
        output[10] = input[10] + x10
        output[11] = input[11] + x11
        output[12] = input[12] + x12
        output[13] = input[13] + x13
        output[14] = input[14] + x14
        output[15] = input[15] + x15
    }

    /**
     * RFC 2898 / RFC 7914 PBKDF2 using HMAC-SHA256.
     */
    fun pbkdf2HmacSha256(
        password: ByteArray,
        salt: ByteArray,
        iterations: Int,
        derivedKeyLength: Int
    ): ByteArray {
        val mac = Mac.getInstance("HmacSHA256")
        mac.init(SecretKeySpec(password, "HmacSHA256"))

        val hLen = mac.macLength // 32
        val l = (derivedKeyLength + hLen - 1) / hLen
        val r = derivedKeyLength - (l - 1) * hLen

        val out = ByteArray(derivedKeyLength)
        val blockBuffer = ByteBuffer.allocate(salt.size + 4)
        val u = ByteArray(hLen)
        val t = ByteArray(hLen)

        for (i in 1..l) {
            blockBuffer.clear()
            blockBuffer.put(salt)
            blockBuffer.putInt(i)

            mac.reset()
            val u1 = mac.doFinal(blockBuffer.array())
            System.arraycopy(u1, 0, u, 0, hLen)
            System.arraycopy(u1, 0, t, 0, hLen)

            for (c in 2..iterations) {
                mac.reset()
                mac.update(u)
                mac.doFinal(u, 0)
                for (k in 0 until hLen) {
                    t[k] = (t[k].toInt() xor u[k].toInt()).toByte()
                }
            }

            val copyLen = if (i == l) r else hLen
            System.arraycopy(t, 0, out, (i - 1) * hLen, copyLen)
        }

        return out
    }
}
