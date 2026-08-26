package io.github.origincoding.totp.security

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.origincoding.totp.data.settings.AppSettingsRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AppLockUiState(
    val isLoading: Boolean = true,
    val isEnabled: Boolean = false,
    val isLocked: Boolean = true,
    val isAuthenticating: Boolean = false,
    val isUpdatingSetting: Boolean = false,
    val lockGeneration: Long = 0L,
    val errorMessage: String? = null,
)

enum class AuthenticationPurpose {
    ENABLE_APP_LOCK,
    UNLOCK_APP,
}

class AppLockViewModel internal constructor(
    private val settingsRepository: AppSettingsRepository,
    private val relockTimeoutMillis: Long = DEFAULT_RELOCK_TIMEOUT_MILLIS,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AppLockUiState())
    val uiState: StateFlow<AppLockUiState> = _uiState.asStateFlow()

    private var hasLoadedSetting = false
    private var backgroundedAtMillis: Long? = null
    private var authenticationPurpose: AuthenticationPurpose? = null

    init {
        viewModelScope.launch {
            settingsRepository.appLockEnabled
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isLocked = false,
                            errorMessage = "Unable to load app lock settings.",
                        )
                    }
                }
                .collect { enabled ->
                    val firstValue = !hasLoadedSetting
                    hasLoadedSetting = true
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            isEnabled = enabled,
                            isLocked = when {
                                !enabled -> false
                                firstValue -> true
                                else -> state.isLocked
                            },
                        )
                    }
                }
        }
    }

    fun beginAuthentication(purpose: AuthenticationPurpose): Boolean {
        if (_uiState.value.isAuthenticating) return false
        authenticationPurpose = purpose
        _uiState.update {
            it.copy(
                isAuthenticating = true,
                errorMessage = null,
            )
        }
        return true
    }

    fun onAuthenticationSucceeded() {
        val purpose = authenticationPurpose ?: return
        authenticationPurpose = null

        when (purpose) {
            AuthenticationPurpose.UNLOCK_APP -> {
                _uiState.update {
                    it.copy(
                        isLocked = false,
                        isAuthenticating = false,
                        errorMessage = null,
                    )
                }
            }

            AuthenticationPurpose.ENABLE_APP_LOCK -> enableAppLock()
        }
    }

    fun onAuthenticationCancelled() {
        authenticationPurpose = null
        _uiState.update { it.copy(isAuthenticating = false) }
    }

    fun onAuthenticationError(message: String) {
        authenticationPurpose = null
        _uiState.update {
            it.copy(
                isAuthenticating = false,
                errorMessage = message,
            )
        }
    }

    fun disableAppLock() {
        if (_uiState.value.isUpdatingSetting) return
        _uiState.update { it.copy(isUpdatingSetting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                settingsRepository.setAppLockEnabled(false)
                _uiState.update {
                    it.copy(
                        isEnabled = false,
                        isLocked = false,
                        isUpdatingSetting = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isUpdatingSetting = false,
                        errorMessage = "Unable to disable app lock.",
                    )
                }
            }
        }
    }

    fun onAppBackgrounded(elapsedRealtimeMillis: Long) {
        backgroundedAtMillis = elapsedRealtimeMillis
    }

    fun onAppForegrounded(elapsedRealtimeMillis: Long) {
        val backgroundedAt = backgroundedAtMillis ?: return
        backgroundedAtMillis = null
        val elapsed = (elapsedRealtimeMillis - backgroundedAt).coerceAtLeast(0L)
        if (_uiState.value.isEnabled && elapsed >= relockTimeoutMillis) {
            lock()
        }
    }

    private fun enableAppLock() {
        _uiState.update {
            it.copy(
                isAuthenticating = false,
                isUpdatingSetting = true,
                errorMessage = null,
            )
        }
        viewModelScope.launch {
            try {
                settingsRepository.setAppLockEnabled(true)
                _uiState.update {
                    it.copy(
                        isEnabled = true,
                        isLocked = false,
                        isUpdatingSetting = false,
                    )
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isEnabled = false,
                        isLocked = false,
                        isUpdatingSetting = false,
                        errorMessage = "Unable to enable app lock.",
                    )
                }
            }
        }
    }

    private fun lock() {
        _uiState.update { state ->
            if (state.isLocked && state.isAuthenticating) {
                state
            } else {
                state.copy(
                    isLocked = true,
                    isAuthenticating = false,
                    lockGeneration = state.lockGeneration + 1L,
                    errorMessage = null,
                )
            }
        }
    }

    companion object {
        private const val DEFAULT_RELOCK_TIMEOUT_MILLIS = 30_000L

        fun factory(settingsRepository: AppSettingsRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AppLockViewModel(settingsRepository) }
            }
    }
}
