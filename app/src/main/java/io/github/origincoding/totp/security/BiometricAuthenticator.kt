package io.github.origincoding.totp.security

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity

class BiometricAuthenticator(
    activity: FragmentActivity,
    onAuthenticationSucceeded: () -> Unit,
    onAuthenticationCancelled: () -> Unit,
    onAuthenticationError: (String) -> Unit,
) {
    private val biometricManager = BiometricManager.from(activity)
    private val prompt = BiometricPrompt(
        activity,
        ContextCompat.getMainExecutor(activity),
        object : BiometricPrompt.AuthenticationCallback() {
            override fun onAuthenticationSucceeded(
                result: BiometricPrompt.AuthenticationResult,
            ) {
                onAuthenticationSucceeded()
            }

            override fun onAuthenticationError(
                errorCode: Int,
                errString: CharSequence,
            ) {
                if (errorCode == BiometricPrompt.ERROR_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_USER_CANCELED ||
                    errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON
                ) {
                    onAuthenticationCancelled()
                } else {
                    onAuthenticationError(errString.toString())
                }
            }
        },
    )

    fun isAuthenticationAvailable(): Boolean =
        biometricManager.canAuthenticate(ALLOWED_AUTHENTICATORS) ==
            BiometricManager.BIOMETRIC_SUCCESS

    fun authenticate() {
        prompt.authenticate(
            BiometricPrompt.PromptInfo.Builder()
                .setTitle("Unlock TOTP")
                .setSubtitle("Use biometrics or your screen lock")
                .setAllowedAuthenticators(ALLOWED_AUTHENTICATORS)
                .build(),
        )
    }

    private companion object {
        const val ALLOWED_AUTHENTICATORS =
            BiometricManager.Authenticators.BIOMETRIC_STRONG or
                BiometricManager.Authenticators.DEVICE_CREDENTIAL
    }
}
