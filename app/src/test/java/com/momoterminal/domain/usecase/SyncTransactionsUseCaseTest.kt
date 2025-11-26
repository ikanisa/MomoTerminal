package com.momoterminal.domain.usecase

import com.google.common.truth.Truth.assertThat
import com.momoterminal.domain.repository.TransactionRepository
import com.momoterminal.util.Result
import io.mockk.coEvery
import io.mockk.coVerify
import io.mockk.mockk
import kotlinx.coroutines.test.runTest
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for SyncTransactionsUseCase.
 */
class SyncTransactionsUseCaseTest {

    private lateinit var transactionRepository: TransactionRepository
    private lateinit var useCase: SyncTransactionsUseCase

    @Before
    fun setup() {
        transactionRepository = mockk()
        useCase = SyncTransactionsUseCase(transactionRepository)
    }

    @Test
    fun `invoke returns success when sync is successful`() = runTest {
        // Given
        coEvery { transactionRepository.syncPendingTransactions() } returns Result.Success(5)

        // When
        val result = useCase()

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val successResult = result as Result.Success
        assertThat(successResult.data.syncedCount).isEqualTo(5)
        assertThat(successResult.data.success).isTrue()
    }

    @Test
    fun `invoke returns success with zero count when no transactions to sync`() = runTest {
        // Given
        coEvery { transactionRepository.syncPendingTransactions() } returns Result.Success(0)

        // When
        val result = useCase()

        // Then
        assertThat(result).isInstanceOf(Result.Success::class.java)
        val successResult = result as Result.Success
        assertThat(successResult.data.syncedCount).isEqualTo(0)
        assertThat(successResult.data.success).isTrue()
    }

    @Test
    fun `invoke returns error when repository fails`() = runTest {
        // Given
        val exception = Exception("Network error")
        coEvery { transactionRepository.syncPendingTransactions() } returns Result.Error(exception)

        // When
        val result = useCase()

        // Then
        assertThat(result).isInstanceOf(Result.Error::class.java)
        val errorResult = result as Result.Error
        assertThat(errorResult.exception.message).isEqualTo("Network error")
    }

    @Test
    fun `invoke calls repository syncPendingTransactions`() = runTest {
        // Given
        coEvery { transactionRepository.syncPendingTransactions() } returns Result.Success(1)

        // When
        useCase()

        // Then
        coVerify(exactly = 1) { transactionRepository.syncPendingTransactions() }
    }

    @Test
    fun `SyncResult has correct default values`() {
        val syncResult = SyncResult(
            syncedCount = 10,
            success = true
        )

        assertThat(syncResult.syncedCount).isEqualTo(10)
        assertThat(syncResult.success).isTrue()
        assertThat(syncResult.errorMessage).isNull()
    }

    @Test
    fun `SyncResult can contain error message`() {
        val syncResult = SyncResult(
            syncedCount = 0,
            success = false,
            errorMessage = "Failed to sync"
        )

        assertThat(syncResult.syncedCount).isEqualTo(0)
        assertThat(syncResult.success).isFalse()
        assertThat(syncResult.errorMessage).isEqualTo("Failed to sync")
    }
}
