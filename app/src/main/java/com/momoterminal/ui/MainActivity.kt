package com.momoterminal.ui

import android.Manifest
import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import android.content.IntentFilter
import android.content.pm.PackageManager
import android.graphics.Color
import android.graphics.drawable.GradientDrawable
import android.nfc.NfcAdapter
import android.os.Build
import android.os.Bundle
import android.provider.Settings
import android.widget.Toast
import androidx.activity.result.contract.ActivityResultContracts
import androidx.appcompat.app.AlertDialog
import androidx.appcompat.app.AppCompatActivity
import androidx.core.content.ContextCompat
import com.momoterminal.MomoTerminalApp
import com.momoterminal.R
import com.momoterminal.databinding.ActivityMainBinding
import com.momoterminal.nfc.MomoHceService
import com.momoterminal.sms.SmsReceiver
import com.momoterminal.ussd.UssdHelper
import dagger.hilt.android.AndroidEntryPoint
import java.text.NumberFormat
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Main Activity for the MomoTerminal app.
 * 
 * This activity provides the merchant interface for:
 * - Entering payment amounts
 * - Activating/deactivating the NFC terminal
 * - Viewing SMS relay status and recent transactions
 */
@AndroidEntryPoint
class MainActivity : AppCompatActivity() {

    private lateinit var binding: ActivityMainBinding
    private var nfcAdapter: NfcAdapter? = null
    private var isNfcTerminalActive = false

    private val paymentReceiver = object : BroadcastReceiver() {
        override fun onReceive(context: Context?, intent: Intent?) {
            if (intent?.action == SmsReceiver.BROADCAST_PAYMENT_RECEIVED) {
                val amount = intent.getDoubleExtra(SmsReceiver.EXTRA_AMOUNT, 0.0)
                val sender = intent.getStringExtra(SmsReceiver.EXTRA_SENDER) ?: ""
                val txId = intent.getStringExtra(SmsReceiver.EXTRA_TRANSACTION_ID) ?: ""
                val timestamp = intent.getLongExtra(SmsReceiver.EXTRA_TIMESTAMP, System.currentTimeMillis())
                
                updateLastTransaction(amount, sender, txId, timestamp)
            }
        }
    }

    private val requestPermissionLauncher = registerForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        val smsGranted = permissions[Manifest.permission.RECEIVE_SMS] == true
        val phoneGranted = permissions[Manifest.permission.CALL_PHONE] == true
        
        updateSmsRelayStatus(smsGranted)
        
        if (!smsGranted) {
            showPermissionExplanation(
                getString(R.string.permission_sms_title),
                getString(R.string.permission_sms_message)
            )
        }
    }

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        binding = ActivityMainBinding.inflate(layoutInflater)
        setContentView(binding.root)

        initializeNfc()
        setupUI()
        checkPermissions()
        loadSavedData()
    }

    /**
     * Initialize NFC adapter and check for HCE support.
     */
    private fun initializeNfc() {
        nfcAdapter = NfcAdapter.getDefaultAdapter(this)
        
        if (nfcAdapter == null) {
            binding.tvNfcStatus.text = getString(R.string.error_nfc_not_supported)
            binding.btnActivateNfc.isEnabled = false
            return
        }

        if (!nfcAdapter!!.isEnabled) {
            binding.tvNfcStatus.text = getString(R.string.error_nfc_disabled)
            showNfcEnableDialog()
        }
    }

    /**
     * Set up UI components and listeners.
     */
    private fun setupUI() {
        binding.btnActivateNfc.setOnClickListener {
            if (isNfcTerminalActive) {
                deactivateNfcTerminal()
            } else {
                activateNfcTerminal()
            }
        }
    }

    /**
     * Activate the NFC terminal with the current amount.
     */
    private fun activateNfcTerminal() {
        val amountText = binding.etAmount.text.toString()
        val merchantCode = binding.etMerchantCode.text.toString()

        if (amountText.isEmpty() || amountText.toDoubleOrNull() == null) {
            Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show()
            return
        }

        if (merchantCode.isEmpty()) {
            binding.etMerchantCode.error = "Merchant code required"
            return
        }

        val amount = amountText.toDouble()
        if (amount <= 0) {
            Toast.makeText(this, R.string.error_invalid_amount, Toast.LENGTH_SHORT).show()
            return
        }

        // Generate USSD code for MTN MoMo (default)
        val ussdCode = UssdHelper.generateUssdCode(
            UssdHelper.Provider.MTN_MOMO,
            merchantCode,
            amount
        )

        // Start HCE service with payment data
        val intent = Intent(this, MomoHceService::class.java).apply {
            action = MomoHceService.ACTION_SET_PAYMENT_DATA
            putExtra(MomoHceService.EXTRA_AMOUNT, amount)
            putExtra(MomoHceService.EXTRA_MERCHANT_CODE, merchantCode)
            putExtra(MomoHceService.EXTRA_USSD_CODE, ussdCode)
        }
        startService(intent)

        // Save the data
        saveMerchantCode(merchantCode)

        // Update UI
        isNfcTerminalActive = true
        updateNfcTerminalUI()

        Toast.makeText(this, "NFC Terminal activated - Ready for tap", Toast.LENGTH_SHORT).show()
    }

    /**
     * Deactivate the NFC terminal.
     */
    private fun deactivateNfcTerminal() {
        val intent = Intent(this, MomoHceService::class.java).apply {
            action = MomoHceService.ACTION_CLEAR_PAYMENT_DATA
        }
        startService(intent)

        isNfcTerminalActive = false
        updateNfcTerminalUI()

        Toast.makeText(this, "NFC Terminal deactivated", Toast.LENGTH_SHORT).show()
    }

    /**
     * Update UI to reflect NFC terminal state.
     */
    private fun updateNfcTerminalUI() {
        if (isNfcTerminalActive) {
            binding.tvNfcStatus.text = getString(R.string.nfc_active)
            binding.btnActivateNfc.text = getString(R.string.deactivate_nfc)
            binding.btnActivateNfc.setBackgroundColor(
                ContextCompat.getColor(this, R.color.error_red)
            )
        } else {
            binding.tvNfcStatus.text = getString(R.string.nfc_inactive)
            binding.btnActivateNfc.text = getString(R.string.activate_nfc)
            binding.btnActivateNfc.setBackgroundColor(
                ContextCompat.getColor(this, R.color.momo_yellow)
            )
        }
    }

    /**
     * Check and request necessary permissions.
     */
    private fun checkPermissions() {
        val permissions = mutableListOf<String>()

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.RECEIVE_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.RECEIVE_SMS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.READ_SMS)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.READ_SMS)
        }

        if (ContextCompat.checkSelfPermission(this, Manifest.permission.CALL_PHONE)
            != PackageManager.PERMISSION_GRANTED) {
            permissions.add(Manifest.permission.CALL_PHONE)
        }

        if (permissions.isNotEmpty()) {
            requestPermissionLauncher.launch(permissions.toTypedArray())
        } else {
            updateSmsRelayStatus(true)
        }
    }

    /**
     * Update SMS relay status indicator.
     */
    private fun updateSmsRelayStatus(isActive: Boolean) {
        val indicator = binding.viewSmsIndicator.background as? GradientDrawable
        if (isActive) {
            indicator?.setColor(ContextCompat.getColor(this, R.color.success_green))
            binding.tvSmsStatus.text = getString(R.string.sms_relay_active)
        } else {
            indicator?.setColor(ContextCompat.getColor(this, R.color.grey_medium))
            binding.tvSmsStatus.text = getString(R.string.sms_relay_inactive)
        }
    }

    /**
     * Update the last transaction display.
     */
    private fun updateLastTransaction(amount: Double, sender: String, txId: String, timestamp: Long) {
        val formatter = NumberFormat.getCurrencyInstance(Locale("en", "GH"))
        val dateFormatter = SimpleDateFormat("dd MMM yyyy, HH:mm", Locale.getDefault())
        
        val transactionText = """
            ${formatter.format(amount)} from $sender
            ID: $txId
            ${dateFormatter.format(Date(timestamp))}
        """.trimIndent()
        
        binding.tvLastTransaction.text = transactionText

        // Flash the indicator green
        val indicator = binding.viewSmsIndicator.background as? GradientDrawable
        indicator?.setColor(Color.GREEN)
    }

    /**
     * Show dialog to enable NFC.
     */
    private fun showNfcEnableDialog() {
        AlertDialog.Builder(this)
            .setTitle("Enable NFC")
            .setMessage("NFC is required for this app. Would you like to enable it?")
            .setPositiveButton("Settings") { _, _ ->
                startActivity(Intent(Settings.ACTION_NFC_SETTINGS))
            }
            .setNegativeButton("Cancel", null)
            .show()
    }

    /**
     * Show permission explanation dialog.
     */
    private fun showPermissionExplanation(title: String, message: String) {
        AlertDialog.Builder(this)
            .setTitle(title)
            .setMessage(message)
            .setPositiveButton("OK", null)
            .show()
    }

    /**
     * Save merchant code to preferences.
     */
    private fun saveMerchantCode(code: String) {
        MomoTerminalApp.getInstance().sharedPreferences.edit()
            .putString(MomoTerminalApp.KEY_MERCHANT_CODE, code)
            .apply()
    }

    /**
     * Load saved data from preferences.
     */
    private fun loadSavedData() {
        val prefs = MomoTerminalApp.getInstance().sharedPreferences
        val savedMerchantCode = prefs.getString(MomoTerminalApp.KEY_MERCHANT_CODE, "")
        binding.etMerchantCode.setText(savedMerchantCode)
    }

    @Suppress("UnspecifiedRegisterReceiverFlag")
    override fun onResume() {
        super.onResume()
        
        // Register for payment broadcasts
        // Using RECEIVER_NOT_EXPORTED for all versions to ensure local-only broadcasts
        val filter = IntentFilter(SmsReceiver.BROADCAST_PAYMENT_RECEIVED)
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.TIRAMISU) {
            registerReceiver(paymentReceiver, filter, RECEIVER_NOT_EXPORTED)
        } else {
            // For older APIs, registerReceiver without flags is local-only 
            // when used with a LocalBroadcastManager-style pattern
            registerReceiver(paymentReceiver, filter)
        }

        // Check NFC state
        nfcAdapter?.let {
            if (!it.isEnabled) {
                binding.tvNfcStatus.text = getString(R.string.error_nfc_disabled)
            }
        }
    }

    override fun onPause() {
        super.onPause()
        unregisterReceiver(paymentReceiver)
    }
}
