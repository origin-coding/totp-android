package io.github.origincoding.totp.security

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Button
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

@Composable
fun AppLockLoadingScreen(modifier: Modifier = Modifier) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier.fillMaxSize(),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            CircularProgressIndicator()
        }
    }
}

@Composable
fun AppLockScreen(
    authenticationAvailable: Boolean,
    isAuthenticating: Boolean,
    errorMessage: String?,
    onUnlock: () -> Unit,
    onOpenSecuritySettings: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Surface(modifier = modifier.fillMaxSize()) {
        Column(
            modifier = Modifier
                .fillMaxSize()
                .padding(32.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.Center,
        ) {
            Text("TOTP is locked", style = MaterialTheme.typography.headlineSmall)
            Text(
                text = if (authenticationAvailable) {
                    "Authenticate to view your verification codes."
                } else {
                    "Set up a device screen lock or strong biometric authentication to continue."
                },
                modifier = Modifier.padding(top = 12.dp),
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
            if (errorMessage != null) {
                Text(
                    text = errorMessage,
                    modifier = Modifier.padding(top = 16.dp),
                    color = MaterialTheme.colorScheme.error,
                )
            }
            if (authenticationAvailable) {
                Button(
                    onClick = onUnlock,
                    enabled = !isAuthenticating,
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    if (isAuthenticating) {
                        CircularProgressIndicator(
                            modifier = Modifier.size(20.dp),
                            strokeWidth = 2.dp,
                        )
                    } else {
                        Text("Unlock")
                    }
                }
            } else {
                OutlinedButton(
                    onClick = onOpenSecuritySettings,
                    modifier = Modifier.padding(top = 24.dp),
                ) {
                    Text("Open security settings")
                }
            }
        }
    }
}
