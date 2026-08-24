package io.github.origincoding.totp.core

sealed interface OtpAuthUri {
    val accountName: String
    val issuer: String?
    val secret: String
    val algorithm: OtpAlgorithm
    val digits: OtpDigits

    data class Totp(
        override val accountName: String,
        override val issuer: String?,
        override val secret: String,
        override val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
        override val digits: OtpDigits = OtpDigits.SIX,
        val periodSeconds: Long = 30,
    ) : OtpAuthUri {
        init {
            validateCommonFields(accountName, issuer, secret)
            require(periodSeconds > 0) { "TOTP period must be positive" }
        }
    }

    data class Hotp(
        override val accountName: String,
        override val issuer: String?,
        override val secret: String,
        override val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
        override val digits: OtpDigits = OtpDigits.SIX,
        val counter: Long,
    ) : OtpAuthUri {
        init {
            validateCommonFields(accountName, issuer, secret)
            require(counter >= 0) { "HOTP counter must not be negative" }
        }
    }
}

private fun validateCommonFields(
    accountName: String,
    issuer: String?,
    secret: String,
) {
    require(accountName.isNotBlank()) { "Account name must not be blank" }
    require(issuer == null || issuer.isNotBlank()) { "Issuer must not be blank" }
    require(secret.isNotEmpty()) { "OTP secret must not be empty" }
    require(Base32.decode(secret).isNotEmpty()) { "OTP secret must contain at least one byte" }
}
