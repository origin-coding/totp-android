package io.github.origincoding.totp.data.crypto

internal data class EncryptedSecret(
    val ciphertext: ByteArray,
    val iv: ByteArray,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is EncryptedSecret) return false

        return ciphertext.contentEquals(other.ciphertext) &&
                iv.contentEquals(other.iv)
    }

    override fun hashCode(): Int {
        var result = ciphertext.contentHashCode()
        result = 31 * result + iv.contentHashCode()
        return result
    }
}

internal interface SecretProtector {
    fun encrypt(secret: ByteArray): EncryptedSecret

    fun decrypt(encryptedSecret: EncryptedSecret): ByteArray
}
