package io.github.origincoding.totp.core

import java.nio.ByteBuffer
import java.nio.ByteOrder
import javax.crypto.Mac
import javax.crypto.spec.SecretKeySpec

object HotpGenerator {
    fun generate(
        secret: ByteArray,
        counter: Long,
        algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
        digits: OtpDigits = OtpDigits.SIX,
    ): String {
        require(secret.isNotEmpty()) { "HOTP secret must not be empty" }
        require(counter >= 0) { "HOTP counter must not be negative" }

        val counterBytes = ByteBuffer
            .allocate(Long.SIZE_BYTES)
            .order(ByteOrder.BIG_ENDIAN)
            .putLong(counter)
            .array()
        val mac = Mac.getInstance(algorithm.jcaName)
        mac.init(SecretKeySpec(secret, algorithm.jcaName))
        val hmac = mac.doFinal(counterBytes)
        val offset = hmac.last().toInt() and 0x0f

        check(offset + 3 < hmac.size) { "HMAC result is too short for dynamic truncation" }

        val binaryCode =
            ((hmac[offset].toInt() and 0x7f) shl 24) or
                ((hmac[offset + 1].toInt() and 0xff) shl 16) or
                ((hmac[offset + 2].toInt() and 0xff) shl 8) or
                (hmac[offset + 3].toInt() and 0xff)
        val modulus = when (digits) {
            OtpDigits.SIX -> 1_000_000
            OtpDigits.EIGHT -> 100_000_000
        }
        val code = binaryCode % modulus

        return code.toString().padStart(digits.value, '0')
    }
}
