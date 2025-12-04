package com.momoterminal.feature.charts

import com.momoterminal.core.database.dao.TransactionDao
import com.patrykandpatrick.vico.core.cartesian.data.CartesianChartModelProducer
import com.patrykandpatrick.vico.core.cartesian.data.columnSeries
import com.patrykandpatrick.vico.core.cartesian.data.lineSeries
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext
import java.text.SimpleDateFormat
import java.util.Calendar
import java.util.Date
import java.util.Locale
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Data class representing daily transaction aggregates.
 */
data class DailyTransaction(
    val date: Date,
    val income: Double,
    val expense: Double,
    val count: Int
) {
    val formattedDate: String
        get() = SimpleDateFormat("MMM dd", Locale.getDefault()).format(date)
    
    val dayOfWeek: String
        get() = SimpleDateFormat("EEE", Locale.getDefault()).format(date)
    
    val net: Double
        get() = income - expense
}

/**
 * Data class representing a transaction summary for a period.
 */
data class TransactionSummary(
    val totalIncome: Double,
    val totalExpense: Double,
    val transactionCount: Int,
    val periodLabel: String
) {
    val netAmount: Double
        get() = totalIncome - totalExpense
    
    val isPositive: Boolean
        get() = netAmount >= 0
}

/**
 * Data class representing transaction breakdown by category.
 * Note: Amount is in main currency units (GHS) for display purposes.
 * This is aggregated data computed from transactions stored in pesewas.
 */
data class CategoryBreakdown(
    val category: String,
    val amount: Double,
    val percentage: Float,
    val count: Int,
    val color: Long
)

/**
 * Provider class for chart data and model producers.
 * Generates sample data for demonstration and can be extended
 * to use real transaction data from the database.
 */
@Singleton
class ChartDataProvider @Inject constructor(
    private val transactionDao: TransactionDao
) {
    
    /**
     * Get transaction summary for the specified number of days.
     */
    suspend fun getTransactionSummary(days: Int = 7): TransactionSummary {
        return withContext(Dispatchers.IO) {
            // In a real implementation, this would query the database
            // For now, return sample data
            TransactionSummary(
                totalIncome = 125000.0,
                totalExpense = 45000.0,
                transactionCount = 42,
                periodLabel = "Last $days days"
            )
        }
    }
    
    /**
     * Get daily transactions for the specified number of days.
     */
    suspend fun getDailyTransactions(days: Int = 7): List<DailyTransaction> {
        return withContext(Dispatchers.IO) {
            // Generate sample data for demonstration
            val calendar = Calendar.getInstance()
            val transactions = mutableListOf<DailyTransaction>()
            
            // Sample income/expense patterns
            val incomeBase = listOf(15000.0, 22000.0, 18000.0, 25000.0, 12000.0, 30000.0, 20000.0)
            val expenseBase = listOf(5000.0, 8000.0, 6500.0, 7000.0, 4500.0, 9000.0, 5500.0)
            val countBase = listOf(5, 8, 6, 9, 4, 12, 7)
            
            for (i in days - 1 downTo 0) {
                calendar.time = Date()
                calendar.add(Calendar.DAY_OF_YEAR, -i)
                
                val index = (days - 1 - i) % incomeBase.size
                transactions.add(
                    DailyTransaction(
                        date = calendar.time,
                        income = incomeBase[index],
                        expense = expenseBase[index],
                        count = countBase[index]
                    )
                )
            }
            
            transactions
        }
    }
    
    /**
     * Get category breakdown for transactions.
     */
    suspend fun getCategoryBreakdown(): List<CategoryBreakdown> {
        return withContext(Dispatchers.IO) {
            listOf(
                CategoryBreakdown(
                    category = "Payments",
                    amount = 65000.0,
                    percentage = 0.52f,
                    count = 18,
                    color = 0xFF4CAF50
                ),
                CategoryBreakdown(
                    category = "Transfers",
                    amount = 35000.0,
                    percentage = 0.28f,
                    count = 12,
                    color = 0xFF2196F3
                ),
                CategoryBreakdown(
                    category = "Withdrawals",
                    amount = 15000.0,
                    percentage = 0.12f,
                    count = 8,
                    color = 0xFFFF9800
                ),
                CategoryBreakdown(
                    category = "Bills",
                    amount = 10000.0,
                    percentage = 0.08f,
                    count = 4,
                    color = 0xFF9C27B0
                )
            )
        }
    }
    
    /**
     * Build a CartesianChartModelProducer for income/expense line chart.
     */
    suspend fun buildIncomeExpenseLineChartProducer(
        days: Int = 7
    ): CartesianChartModelProducer {
        val dailyTransactions = getDailyTransactions(days)
        val producer = CartesianChartModelProducer()
        
        producer.runTransaction {
            lineSeries {
                // Income series
                series(dailyTransactions.map { it.income })
                // Expense series  
                series(dailyTransactions.map { it.expense })
            }
        }
        
        return producer
    }
    
    /**
     * Build a CartesianChartModelProducer for transaction volume column chart.
     */
    suspend fun buildTransactionVolumeColumnChartProducer(
        days: Int = 7
    ): CartesianChartModelProducer {
        val dailyTransactions = getDailyTransactions(days)
        val producer = CartesianChartModelProducer()
        
        producer.runTransaction {
            columnSeries {
                series(dailyTransactions.map { it.count.toDouble() })
            }
        }
        
        return producer
    }
    
    /**
     * Get x-axis labels for the chart.
     */
    suspend fun getChartLabels(days: Int = 7): List<String> {
        val dailyTransactions = getDailyTransactions(days)
        return dailyTransactions.map { it.dayOfWeek }
    }
}
