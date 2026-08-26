package io.github.origincoding.totp

import android.content.Intent
import android.os.Bundle
import android.os.SystemClock
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.setValue
import androidx.fragment.app.FragmentActivity
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import io.github.origincoding.totp.navigation.TotpApp
import io.github.origincoding.totp.security.AppLockLoadingScreen
import io.github.origincoding.totp.security.AppLockScreen
import io.github.origincoding.totp.security.AppLockViewModel
import io.github.origincoding.totp.security.AuthenticationPurpose
import io.github.origincoding.totp.security.BiometricAuthenticator
import io.github.origincoding.totp.ui.theme.TOTPTheme

class MainActivity : FragmentActivity() {
    private val appLockViewModel: AppLockViewModel by viewModels {
        AppLockViewModel.factory(
            (application as TotpApplication).dataContainer.appSettingsRepository,
        )
    }

    private lateinit var biometricAuthenticator: BiometricAuthenticator
    private var authenticationAvailable by mutableStateOf(false)

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        val dataContainer = (application as TotpApplication).dataContainer
        biometricAuthenticator = BiometricAuthenticator(
            activity = this,
            onAuthenticationSucceeded = appLockViewModel::onAuthenticationSucceeded,
            onAuthenticationCancelled = appLockViewModel::onAuthenticationCancelled,
            onAuthenticationError = appLockViewModel::onAuthenticationError,
        )
        authenticationAvailable = biometricAuthenticator.isAuthenticationAvailable()

        setContent {
            val appLockState by appLockViewModel.uiState.collectAsStateWithLifecycle()

            TOTPTheme {
                LaunchedEffect(
                    appLockState.isLoading,
                    appLockState.isLocked,
                    appLockState.lockGeneration,
                    authenticationAvailable,
                ) {
                    if (!appLockState.isLoading &&
                        appLockState.isLocked &&
                        authenticationAvailable
                    ) {
                        requestAuthentication(AuthenticationPurpose.UNLOCK_APP)
                    }
                }

                when {
                    appLockState.isLoading -> AppLockLoadingScreen()

                    appLockState.isLocked -> AppLockScreen(
                        authenticationAvailable = authenticationAvailable,
                        isAuthenticating = appLockState.isAuthenticating,
                        errorMessage = appLockState.errorMessage,
                        onUnlock = {
                            requestAuthentication(AuthenticationPurpose.UNLOCK_APP)
                        },
                        onOpenSecuritySettings = {
                            startActivity(Intent(android.provider.Settings.ACTION_SECURITY_SETTINGS))
                        },
                    )

                    else -> TotpApp(
                        repository = dataContainer.totpAccountRepository,
                        appLockEnabled = appLockState.isEnabled,
                        authenticationAvailable = authenticationAvailable,
                        appLockSettingInProgress = appLockState.isUpdatingSetting,
                        appLockErrorMessage = appLockState.errorMessage,
                        onEnableAppLock = {
                            requestAuthentication(AuthenticationPurpose.ENABLE_APP_LOCK)
                        },
                        onDisableAppLock = appLockViewModel::disableAppLock,
                    )
                }
            }
        }
    }

    override fun onStart() {
        super.onStart()
        authenticationAvailable = biometricAuthenticator.isAuthenticationAvailable()
        appLockViewModel.onAppForegrounded(SystemClock.elapsedRealtime())
    }

    override fun onStop() {
        appLockViewModel.onAppBackgrounded(SystemClock.elapsedRealtime())
        super.onStop()
    }

    private fun requestAuthentication(purpose: AuthenticationPurpose) {
        authenticationAvailable = biometricAuthenticator.isAuthenticationAvailable()
        if (!authenticationAvailable) {
            appLockViewModel.onAuthenticationError(
                "Device authentication is not available.",
            )
            return
        }
        if (appLockViewModel.beginAuthentication(purpose)) {
            biometricAuthenticator.authenticate()
        }
    }
}
