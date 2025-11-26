package com.momoterminal.ui.webhook

import android.os.Bundle
import android.view.View
import android.widget.AdapterView
import android.widget.ArrayAdapter
import android.widget.ImageButton
import android.widget.Spinner
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.momoterminal.R
import com.momoterminal.data.local.entity.SmsDeliveryLogEntity
import com.momoterminal.data.local.entity.WebhookConfigEntity
import com.momoterminal.data.repository.WebhookRepository
import com.momoterminal.webhook.WebhookDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for viewing SMS delivery logs with filtering capabilities.
 */
@AndroidEntryPoint
class DeliveryLogsActivity : AppCompatActivity() {
    
    @Inject
    lateinit var webhookRepository: WebhookRepository
    
    @Inject
    lateinit var webhookDispatcher: WebhookDispatcher
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var btnBack: ImageButton
    private lateinit var spinnerStatus: Spinner
    private lateinit var spinnerWebhook: Spinner
    private lateinit var tvPendingCount: TextView
    
    private lateinit var adapter: DeliveryLogAdapter
    
    private var webhooks: List<WebhookConfigEntity> = emptyList()
    private var selectedStatus: String? = null
    private var selectedWebhookId: Long? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_delivery_logs)
        
        initViews()
        setupRecyclerView()
        setupFilters()
        setupListeners()
        loadWebhooks()
        observeLogs()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.tvEmptyView)
        btnBack = findViewById(R.id.btnBack)
        spinnerStatus = findViewById(R.id.spinnerStatus)
        spinnerWebhook = findViewById(R.id.spinnerWebhook)
        tvPendingCount = findViewById(R.id.tvPendingCount)
    }
    
    private fun setupRecyclerView() {
        adapter = DeliveryLogAdapter(
            onItemClick = { log -> showLogDetails(log) },
            onRetryClick = { log -> retryDelivery(log) },
            getWebhookName = { webhookId -> 
                webhooks.find { it.id == webhookId }?.name ?: "Unknown"
            }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupFilters() {
        // Status filter
        val statusOptions = listOf("All Status", "Pending", "Sent", "Failed", "Delivered")
        val statusAdapter = ArrayAdapter(this, android.R.layout.simple_spinner_item, statusOptions)
        statusAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
        spinnerStatus.adapter = statusAdapter
        
        spinnerStatus.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
            override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                selectedStatus = when (position) {
                    1 -> SmsDeliveryLogEntity.STATUS_PENDING
                    2 -> SmsDeliveryLogEntity.STATUS_SENT
                    3 -> SmsDeliveryLogEntity.STATUS_FAILED
                    4 -> SmsDeliveryLogEntity.STATUS_DELIVERED
                    else -> null
                }
                observeLogs()
            }
            
            override fun onNothingSelected(parent: AdapterView<*>?) {}
        }
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun loadWebhooks() {
        lifecycleScope.launch {
            webhookRepository.getAllWebhooks().collectLatest { webhookList ->
                webhooks = webhookList
                
                // Update webhook filter spinner
                val webhookOptions = mutableListOf("All Webhooks")
                webhookOptions.addAll(webhookList.map { it.name })
                
                val webhookAdapter = ArrayAdapter(
                    this@DeliveryLogsActivity,
                    android.R.layout.simple_spinner_item,
                    webhookOptions
                )
                webhookAdapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item)
                spinnerWebhook.adapter = webhookAdapter
                
                spinnerWebhook.onItemSelectedListener = object : AdapterView.OnItemSelectedListener {
                    override fun onItemSelected(parent: AdapterView<*>?, view: View?, position: Int, id: Long) {
                        selectedWebhookId = if (position == 0) null else webhookList.getOrNull(position - 1)?.id
                        observeLogs()
                    }
                    
                    override fun onNothingSelected(parent: AdapterView<*>?) {}
                }
            }
        }
    }
    
    private fun observeLogs() {
        lifecycleScope.launch {
            // Get filtered logs based on selection
            val logsFlow = if (selectedWebhookId != null) {
                webhookRepository.getLogsByWebhook(selectedWebhookId!!)
            } else if (selectedStatus != null) {
                webhookRepository.getLogsByStatus(selectedStatus!!)
            } else {
                webhookRepository.getRecentDeliveryLogs(100)
            }
            
            logsFlow.collectLatest { logs ->
                // Apply additional filter if both are selected
                val filteredLogs = logs.filter { log ->
                    val statusMatch = selectedStatus == null || log.status == selectedStatus
                    val webhookMatch = selectedWebhookId == null || log.webhookId == selectedWebhookId
                    statusMatch && webhookMatch
                }
                
                adapter.submitList(filteredLogs)
                
                if (filteredLogs.isEmpty()) {
                    recyclerView.visibility = View.GONE
                    emptyView.visibility = View.VISIBLE
                } else {
                    recyclerView.visibility = View.VISIBLE
                    emptyView.visibility = View.GONE
                }
            }
        }
        
        // Observe pending count
        lifecycleScope.launch {
            webhookRepository.getPendingDeliveryCount().collectLatest { count ->
                tvPendingCount.text = "$count pending"
            }
        }
    }
    
    private fun showLogDetails(log: SmsDeliveryLogEntity) {
        val webhookName = webhooks.find { it.id == log.webhookId }?.name ?: "Unknown"
        
        val details = buildString {
            appendLine("Webhook: $webhookName")
            appendLine("Phone: ${log.phoneNumber}")
            appendLine("Sender: ${log.sender}")
            appendLine("Status: ${log.status}")
            appendLine("Retries: ${log.retryCount}")
            if (log.responseCode != null) {
                appendLine("Response Code: ${log.responseCode}")
            }
            if (log.responseBody != null) {
                appendLine("Response: ${log.responseBody}")
            }
            appendLine()
            appendLine("Message:")
            appendLine(log.message)
        }
        
        AlertDialog.Builder(this)
            .setTitle("Delivery Log Details")
            .setMessage(details)
            .setPositiveButton("OK", null)
            .apply {
                if (log.status == SmsDeliveryLogEntity.STATUS_FAILED || 
                    log.status == SmsDeliveryLogEntity.STATUS_PENDING) {
                    setNeutralButton("Retry") { _, _ ->
                        retryDelivery(log)
                    }
                }
            }
            .show()
    }
    
    private fun retryDelivery(log: SmsDeliveryLogEntity) {
        lifecycleScope.launch {
            val success = webhookDispatcher.deliverLog(log.id)
            runOnUiThread {
                val message = if (success) "Delivery successful" else "Delivery failed"
                android.widget.Toast.makeText(this@DeliveryLogsActivity, message, android.widget.Toast.LENGTH_SHORT).show()
            }
        }
    }
}
