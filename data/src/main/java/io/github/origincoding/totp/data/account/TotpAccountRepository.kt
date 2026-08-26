package io.github.origincoding.totp.data.account

import io.github.origincoding.totp.core.OtpAuthUri
import kotlinx.coroutines.flow.Flow

interface TotpAccountRepository {
    val accounts: Flow<List<TotpAccount>>

    suspend fun add(account: OtpAuthUri.Totp): Long

    suspend fun update(account: TotpAccount): Boolean

    suspend fun replaceSecret(
        id: Long,
        secret: String,
    ): Boolean

    suspend fun delete(id: Long): Boolean

    suspend fun generateCode(
        id: Long,
        unixTimeSeconds: Long,
    ): String?
}
