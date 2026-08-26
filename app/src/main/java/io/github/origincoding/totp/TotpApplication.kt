package io.github.origincoding.totp

import android.app.Application
import io.github.origincoding.totp.data.DataContainer

class TotpApplication : Application() {
    val dataContainer: DataContainer by lazy { DataContainer(this) }
}
