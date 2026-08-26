package io.github.origincoding.totp.data.account

import androidx.room3.Dao
import androidx.room3.Insert
import androidx.room3.Query
import androidx.room3.Update
import kotlinx.coroutines.flow.Flow

@Dao
internal interface TotpAccountDao {
    @Query("SELECT * FROM totp_accounts ORDER BY id ASC")
    fun observeAll(): Flow<List<TotpAccountEntity>>

    @Query("SELECT * FROM totp_accounts WHERE id = :id")
    suspend fun findById(id: Long): TotpAccountEntity?

    @Insert
    suspend fun insert(account: TotpAccountEntity): Long

    @Update
    suspend fun update(account: TotpAccountEntity): Int

    @Query("DELETE FROM totp_accounts WHERE id = :id")
    suspend fun deleteById(id: Long): Int
}
