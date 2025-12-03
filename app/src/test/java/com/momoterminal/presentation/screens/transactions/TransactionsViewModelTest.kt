package com.momoterminal.presentation.screens.transactions

import app.cash.turbine.test
import com.google.common.truth.Truth.assertThat
import com.momoterminal.config.AppConfig
import com.momoterminal.data.local.MomoDatabase
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.data.local.entity.TransactionEntity
import com.momoterminal.sync.SyncManager
import io.mockk.every
import io.mockk.mockk
import io.mockk.verify
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.ExperimentalCoroutinesApi
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.flowOf
import kotlinx.coroutines.test.StandardTestDispatcher
import kotlinx.coroutines.test.advanceTimeBy
import kotlinx.coroutines.test.resetMain
import kotlinx.coroutines.test.runTest
import kotlinx.coroutines.test.setMain
import org.junit.After
import org.junit.Before
import org.junit.Test

/**
 * Unit tests for TransactionsViewModel.
 */
@OptIn(ExperimentalCoroutinesApi::class)
class TransactionsViewModelTest {

    private lateinit var database: MomoDatabase
    private lateinit var transactionDao: TransactionDao
    private lateinit var syncManager: SyncManager
    private lateinit var appConfig: AppConfig
    private lateinit var viewModel: TransactionsViewModel
    private val testDispatcher = StandardTestDispatcher()

    private val transactionsFlow = MutableStateFlow<List<TransactionEntity>>(emptyList())
    private val pendingCountFlow = MutableStateFlow(0)

    @Before
    fun setup() {
        Dispatchers.setMain(testDispatcher)
        
        transactionDao = mockk(relaxed = true)
        database = mockk {
            every { transactionDao() } returns transactionDao
        }
        syncManager = mockk(relaxed = true)
        appConfig = mockk(relaxed = true) {
            every { getCurrency() } returns "RWF"
        }
        
        every { transactionDao.getRecentTransactions() } returns transactionsFlow
        every { transactionDao.getPendingCount() } returns pendingCountFlow
        
        viewModel = TransactionsViewModel(database, syncManager, appConfig)
    }

    @After
    fun tearDown() {
        Dispatchers.resetMain()
    }

    @Test
    fun `initial state has ALL filter`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.filter).isEqualTo(TransactionsViewModel.TransactionFilter.ALL)
    }

    @Test
    fun `initial state is not refreshing`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
    }

    @Test
    fun `setFilter updates filter`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.setFilter(TransactionsViewModel.TransactionFilter.PENDING)
        assertThat(viewModel.uiState.value.filter).isEqualTo(TransactionsViewModel.TransactionFilter.PENDING)
        
        viewModel.setFilter(TransactionsViewModel.TransactionFilter.SENT)
        assertThat(viewModel.uiState.value.filter).isEqualTo(TransactionsViewModel.TransactionFilter.SENT)
        
        viewModel.setFilter(TransactionsViewModel.TransactionFilter.FAILED)
        assertThat(viewModel.uiState.value.filter).isEqualTo(TransactionsViewModel.TransactionFilter.FAILED)
    }

    @Test
    fun `refresh sets isRefreshing to true then false`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.refresh()
        testDispatcher.scheduler.runCurrent()
        
        // isRefreshing should be true immediately after refresh
        assertThat(viewModel.uiState.value.isRefreshing).isTrue()
        
        // Advance time to complete the refresh delay
        advanceTimeBy(2000)
        testDispatcher.scheduler.advanceUntilIdle()
        
        assertThat(viewModel.uiState.value.isRefreshing).isFalse()
    }

    @Test
    fun `refresh calls syncManager enqueueSyncNow`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.refresh()
        testDispatcher.scheduler.runCurrent()
        
        verify { syncManager.enqueueSyncNow() }
    }

    @Test
    fun `getFilteredTransactions returns all transactions for ALL filter`() {
        val transactions = createTestTransactions()
        
        val filtered = viewModel.getFilteredTransactions(
            transactions,
            TransactionsViewModel.TransactionFilter.ALL
        )
        
        assertThat(filtered).hasSize(4)
    }

    @Test
    fun `getFilteredTransactions returns only pending for PENDING filter`() {
        val transactions = createTestTransactions()
        
        val filtered = viewModel.getFilteredTransactions(
            transactions,
            TransactionsViewModel.TransactionFilter.PENDING
        )
        
        assertThat(filtered).hasSize(2)
        assertThat(filtered.all { it.status == "PENDING" }).isTrue()
    }

    @Test
    fun `getFilteredTransactions returns only sent for SENT filter`() {
        val transactions = createTestTransactions()
        
        val filtered = viewModel.getFilteredTransactions(
            transactions,
            TransactionsViewModel.TransactionFilter.SENT
        )
        
        assertThat(filtered).hasSize(1)
        assertThat(filtered.all { it.status == "SENT" }).isTrue()
    }

    @Test
    fun `getFilteredTransactions returns only failed for FAILED filter`() {
        val transactions = createTestTransactions()
        
        val filtered = viewModel.getFilteredTransactions(
            transactions,
            TransactionsViewModel.TransactionFilter.FAILED
        )
        
        assertThat(filtered).hasSize(1)
        assertThat(filtered.all { it.status == "FAILED" }).isTrue()
    }

    @Test
    fun `transactions flow emits database transactions`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.transactions.test {
            assertThat(awaitItem()).isEmpty()
            
            val newTransactions = createTestTransactions()
            transactionsFlow.value = newTransactions
            
            assertThat(awaitItem()).hasSize(4)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `pendingCount updates from database`() = runTest {
        testDispatcher.scheduler.advanceUntilIdle()
        
        viewModel.uiState.test {
            assertThat(awaitItem().pendingCount).isEqualTo(0)
            
            pendingCountFlow.value = 5
            assertThat(awaitItem().pendingCount).isEqualTo(5)
            
            pendingCountFlow.value = 10
            assertThat(awaitItem().pendingCount).isEqualTo(10)
            
            cancelAndIgnoreRemainingEvents()
        }
    }

    @Test
    fun `getFilteredTransactions returns empty list for empty input`() {
        val filtered = viewModel.getFilteredTransactions(
            emptyList(),
            TransactionsViewModel.TransactionFilter.ALL
        )
        
        assertThat(filtered).isEmpty()
    }

    @Test
    fun `getFilteredTransactions handles no matching filter`() {
        val transactions = listOf(
            createTransactionEntity(1, "PENDING")
        )
        
        val filtered = viewModel.getFilteredTransactions(
            transactions,
            TransactionsViewModel.TransactionFilter.SENT
        )
        
        assertThat(filtered).isEmpty()
    }

    private fun createTestTransactions(): List<TransactionEntity> {
        return listOf(
            createTransactionEntity(1, "PENDING"),
            createTransactionEntity(2, "PENDING"),
            createTransactionEntity(3, "SENT"),
            createTransactionEntity(4, "FAILED")
        )
    }

    private fun createTransactionEntity(id: Long, status: String): TransactionEntity {
        return TransactionEntity(
            id = id,
            sender = "0244123456",
            body = "Test message $id",
            timestamp = System.currentTimeMillis(),
            status = status,
            amount = 50.0,
            transactionId = "TX$id"
        )
    }
}
