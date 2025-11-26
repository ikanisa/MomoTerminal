package com.momoterminal.ui.webhook

import android.os.Bundle
import android.widget.ImageButton
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.momoterminal.R
import com.momoterminal.data.local.entity.WebhookConfigEntity
import com.momoterminal.data.repository.WebhookRepository
import com.momoterminal.webhook.WebhookDispatcher
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Activity for adding or editing a webhook configuration.
 */
@AndroidEntryPoint
class WebhookEditActivity : AppCompatActivity() {
    
    companion object {
        const val EXTRA_WEBHOOK_ID = "extra_webhook_id"
    }
    
    @Inject
    lateinit var webhookRepository: WebhookRepository
    
    @Inject
    lateinit var webhookDispatcher: WebhookDispatcher
    
    private lateinit var etName: TextInputEditText
    private lateinit var etUrl: TextInputEditText
    private lateinit var etPhoneNumber: TextInputEditText
    private lateinit var etApiKey: TextInputEditText
    private lateinit var etHmacSecret: TextInputEditText
    private lateinit var btnSave: MaterialButton
    private lateinit var btnTest: MaterialButton
    private lateinit var btnBack: ImageButton
    
    private var webhookId: Long = 0
    private var existingWebhook: WebhookConfigEntity? = null
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_webhook_edit)
        
        webhookId = intent.getLongExtra(EXTRA_WEBHOOK_ID, 0)
        
        initViews()
        setupListeners()
        
        if (webhookId > 0) {
            loadExistingWebhook()
        }
    }
    
    private fun initViews() {
        etName = findViewById(R.id.etName)
        etUrl = findViewById(R.id.etUrl)
        etPhoneNumber = findViewById(R.id.etPhoneNumber)
        etApiKey = findViewById(R.id.etApiKey)
        etHmacSecret = findViewById(R.id.etHmacSecret)
        btnSave = findViewById(R.id.btnSave)
        btnTest = findViewById(R.id.btnTest)
        btnBack = findViewById(R.id.btnBack)
    }
    
    private fun setupListeners() {
        btnBack.setOnClickListener {
            finish()
        }
        
        btnSave.setOnClickListener {
            saveWebhook()
        }
        
        btnTest.setOnClickListener {
            testConnection()
        }
    }
    
    private fun loadExistingWebhook() {
        lifecycleScope.launch {
            existingWebhook = webhookRepository.getWebhookById(webhookId)
            existingWebhook?.let { webhook ->
                runOnUiThread {
                    etName.setText(webhook.name)
                    etUrl.setText(webhook.url)
                    etPhoneNumber.setText(webhook.phoneNumber)
                    etApiKey.setText(webhook.apiKey)
                    etHmacSecret.setText(webhook.hmacSecret)
                }
            }
        }
    }
    
    private fun saveWebhook() {
        val name = etName.text?.toString()?.trim() ?: ""
        val url = etUrl.text?.toString()?.trim() ?: ""
        val phoneNumber = etPhoneNumber.text?.toString()?.trim() ?: ""
        val apiKey = etApiKey.text?.toString()?.trim() ?: ""
        val hmacSecret = etHmacSecret.text?.toString()?.trim() ?: ""
        
        // Validation
        if (name.isEmpty()) {
            etName.error = "Name is required"
            return
        }
        
        if (url.isEmpty()) {
            etUrl.error = "URL is required"
            return
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            etUrl.error = "URL must start with http:// or https://"
            return
        }
        
        // Warn about insecure HTTP URLs
        if (url.startsWith("http://") && !url.startsWith("http://localhost") && !url.startsWith("http://127.0.0.1")) {
            androidx.appcompat.app.AlertDialog.Builder(this)
                .setTitle("Security Warning")
                .setMessage("HTTP URLs transmit data in plain text. For Mobile Money data, HTTPS is strongly recommended. Continue anyway?")
                .setPositiveButton("Continue") { _, _ ->
                    proceedWithSave(name, url, phoneNumber, apiKey, hmacSecret)
                }
                .setNegativeButton("Cancel", null)
                .show()
            return
        }
        
        proceedWithSave(name, url, phoneNumber, apiKey, hmacSecret)
    }
    
    private fun proceedWithSave(
        name: String,
        url: String,
        phoneNumber: String,
        apiKey: String,
        hmacSecret: String
    ) {
        if (apiKey.isEmpty()) {
            etApiKey.error = "API Key is required"
            return
        }
        
        if (hmacSecret.isEmpty()) {
            etHmacSecret.error = "HMAC Secret is required"
            return
        }
        
        btnSave.isEnabled = false
        btnSave.text = "Saving..."
        
        lifecycleScope.launch {
            try {
                // Check for duplicate URL
                if (webhookRepository.isUrlDuplicate(url, webhookId)) {
                    runOnUiThread {
                        etUrl.error = "A webhook with this URL already exists"
                        btnSave.isEnabled = true
                        btnSave.text = "Save"
                    }
                    return@launch
                }
                
                val webhook = WebhookConfigEntity(
                    id = webhookId,
                    name = name,
                    url = url,
                    phoneNumber = phoneNumber,
                    apiKey = apiKey,
                    hmacSecret = hmacSecret,
                    isActive = existingWebhook?.isActive ?: true,
                    createdAt = existingWebhook?.createdAt ?: System.currentTimeMillis()
                )
                
                webhookRepository.saveWebhook(webhook)
                
                runOnUiThread {
                    Toast.makeText(
                        this@WebhookEditActivity,
                        if (webhookId == 0L) "Webhook created" else "Webhook updated",
                        Toast.LENGTH_SHORT
                    ).show()
                    finish()
                }
            } catch (e: Exception) {
                runOnUiThread {
                    Toast.makeText(
                        this@WebhookEditActivity,
                        "Error: ${e.message}",
                        Toast.LENGTH_LONG
                    ).show()
                    btnSave.isEnabled = true
                    btnSave.text = "Save"
                }
            }
        }
    }
    
    private fun testConnection() {
        val url = etUrl.text?.toString()?.trim() ?: ""
        val apiKey = etApiKey.text?.toString()?.trim() ?: ""
        val hmacSecret = etHmacSecret.text?.toString()?.trim() ?: ""
        
        if (url.isEmpty()) {
            etUrl.error = "URL is required for testing"
            return
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            etUrl.error = "URL must start with http:// or https://"
            return
        }
        
        if (hmacSecret.isEmpty()) {
            etHmacSecret.error = "HMAC Secret is required for testing"
            return
        }
        
        btnTest.isEnabled = false
        btnTest.text = "Testing..."
        
        lifecycleScope.launch {
            try {
                val testWebhook = WebhookConfigEntity(
                    id = 0,
                    name = "Test",
                    url = url,
                    phoneNumber = "",
                    apiKey = apiKey,
                    hmacSecret = hmacSecret
                )
                
                val result = webhookDispatcher.testWebhook(testWebhook)
                
                runOnUiThread {
                    Toast.makeText(
                        this@WebhookEditActivity,
                        result.second,
                        if (result.first) Toast.LENGTH_SHORT else Toast.LENGTH_LONG
                    ).show()
                }
            } finally {
                runOnUiThread {
                    btnTest.isEnabled = true
                    btnTest.text = "Test Connection"
                }
            }
        }
    }
}
