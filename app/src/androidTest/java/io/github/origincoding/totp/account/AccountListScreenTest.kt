package io.github.origincoding.totp.account

import androidx.compose.material3.MaterialTheme
import androidx.compose.ui.test.assertIsDisplayed
import androidx.compose.ui.test.junit4.v2.createComposeRule
import androidx.compose.ui.test.onNodeWithContentDescription
import androidx.compose.ui.test.onNodeWithText
import androidx.compose.ui.test.performClick
import androidx.test.ext.junit.runners.AndroidJUnit4
import org.junit.Assert.assertEquals
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

@RunWith(AndroidJUnit4::class)
class AccountListScreenTest {
    @get:Rule
    val composeRule = createComposeRule()

    @Test
    fun addMenuExposesMethodsAndSelectsSetupKey() {
        var selectedMethod: AddAccountMethod? = null
        composeRule.setContent {
            MaterialTheme {
                AccountListScreen(
                    uiState = AccountListUiState(isLoading = false),
                    onToggleAccount = {},
                    onAddAccount = { selectedMethod = it },
                    onScanQrCode = {},
                    onOpenSettings = {},
                    onEditAccount = {},
                    onClearError = {},
                )
            }
        }

        composeRule.onNodeWithContentDescription("Add account").performClick()
        composeRule.onNodeWithText("Scan QR code").assertIsDisplayed()
        composeRule.onNodeWithText("Paste URI").assertIsDisplayed()
        composeRule.onNodeWithText("Setup key").assertIsDisplayed().performClick()

        composeRule.runOnIdle {
            assertEquals(AddAccountMethod.SETUP_KEY, selectedMethod)
        }
    }
}
