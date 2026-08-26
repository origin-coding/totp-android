package io.github.origincoding.totp.data.account

import io.github.origincoding.totp.core.Base32
import io.github.origincoding.totp.core.OtpAuthUri
import io.github.origincoding.totp.core.TotpGenerator
import io.github.origincoding.totp.data.crypto.EncryptedSecret
import io.github.origincoding.totp.data.crypto.SecretProtector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

internal class RoomTotpAccountRepository(
    private val accountDao: TotpAccountDao,
    private val secretProtector: SecretProtector,
) : TotpAccountRepository {
    override val accounts: Flow<List<TotpAccount>> =
        accountDao.observeAll().map { entities -> entities.map(TotpAccountEntity::toAccount) }

    override suspend fun add(account: OtpAuthUri.Totp): Long {
        val secret = Base32.decode(account.secret)
        return try {
            val encryptedSecret = secretProtector.encrypt(secret)
            accountDao.insert(
                TotpAccountEntity(
                    accountName = account.accountName,
                    issuer = account.issuer,
                    secretCiphertext = encryptedSecret.ciphertext,
                    secretIv = encryptedSecret.iv,
                    algorithm = account.algorithm,
                    digits = account.digits,
                    periodSeconds = account.periodSeconds,
                ),
            )
        } finally {
            secret.fill(0)
        }
    }

    override suspend fun update(account: TotpAccount): Boolean {
        val storedAccount = accountDao.findById(account.id) ?: return false
        return accountDao.update(
            storedAccount.copy(
                accountName = account.accountName,
                issuer = account.issuer,
                algorithm = account.algorithm,
                digits = account.digits,
                periodSeconds = account.periodSeconds,
            ),
        ) == 1
    }

    override suspend fun replaceSecret(
        id: Long,
        secret: String,
    ): Boolean {
        require(id > 0) { "Account ID must be positive" }
        val storedAccount = accountDao.findById(id) ?: return false
        val decodedSecret = Base32.decode(secret)
        require(decodedSecret.isNotEmpty()) { "OTP secret must contain at least one byte" }

        return try {
            val encryptedSecret = secretProtector.encrypt(decodedSecret)
            accountDao.update(
                storedAccount.copy(
                    secretCiphertext = encryptedSecret.ciphertext,
                    secretIv = encryptedSecret.iv,
                ),
            ) == 1
        } finally {
            decodedSecret.fill(0)
        }
    }

    override suspend fun delete(id: Long): Boolean {
        require(id > 0) { "Account ID must be positive" }
        return accountDao.deleteById(id) == 1
    }

    override suspend fun generateCode(
        id: Long,
        unixTimeSeconds: Long,
    ): String? {
        require(id > 0) { "Account ID must be positive" }
        val account = accountDao.findById(id) ?: return null
        val secret = secretProtector.decrypt(
            EncryptedSecret(
                ciphertext = account.secretCiphertext,
                iv = account.secretIv,
            ),
        )

        return try {
            TotpGenerator.generate(
                secret = secret,
                unixTimeSeconds = unixTimeSeconds,
                periodSeconds = account.periodSeconds,
                algorithm = account.algorithm,
                digits = account.digits,
            )
        } finally {
            secret.fill(0)
        }
    }
}

private fun TotpAccountEntity.toAccount(): TotpAccount =
    TotpAccount(
        id = id,
        accountName = accountName,
        issuer = issuer,
        algorithm = algorithm,
        digits = digits,
        periodSeconds = periodSeconds,
    )
