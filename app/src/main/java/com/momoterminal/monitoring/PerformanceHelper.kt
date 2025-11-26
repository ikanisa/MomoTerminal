package com.momoterminal.monitoring

import com.google.firebase.perf.FirebasePerformance
import com.google.firebase.perf.metrics.Trace
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Helper class for Firebase Performance Monitoring integration.
 * Provides methods for creating and managing custom traces.
 */
@Singleton
class PerformanceHelper @Inject constructor() {

    private val performance: FirebasePerformance by lazy {
        FirebasePerformance.getInstance()
    }

    // Active traces cache
    private val activeTraces = mutableMapOf<String, Trace>()

    /**
     * Start a new custom trace.
     * @param traceName The name of the trace
     * @return The started Trace object
     */
    fun startTrace(traceName: String): Trace {
        val trace = performance.newTrace(traceName)
        trace.start()
        activeTraces[traceName] = trace
        return trace
    }

    /**
     * Stop an active trace.
     * @param traceName The name of the trace to stop
     */
    fun stopTrace(traceName: String) {
        activeTraces.remove(traceName)?.stop()
    }

    /**
     * Get an active trace by name.
     * @param traceName The name of the trace
     * @return The Trace object or null if not found
     */
    fun getTrace(traceName: String): Trace? {
        return activeTraces[traceName]
    }

    /**
     * Add a metric to an active trace.
     * @param traceName The name of the trace
     * @param metricName The name of the metric
     * @param value The metric value
     */
    fun putMetric(traceName: String, metricName: String, value: Long) {
        activeTraces[traceName]?.putMetric(metricName, value)
    }

    /**
     * Increment a metric on an active trace.
     * @param traceName The name of the trace
     * @param metricName The name of the metric
     * @param incrementBy The amount to increment
     */
    fun incrementMetric(traceName: String, metricName: String, incrementBy: Long = 1) {
        activeTraces[traceName]?.incrementMetric(metricName, incrementBy)
    }

    /**
     * Add an attribute to an active trace.
     * @param traceName The name of the trace
     * @param attributeName The attribute name
     * @param attributeValue The attribute value
     */
    fun putAttribute(traceName: String, attributeName: String, attributeValue: String) {
        activeTraces[traceName]?.putAttribute(attributeName, attributeValue)
    }

    // ==================== NFC Transaction Traces ====================

    /**
     * Start tracking an NFC transaction.
     * @param amount The payment amount
     * @param provider The mobile money provider
     * @return The NFC transaction trace
     */
    fun startNfcTransaction(amount: String, provider: String): Trace {
        val trace = startTrace(TRACE_NFC_TRANSACTION)
        trace.putAttribute(ATTR_AMOUNT, amount)
        trace.putAttribute(ATTR_PROVIDER, provider)
        return trace
    }

    /**
     * Complete an NFC transaction successfully.
     * @param transactionId The transaction ID
     */
    fun completeNfcTransaction(transactionId: String) {
        getTrace(TRACE_NFC_TRANSACTION)?.apply {
            putAttribute(ATTR_TRANSACTION_ID, transactionId)
            putAttribute(ATTR_STATUS, STATUS_SUCCESS)
        }
        stopTrace(TRACE_NFC_TRANSACTION)
    }

    /**
     * Fail an NFC transaction.
     * @param errorCode The error code
     */
    fun failNfcTransaction(errorCode: String) {
        getTrace(TRACE_NFC_TRANSACTION)?.apply {
            putAttribute(ATTR_ERROR_CODE, errorCode)
            putAttribute(ATTR_STATUS, STATUS_FAILED)
        }
        stopTrace(TRACE_NFC_TRANSACTION)
    }

    /**
     * Cancel an NFC transaction.
     */
    fun cancelNfcTransaction() {
        getTrace(TRACE_NFC_TRANSACTION)?.apply {
            putAttribute(ATTR_STATUS, STATUS_CANCELLED)
        }
        stopTrace(TRACE_NFC_TRANSACTION)
    }

    // ==================== Database Operation Traces ====================

    /**
     * Start tracking a database operation.
     * @param operationType The type of operation (insert, query, update, delete)
     * @param tableName The table name
     * @return The database operation trace
     */
    fun startDbOperation(operationType: String, tableName: String): Trace {
        val trace = startTrace(TRACE_DB_OPERATION)
        trace.putAttribute(ATTR_OPERATION_TYPE, operationType)
        trace.putAttribute(ATTR_TABLE_NAME, tableName)
        return trace
    }

    /**
     * Complete a database operation.
     * @param recordCount Number of records affected
     */
    fun completeDbOperation(recordCount: Int) {
        getTrace(TRACE_DB_OPERATION)?.apply {
            putMetric(METRIC_RECORD_COUNT, recordCount.toLong())
            putAttribute(ATTR_STATUS, STATUS_SUCCESS)
        }
        stopTrace(TRACE_DB_OPERATION)
    }

    /**
     * Fail a database operation.
     * @param errorMessage The error message
     */
    fun failDbOperation(errorMessage: String) {
        getTrace(TRACE_DB_OPERATION)?.apply {
            putAttribute(ATTR_ERROR_MESSAGE, errorMessage.take(100)) // Limit attribute length
            putAttribute(ATTR_STATUS, STATUS_FAILED)
        }
        stopTrace(TRACE_DB_OPERATION)
    }

    // ==================== Sync Operation Traces ====================

    /**
     * Start tracking a sync operation.
     * @param pendingCount Number of pending transactions
     * @return The sync operation trace
     */
    fun startSyncOperation(pendingCount: Int): Trace {
        val trace = startTrace(TRACE_SYNC_OPERATION)
        trace.putMetric(METRIC_PENDING_COUNT, pendingCount.toLong())
        return trace
    }

    /**
     * Update sync progress.
     * @param syncedCount Number of transactions synced so far
     */
    fun updateSyncProgress(syncedCount: Int) {
        getTrace(TRACE_SYNC_OPERATION)?.putMetric(METRIC_SYNCED_COUNT, syncedCount.toLong())
    }

    /**
     * Complete a sync operation.
     * @param syncedCount Total number of transactions synced
     * @param failedCount Number of transactions that failed to sync
     */
    fun completeSyncOperation(syncedCount: Int, failedCount: Int = 0) {
        getTrace(TRACE_SYNC_OPERATION)?.apply {
            putMetric(METRIC_SYNCED_COUNT, syncedCount.toLong())
            putMetric(METRIC_FAILED_COUNT, failedCount.toLong())
            putAttribute(ATTR_STATUS, if (failedCount == 0) STATUS_SUCCESS else STATUS_PARTIAL)
        }
        stopTrace(TRACE_SYNC_OPERATION)
    }

    /**
     * Fail a sync operation.
     * @param errorMessage The error message
     */
    fun failSyncOperation(errorMessage: String) {
        getTrace(TRACE_SYNC_OPERATION)?.apply {
            putAttribute(ATTR_ERROR_MESSAGE, errorMessage.take(100))
            putAttribute(ATTR_STATUS, STATUS_FAILED)
        }
        stopTrace(TRACE_SYNC_OPERATION)
    }

    // ==================== SMS Parsing Traces ====================

    /**
     * Start tracking SMS parsing.
     * @param provider The detected provider
     * @return The SMS parse trace
     */
    fun startSmsParsing(provider: String): Trace {
        val trace = startTrace(TRACE_SMS_PARSE)
        trace.putAttribute(ATTR_PROVIDER, provider)
        return trace
    }

    /**
     * Complete SMS parsing successfully.
     * @param amount The parsed amount
     * @param transactionType The transaction type
     */
    fun completeSmsParsingSuccess(amount: String, transactionType: String) {
        getTrace(TRACE_SMS_PARSE)?.apply {
            putAttribute(ATTR_AMOUNT, amount)
            putAttribute(ATTR_TRANSACTION_TYPE, transactionType)
            putAttribute(ATTR_STATUS, STATUS_SUCCESS)
        }
        stopTrace(TRACE_SMS_PARSE)
    }

    /**
     * Fail SMS parsing.
     * @param reason The failure reason
     */
    fun completeSmsParsingFailed(reason: String) {
        getTrace(TRACE_SMS_PARSE)?.apply {
            putAttribute(ATTR_FAILURE_REASON, reason.take(100))
            putAttribute(ATTR_STATUS, STATUS_FAILED)
        }
        stopTrace(TRACE_SMS_PARSE)
    }

    /**
     * Enable or disable performance collection.
     * @param enabled Whether to enable performance monitoring
     */
    fun setPerformanceCollectionEnabled(enabled: Boolean) {
        performance.isPerformanceCollectionEnabled = enabled
    }

    companion object {
        // Trace Names
        const val TRACE_NFC_TRANSACTION = "nfc_transaction"
        const val TRACE_DB_OPERATION = "db_operation"
        const val TRACE_SYNC_OPERATION = "sync_operation"
        const val TRACE_SMS_PARSE = "sms_parse"

        // Attributes
        private const val ATTR_AMOUNT = "amount"
        private const val ATTR_PROVIDER = "provider"
        private const val ATTR_TRANSACTION_ID = "transaction_id"
        private const val ATTR_STATUS = "status"
        private const val ATTR_ERROR_CODE = "error_code"
        private const val ATTR_ERROR_MESSAGE = "error_message"
        private const val ATTR_OPERATION_TYPE = "operation_type"
        private const val ATTR_TABLE_NAME = "table_name"
        private const val ATTR_TRANSACTION_TYPE = "transaction_type"
        private const val ATTR_FAILURE_REASON = "failure_reason"

        // Metrics
        private const val METRIC_RECORD_COUNT = "record_count"
        private const val METRIC_PENDING_COUNT = "pending_count"
        private const val METRIC_SYNCED_COUNT = "synced_count"
        private const val METRIC_FAILED_COUNT = "failed_count"

        // Status Values
        private const val STATUS_SUCCESS = "success"
        private const val STATUS_FAILED = "failed"
        private const val STATUS_CANCELLED = "cancelled"
        private const val STATUS_PARTIAL = "partial"
    }
}
