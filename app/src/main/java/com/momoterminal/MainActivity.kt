package com.momoterminal

import android.Manifest
import android.animation.ObjectAnimator
import android.animation.ValueAnimator
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.os.VibrationEffect
import android.os.Vibrator
import android.os.VibratorManager
import android.view.View
import android.widget.ImageButton
import android.widget.ImageView
import android.widget.LinearLayout
import android.widget.ScrollView
import android.widget.TextView
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import androidx.lifecycle.lifecycleScope
import androidx.recyclerview.widget.LinearLayoutManager
import androidx.recyclerview.widget.RecyclerView
import com.google.android.material.button.MaterialButton
import com.google.android.material.textfield.TextInputEditText
import com.momoterminal.adapter.TransactionAdapter
import com.momoterminal.config.AppConfig
import com.momoterminal.data.local.dao.TransactionDao
import com.momoterminal.sync.SyncManager
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.launch
import javax.inject.Inject

/**
 * Main Activity for the MomoTerminal application.
 * Handles NFC payment broadcasting, SMS relay functionality, and transaction history.
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {
    
    // UI Components
    private lateinit var etAmount: TextInputEditText
    private lateinit var etPhone: TextInputEditText
    private lateinit var btnActivate: MaterialButton
    private lateinit var btnSettings: ImageButton
    private lateinit var layoutStatus: LinearLayout
    private lateinit var imgNfcIcon: ImageView
    private lateinit var tvStatus: TextView
    private lateinit var tvLog: TextView
    private lateinit var scrollLog: ScrollView
    private lateinit var tvConnectionStatus: TextView
    private lateinit var tvPendingCount: TextView
    private lateinit var recyclerTransactions: RecyclerView
    
    // Animation
    private var pulseAnimator: ObjectAnimator? = null
    
    // NFC Adapter
    private var nfcAdapter: NfcAdapter? = null
    
    // Config
    private lateinit var appConfig: AppConfig
    private lateinit var transactionAdapter: TransactionAdapter
    
    // Injected dependencies
    @Inject
    lateinit var transactionDao: TransactionDao
    
    // Permission launcher
    private val permissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val allGranted = permissions.all { it.value }
        if (allGranted) {
            PaymentState.appendLog("SMS permissions granted")
        } else {
            PaymentState.appendLog("SMS permissions denied - relay disabled")
            Toast.makeText(this, "SMS permissions required for relay feature", Toast.LENGTH_LONG).show()
        }
    }
    
    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        setContentView(R.layout.activity_main)
        
        appConfig = AppConfig(this)
        
        initViews()
        setupRecyclerView()
        checkNfcSupport()
        requestSmsPermissions()
        setupListeners()
        setupObservers()
        schedulePeriodicSync()
        
        PaymentState.appendLog("MomoTerminal started")
    }
    
    override fun onResume() {
        super.onResume()
        updateConfigStatus()
        loadMerchantPhone()
        
        // Check if NFC got enabled
        nfcAdapter?.let {
            if (it.isEnabled) {
                PaymentState.appendLog("NFC is enabled")
            }
        }
    }
    
    private fun initViews() {
        etAmount = findViewById(R.id.etAmount)
        etPhone = findViewById(R.id.etPhone)
        btnActivate = findViewById(R.id.btnActivate)
        btnSettings = findViewById(R.id.btnSettings)
        layoutStatus = findViewById(R.id.layoutStatus)
        imgNfcIcon = findViewById(R.id.imgNfcIcon)
        tvStatus = findViewById(R.id.tvStatus)
        tvLog = findViewById(R.id.tvLog)
        scrollLog = findViewById(R.id.scrollLog)
        tvConnectionStatus = findViewById(R.id.tvConnectionStatus)
        tvPendingCount = findViewById(R.id.tvPendingCount)
        recyclerTransactions = findViewById(R.id.recyclerTransactions)
    }
    
    private fun setupRecyclerView() {
        transactionAdapter = TransactionAdapter()
        recyclerTransactions.apply {
            layoutManager = LinearLayoutManager(this@MainActivity)
            adapter = transactionAdapter
        }
    }
    
    private fun checkNfcSupport() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        when {
            nfcAdapter == null -> {
                PaymentState.appendLog("NFC not supported on this device")
                Toast.makeText(this, "NFC is not supported on this device", Toast.LENGTH_LONG).show()
            }
            !nfcAdapter!!.isEnabled -> {
                PaymentState.appendLog("NFC is disabled")
                Toast.makeText(this, "Please enable NFC in settings", Toast.LENGTH_LONG).show()
            }
            else -> {
                PaymentState.appendLog("NFC is ready")
            }
        }
    }
    
    private fun requestSmsPermissions() {
        val permissions = arrayOf(
            Manifest.permission.RECEIVE_SMS,
            Manifest.permission.READ_SMS
        )
        
        val notGranted = permissions.filter {
            ContextCompat.checkSelfPermission(this, it) != PackageManager.PERMISSION_GRANTED
        }
        
        if (notGranted.isNotEmpty()) {
            permissionLauncher.launch(notGranted.toTypedArray())
        } else {
            PaymentState.appendLog("SMS permissions already granted")
        }
    }
    
    private fun setupListeners() {
        // Activate button - start payment broadcasting
        btnActivate.setOnClickListener {
            activatePayment()
        }
        
        // Settings button - open settings activity
        btnSettings.setOnClickListener {
            startActivity(Intent(this, SettingsActivity::class.java))
        }
        
        // NFC icon click - cancel payment mode
        imgNfcIcon.setOnClickListener {
            cancelPayment()
        }
        
        // Long press on status area - cancel payment mode
        layoutStatus.setOnLongClickListener {
            cancelPayment()
            true
        }
    }
    
    private fun setupObservers() {
        // Observe SMS log updates
        PaymentState.smsLog.observe(this) { log ->
            tvLog.text = log
            // Auto-scroll to top (newest entries)
            scrollLog.post {
                scrollLog.fullScroll(View.FOCUS_UP)
            }
        }
        
        // Observe status updates
        PaymentState.statusUpdate.observe(this) { status ->
            if (status.isNotEmpty()) {
                tvStatus.text = status
            }
        }
        
        // Observe recent transactions from Room database
        lifecycleScope.launch {
            transactionDao.getRecentTransactions().collectLatest { transactions ->
                transactionAdapter.submitList(transactions)
            }
        }
        
        // Observe pending count
        lifecycleScope.launch {
            transactionDao.getPendingCount().collectLatest { count ->
                tvPendingCount.text = "$count pending uploads"
            }
        }
    }
    
    private fun schedulePeriodicSync() {
        SyncManager(this).schedulePeriodicSync()
        PaymentState.appendLog("Periodic sync scheduled")
    }
    
    private fun updateConfigStatus() {
        if (appConfig.isConfigured()) {
            val url = appConfig.getGatewayUrl()
            // Show truncated URL
            val displayUrl = if (url.length > 40) url.take(40) + "..." else url
            tvConnectionStatus.text = "✓ Connected to: $displayUrl"
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_green_light))
        } else {
            tvConnectionStatus.text = "⚠️ Not Configured - Tap Settings"
            tvConnectionStatus.setTextColor(getColor(android.R.color.holo_red_light))
        }
    }
    
    private fun loadMerchantPhone() {
        val merchantPhone = appConfig.getMerchantPhone()
        if (merchantPhone.isNotBlank()) {
            etPhone.setText(merchantPhone)
        }
    }
    
    private fun activatePayment() {
        val amount = etAmount.text?.toString()?.trim() ?: ""
        val merchant = appConfig.getMerchantPhone()
        
        // Validate amount
        if (amount.isEmpty()) {
            etAmount.error = "Please enter an amount"
            return
        }
        
        // Check if configured
        if (!appConfig.isConfigured()) {
            Toast.makeText(this, "Please configure settings first", Toast.LENGTH_SHORT).show()
            startActivity(Intent(this, SettingsActivity::class.java))
            return
        }
        
        if (merchant.isEmpty()) {
            Toast.makeText(this, "Merchant phone not configured", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Check NFC
        if (nfcAdapter == null || !nfcAdapter!!.isEnabled) {
            Toast.makeText(this, "NFC is not available", Toast.LENGTH_SHORT).show()
            return
        }
        
        // Generate payment URI and save to state
        val paymentUri = PaymentState.generatePaymentUri(merchant, amount)
        PaymentState.currentPaymentUri = paymentUri
        PaymentState.currentAmount = amount
        
        PaymentState.appendLog("Payment URI: $paymentUri")
        PaymentState.appendLog("Ready for tap... (${amount} RWF to $merchant)")
        
        // Update UI - hide button, show status
        btnActivate.visibility = View.GONE
        layoutStatus.visibility = View.VISIBLE
        tvStatus.text = "Ready for tap..."
        
        // Start pulse animation
        startPulseAnimation()
        
        // Vibrate to indicate ready
        vibrate()
    }
    
    private fun cancelPayment() {
        // Stop animation
        stopPulseAnimation()
        
        // Clear payment state
        PaymentState.clearPayment()
        
        // Update UI - show button, hide status
        btnActivate.visibility = View.VISIBLE
        layoutStatus.visibility = View.INVISIBLE
        
        PaymentState.appendLog("Payment cancelled")
    }
    
    private fun startPulseAnimation() {
        pulseAnimator = ObjectAnimator.ofFloat(imgNfcIcon, "alpha", 1f, 0.3f).apply {
            duration = 800
            repeatMode = ValueAnimator.REVERSE
            repeatCount = ValueAnimator.INFINITE
            start()
        }
    }
    
    private fun stopPulseAnimation() {
        pulseAnimator?.cancel()
        pulseAnimator = null
        imgNfcIcon.alpha = 1f
    }
    
    private fun vibrate() {
        val vibrator = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            val vibratorManager = getSystemService(Context.VIBRATOR_MANAGER_SERVICE) as VibratorManager
            vibratorManager.defaultVibrator
        } else {
            @Suppress("DEPRECATION")
            getSystemService(Context.VIBRATOR_SERVICE) as Vibrator
        }
        
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            vibrator.vibrate(VibrationEffect.createOneShot(200, VibrationEffect.DEFAULT_AMPLITUDE))
        } else {
            @Suppress("DEPRECATION")
            vibrator.vibrate(200)
        }
    }
    
    override fun onDestroy() {
        super.onDestroy()
        stopPulseAnimation()
    }
}
