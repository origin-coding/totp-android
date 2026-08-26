package io.github.origincoding.totp.navigation

import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.lifecycle.compose.LifecycleStartEffect
import androidx.lifecycle.compose.collectAsStateWithLifecycle
import androidx.lifecycle.viewmodel.compose.viewModel
import androidx.lifecycle.viewmodel.navigation3.rememberViewModelStoreNavEntryDecorator
import androidx.navigation3.runtime.NavKey
import androidx.navigation3.runtime.entryProvider
import androidx.navigation3.runtime.rememberNavBackStack
import androidx.navigation3.runtime.rememberSaveableStateHolderNavEntryDecorator
import androidx.navigation3.ui.NavDisplay
import io.github.origincoding.totp.account.AccountEditorScreen
import io.github.origincoding.totp.account.AccountEditorViewModel
import io.github.origincoding.totp.account.AddAccountMethod
import io.github.origincoding.totp.account.AccountListScreen
import io.github.origincoding.totp.account.AccountListViewModel
import io.github.origincoding.totp.account.ReplaceSecretScreen
import io.github.origincoding.totp.account.ReplaceSecretViewModel
import io.github.origincoding.totp.data.account.TotpAccountRepository
import io.github.origincoding.totp.scanner.QrScannerScreen
import io.github.origincoding.totp.settings.SettingsScreen
import kotlinx.serialization.Serializable

@Serializable
private data object AccountListRoute : NavKey

@Serializable
private data class AddAccountRoute(
    val method: AddAccountMethod,
) : NavKey

@Serializable
private data object QrScannerRoute : NavKey

@Serializable
private data object SettingsRoute : NavKey

@Serializable
private data class EditAccountRoute(
    val accountId: Long,
) : NavKey

@Serializable
private data class ReplaceSecretRoute(
    val accountId: Long,
) : NavKey

@Composable
fun TotpApp(
    repository: TotpAccountRepository,
    appLockEnabled: Boolean,
    authenticationAvailable: Boolean,
    appLockSettingInProgress: Boolean,
    appLockErrorMessage: String?,
    onEnableAppLock: () -> Unit,
    onDisableAppLock: () -> Unit,
) {
    val backStack = rememberNavBackStack(AccountListRoute)
    var pendingScannedUri by remember { mutableStateOf<String?>(null) }

    NavDisplay(
        backStack = backStack,
        onBack = { backStack.removeLastOrNull() },
        entryDecorators = listOf(
            rememberSaveableStateHolderNavEntryDecorator(),
            rememberViewModelStoreNavEntryDecorator(),
        ),
        entryProvider = entryProvider {
            entry<AccountListRoute> {
                val viewModel = viewModel<AccountListViewModel>(
                    factory = AccountListViewModel.factory(repository),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LifecycleStartEffect(viewModel) {
                    viewModel.onScreenStarted()
                    onStopOrDispose { viewModel.onScreenStopped() }
                }

                AccountListScreen(
                    uiState = uiState,
                    onToggleAccount = viewModel::toggleAccount,
                    onAddAccount = { method ->
                        viewModel.collapseAccount()
                        backStack.add(AddAccountRoute(method))
                    },
                    onScanQrCode = {
                        viewModel.collapseAccount()
                        backStack.add(QrScannerRoute)
                    },
                    onOpenSettings = {
                        viewModel.collapseAccount()
                        backStack.add(SettingsRoute)
                    },
                    onEditAccount = { accountId ->
                        viewModel.collapseAccount()
                        backStack.add(EditAccountRoute(accountId))
                    },
                    onClearError = viewModel::clearError,
                )
            }

            entry<AddAccountRoute> { route ->
                val viewModel = viewModel<AccountEditorViewModel>(
                    factory = AccountEditorViewModel.factory(
                        repository = repository,
                        accountId = null,
                        initialOtpAuthUri = pendingScannedUri,
                    ),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(Unit) {
                    pendingScannedUri = null
                }

                LaunchedEffect(uiState.completion) {
                    if (uiState.completion != null) backStack.removeLastOrNull()
                }

                AccountEditorScreen(
                    uiState = uiState,
                    isAdding = true,
                    addMethod = route.method,
                    onOtpAuthUriChange = viewModel::updateOtpAuthUri,
                    onApplyOtpAuthUri = viewModel::applyOtpAuthUri,
                    onAccountNameChange = viewModel::updateAccountName,
                    onIssuerChange = viewModel::updateIssuer,
                    onSecretChange = viewModel::updateSecret,
                    onAlgorithmChange = viewModel::updateAlgorithm,
                    onDigitsChange = viewModel::updateDigits,
                    onPeriodChange = viewModel::updatePeriodSeconds,
                    onSave = viewModel::save,
                    onReplaceSecret = {},
                    onDelete = {},
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<QrScannerRoute> {
                QrScannerScreen(
                    onQrCodeScanned = { otpAuthUri ->
                        pendingScannedUri = otpAuthUri
                        backStack.removeLastOrNull()
                        backStack.add(AddAccountRoute(AddAccountMethod.OTP_AUTH_URI))
                    },
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<SettingsRoute> {
                SettingsScreen(
                    appLockEnabled = appLockEnabled,
                    authenticationAvailable = authenticationAvailable,
                    isUpdating = appLockSettingInProgress,
                    errorMessage = appLockErrorMessage,
                    onEnableAppLock = onEnableAppLock,
                    onDisableAppLock = onDisableAppLock,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<EditAccountRoute> { route ->
                val viewModel = viewModel<AccountEditorViewModel>(
                    factory = AccountEditorViewModel.factory(repository, route.accountId),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.completion) {
                    if (uiState.completion != null) backStack.removeLastOrNull()
                }

                AccountEditorScreen(
                    uiState = uiState,
                    isAdding = false,
                    addMethod = null,
                    onOtpAuthUriChange = {},
                    onApplyOtpAuthUri = {},
                    onAccountNameChange = viewModel::updateAccountName,
                    onIssuerChange = viewModel::updateIssuer,
                    onSecretChange = viewModel::updateSecret,
                    onAlgorithmChange = viewModel::updateAlgorithm,
                    onDigitsChange = viewModel::updateDigits,
                    onPeriodChange = viewModel::updatePeriodSeconds,
                    onSave = viewModel::save,
                    onReplaceSecret = {
                        backStack.add(ReplaceSecretRoute(route.accountId))
                    },
                    onDelete = viewModel::delete,
                    onBack = { backStack.removeLastOrNull() },
                )
            }

            entry<ReplaceSecretRoute> { route ->
                val viewModel = viewModel<ReplaceSecretViewModel>(
                    factory = ReplaceSecretViewModel.factory(repository, route.accountId),
                )
                val uiState by viewModel.uiState.collectAsStateWithLifecycle()

                LaunchedEffect(uiState.isComplete) {
                    if (uiState.isComplete) backStack.removeLastOrNull()
                }

                ReplaceSecretScreen(
                    uiState = uiState,
                    onSecretChange = viewModel::updateSecret,
                    onReplace = viewModel::replaceSecret,
                    onBack = { backStack.removeLastOrNull() },
                )
            }
        },
    )
}
