package io.github.origincoding.totp.account

import androidx.lifecycle.viewModelScope
import io.github.origincoding.totp.core.OtpAlgorithm
import io.github.origincoding.totp.core.OtpAuthUri
import io.github.origincoding.totp.core.OtpDigits
import io.github.origincoding.totp.data.account.TotpAccount
import io.github.origincoding.totp.data.account.TotpAccountRepository
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.cancel
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.TestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runCurrent
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.Assert.assertEquals
import org.junit.Assert.assertNull
import org.junit.Assert.assertTrue
import org.junit.Rule
import org.junit.Test
import org.junit.rules.TestWatcher
import org.junit.runner.Description
import kotlin.time.Duration.Companion.milliseconds

@OptIn(ExperimentalCoroutinesApi::class)
class AccountListViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `only expanded account generates and refreshes a code`() =
        runTest(mainDispatcherRule.testDispatcher) {
            var currentTime = 59L
            val repository = FakeTotpAccountRepository(
                initialAccounts = listOf(account(id = 1), account(id = 2)),
            )
            val viewModel = AccountListViewModel(
                repository = repository,
                currentTimeSeconds = { currentTime },
            )
            runCurrent()
            viewModel.onScreenStarted()

            viewModel.toggleAccount(1)
            runCurrent()
            assertEquals(listOf(1L), repository.generatedAccountIds)
            assertEquals(1L, viewModel.uiState.value.expandedAccount?.remainingSeconds)

            viewModel.toggleAccount(2)
            runCurrent()
            assertEquals(listOf(1L, 2L), repository.generatedAccountIds)
            assertEquals(2L, viewModel.uiState.value.expandedAccount?.accountId)

            currentTime = 60L
            advanceTimeBy(1_000L.milliseconds)
            runCurrent()
            assertEquals(listOf(1L, 2L, 2L), repository.generatedAccountIds)
            assertEquals(30L, viewModel.uiState.value.expandedAccount?.remainingSeconds)

            viewModel.onScreenStopped()
            currentTime = 90L
            advanceTimeBy(1_000L.milliseconds)
            runCurrent()
            assertEquals(listOf(1L, 2L, 2L), repository.generatedAccountIds)

            viewModel.onScreenStarted()
            runCurrent()
            assertEquals(listOf(1L, 2L, 2L, 2L), repository.generatedAccountIds)

            viewModel.toggleAccount(2)
            assertNull(viewModel.uiState.value.expandedAccount)
            viewModel.viewModelScope.cancel()
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class AccountEditorViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `TOTP URI populates and saves the add form`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeTotpAccountRepository()
            val viewModel = AccountEditorViewModel(repository, accountId = null)
            viewModel.updateOtpAuthUri(
                "otpauth://totp/Example:user@example.com" +
                    "?secret=JBSWY3DPEHPK3PXP&issuer=Example" +
                    "&algorithm=SHA256&digits=8&period=45",
            )

            viewModel.applyOtpAuthUri()
            val populatedState = viewModel.uiState.value
            assertTrue(populatedState.isOtpAuthUriApplied)
            assertEquals("user@example.com", populatedState.accountName)
            assertEquals("Example", populatedState.issuer)
            assertEquals(OtpAlgorithm.SHA256, populatedState.algorithm)
            assertEquals(OtpDigits.EIGHT, populatedState.digits)
            assertEquals("45", populatedState.periodSeconds)

            viewModel.updateOtpAuthUri(populatedState.otpAuthUri)
            assertTrue(!viewModel.uiState.value.isOtpAuthUriApplied)
            viewModel.applyOtpAuthUri()

            viewModel.save()
            runCurrent()
            val savedAccount = repository.accountsState.value.single()
            assertEquals("user@example.com", savedAccount.accountName)
            assertEquals(AccountEditorCompletion.SAVED, viewModel.uiState.value.completion)
            assertTrue(viewModel.uiState.value.secret.isEmpty())
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `existing account metadata can be loaded and saved`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeTotpAccountRepository(
                initialAccounts = listOf(account(id = 7)),
            )
            val viewModel = AccountEditorViewModel(repository, accountId = 7)
            runCurrent()

            assertEquals("account-7", viewModel.uiState.value.accountName)
            assertEquals("Example", viewModel.uiState.value.issuer)

            viewModel.updateAccountName(" updated@example.com ")
            viewModel.updateIssuer(" ")
            viewModel.updateAlgorithm(OtpAlgorithm.SHA512)
            viewModel.updateDigits(OtpDigits.EIGHT)
            viewModel.updatePeriodSeconds("60")
            viewModel.save()
            runCurrent()

            assertEquals(
                TotpAccount(
                    id = 7,
                    accountName = "updated@example.com",
                    issuer = null,
                    algorithm = OtpAlgorithm.SHA512,
                    digits = OtpDigits.EIGHT,
                    periodSeconds = 60,
                ),
                repository.accountsState.value.single(),
            )
            assertEquals(AccountEditorCompletion.SAVED, viewModel.uiState.value.completion)
            viewModel.viewModelScope.cancel()
        }

    @Test
    fun `existing account can be deleted`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeTotpAccountRepository(
                initialAccounts = listOf(account(id = 3)),
            )
            val viewModel = AccountEditorViewModel(repository, accountId = 3)
            runCurrent()

            viewModel.delete()
            runCurrent()

            assertTrue(repository.accountsState.value.isEmpty())
            assertEquals(AccountEditorCompletion.DELETED, viewModel.uiState.value.completion)
            viewModel.viewModelScope.cancel()
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class ReplaceSecretViewModelTest {
    @get:Rule
    val mainDispatcherRule = MainDispatcherRule()

    @Test
    fun `secret replacement trims input and clears it after success`() =
        runTest(mainDispatcherRule.testDispatcher) {
            val repository = FakeTotpAccountRepository(
                initialAccounts = listOf(account(id = 5)),
            )
            val viewModel = ReplaceSecretViewModel(repository, accountId = 5)

            viewModel.updateSecret(" JBSWY3DPEHPK3PXP ")
            viewModel.replaceSecret()
            runCurrent()

            assertEquals(
                listOf(5L to "JBSWY3DPEHPK3PXP"),
                repository.replacedSecrets,
            )
            assertTrue(viewModel.uiState.value.isComplete)
            assertTrue(viewModel.uiState.value.secret.isEmpty())
            viewModel.viewModelScope.cancel()
        }
}

@OptIn(ExperimentalCoroutinesApi::class)
class MainDispatcherRule(
    val testDispatcher: TestDispatcher = StandardTestDispatcher(),
) : TestWatcher() {
    override fun starting(description: Description) {
        Dispatchers.setMain(testDispatcher)
    }

    override fun finished(description: Description) {
        Dispatchers.resetMain()
    }
}

private class FakeTotpAccountRepository(
    initialAccounts: List<TotpAccount> = emptyList(),
) : TotpAccountRepository {
    val accountsState = MutableStateFlow(initialAccounts)
    val generatedAccountIds = mutableListOf<Long>()
    val replacedSecrets = mutableListOf<Pair<Long, String>>()
    private var nextId = (initialAccounts.maxOfOrNull(TotpAccount::id) ?: 0L) + 1L

    override val accounts: Flow<List<TotpAccount>> = accountsState

    override suspend fun add(account: OtpAuthUri.Totp): Long {
        val id = nextId++
        accountsState.value += TotpAccount(
            id = id,
            accountName = account.accountName,
            issuer = account.issuer,
            algorithm = account.algorithm,
            digits = account.digits,
            periodSeconds = account.periodSeconds,
        )
        return id
    }

    override suspend fun update(account: TotpAccount): Boolean {
        val index = accountsState.value.indexOfFirst { it.id == account.id }
        if (index < 0) return false
        accountsState.value = accountsState.value.toMutableList().also { it[index] = account }
        return true
    }

    override suspend fun replaceSecret(
        id: Long,
        secret: String,
    ): Boolean {
        if (accountsState.value.none { it.id == id }) return false
        replacedSecrets += id to secret
        return true
    }

    override suspend fun delete(id: Long): Boolean {
        val previousSize = accountsState.value.size
        accountsState.value = accountsState.value.filterNot { it.id == id }
        return accountsState.value.size != previousSize
    }

    override suspend fun generateCode(
        id: Long,
        unixTimeSeconds: Long,
    ): String? {
        val account = accountsState.value.firstOrNull { it.id == id } ?: return null
        generatedAccountIds += id
        return (unixTimeSeconds / account.periodSeconds).toString().padStart(6, '0')
    }
}

private fun account(id: Long): TotpAccount =
    TotpAccount(
        id = id,
        accountName = "account-$id",
        issuer = "Example",
        algorithm = OtpAlgorithm.SHA1,
        digits = OtpDigits.SIX,
        periodSeconds = 30,
    )
