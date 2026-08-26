package io.github.origincoding.totp.account

import android.content.ClipData
import android.content.ClipDescription
import android.content.ClipboardManager
import android.content.Context
import android.os.Build
import android.os.PersistableBundle
import androidx.activity.compose.BackHandler
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.core.FastOutSlowInEasing
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.animateFloatAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.Card
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FloatingActionButton
import androidx.compose.material3.Icon
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.SmallFloatingActionButton
import androidx.compose.material3.SnackbarHost
import androidx.compose.material3.SnackbarHostState
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.TopAppBar
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.rememberCoroutineScope
import androidx.compose.runtime.saveable.rememberSaveable
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.res.painterResource
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import io.github.origincoding.totp.R
import io.github.origincoding.totp.data.account.TotpAccount
import kotlinx.coroutines.launch

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun AccountListScreen(
    uiState: AccountListUiState,
    onToggleAccount: (Long) -> Unit,
    onAddAccount: (AddAccountMethod) -> Unit,
    onScanQrCode: () -> Unit,
    onEditAccount: (Long) -> Unit,
    onClearError: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val context = LocalContext.current
    val snackbarHostState = remember { SnackbarHostState() }
    val coroutineScope = rememberCoroutineScope()
    var addMenuExpanded by rememberSaveable { mutableStateOf(false) }

    BackHandler(enabled = addMenuExpanded) {
        addMenuExpanded = false
    }

    uiState.errorMessage?.let { message ->
        androidx.compose.runtime.LaunchedEffect(message) {
            snackbarHostState.showSnackbar(message)
            onClearError()
        }
    }

    Scaffold(
        modifier = modifier.fillMaxSize(),
        topBar = { TopAppBar(title = { Text("TOTP accounts") }) },
        floatingActionButton = {
            AddAccountMenu(
                expanded = addMenuExpanded,
                onToggle = { addMenuExpanded = !addMenuExpanded },
                onScanQrCode = {
                    addMenuExpanded = false
                    onScanQrCode()
                },
                onSelect = { method ->
                    addMenuExpanded = false
                    onAddAccount(method)
                },
            )
        },
        snackbarHost = { SnackbarHost(snackbarHostState) },
    ) { innerPadding ->
        Box(modifier = Modifier.fillMaxSize()) {
            when {
                uiState.isLoading -> {
                    Box(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentAlignment = Alignment.Center,
                    ) {
                        CircularProgressIndicator()
                    }
                }

                uiState.accounts.isEmpty() -> {
                    EmptyAccountList(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                    )
                }

                else -> {
                    LazyColumn(
                        modifier = Modifier
                            .fillMaxSize()
                            .padding(innerPadding),
                        contentPadding = androidx.compose.foundation.layout.PaddingValues(16.dp),
                        verticalArrangement = Arrangement.spacedBy(12.dp),
                    ) {
                        items(
                            items = uiState.accounts,
                            key = TotpAccount::id,
                        ) { account ->
                            val expandedState = uiState.expandedAccount
                                ?.takeIf { it.accountId == account.id }
                            AccountCard(
                                account = account,
                                expandedState = expandedState,
                                onToggle = { onToggleAccount(account.id) },
                                onCopyCode = { code ->
                                    copySensitiveCode(context, code)
                                    coroutineScope.launch {
                                        snackbarHostState.showSnackbar("Code copied")
                                    }
                                },
                                onEdit = { onEditAccount(account.id) },
                            )
                        }
                    }
                }
            }

            if (addMenuExpanded) {
                Box(
                    modifier = Modifier
                        .fillMaxSize()
                        .padding(innerPadding)
                        .background(MaterialTheme.colorScheme.scrim.copy(alpha = 0.12f))
                        .clickable { addMenuExpanded = false },
                )
            }
        }
    }
}

@Composable
private fun AddAccountMenu(
    expanded: Boolean,
    onToggle: () -> Unit,
    onScanQrCode: () -> Unit,
    onSelect: (AddAccountMethod) -> Unit,
) {
    Column(
        horizontalAlignment = Alignment.End,
        verticalArrangement = Arrangement.spacedBy(12.dp),
    ) {
        AnimatedVisibility(visible = expanded) {
            Column(
                horizontalAlignment = Alignment.End,
                verticalArrangement = Arrangement.spacedBy(12.dp),
            ) {
                AddAccountAction(
                    label = "Scan QR code",
                    iconResId = R.drawable.ic_qr_code_scanner,
                    onClick = onScanQrCode,
                )
                AddAccountAction(
                    label = "Paste URI",
                    iconResId = R.drawable.ic_content_paste,
                    onClick = { onSelect(AddAccountMethod.OTP_AUTH_URI) },
                )
                AddAccountAction(
                    label = "Setup key",
                    iconResId = R.drawable.ic_key,
                    onClick = { onSelect(AddAccountMethod.SETUP_KEY) },
                )
            }
        }
        FloatingActionButton(onClick = onToggle) {
            Icon(
                painter = painterResource(
                    if (expanded) R.drawable.ic_close else R.drawable.ic_add,
                ),
                contentDescription = if (expanded) "Close add menu" else "Add account",
            )
        }
    }
}

@Composable
private fun AddAccountAction(
    label: String,
    iconResId: Int,
    onClick: () -> Unit,
) {
    Row(
        horizontalArrangement = Arrangement.spacedBy(12.dp),
        verticalAlignment = Alignment.CenterVertically,
    ) {
        Surface(
            onClick = onClick,
            shape = MaterialTheme.shapes.small,
            tonalElevation = 3.dp,
            shadowElevation = 2.dp,
        ) {
            Text(
                text = label,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 8.dp),
                style = MaterialTheme.typography.labelLarge,
            )
        }
        SmallFloatingActionButton(onClick = onClick) {
            Icon(
                painter = painterResource(iconResId),
                contentDescription = label,
            )
        }
    }
}

@Composable
private fun EmptyAccountList(modifier: Modifier = Modifier) {
    Box(modifier = modifier, contentAlignment = Alignment.Center) {
        Column(
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(8.dp),
        ) {
            Text("No accounts yet", style = MaterialTheme.typography.titleMedium)
            Text(
                "Add an account to generate TOTP codes.",
                color = MaterialTheme.colorScheme.onSurfaceVariant,
            )
        }
    }
}

@Composable
private fun AccountCard(
    account: TotpAccount,
    expandedState: ExpandedAccountUiState?,
    onToggle: () -> Unit,
    onCopyCode: (String) -> Unit,
    onEdit: () -> Unit,
    modifier: Modifier = Modifier,
) {
    val expanded = expandedState != null
    Card(
        onClick = onToggle,
        modifier = modifier.fillMaxWidth(),
    ) {
        Column(modifier = Modifier.padding(16.dp)) {
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.Top,
            ) {
                Column(modifier = Modifier.weight(1f)) {
                    Text(
                        text = account.issuer ?: account.accountName,
                        style = MaterialTheme.typography.titleMedium,
                        fontWeight = FontWeight.SemiBold,
                    )
                    if (account.issuer != null) {
                        Text(
                            text = account.accountName,
                            style = MaterialTheme.typography.bodyMedium,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
                if (expanded) {
                    TextButton(onClick = onEdit) {
                        Text("Edit")
                    }
                } else {
                    Text(
                        text = "Show code",
                        style = MaterialTheme.typography.labelLarge,
                        color = MaterialTheme.colorScheme.primary,
                    )
                }
            }

            if (expandedState != null) {
                Spacer(Modifier.height(12.dp))
                if (expandedState.isLoading) {
                    CircularProgressIndicator(
                        modifier = Modifier
                            .align(Alignment.CenterHorizontally)
                            .size(28.dp),
                    )
                } else {
                    val code = expandedState.code
                    if (code != null) {
                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically,
                        ) {
                            Text(
                                text = formatCode(code),
                                style = MaterialTheme.typography.headlineMedium,
                                fontFamily = FontFamily.Monospace,
                                letterSpacing = 2.sp,
                            )
                            Button(onClick = { onCopyCode(code) }) {
                                Text("Copy")
                            }
                        }
                    } else {
                        Text(
                            "Code unavailable",
                            color = MaterialTheme.colorScheme.error,
                        )
                    }

                    val remainingSeconds = expandedState.remainingSeconds
                    if (remainingSeconds != null) {
                        val targetProgress =
                            (remainingSeconds.toFloat() / account.periodSeconds)
                                .coerceIn(0f, 1f)
                        val animatedProgress by animateFloatAsState(
                            targetValue = targetProgress,
                            animationSpec = if (remainingSeconds == account.periodSeconds) {
                                tween(
                                    durationMillis = 300,
                                    easing = FastOutSlowInEasing,
                                )
                            } else {
                                tween(durationMillis = 1_000, easing = LinearEasing)
                            },
                            label = "TOTP countdown progress",
                        )

                        Spacer(Modifier.height(12.dp))
                        LinearProgressIndicator(
                            progress = { animatedProgress },
                            modifier = Modifier.fillMaxWidth(),
                        )
                        Spacer(Modifier.height(8.dp))
                        Text(
                            text = "$remainingSeconds seconds remaining",
                            style = MaterialTheme.typography.bodySmall,
                            color = MaterialTheme.colorScheme.onSurfaceVariant,
                        )
                    }
                }
            }
        }
    }
}

private fun formatCode(code: String): String {
    val groupSize = if (code.length == 8) 4 else 3
    return code.chunked(groupSize).joinToString(" ")
}

private fun copySensitiveCode(
    context: Context,
    code: String,
) {
    val clipboard = context.getSystemService(ClipboardManager::class.java)
    val clip = ClipData.newPlainText("TOTP code", code)
    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
        clip.description.extras = PersistableBundle().apply {
            putBoolean(ClipDescription.EXTRA_IS_SENSITIVE, true)
        }
    }
    clipboard.setPrimaryClip(clip)
}
