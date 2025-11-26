package com.momoterminal

import android.os.Bundle
import android.widget.Toast
import androidx.appcompat.app.AppCompatActivity
import androidx.lifecycle.lifecycleScope
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.momoterminal.config.AppConfig
import android.widget.ImageButton
import android.widget.TextView
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import okhttp3.MediaType.Companion.toMediaType
import okhttp3.OkHttpClient
import okhttp3.Request
import okhttp3.RequestBody.Companion.toRequestBody
import org.json.JSONObject
import java.util.concurrent.TimeUnit

/**
 * Settings Activity for configuring the gateway connection.
 * Allows admin to configure webhook URL, API secret, and merchant phone number.
 */
class SettingsActivity : AppCompatActivity() {
    
    private lateinit var etWebhookUrl: TextInputEditText
    private lateinit var etApiSecret: TextInputEditText
    private lateinit var etMerchantPhone: TextInputEditText
    private lateinit var btnSaveConfig: MaterialButton
    private lateinit var btnTestConnection: MaterialButton
    private lateinit var btnBack: ImageButton
    private lateinit var tvStatus: TextView
    
    private lateinit var appConfig: AppConfig
    
    private val client = OkHttpClient.Builder()
        .connectTimeout(10, TimeUnit.SECONDS)
        .readTimeout(10, TimeUnit.SECONDS)
        .writeTimeout(10, TimeUnit.SECONDS)
        .build()
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_settings)
        
        appConfig = AppConfig(this)
        
        initViews()
        loadExistingConfig()
        setupListeners()
    }
    
    private fun initViews() {
        etWebhookUrl = findViewById(R.id.etWebhookUrl)
        etApiSecret = findViewById(R.id.etApiSecret)
        etMerchantPhone = findViewById(R.id.etMerchantPhone)
        btnSaveConfig = findViewById(R.id.btnSaveConfig)
        btnTestConnection = findViewById(R.id.btnTestConnection)
        btnBack = findViewById(R.id.btnBack)
        tvStatus = findViewById(R.id.tvStatus)
    }
    
    private fun loadExistingConfig() {
        if (appConfig.isConfigured()) {
            etWebhookUrl.setText(appConfig.getGatewayUrl())
            etApiSecret.setText(appConfig.getApiSecret())
            etMerchantPhone.setText(appConfig.getMerchantPhone())
            updateStatus(true)
        } else {
            updateStatus(false)
        }
    }
    
    private fun setupListeners() {
        btnSaveConfig.setOnClickListener {
            saveConfiguration()
        }
        
        btnTestConnection.setOnClickListener {
            testConnection()
        }
        
        btnBack.setOnClickListener {
            finish()
        }
    }
    
    private fun saveConfiguration() {
        val url = etWebhookUrl.text?.toString()?.trim() ?: ""
        val secret = etApiSecret.text?.toString()?.trim() ?: ""
        val phone = etMerchantPhone.text?.toString()?.trim() ?: ""
        
        // Validate URL format
        if (url.isEmpty()) {
            etWebhookUrl.error = "URL is required"
            return
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            etWebhookUrl.error = "URL must start with http:// or https://"
            return
        }
        
        // Validate phone is not empty
        if (phone.isEmpty()) {
            etMerchantPhone.error = "Merchant phone is required"
            return
        }
        
        // Save to AppConfig
        appConfig.saveConfig(url, secret, phone)
        
        updateStatus(true)
        Toast.makeText(this, "Configuration saved successfully", Toast.LENGTH_SHORT).show()
        
        PaymentState.appendLog("Settings: Configuration updated")
    }
    
    private fun testConnection() {
        val url = etWebhookUrl.text?.toString()?.trim() ?: ""
        val secret = etApiSecret.text?.toString()?.trim() ?: ""
        
        if (url.isEmpty()) {
            Toast.makeText(this, "Please enter a webhook URL", Toast.LENGTH_SHORT).show()
            return
        }
        
        if (!url.startsWith("http://") && !url.startsWith("https://")) {
            Toast.makeText(this, "Invalid URL format", Toast.LENGTH_SHORT).show()
            return
        }
        
        btnTestConnection.isEnabled = false
        btnTestConnection.text = "Testing..."
        
        lifecycleScope.launch {
            try {
                val result = withContext(Dispatchers.IO) {
                    // Send a test POST to the URL
                    val json = JSONObject().apply {
                        put("test", true)
                        put("message", "MomoTerminal connection test")
                        put("timestamp", System.currentTimeMillis())
                    }
                    
                    val request = Request.Builder()
                        .url(url)
                        .post(json.toString().toRequestBody("application/json".toMediaType()))
                        .addHeader("X-Api-Key", secret)
                        .addHeader("Content-Type", "application/json")
                        .build()
                    
                    try {
                        val response = client.newCall(request).execute()
                        val success = response.isSuccessful
                        response.close()
                        success
                    } catch (e: Exception) {
                        false
                    }
                }
                
                if (result) {
                    Toast.makeText(this@SettingsActivity, "Connection successful!", Toast.LENGTH_SHORT).show()
                    PaymentState.appendLog("Settings: Connection test successful")
                } else {
                    Toast.makeText(this@SettingsActivity, "Connection failed. Check URL and try again.", Toast.LENGTH_LONG).show()
                    PaymentState.appendLog("Settings: Connection test failed")
                }
            } finally {
                btnTestConnection.isEnabled = true
                btnTestConnection.text = "Test Connection"
            }
        }
    }
    
    private fun updateStatus(configured: Boolean) {
        if (configured) {
            tvStatus.text = "✓ Configured"
            tvStatus.setTextColor(getColor(android.R.color.holo_green_light))
        } else {
            tvStatus.text = "⚠ Not Configured"
            tvStatus.setTextColor(getColor(android.R.color.holo_red_light))
        }
    }
}
