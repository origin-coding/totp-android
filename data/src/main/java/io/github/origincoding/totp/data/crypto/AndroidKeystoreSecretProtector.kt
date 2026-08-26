package io.github.origincoding.totp.data.crypto

import android.security.keystore.KeyGenParameterSpec
import android.security.keystore.KeyProperties
import java.security.KeyStore
import javax.crypto.Cipher
import javax.crypto.KeyGenerator
import javax.crypto.SecretKey
import javax.crypto.spec.GCMParameterSpec

internal class AndroidKeystoreSecretProtector : SecretProtector {
    private val keyStore = KeyStore.getInstance(KEYSTORE_PROVIDER).apply { load(null) }
    private val keyLock = Any()

    override fun encrypt(secret: ByteArray): EncryptedSecret {
        require(secret.isNotEmpty()) { "OTP secret must contain at least one byte" }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(Cipher.ENCRYPT_MODE, getOrCreateKey())
        return EncryptedSecret(
            ciphertext = cipher.doFinal(secret),
            iv = cipher.iv,
        )
    }

    override fun decrypt(encryptedSecret: EncryptedSecret): ByteArray {
        require(encryptedSecret.ciphertext.isNotEmpty()) { "Encrypted secret must not be empty" }
        require(encryptedSecret.iv.isNotEmpty()) { "Encryption IV must not be empty" }
        val cipher = Cipher.getInstance(CIPHER_TRANSFORMATION)
        cipher.init(
            Cipher.DECRYPT_MODE,
            getOrCreateKey(),
            GCMParameterSpec(AUTHENTICATION_TAG_BITS, encryptedSecret.iv),
        )
        return cipher.doFinal(encryptedSecret.ciphertext)
    }

    private fun getOrCreateKey(): SecretKey = synchronized(keyLock) {
        val existingKey = keyStore.getKey(KEY_ALIAS, null)
        if (existingKey != null) {
            return@synchronized existingKey as? SecretKey
                ?: error("Keystore entry '$KEY_ALIAS' is not a secret key")
        }

        val keyGenerator = KeyGenerator.getInstance(
            KeyProperties.KEY_ALGORITHM_AES,
            KEYSTORE_PROVIDER,
        )
        keyGenerator.init(
            KeyGenParameterSpec.Builder(
                KEY_ALIAS,
                KeyProperties.PURPOSE_ENCRYPT or KeyProperties.PURPOSE_DECRYPT,
            ).setBlockModes(KeyProperties.BLOCK_MODE_GCM)
                .setEncryptionPaddings(KeyProperties.ENCRYPTION_PADDING_NONE)
                .setRandomizedEncryptionRequired(true)
                .build(),
        )
        keyGenerator.generateKey()
    }

    private companion object {
        const val KEYSTORE_PROVIDER = "AndroidKeyStore"
        const val KEY_ALIAS = "totp_secret_encryption_key"
        const val CIPHER_TRANSFORMATION = "AES/GCM/NoPadding"
        const val AUTHENTICATION_TAG_BITS = 128
    }
}
