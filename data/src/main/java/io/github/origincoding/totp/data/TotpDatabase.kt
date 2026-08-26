package io.github.origincoding.totp.data

import android.content.Context
import androidx.room3.ColumnTypeConverters
import androidx.room3.Database
import androidx.room3.Room
import androidx.room3.RoomDatabase
import androidx.sqlite.driver.AndroidSQLiteDriver
import io.github.origincoding.totp.data.account.OtpColumnTypeConverters
import io.github.origincoding.totp.data.account.TotpAccountDao
import io.github.origincoding.totp.data.account.TotpAccountEntity

@Database(
    entities = [TotpAccountEntity::class],
    version = 1,
    exportSchema = true,
)
@ColumnTypeConverters(OtpColumnTypeConverters::class)
internal abstract class TotpDatabase : RoomDatabase() {
    abstract fun totpAccountDao(): TotpAccountDao

    companion object {
        private const val DATABASE_NAME = "totp.db"

        fun create(context: Context): TotpDatabase =
            Room.databaseBuilder<TotpDatabase>(
                context = context,
                name = DATABASE_NAME,
            ).setDriver(AndroidSQLiteDriver())
                .build()
    }
}
