package com.momoterminal.data.local

import android.content.Context
import androidx.room.Room
import androidx.test.core.app.ApplicationProvider
import androidx.test.ext.junit.runners.AndroidJUnit4
import com.google.common.truth.Truth.assertThat
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.data.local.entity.TransactionEntity
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.test.runTest
import org.junit.After
import org.junit.Before
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Instrumented tests for TransactionDao using in-memory database.
 */
@RunWith(AndroidJUnit4::class)
class TransactionDaoTest {

    private lateinit var database: MomoDatabase
    private lateinit var transactionDao: TransactionDao

    @Before
    fun setup() {
        val context = ApplicationProvider.getApplicationContext<Context>()
        database = Room.inMemoryDatabaseBuilder(context, MomoDatabase::class.java)
            .allowMainThreadQueries()
            .build()
        transactionDao = database.transactionDao()
    }

    @After
    fun tearDown() {
        database.close()
    }

    @Test
    fun insertAndRetrieveTransaction() = runTest {
        val transaction = createTestEntity(0)
        
        val id = transactionDao.insert(transaction)
        val retrieved = transactionDao.getRecentTransactions(10).first()
        
        assertThat(id).isGreaterThan(0)
        assertThat(retrieved).hasSize(1)
        assertThat(retrieved[0].amount).isEqualTo(50.0)
    }

    @Test
    fun insertMultipleTransactions() = runTest {
        val transactions = listOf(
            createTestEntity(0, 50.0),
            createTestEntity(0, 100.0),
            createTestEntity(0, 150.0)
        )
        
        transactions.forEach { transactionDao.insert(it) }
        val retrieved = transactionDao.getRecentTransactions(10).first()
        
        assertThat(retrieved).hasSize(3)
    }

    @Test
    fun getRecentTransactionsLimitsResults() = runTest {
        repeat(20) { i ->
            transactionDao.insert(createTestEntity(0, i.toDouble()))
        }
        
        val retrieved = transactionDao.getRecentTransactions(10).first()
        
        assertThat(retrieved).hasSize(10)
    }

    @Test
    fun getRecentTransactionsOrderedByTimestamp() = runTest {
        val older = createTestEntity(0, 50.0).copy(timestamp = 1000L)
        val newer = createTestEntity(0, 100.0).copy(timestamp = 2000L)
        
        transactionDao.insert(older)
        transactionDao.insert(newer)
        
        val retrieved = transactionDao.getRecentTransactions(10).first()
        
        assertThat(retrieved[0].timestamp).isGreaterThan(retrieved[1].timestamp)
    }

    @Test
    fun getPendingTransactionsReturnsPendingOnly() = runTest {
        transactionDao.insert(createTestEntity(0).copy(status = "PENDING"))
        transactionDao.insert(createTestEntity(0).copy(status = "SENT"))
        transactionDao.insert(createTestEntity(0).copy(status = "PENDING"))
        transactionDao.insert(createTestEntity(0).copy(status = "FAILED"))
        
        val pending = transactionDao.getPendingTransactions()
        
        assertThat(pending).hasSize(2)
        assertThat(pending.all { it.status == "PENDING" }).isTrue()
    }

    @Test
    fun getPendingCountReturnsCorrectCount() = runTest {
        transactionDao.insert(createTestEntity(0).copy(status = "PENDING"))
        transactionDao.insert(createTestEntity(0).copy(status = "SENT"))
        transactionDao.insert(createTestEntity(0).copy(status = "PENDING"))
        
        val count = transactionDao.getPendingCount().first()
        
        assertThat(count).isEqualTo(2)
    }

    @Test
    fun updateStatusChangesTransactionStatus() = runTest {
        val id = transactionDao.insert(createTestEntity(0).copy(status = "PENDING"))
        
        transactionDao.updateStatus(id, "SENT")
        
        val retrieved = transactionDao.getRecentTransactions(10).first()
        assertThat(retrieved[0].status).isEqualTo("SENT")
    }

    @Test
    fun deleteOldTransactionsRemovesOldRecords() = runTest {
        val oldTimestamp = System.currentTimeMillis() - 100_000L
        val newTimestamp = System.currentTimeMillis()
        
        transactionDao.insert(createTestEntity(0).copy(timestamp = oldTimestamp))
        transactionDao.insert(createTestEntity(0).copy(timestamp = oldTimestamp))
        transactionDao.insert(createTestEntity(0).copy(timestamp = newTimestamp))
        
        val threshold = System.currentTimeMillis() - 50_000L
        val deleted = transactionDao.deleteOldTransactions(threshold)
        val remaining = transactionDao.getRecentTransactions(10).first()
        
        assertThat(deleted).isEqualTo(2)
        assertThat(remaining).hasSize(1)
    }

    @Test
    fun getByIdReturnsCorrectTransaction() = runTest {
        val transaction = createTestEntity(0, 99.99)
        val id = transactionDao.insert(transaction)
        
        val retrieved = transactionDao.getById(id)
        
        assertThat(retrieved).isNotNull()
        assertThat(retrieved?.amount).isEqualTo(99.99)
    }

    @Test
    fun getByIdReturnsNullForNonExistent() = runTest {
        val retrieved = transactionDao.getById(999L)
        
        assertThat(retrieved).isNull()
    }

    @Test
    fun deleteTransactionRemovesRecord() = runTest {
        val transaction = createTestEntity(0)
        val id = transactionDao.insert(transaction)
        
        transactionDao.delete(transactionDao.getById(id)!!)
        val retrieved = transactionDao.getById(id)
        
        assertThat(retrieved).isNull()
    }

    @Test
    fun transactionsFlowUpdatesOnChange() = runTest {
        val flow = transactionDao.getRecentTransactions(10)
        
        // Initial state
        var transactions = flow.first()
        assertThat(transactions).isEmpty()
        
        // Insert transaction
        transactionDao.insert(createTestEntity(0))
        transactions = flow.first()
        assertThat(transactions).hasSize(1)
    }

    @Test
    fun pendingCountFlowUpdatesOnChange() = runTest {
        val flow = transactionDao.getPendingCount()
        
        // Initial state
        var count = flow.first()
        assertThat(count).isEqualTo(0)
        
        // Insert pending transaction
        transactionDao.insert(createTestEntity(0).copy(status = "PENDING"))
        count = flow.first()
        assertThat(count).isEqualTo(1)
        
        // Update to sent
        val all = transactionDao.getRecentTransactions(10).first()
        transactionDao.updateStatus(all[0].id, "SENT")
        count = flow.first()
        assertThat(count).isEqualTo(0)
    }

    private fun createTestEntity(id: Long, amount: Double = 50.0): TransactionEntity {
        return TransactionEntity(
            id = id,
            amount = amount,
            senderPhone = "0244123456",
            transactionId = "TX${System.currentTimeMillis()}",
            provider = "MTN",
            timestamp = System.currentTimeMillis(),
            status = "PENDING",
            rawMessage = "Test transaction"
        )
    }
}
