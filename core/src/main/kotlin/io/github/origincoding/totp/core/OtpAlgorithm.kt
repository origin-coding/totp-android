package io.github.origincoding.totp.core

enum class OtpAlgorithm(
    internal val jcaName: String,
) {
    SHA1("HmacSHA1"),
    SHA256("HmacSHA256"),
    SHA512("HmacSHA512"),
}
