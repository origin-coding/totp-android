package io.github.origincoding.totp.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class TotpGeneratorTest {
    @Test
    fun `generate matches RFC 6238 test vectors`() {
        for (vector in RFC_6238_VECTORS) {
            assertEquals(
                "Unexpected TOTP for ${vector.algorithm} at ${vector.unixTimeSeconds}",
                vector.expected,
                TotpGenerator.generate(
                    secret = secretFor(vector.algorithm),
                    unixTimeSeconds = vector.unixTimeSeconds,
                    algorithm = vector.algorithm,
                    digits = OtpDigits.EIGHT,
                ),
            )
        }
    }

    @Test
    fun `generate changes at a period boundary`() {
        assertEquals(
            "755224",
            TotpGenerator.generate(
                secret = SHA1_SECRET,
                unixTimeSeconds = 29,
            ),
        )
        assertEquals(
            "287082",
            TotpGenerator.generate(
                secret = SHA1_SECRET,
                unixTimeSeconds = 30,
            ),
        )
    }

    @Test
    fun `generate supports a custom period`() {
        assertEquals(
            "755224",
            TotpGenerator.generate(
                secret = SHA1_SECRET,
                unixTimeSeconds = 59,
                periodSeconds = 60,
            ),
        )
    }

    @Test
    fun `generate rejects an empty secret`() {
        assertThrows(IllegalArgumentException::class.java) {
            TotpGenerator.generate(
                secret = byteArrayOf(),
                unixTimeSeconds = 0,
            )
        }
    }

    @Test
    fun `generate rejects a negative time`() {
        assertThrows(IllegalArgumentException::class.java) {
            TotpGenerator.generate(
                secret = SHA1_SECRET,
                unixTimeSeconds = -1,
            )
        }
    }

    @Test
    fun `generate rejects a non-positive period`() {
        for (period in listOf(0L, -1L)) {
            assertThrows(IllegalArgumentException::class.java) {
                TotpGenerator.generate(
                    secret = SHA1_SECRET,
                    unixTimeSeconds = 0,
                    periodSeconds = period,
                )
            }
        }
    }

    private fun secretFor(algorithm: OtpAlgorithm): ByteArray =
        when (algorithm) {
            OtpAlgorithm.SHA1 -> SHA1_SECRET
            OtpAlgorithm.SHA256 -> SHA256_SECRET
            OtpAlgorithm.SHA512 -> SHA512_SECRET
        }

    private data class Rfc6238Vector(
        val unixTimeSeconds: Long,
        val algorithm: OtpAlgorithm,
        val expected: String,
    )

    private companion object {
        val SHA1_SECRET = "12345678901234567890".toByteArray(Charsets.US_ASCII)
        val SHA256_SECRET = "12345678901234567890123456789012".toByteArray(Charsets.US_ASCII)
        val SHA512_SECRET =
            "1234567890123456789012345678901234567890123456789012345678901234"
                .toByteArray(Charsets.US_ASCII)

        val RFC_6238_VECTORS = listOf(
            Rfc6238Vector(59, OtpAlgorithm.SHA1, "94287082"),
            Rfc6238Vector(59, OtpAlgorithm.SHA256, "46119246"),
            Rfc6238Vector(59, OtpAlgorithm.SHA512, "90693936"),
            Rfc6238Vector(1_111_111_109, OtpAlgorithm.SHA1, "07081804"),
            Rfc6238Vector(1_111_111_109, OtpAlgorithm.SHA256, "68084774"),
            Rfc6238Vector(1_111_111_109, OtpAlgorithm.SHA512, "25091201"),
            Rfc6238Vector(1_111_111_111, OtpAlgorithm.SHA1, "14050471"),
            Rfc6238Vector(1_111_111_111, OtpAlgorithm.SHA256, "67062674"),
            Rfc6238Vector(1_111_111_111, OtpAlgorithm.SHA512, "99943326"),
            Rfc6238Vector(1_234_567_890, OtpAlgorithm.SHA1, "89005924"),
            Rfc6238Vector(1_234_567_890, OtpAlgorithm.SHA256, "91819424"),
            Rfc6238Vector(1_234_567_890, OtpAlgorithm.SHA512, "93441116"),
            Rfc6238Vector(2_000_000_000, OtpAlgorithm.SHA1, "69279037"),
            Rfc6238Vector(2_000_000_000, OtpAlgorithm.SHA256, "90698825"),
            Rfc6238Vector(2_000_000_000, OtpAlgorithm.SHA512, "38618901"),
            Rfc6238Vector(20_000_000_000, OtpAlgorithm.SHA1, "65353130"),
            Rfc6238Vector(20_000_000_000, OtpAlgorithm.SHA256, "77737706"),
            Rfc6238Vector(20_000_000_000, OtpAlgorithm.SHA512, "47863826"),
        )
    }
}
