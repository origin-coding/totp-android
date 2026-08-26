package io.github.origincoding.totp.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.origincoding.totp.core.OtpAlgorithm
import io.github.origincoding.totp.core.OtpAuthUri
import io.github.origincoding.totp.core.OtpAuthUriCodec
import io.github.origincoding.totp.core.OtpDigits
import io.github.origincoding.totp.data.account.TotpAccount
import io.github.origincoding.totp.data.account.TotpAccountRepository
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.launch

data class AccountEditorUiState(
    val otpAuthUri: String = "",
    val isOtpAuthUriApplied: Boolean = false,
    val accountName: String = "",
    val issuer: String = "",
    val secret: String = "",
    val algorithm: OtpAlgorithm = OtpAlgorithm.SHA1,
    val digits: OtpDigits = OtpDigits.SIX,
    val periodSeconds: String = "30",
    val isLoading: Boolean = false,
    val isSaving: Boolean = false,
    val isDeleting: Boolean = false,
    val accountNotFound: Boolean = false,
    val completion: AccountEditorCompletion? = null,
    val errorMessage: String? = null,
)

enum class AccountEditorCompletion {
    SAVED,
    DELETED,
}

class AccountEditorViewModel internal constructor(
    private val repository: TotpAccountRepository,
    private val accountId: Long?,
) : ViewModel() {
    private val _uiState = MutableStateFlow(
        AccountEditorUiState(isLoading = accountId != null),
    )
    val uiState: StateFlow<AccountEditorUiState> = _uiState.asStateFlow()

    val isAdding: Boolean = accountId == null

    init {
        if (accountId != null) loadAccount(accountId)
    }

    fun updateAccountName(value: String) = updateForm { copy(accountName = value) }

    fun updateOtpAuthUri(value: String) = updateForm {
        copy(
            otpAuthUri = value,
            isOtpAuthUriApplied = false,
        )
    }

    fun updateIssuer(value: String) = updateForm { copy(issuer = value) }

    fun updateSecret(value: String) = updateForm { copy(secret = value) }

    fun updateAlgorithm(value: OtpAlgorithm) = updateForm { copy(algorithm = value) }

    fun updateDigits(value: OtpDigits) = updateForm { copy(digits = value) }

    fun updatePeriodSeconds(value: String) = updateForm { copy(periodSeconds = value) }

    fun applyOtpAuthUri() {
        val state = _uiState.value
        if (!isAdding || state.isSaving || state.isDeleting) return

        try {
            val parsed = OtpAuthUriCodec.parse(state.otpAuthUri.trim())
            require(parsed is OtpAuthUri.Totp) { "Only TOTP URIs are supported." }
            _uiState.update {
                it.copy(
                    accountName = parsed.accountName,
                    issuer = parsed.issuer.orEmpty(),
                    secret = parsed.secret,
                    algorithm = parsed.algorithm,
                    digits = parsed.digits,
                    periodSeconds = parsed.periodSeconds.toString(),
                    isOtpAuthUriApplied = true,
                    errorMessage = null,
                )
            }
        } catch (exception: IllegalArgumentException) {
            _uiState.update {
                it.copy(
                    isOtpAuthUriApplied = false,
                    errorMessage = exception.message ?: "Invalid OTP Auth URI.",
                )
            }
        }
    }

    fun save() {
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.isDeleting) return

        val periodSeconds = state.periodSeconds.toLongOrNull()
        if (periodSeconds == null || periodSeconds <= 0) {
            _uiState.update { it.copy(errorMessage = "Period must be a positive integer.") }
            return
        }

        _uiState.update { it.copy(isSaving = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                val saved = if (accountId == null) {
                    repository.add(
                        OtpAuthUri.Totp(
                            accountName = state.accountName.trim(),
                            issuer = state.issuer.trim().ifBlank { null },
                            secret = state.secret.trim(),
                            algorithm = state.algorithm,
                            digits = state.digits,
                            periodSeconds = periodSeconds,
                        ),
                    ) > 0
                } else {
                    repository.update(
                        TotpAccount(
                            id = accountId,
                            accountName = state.accountName.trim(),
                            issuer = state.issuer.trim().ifBlank { null },
                            algorithm = state.algorithm,
                            digits = state.digits,
                            periodSeconds = periodSeconds,
                        ),
                    )
                }

                _uiState.update {
                    if (saved) {
                        it.copy(
                            otpAuthUri = "",
                            isOtpAuthUriApplied = false,
                            secret = "",
                            isSaving = false,
                            completion = AccountEditorCompletion.SAVED,
                        )
                    } else {
                        it.copy(isSaving = false, errorMessage = "Unable to save the account.")
                    }
                }
            } catch (exception: IllegalArgumentException) {
                _uiState.update {
                    it.copy(
                        isSaving = false,
                        errorMessage = exception.message ?: "Invalid account information.",
                    )
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isSaving = false, errorMessage = "Unable to save the account.")
                }
            }
        }
    }

    fun delete() {
        val id = accountId ?: return
        val state = _uiState.value
        if (state.isLoading || state.isSaving || state.isDeleting) return

        _uiState.update { it.copy(isDeleting = true, errorMessage = null) }
        viewModelScope.launch {
            try {
                if (repository.delete(id)) {
                    _uiState.update {
                        it.copy(
                            isDeleting = false,
                            completion = AccountEditorCompletion.DELETED,
                        )
                    }
                } else {
                    _uiState.update {
                        it.copy(isDeleting = false, errorMessage = "Account no longer exists.")
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(isDeleting = false, errorMessage = "Unable to delete the account.")
                }
            }
        }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun loadAccount(id: Long) {
        viewModelScope.launch {
            try {
                val account = repository.accounts.first().firstOrNull { it.id == id }
                _uiState.update {
                    if (account == null) {
                        it.copy(isLoading = false, accountNotFound = true)
                    } else {
                        it.copy(
                            accountName = account.accountName,
                            issuer = account.issuer.orEmpty(),
                            algorithm = account.algorithm,
                            digits = account.digits,
                            periodSeconds = account.periodSeconds.toString(),
                            isLoading = false,
                        )
                    }
                }
            } catch (_: Exception) {
                _uiState.update {
                    it.copy(
                        isLoading = false,
                        accountNotFound = true,
                        errorMessage = "Unable to load the account.",
                    )
                }
            }
        }
    }

    private inline fun updateForm(transform: AccountEditorUiState.() -> AccountEditorUiState) {
        _uiState.update { state ->
            if (state.isSaving || state.isDeleting) state else state.transform().copy(errorMessage = null)
        }
    }

    companion object {
        fun factory(
            repository: TotpAccountRepository,
            accountId: Long?,
        ): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AccountEditorViewModel(repository, accountId) }
            }
    }
}
