package io.github.origincoding.totp.data.account

import io.github.origincoding.totp.core.OtpAlgorithm
import io.github.origincoding.totp.core.OtpDigits

data class TotpAccount(
    val id: Long,
    val accountName: String,
    val issuer: String?,
    val algorithm: OtpAlgorithm,
    val digits: OtpDigits,
    val periodSeconds: Long,
) {
    init {
        require(id > 0) { "Account ID must be positive" }
        require(accountName.isNotBlank()) { "Account name must not be blank" }
        require(issuer == null || issuer.isNotBlank()) { "Issuer must not be blank" }
        require(periodSeconds > 0) { "TOTP period must be positive" }
    }
}
