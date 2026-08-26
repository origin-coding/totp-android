package io.github.origincoding.totp.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.origincoding.totp.data.account.TotpAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class ReplaceSecretUiState(
    val secret: String = "",
    val isSaving: Boolean = false,
    val isComplete: Boolean = false,
    val errorMessage: String? = null,
)

class ReplaceSecretViewModel internal constructor(
    private val repository: TotpAccountRepository,
    private val accountId: Long,
) : ViewModel() {
    private val _uiState = MutableStateFlow(ReplaceSecretUiState())
    val uiState: StateFlow<ReplaceSecretUiState> = _uiState.asStateFlow()

    fun updateSecret(value: String) {
        _uiState.update { state ->
            if (state.isSaving) state else state.copy(secret = value, errorMessage = null)
        }
    }

    fun replaceSecret() {
        val state = _uiState.value
        if (state.isSaving) return

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                if (repository.replaceSecret(accountId, state.secret.trim())) {
                    _uiState.value = ReplaceSecretUiState(isComplete = true)
                } else {
                    _uiState.update {
                        it.copy(isSaving = false, errorMessage = "Account no longer exists.")
                    }
                }
            } catch (exception: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = exception.message ?: "Invalid Base32 secret.",
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Unable to replace the secret.")
                }
            }
        }
    }

    companion object {
        fun factory(
            repository: TotpAccountRepository,
            accountId: Long,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { ReplaceSecretViewModel(repository, accountId) }
            }
    }
}
