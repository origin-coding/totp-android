package io.github.origincoding.totp.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertThrows
import org.junit.Test

class HotpGeneratorTest {
    @Test
    fun `generate matches RFC 4226 test vectors`() {
        val expectedCodes = listOf(
            "755224",
            "287082",
            "359152",
            "969429",
            "338314",
            "254676",
            "287922",
            "162583",
            "399871",
            "520489",
        )

        expectedCodes.forEachIndexed { counter, expected ->
            assertEquals(
                "Unexpected HOTP for counter $counter",
                expected,
                HotpGenerator.generate(
                    secret = RFC_4226_SECRET,
                    counter = counter.toLong(),
                ),
            )
        }
    }

    @Test
    fun `generate supports eight digit codes`() {
        assertEquals(
            "84755224",
            HotpGenerator.generate(
                secret = RFC_4226_SECRET,
                counter = 0,
                digits = OtpDigits.EIGHT,
            ),
        )
    }

    @Test
    fun `generate rejects an empty secret`() {
        assertThrows(IllegalArgumentException::class.java) {
            HotpGenerator.generate(
                secret = byteArrayOf(),
                counter = 0,
            )
        }
    }

    @Test
    fun `generate rejects a negative counter`() {
        assertThrows(IllegalArgumentException::class.java) {
            HotpGenerator.generate(
                secret = RFC_4226_SECRET,
                counter = -1,
            )
        }
    }

    private companion object {
        val RFC_4226_SECRET = "12345678901234567890".toByteArray(Charsets.US_ASCII)
    }
}
