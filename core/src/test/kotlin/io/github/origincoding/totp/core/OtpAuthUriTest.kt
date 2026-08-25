package io.github.origincoding.totp.core

import org.junit.Assert.assertThrows
import org.junit.Test

class OtpAuthUriTest {
    @Test
    fun `TOTP rejects invalid common fields`() {
        val invalidFields = listOf(
            CommonFields(accountName = "", issuer = null, secret = VALID_SECRET),
            CommonFields(accountName = "alice", issuer = "", secret = VALID_SECRET),
            CommonFields(accountName = "alice", issuer = null, secret = ""),
            CommonFields(accountName = "alice", issuer = null, secret = "INVALID1"),
        )

        for (fields in invalidFields) {
            assertThrows(
                "Expected TOTP fields to be rejected: $fields",
                IllegalArgumentException::class.java,
            ) {
                OtpAuthUri.Totp(
                    accountName = fields.accountName,
                    issuer = fields.issuer,
                    secret = fields.secret,
                )
            }
        }
    }

    @Test
    fun `TOTP rejects a non-positive period`() {
        for (period in listOf(0L, -1L)) {
            assertThrows(
                "Expected TOTP period to be rejected: $period",
                IllegalArgumentException::class.java,
            ) {
                OtpAuthUri.Totp(
                    accountName = "alice",
                    issuer = null,
                    secret = VALID_SECRET,
                    periodSeconds = period,
                )
            }
        }
    }

    @Test
    fun `HOTP rejects a negative counter`() {
        assertThrows(IllegalArgumentException::class.java) {
            OtpAuthUri.Hotp(
                accountName = "alice",
                issuer = null,
                secret = VALID_SECRET,
                counter = -1,
            )
        }
    }

    private data class CommonFields(
        val accountName: String,
        val issuer: String?,
        val secret: String,
    )

    private companion object {
        const val VALID_SECRET = "JBSWY3DPEHPK3PXP"
    }
}
