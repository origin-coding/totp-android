package io.github.origincoding.totp.core

object TotpGenerator {
    fun generate(
        secret: ByteArray,
        unixTimeSeconds: Long,
        periodSeconds: Long = 30,
        algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
        digits: OtpDigits = OtpDigits.SIX,
    ): String {
        require(unixTimeSeconds >= 0) { "Unix time must not be negative" }
        require(periodSeconds > 0) { "TOTP period must be positive" }

        return HotpGenerator.generate(
            secret = secret,
            counter = unixTimeSeconds / periodSeconds,
            algorithm = algorithm,
            digits = digits,
        )
    }
}
