package com.momoterminal.ui.webhook

import android.content.Intent
import android.os.Bundle
import android.widget.ImageButton
import android.widget.TextView
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.floatingactionbutton.FloatingActionButton
import com.momoterminal.R
import com.momoterminal.data.local.entity.WebhookConfigEntity
import com.momoterminal.data.repository.WebhookRepository
import com.momoterminal.webhook.WebhookDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for managing webhook configurations.
 * Displays a list of configured webhooks with options to add, edit, delete, and toggle.
 */
@AndroidEntryPoint
class WebhookListActivity : AppCompatActivity() {
    
    @Inject
    lateinit var webhookRepository: WebhookRepository
    
    @Inject
    lateinit var webhookDispatcher: WebhookDispatcher
    
    private lateinit var recyclerView: RecyclerView
    private lateinit var emptyView: TextView
    private lateinit var fabAdd: FloatingActionButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvActiveCount: TextView
    
    private lateinit var adapter: WebhookAdapter
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_list)
        
        initViews()
        setupRecyclerView()
        setupListeners()
        observeWebhooks()
    }
    
    private fun initViews() {
        recyclerView = findViewById(R.id.recyclerView)
        emptyView = findViewById(R.id.tvEmptyView)
        fabAdd = findViewById(R.id.fabAddWebhook)
        btnBack = findViewById(R.id.btnBack)
        tvActiveCount = findViewById(R.id.tvActiveCount)
    }
    
    private fun setupRecyclerView() {
        adapter = WebhookAdapter(
            onItemClick = { webhook -> openEditActivity(webhook) },
            onToggleClick = { webhook -> toggleWebhook(webhook) },
            onTestClick = { webhook -> testWebhook(webhook) },
            onDeleteClick = { webhook -> confirmDelete(webhook) }
        )
        
        recyclerView.layoutManager = LinearLayoutManager(this)
        recyclerView.adapter = adapter
    }
    
    private fun setupListeners() {
        fabAdd.setOnClickListener {
            openEditActivity(null)
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun observeWebhooks() {
        lifecycleScope.launch {
            webhookRepository.getAllWebhooks().collectLatest { webhooks ->
                adapter.submitList(webhooks)
                
                if (webhooks.isEmpty()) {
                    recyclerView.visibility = android.view.View.GONE
                    emptyView.visibility = android.view.View.VISIBLE
                } else {
                    recyclerView.visibility = android.view.View.VISIBLE
                    emptyView.visibility = android.view.View.GONE
                }
                
                val activeCount = webhooks.count { it.isActive }
                tvActiveCount.text = "$activeCount active webhook${if (activeCount != 1) "s" else ""}"
            }
        }
    }
    
    private fun openEditActivity(webhook: WebhookConfigEntity?) {
        val intent = Intent(this, WebhookEditActivity::class.java)
        webhook?.let {
            intent.putExtra(WebhookEditActivity.EXTRA_WEBHOOK_ID, it.id)
        }
        startActivity(intent)
    }
    
    private fun toggleWebhook(webhook: WebhookConfigEntity) {
        lifecycleScope.launch {
            webhookRepository.setWebhookActive(webhook.id, !webhook.isActive)
        }
    }
    
    private fun testWebhook(webhook: WebhookConfigEntity) {
        lifecycleScope.launch {
            val result = webhookDispatcher.testWebhook(webhook)
            runOnUiThread {
                AlertDialog.Builder(this@WebhookListActivity)
                    .setTitle(if (result.first) "Success" else "Failed")
                    .setMessage(result.second)
                    .setPositiveButton("OK", null)
                    .show()
            }
        }
    }
    
    private fun confirmDelete(webhook: WebhookConfigEntity) {
        AlertDialog.Builder(this)
            .setTitle("Delete Webhook")
            .setMessage("Are you sure you want to delete \"${webhook.name}\"? This will also delete all delivery logs for this webhook.")
            .setPositiveButton("Delete") { _, _ ->
                lifecycleScope.launch {
                    webhookRepository.deleteWebhook(webhook)
                }
            }
            .setNegativeButton("Cancel", null)
            .show()
    }
    
    override fun onResume() {
        super.onResume()
        // Refresh the list when returning from edit activity
    }
}
