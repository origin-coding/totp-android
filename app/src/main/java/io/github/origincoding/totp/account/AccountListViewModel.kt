package io.github.origincoding.totp.account

import androidx.lifecycle.ViewModel
import androidx.lifecycle.ViewModelProvider
import androidx.lifecycle.viewModelScope
import androidx.lifecycle.viewmodel.initializer
import androidx.lifecycle.viewmodel.viewModelFactory
import io.github.origincoding.totp.data.account.TotpAccount
import io.github.origincoding.totp.data.account.TotpAccountRepository
import kotlinx.coroutines.CancellationException
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.catch
import kotlinx.coroutines.flow.update
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch
import kotlin.time.Duration.Companion.milliseconds

data class AccountListUiState(
    val accounts: List<TotpAccount> = emptyList(),
    val expandedAccount: ExpandedAccountUiState? = null,
    val isLoading: Boolean = true,
    val errorMessage: String? = null,
)

data class ExpandedAccountUiState(
    val accountId: Long,
    val code: String? = null,
    val remainingSeconds: Long? = null,
    val isLoading: Boolean = true,
)

class AccountListViewModel internal constructor(
    private val repository: TotpAccountRepository,
    private val currentTimeSeconds: () -> Long = { System.currentTimeMillis() / 1_000L },
    private val tickDelayMillis: Long = 1_000L,
) : ViewModel() {
    private val _uiState = MutableStateFlow(AccountListUiState())
    val uiState: StateFlow<AccountListUiState> = _uiState.asStateFlow()

    private var refreshJob: Job? = null
    private var screenStarted = false

    init {
        viewModelScope.launch {
            repository.accounts
                .catch {
                    _uiState.update { state ->
                        state.copy(
                            isLoading = false,
                            errorMessage = "Unable to load accounts.",
                        )
                    }
                }
                .collect { accounts ->
                    val expandedId = _uiState.value.expandedAccount?.accountId
                    val expandedStillExists = expandedId == null ||
                        accounts.any { it.id == expandedId }

                    if (!expandedStillExists) {
                        refreshJob?.cancel()
                    }

                    _uiState.update { state ->
                        state.copy(
                            accounts = accounts,
                            expandedAccount = state.expandedAccount.takeIf { expandedStillExists },
                            isLoading = false,
                        )
                    }
                }
        }
    }

    fun toggleAccount(accountId: Long) {
        val state = _uiState.value
        if (state.expandedAccount?.accountId == accountId) {
            collapseAccount()
            return
        }
        if (state.accounts.none { it.id == accountId }) return

        refreshJob?.cancel()
        _uiState.update {
            it.copy(
                expandedAccount = ExpandedAccountUiState(accountId = accountId),
                errorMessage = null,
            )
        }
        if (screenStarted) {
            refreshJob = refreshExpandedAccount(accountId)
        }
    }

    fun onScreenStarted() {
        screenStarted = true
        val accountId = _uiState.value.expandedAccount?.accountId ?: return
        if (refreshJob?.isActive != true) {
            refreshJob = refreshExpandedAccount(accountId)
        }
    }

    fun onScreenStopped() {
        screenStarted = false
        refreshJob?.cancel()
        refreshJob = null
    }

    fun collapseAccount() {
        refreshJob?.cancel()
        refreshJob = null
        _uiState.update { it.copy(expandedAccount = null) }
    }

    fun clearError() {
        _uiState.update { it.copy(errorMessage = null) }
    }

    private fun refreshExpandedAccount(accountId: Long): Job =
        viewModelScope.launch {
            var generatedCounter: Long? = null
            var code: String? = null

            try {
                while (isActive) {
                    val account = _uiState.value.accounts.firstOrNull { it.id == accountId }
                        ?: run {
                            collapseAccount()
                            return@launch
                        }
                    val now = currentTimeSeconds()
                    val counter = Math.floorDiv(now, account.periodSeconds)

                    if (generatedCounter != counter) {
                        code = repository.generateCode(accountId, now)
                            ?: run {
                                collapseAccount()
                                return@launch
                            }
                        generatedCounter = counter
                    }

                    val elapsedSeconds = Math.floorMod(now, account.periodSeconds)
                    val remainingSeconds = account.periodSeconds - elapsedSeconds
                    _uiState.update { state ->
                        if (state.expandedAccount?.accountId != accountId) {
                            state
                        } else {
                            state.copy(
                                expandedAccount = ExpandedAccountUiState(
                                    accountId = accountId,
                                    code = code,
                                    remainingSeconds = remainingSeconds,
                                    isLoading = false,
                                ),
                            )
                        }
                    }

                    delay(tickDelayMillis.milliseconds)
                }
            } catch (exception: CancellationException) {
                throw exception
            } catch (_: Exception) {
                _uiState.update { state ->
                    if (state.expandedAccount?.accountId != accountId) {
                        state
                    } else {
                        state.copy(
                            expandedAccount = state.expandedAccount.copy(isLoading = false),
                            errorMessage = "Unable to generate the code. Try again.",
                        )
                    }
                }
            }
        }

    companion object {
        fun factory(repository: TotpAccountRepository): ViewModelProvider.Factory =
            viewModelFactory {
                initializer { AccountListViewModel(repository) }
            }
    }
}
