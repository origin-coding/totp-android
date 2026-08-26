package io.github.origincoding.totp.security

import androidx.lifecycle.viewModelScope
import io.github.origincoding.totp.account.MainDispatcherRule
import io.github.origincoding.totp.data.settings.AppSettingsRepository
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import org.junit.Assert.assertFalse
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test

@OptIn(ExperimentalCoroutinesApi::class)
class AppLockViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `enabling app lock keeps the authenticated session unlocked`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAppSettingsRepository(initiallyEnabled = false)
            val viewModel = AppLockViewModel(repository)
            runCurrent()

            assertFalse(viewModel.uiState.value.isEnabled)
            assertTrue(viewModel.beginAuthentication(AuthenticationPurpose.ENABLE_APP_LOCK))
            viewModel.onAuthenticationSucceeded()
            runCurrent()

            assertTrue(repository.enabled.value)
            assertTrue(viewModel.uiState.value.isEnabled)
            assertFalse(viewModel.uiState.value.isLocked)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `enabled app lock relocks after thirty seconds in the background`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeAppSettingsRepository(initiallyEnabled = true)
            val viewModel = AppLockViewModel(
                settingsRepository = repository,
                relockTimeoutMillis = 30_000L,
            )
            runCurrent()

            assertTrue(viewModel.uiState.value.isLocked)
            viewModel.beginAuthentication(AuthenticationPurpose.UNLOCK_APP)
            viewModel.onAuthenticationSucceeded()
            assertFalse(viewModel.uiState.value.isLocked)

            viewModel.onAppBackgrounded(elapsedRealtimeMillis = 10_000L)
            viewModel.onAppForegrounded(elapsedRealtimeMillis = 39_999L)
            assertFalse(viewModel.uiState.value.isLocked)

            viewModel.onAppBackgrounded(elapsedRealtimeMillis = 50_000L)
            viewModel.onAppForegrounded(elapsedRealtimeMillis = 80_000L)
            assertTrue(viewModel.uiState.value.isLocked)
            viewModel.viewModelScope.cancel()
        }
}

private class FakeAppSettingsRepository(initiallyEnabled: Boolean) : AppSettingsRepository {
    val enabled = MutableStateFlow(initiallyEnabled)

    override val appLockEnabled: Flow<Boolean> = enabled

    override suspend fun setAppLockEnabled(enabled: Boolean) {
        this.enabled.value = enabled
    }
}
