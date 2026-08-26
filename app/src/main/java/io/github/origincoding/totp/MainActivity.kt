package io.github.origincoding.totp

import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import io.github.origincoding.totp.navigation.TotpApp
import io.github.origincoding.totp.ui.theme.TOTPTheme

class MainActivity : ComponentActivity() {
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val repository = (application as TotpApplication)
            .dataContainer
            .totpAccountRepository

        setContent {
            TOTPTheme {
                TotpApp(repository = repository)
            }
        }
    }
}
