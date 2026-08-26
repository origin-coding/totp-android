package io.github.origincoding.totp.data

import android.content.Context
import io.github.origincoding.totp.data.account.RoomTotpAccountRepository
import io.github.origincoding.totp.data.account.TotpAccountRepository
import io.github.origincoding.totp.data.crypto.AndroidKeystoreSecretProtector

class DataContainer(context: Context) {
    private val database = TotpDatabase.create(context.applicationContext)

    val totpAccountRepository: TotpAccountRepository = RoomTotpAccountRepository(
        accountDao = database.totpAccountDao(),
        secretProtector = AndroidKeystoreSecretProtector(),
    )
}
