package io.github.origincoding.totp.data.account

import androidx.room3.ColumnTypeConverter
import io.github.origincoding.totp.core.OtpAlgorithm
import io.github.origincoding.totp.core.OtpDigits

internal class OtpColumnTypeConverters {
    @ColumnTypeConverter
    fun algorithmToString(value: OtpAlgorithm): String = value.name

    @ColumnTypeConverter
    fun stringToAlgorithm(value: String): OtpAlgorithm = OtpAlgorithm.valueOf(value)

    @ColumnTypeConverter
    fun digitsToInt(value: OtpDigits): Int = value.value

    @ColumnTypeConverter
    fun intToDigits(value: Int): OtpDigits =
        requireNotNull(OtpDigits.entries.firstOrNull { it.value == value }) {
            "Unsupported OTP digit count: $value"
        }
}
