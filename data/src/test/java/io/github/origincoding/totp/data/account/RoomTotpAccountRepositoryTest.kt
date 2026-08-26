package io.github.origincoding.totp.data.account

import io.github.origincoding.totp.core.Base32
import io.github.origincoding.totp.core.OtpAlgorithm
import io.github.origincoding.totp.core.OtpAuthUri
import io.github.origincoding.totp.core.OtpDigits
import io.github.origincoding.totp.data.crypto.EncryptedSecret
import io.github.origincoding.totp.data.crypto.SecretProtector
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertArrayEquals
import org.junit.Assert.assertEquals
import org.junit.Assert.assertFalse
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Test

class RoomTotpAccountRepositoryTest {
    private val accountDao = FakeTotpAccountDao()
    private val repository = RoomTotpAccountRepository(
        accountDao = accountDao,
        secretProtector = FakeSecretProtector(),
    )

    @Test
    fun `add encrypts secret and exposes account metadata`() = runTest {
        val secret = "12345678901234567890".toByteArray()

        val id = repository.add(
            OtpAuthUri.Totp(
                accountName = "user@example.com",
                issuer = "Example",
                secret = Base32.encode(secret),
                algorithm = OtpAlgorithm.SHA256,
                digits = OtpDigits.EIGHT,
                periodSeconds = 45,
            ),
        )

        val storedAccount = requireNotNull(accountDao.findById(id))
        assertFalse(secret.contentEquals(storedAccount.secretCiphertext))
        assertEquals(
            listOf(
                TotpAccount(
                    id = id,
                    accountName = "user@example.com",
                    issuer = "Example",
                    algorithm = OtpAlgorithm.SHA256,
                    digits = OtpDigits.EIGHT,
                    periodSeconds = 45,
                ),
            ),
            repository.accounts.first(),
        )
    }

    @Test
    fun `generateCode decrypts secret and uses stored parameters`() = runTest {
        val id = repository.add(
            OtpAuthUri.Totp(
                accountName = "test",
                issuer = null,
                secret = Base32.encode("12345678901234567890".toByteArray()),
                algorithm = OtpAlgorithm.SHA1,
                digits = OtpDigits.EIGHT,
                periodSeconds = 30,
            ),
        )

        assertEquals("94287082", repository.generateCode(id, unixTimeSeconds = 59))
        assertNull(repository.generateCode(id + 1, unixTimeSeconds = 59))
    }

    @Test
    fun `update preserves secret and replaceSecret changes it`() = runTest {
        val id = repository.add(
            OtpAuthUri.Totp(
                accountName = "old name",
                issuer = "Old issuer",
                secret = Base32.encode(byteArrayOf(1, 2, 3)),
            ),
        )
        val originalCiphertext = requireNotNull(accountDao.findById(id)).secretCiphertext.copyOf()

        assertTrue(
            repository.update(
                TotpAccount(
                    id = id,
                    accountName = "new name",
                    issuer = null,
                    algorithm = OtpAlgorithm.SHA512,
                    digits = OtpDigits.EIGHT,
                    periodSeconds = 60,
                ),
            ),
        )
        assertArrayEquals(originalCiphertext, requireNotNull(accountDao.findById(id)).secretCiphertext)

        assertTrue(repository.replaceSecret(id, Base32.encode(byteArrayOf(4, 5, 6))))
        assertFalse(
            originalCiphertext.contentEquals(requireNotNull(accountDao.findById(id)).secretCiphertext),
        )
    }

    @Test
    fun `delete reports whether an account existed`() = runTest {
        val id = repository.add(
            OtpAuthUri.Totp(
                accountName = "account",
                issuer = null,
                secret = Base32.encode(byteArrayOf(1)),
            ),
        )

        assertTrue(repository.delete(id))
        assertFalse(repository.delete(id))
    }
}

private class FakeTotpAccountDao : TotpAccountDao {
    private val accountsById = linkedMapOf<Long, TotpAccountEntity>()
    private val accountFlow = MutableStateFlow<List<TotpAccountEntity>>(emptyList())
    private var nextId = 1L

    override fun observeAll(): Flow<List<TotpAccountEntity>> = accountFlow

    override suspend fun findById(id: Long): TotpAccountEntity? = accountsById[id]

    override suspend fun insert(account: TotpAccountEntity): Long {
        val id = nextId++
        accountsById[id] = account.copy(id = id)
        publishAccounts()
        return id
    }

    override suspend fun update(account: TotpAccountEntity): Int {
        if (account.id !in accountsById) return 0
        accountsById[account.id] = account
        publishAccounts()
        return 1
    }

    override suspend fun deleteById(id: Long): Int {
        if (accountsById.remove(id) == null) return 0
        publishAccounts()
        return 1
    }

    private fun publishAccounts() {
        accountFlow.value = accountsById.values.sortedBy(TotpAccountEntity::id)
    }
}

private class FakeSecretProtector : SecretProtector {
    override fun encrypt(secret: ByteArray): EncryptedSecret =
        EncryptedSecret(
            ciphertext = secret.map { byte -> (byte.toInt() xor MASK).toByte() }.toByteArray(),
            iv = byteArrayOf(1),
        )

    override fun decrypt(encryptedSecret: EncryptedSecret): ByteArray =
        encryptedSecret.ciphertext
            .map { byte -> (byte.toInt() xor MASK).toByte() }
            .toByteArray()

    private companion object {
        const val MASK = 0x5a
    }
}
