package io.github.origincoding.totp.core

import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertThrows
import org.junit.Test

class OtpAuthUriCodecTest {
    @Test
    fun `parse reads a TOTP URI with default parameters`() {
        val parsed = OtpAuthUriCodec.parse(
            "otpauth://totp/Example:alice@google.com" +
                "?secret=JBSWY3DPEHPK3PXP&issuer=Example",
        ) as OtpAuthUri.Totp

        assertEquals("alice@google.com", parsed.accountName)
        assertEquals("Example", parsed.issuer)
        assertEquals("JBSWY3DPEHPK3PXP", parsed.secret)
        assertEquals(OtpAlgorithm.SHA1, parsed.algorithm)
        assertEquals(OtpDigits.SIX, parsed.digits)
        assertEquals(30L, parsed.periodSeconds)
    }

    @Test
    fun `parse reads explicit TOTP parameters and percent-encoded values`() {
        val parsed = OtpAuthUriCodec.parse(
            "otpauth://totp/ACME%20Co:john.doe%2Bwork%40example.com" +
                "?secret=HXDMVJECJJWSRB3HWIZR4IFUGFTMXBOZ" +
                "&issuer=ACME%20Co&algorithm=SHA256&digits=8&period=45",
        ) as OtpAuthUri.Totp

        assertEquals("john.doe+work@example.com", parsed.accountName)
        assertEquals("ACME Co", parsed.issuer)
        assertEquals(OtpAlgorithm.SHA256, parsed.algorithm)
        assertEquals(OtpDigits.EIGHT, parsed.digits)
        assertEquals(45L, parsed.periodSeconds)
    }

    @Test
    fun `parse reads an HOTP URI`() {
        val parsed = OtpAuthUriCodec.parse(
            "otpauth://hotp/alice?secret=JBSWY3DPEHPK3PXP&counter=7",
        ) as OtpAuthUri.Hotp

        assertEquals("alice", parsed.accountName)
        assertNull(parsed.issuer)
        assertEquals(7L, parsed.counter)
        assertEquals(OtpAlgorithm.SHA1, parsed.algorithm)
        assertEquals(OtpDigits.SIX, parsed.digits)
    }

    @Test
    fun `parse infers issuer from the label`() {
        val parsed = OtpAuthUriCodec.parse(
            "otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP",
        ) as OtpAuthUri.Totp

        assertEquals("Example", parsed.issuer)
        assertEquals("alice", parsed.accountName)
    }

    @Test
    fun `parse preserves a literal plus sign`() {
        val parsed = OtpAuthUriCodec.parse(
            "otpauth://totp/alice+work@example.com?secret=JBSWY3DPEHPK3PXP",
        ) as OtpAuthUri.Totp

        assertEquals("alice+work@example.com", parsed.accountName)
    }

    @Test
    fun `parse normalizes lowercase Base32 and algorithm values`() {
        val parsed = OtpAuthUriCodec.parse(
            "otpauth://totp/alice?secret=jbswy3dpehpk3pxp&algorithm=sha512",
        ) as OtpAuthUri.Totp

        assertEquals("JBSWY3DPEHPK3PXP", parsed.secret)
        assertEquals(OtpAlgorithm.SHA512, parsed.algorithm)
    }

    @Test
    fun `format produces a canonical TOTP URI`() {
        val value = OtpAuthUri.Totp(
            accountName = "alice+work@example.com",
            issuer = "ACME Co",
            secret = "jbswy3dpehpk3pxp",
            algorithm = OtpAlgorithm.SHA256,
            digits = OtpDigits.EIGHT,
            periodSeconds = 45,
        )

        assertEquals(
            "otpauth://totp/ACME%20Co:alice%2Bwork%40example.com" +
                "?secret=JBSWY3DPEHPK3PXP&issuer=ACME%20Co" +
                "&algorithm=SHA256&digits=8&period=45",
            OtpAuthUriCodec.format(value),
        )
    }

    @Test
    fun `format produces a canonical HOTP URI`() {
        val value = OtpAuthUri.Hotp(
            accountName = "alice",
            issuer = null,
            secret = "JBSWY3DPEHPK3PXP",
            counter = 7,
        )

        assertEquals(
            "otpauth://hotp/alice?secret=JBSWY3DPEHPK3PXP" +
                "&algorithm=SHA1&digits=6&counter=7",
            OtpAuthUriCodec.format(value),
        )
    }

    @Test
    fun `format and parse round trip Unicode values`() {
        val original = OtpAuthUri.Totp(
            accountName = "用户@example.com",
            issuer = "示例服务",
            secret = "JBSWY3DPEHPK3PXP",
            algorithm = OtpAlgorithm.SHA512,
            digits = OtpDigits.EIGHT,
            periodSeconds = 60,
        )

        assertEquals(original, OtpAuthUriCodec.parse(OtpAuthUriCodec.format(original)))
    }

    @Test
    fun `parse rejects invalid URI structures`() {
        assertUrisRejected(
            "",
            "https://totp/alice?secret=JBSWY3DPEHPK3PXP",
            "otpauth://steam/alice?secret=JBSWY3DPEHPK3PXP",
            "otpauth://totp/?secret=JBSWY3DPEHPK3PXP",
            "otpauth://totp/a/b?secret=JBSWY3DPEHPK3PXP",
            "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP#fragment",
        )
    }

    @Test
    fun `parse rejects invalid common parameters`() {
        assertUrisRejected(
            "otpauth://totp/alice",
            "otpauth://totp/alice?secret=",
            "otpauth://totp/alice?secret=INVALID1",
            "otpauth://totp/Example:alice?secret=JBSWY3DPEHPK3PXP&issuer=Other",
            "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&algorithm=MD5",
            "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&digits=7",
            "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&secret=JBSWY3DPEHPK3PXP",
        )
    }

    @Test
    fun `parse rejects invalid TOTP parameters`() {
        assertUrisRejected(
            "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&period=0",
            "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&period=abc",
            "otpauth://totp/alice?secret=JBSWY3DPEHPK3PXP&counter=0",
        )
    }

    @Test
    fun `parse rejects invalid HOTP parameters`() {
        assertUrisRejected(
            "otpauth://hotp/alice?secret=JBSWY3DPEHPK3PXP",
            "otpauth://hotp/alice?secret=JBSWY3DPEHPK3PXP&counter=-1",
            "otpauth://hotp/alice?secret=JBSWY3DPEHPK3PXP&counter=0&period=30",
        )
    }

    private fun assertUrisRejected(vararg uris: String) {
        for (uri in uris) {
            assertThrows(
                "Expected URI to be rejected: $uri",
                IllegalArgumentException::class.java,
            ) {
                OtpAuthUriCodec.parse(uri)
            }
        }
    }
}
