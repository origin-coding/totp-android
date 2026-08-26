package io.github.origincoding.totp.data.account

import androidx.room3.ColumnInfo
import androidx.room3.Entity
import androidx.room3.PrimaryKey
import io.github.origincoding.totp.core.OtpAlgorithm
import io.github.origincoding.totp.core.OtpDigits

@Entity(tableName = "totp_accounts")
internal data class TotpAccountEntity(
    @PrimaryKey(autoGenerate = true)
    val id: Long = 0,
    @ColumnInfo(name = "account_name")
    val accountName: String,
    val issuer: String?,
    @ColumnInfo(name = "secret_ciphertext")
    val secretCiphertext: ByteArray,
    @ColumnInfo(name = "secret_iv")
    val secretIv: ByteArray,
    val algorithm: OtpAlgorithm,
    val digits: OtpDigits,
    @ColumnInfo(name = "period_seconds")
    val periodSeconds: Long,
) {
    override fun equals(other: Any?): Boolean {
        if (this === other) return true
        if (other !is TotpAccountEntity) return false

        return id == other.id &&
                accountName == other.accountName &&
                issuer == other.issuer &&
                secretCiphertext.contentEquals(other.secretCiphertext) &&
                secretIv.contentEquals(other.secretIv) &&
                algorithm == other.algorithm &&
                digits == other.digits &&
                periodSeconds == other.periodSeconds
    }

    override fun hashCode(): Int {
        var result = id.hashCode()
        result = 31 * result + accountName.hashCode()
        result = 31 * result + (issuer?.hashCode() ?: 0)
        result = 31 * result + secretCiphertext.contentHashCode()
        result = 31 * result + secretIv.contentHashCode()
        result = 31 * result + algorithm.hashCode()
        result = 31 * result + digits.hashCode()
        result = 31 * result + periodSeconds.hashCode()
        return result
    }
}
