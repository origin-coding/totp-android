package io.github.origincoding.totp.account

import androidx.activity.compose.BackHandler
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.imePadding
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.text.KeyboardOptions
import androidx.compose.foundation.verticalScroll
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.OutlinedCard
import androidx.compose.material3.OutlinedTextField
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.input.KeyboardType
import androidx.compose.ui.text.input.PasswordVisualTransformation
import androidx.compose.ui.text.input.VisualTransformation
import androidx.compose.ui.unit.dp
import io.github.origincoding.totp.core.OtpAlgorithm
import io.github.origincoding.totp.core.OtpDigits
import kotlinx.serialization.Serializable

@Serializable
enum class AddAccountMethod {
    OTP_AUTH_URI,
    SETUP_KEY,
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountEditorScreen(
    uiState: AccountEditorUiState,
    isAdding: Boolean,
    addMethod: AddAccountMethod?,
    onOtpAuthUriChange: (String) -> Unit,
    onApplyOtpAuthUri: () -> Unit,
    onAccountNameChange: (String) -> Unit,
    onIssuerChange: (String) -> Unit,
    onSecretChange: (String) -> Unit,
    onAlgorithmChange: (OtpAlgorithm) -> Unit,
    onDigitsChange: (OtpDigits) -> Unit,
    onPeriodChange: (String) -> Unit,
    onSave: () -> Unit,
    onReplaceSecret: () -> Unit,
    onDelete: () -> Unit,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    var showAdvancedSettings by rememberSaveable { mutableStateOf(false) }
    var showSecret by rememberSaveable { mutableStateOf(false) }
    var showDeleteConfirmation by rememberSaveable { mutableStateOf(false) }
    val operationInProgress = uiState.isSaving || uiState.isDeleting

    BackHandler(enabled = operationInProgress) {}

    if (showDeleteConfirmation) {
        AlertDialog(
            onDismissRequest = { showDeleteConfirmation = false },
            title = { Text("Delete account?") },
            text = { Text("This account and its encrypted secret will be removed from this device.") },
            confirmButton = {
                TextButton(
                    onClick = {
                        showDeleteConfirmation = false
                        onDelete()
                    },
                    colors = ButtonDefaults.textButtonColors(
                        contentColor = MaterialTheme.colorScheme.error,
                    ),
                ) {
                    Text("Delete")
                }
            },
            dismissButton = {
                TextButton(onClick = { showDeleteConfirmation = false }) {
                    Text("Cancel")
                }
            },
        )
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = {
            TopAppBar(
                title = { Text(if (isAdding) "Add account" else "Edit account") },
                navigationIcon = {
                    TextButton(
                        onClick = onBack,
                        enabled = !operationInProgress,
                    ) {
                        Text("Back")
                    }
                },
            )
        },
    ) { innerPadding ->
        when {
            uiState.isLoading -> LoadingContent(
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            uiState.accountNotFound -> AccountNotFoundContent(
                message = uiState.errorMessage,
                onBack = onBack,
                modifier = Modifier
                    .fillMaxSize()
                    .padding(innerPadding),
            )

            else -> {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .verticalScroll(rememberScrollState())
                        .imePadding()
                        .padding(16.dp),
                    verticalArrangement = Arrangement.spacedBy(16.dp),
                ) {
                    when {
                        isAdding && addMethod == AddAccountMethod.OTP_AUTH_URI -> {
                            UriImportContent(
                                uiState = uiState,
                                showAdvancedSettings = showAdvancedSettings,
                                onOtpAuthUriChange = onOtpAuthUriChange,
                                onApplyOtpAuthUri = onApplyOtpAuthUri,
                                onToggleAdvancedSettings = {
                                    showAdvancedSettings = !showAdvancedSettings
                                },
                                onAlgorithmChange = onAlgorithmChange,
                                onDigitsChange = onDigitsChange,
                                onPeriodChange = onPeriodChange,
                            )
                            if (uiState.isOtpAuthUriApplied) {
                                FormActions(
                                    uiState = uiState,
                                    isAdding = true,
                                    onSave = onSave,
                                )
                            } else {
                                ErrorMessage(uiState.errorMessage)
                            }
                        }

                        else -> {
                            AccountDetailsForm(
                                uiState = uiState,
                                isAdding = isAdding,
                                showSecret = showSecret,
                                showAdvancedSettings = showAdvancedSettings,
                                onAccountNameChange = onAccountNameChange,
                                onIssuerChange = onIssuerChange,
                                onSecretChange = onSecretChange,
                                onToggleSecretVisibility = { showSecret = !showSecret },
                                onToggleAdvancedSettings = {
                                    showAdvancedSettings = !showAdvancedSettings
                                },
                                onAlgorithmChange = onAlgorithmChange,
                                onDigitsChange = onDigitsChange,
                                onPeriodChange = onPeriodChange,
                            )
                            FormActions(
                                uiState = uiState,
                                isAdding = isAdding,
                                onSave = onSave,
                            )

                            if (!isAdding) {
                                ExistingSecretSection(
                                    operationInProgress = operationInProgress,
                                    onReplaceSecret = onReplaceSecret,
                                )
                                DangerZone(
                                    isDeleting = uiState.isDeleting,
                                    operationInProgress = operationInProgress,
                                    onRequestDelete = { showDeleteConfirmation = true },
                                )
                            }
                        }
                    }
                }
            }
        }
    }
}

@Composable
private fun LoadingContent(modifier: Modifier = Modifier) {
    Column(
        modifier = modifier,
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        CircularProgressIndicator()
    }
}

@Composable
private fun AccountNotFoundContent(
    message: String?,
    onBack: () -> Unit,
    modifier: Modifier = Modifier,
) {
    Column(
        modifier = modifier.padding(24.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center,
    ) {
        Text(
            message ?: "Account not found",
            style = MaterialTheme.typography.titleLarge,
        )
        OutlinedButton(onClick = onBack, modifier = Modifier.padding(top = 16.dp)) {
            Text("Back to accounts")
        }
    }
}

@Composable
private fun UriImportContent(
    uiState: AccountEditorUiState,
    showAdvancedSettings: Boolean,
    onOtpAuthUriChange: (String) -> Unit,
    onApplyOtpAuthUri: () -> Unit,
    onToggleAdvancedSettings: () -> Unit,
    onAlgorithmChange: (OtpAlgorithm) -> Unit,
    onDigitsChange: (OtpDigits) -> Unit,
    onPeriodChange: (String) -> Unit,
) {
    if (uiState.isOtpAuthUriApplied) {
        Text("Review account", style = MaterialTheme.typography.titleMedium)
        Text(
            "Check the account before saving it on this device.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        AccountPreview(uiState)
        OutlinedButton(
            onClick = { onOtpAuthUriChange("") },
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Use another URI")
        }
        AdvancedSettings(
            uiState = uiState,
            expanded = showAdvancedSettings,
            onToggle = onToggleAdvancedSettings,
            onAlgorithmChange = onAlgorithmChange,
            onDigitsChange = onDigitsChange,
            onPeriodChange = onPeriodChange,
        )
    } else {
        Text("Paste OTP Auth URI", style = MaterialTheme.typography.titleMedium)
        Text(
            "Paste a standard otpauth:// URI to import its account details.",
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
        OutlinedTextField(
            value = uiState.otpAuthUri,
            onValueChange = onOtpAuthUriChange,
            label = { Text("otpauth:// URI") },
            minLines = 2,
            maxLines = 4,
            modifier = Modifier.fillMaxWidth(),
        )
        Button(
            onClick = onApplyOtpAuthUri,
            enabled = uiState.otpAuthUri.isNotBlank(),
            modifier = Modifier.fillMaxWidth(),
        ) {
            Text("Review account")
        }
    }
}

@Composable
private fun AccountPreview(uiState: AccountEditorUiState) {
    OutlinedCard(modifier = Modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp),
        ) {
            if (uiState.issuer.isNotBlank()) {
                Text(uiState.issuer, style = MaterialTheme.typography.titleLarge)
                Text(
                    uiState.accountName,
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            } else {
                Text(uiState.accountName, style = MaterialTheme.typography.titleLarge)
                Text(
                    "No issuer",
                    color = MaterialTheme.colorScheme.onSurfaceVariant,
                )
            }
        }
    }
}

@Composable
private fun AccountDetailsForm(
    uiState: AccountEditorUiState,
    isAdding: Boolean,
    showSecret: Boolean,
    showAdvancedSettings: Boolean,
    onAccountNameChange: (String) -> Unit,
    onIssuerChange: (String) -> Unit,
    onSecretChange: (String) -> Unit,
    onToggleSecretVisibility: () -> Unit,
    onToggleAdvancedSettings: () -> Unit,
    onAlgorithmChange: (OtpAlgorithm) -> Unit,
    onDigitsChange: (OtpDigits) -> Unit,
    onPeriodChange: (String) -> Unit,
) {
    Text("Account details", style = MaterialTheme.typography.titleMedium)
    OutlinedTextField(
        value = uiState.accountName,
        onValueChange = onAccountNameChange,
        label = { Text("Account name") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )
    OutlinedTextField(
        value = uiState.issuer,
        onValueChange = onIssuerChange,
        label = { Text("Issuer (optional)") },
        singleLine = true,
        modifier = Modifier.fillMaxWidth(),
    )

    if (isAdding) {
        OutlinedTextField(
            value = uiState.secret,
            onValueChange = onSecretChange,
            label = { Text("Base32 setup key") },
            singleLine = true,
            visualTransformation = if (showSecret) {
                VisualTransformation.None
            } else {
                PasswordVisualTransformation()
            },
            trailingIcon = {
                TextButton(onClick = onToggleSecretVisibility) {
                    Text(if (showSecret) "Hide" else "Show")
                }
            },
            modifier = Modifier.fillMaxWidth(),
        )
    }

    AdvancedSettings(
        uiState = uiState,
        expanded = showAdvancedSettings,
        onToggle = onToggleAdvancedSettings,
        onAlgorithmChange = onAlgorithmChange,
        onDigitsChange = onDigitsChange,
        onPeriodChange = onPeriodChange,
    )
}

@Composable
private fun AdvancedSettings(
    uiState: AccountEditorUiState,
    expanded: Boolean,
    onToggle: () -> Unit,
    onAlgorithmChange: (OtpAlgorithm) -> Unit,
    onDigitsChange: (OtpDigits) -> Unit,
    onPeriodChange: (String) -> Unit,
) {
    OutlinedButton(
        onClick = onToggle,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text(if (expanded) "Hide advanced settings" else "Advanced settings")
    }
    if (expanded) {
        SelectionSection(
            title = "Algorithm",
            values = OtpAlgorithm.entries,
            selected = uiState.algorithm,
            label = OtpAlgorithm::name,
            onSelected = onAlgorithmChange,
        )
        SelectionSection(
            title = "Digits",
            values = OtpDigits.entries,
            selected = uiState.digits,
            label = { it.value.toString() },
            onSelected = onDigitsChange,
        )
        OutlinedTextField(
            value = uiState.periodSeconds,
            onValueChange = onPeriodChange,
            label = { Text("Period in seconds") },
            keyboardOptions = KeyboardOptions(keyboardType = KeyboardType.Number),
            singleLine = true,
            modifier = Modifier.fillMaxWidth(),
        )
    } else {
        Text(
            "${uiState.algorithm.name} · ${uiState.digits.value} digits · " +
                "${uiState.periodSeconds} seconds",
            style = MaterialTheme.typography.bodySmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant,
        )
    }
}

@Composable
private fun FormActions(
    uiState: AccountEditorUiState,
    isAdding: Boolean,
    onSave: () -> Unit,
) {
    ErrorMessage(uiState.errorMessage)
    Button(
        onClick = onSave,
        enabled = !uiState.isSaving && !uiState.isDeleting,
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (uiState.isSaving) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text(if (isAdding) "Add account" else "Save changes")
        }
    }
}

@Composable
private fun ErrorMessage(message: String?) {
    if (message != null) {
        Text(message, color = MaterialTheme.colorScheme.error)
    }
}

@Composable
private fun ExistingSecretSection(
    operationInProgress: Boolean,
    onReplaceSecret: () -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text("Secret", style = MaterialTheme.typography.titleMedium)
    Text(
        "The existing secret is stored securely and cannot be displayed here.",
        color = MaterialTheme.colorScheme.onSurfaceVariant,
    )
    OutlinedButton(
        onClick = onReplaceSecret,
        enabled = !operationInProgress,
        modifier = Modifier.fillMaxWidth(),
    ) {
        Text("Replace secret")
    }
}

@Composable
private fun DangerZone(
    isDeleting: Boolean,
    operationInProgress: Boolean,
    onRequestDelete: () -> Unit,
) {
    HorizontalDivider(modifier = Modifier.padding(vertical = 8.dp))
    Text("Danger zone", style = MaterialTheme.typography.titleMedium)
    OutlinedButton(
        onClick = onRequestDelete,
        enabled = !operationInProgress,
        colors = ButtonDefaults.outlinedButtonColors(
            contentColor = MaterialTheme.colorScheme.error,
        ),
        modifier = Modifier.fillMaxWidth(),
    ) {
        if (isDeleting) {
            CircularProgressIndicator(
                modifier = Modifier.size(20.dp),
                strokeWidth = 2.dp,
            )
        } else {
            Text("Delete account")
        }
    }
}

@Composable
private fun <T> SelectionSection(
    title: String,
    values: List<T>,
    selected: T,
    label: (T) -> String,
    onSelected: (T) -> Unit,
) {
    Column(verticalArrangement = Arrangement.spacedBy(8.dp)) {
        Text(title, style = MaterialTheme.typography.labelLarge)
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            values.forEach { value ->
                FilterChip(
                    selected = selected == value,
                    onClick = { onSelected(value) },
                    label = { Text(label(value)) },
                )
            }
        }
    }
}
