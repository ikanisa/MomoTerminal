package com.momoterminal.core.logging

import android.util.Log
import com.google.firebase.crashlytics.FirebaseCrashlytics

/**
 * Centralized logging with automatic PII redaction for MomoTerminal.
 * Designed for low-connectivity environments where logs may be batched.
 */
object MomoLogger {
    private const val TAG_PREFIX = "Momo_"

    fun d(tag: String, message: String) = Log.d("$TAG_PREFIX$tag", redact(message))
    fun i(tag: String, message: String) = Log.i("$TAG_PREFIX$tag", redact(message))
    fun w(tag: String, message: String) = Log.w("$TAG_PREFIX$tag", redact(message))
    
    fun e(tag: String, message: String, throwable: Throwable? = null) {
        Log.e("$TAG_PREFIX$tag", redact(message), throwable)
        throwable?.let { 
            try {
                FirebaseCrashlytics.getInstance().recordException(it)
            } catch (_: Exception) { /* Crashlytics not initialized */ }
        }
    }

    /**
     * Redact sensitive data from log messages.
     */
    private fun redact(message: String): String = message
        .replace(Regex("\\d{10,}"), "[PHONE_REDACTED]")           // Phone numbers
        .replace(Regex("(?i)(GHS|RWF|UGX|KES|TZS|XOF|XAF)\\s?[\\d,.]+"), "[AMOUNT_REDACTED]")  // Amounts
        .replace(Regex("\\b\\d{6,}\\b"), "[ID_REDACTED]")         // Transaction IDs
        .replace(Regex("(?i)pin\\s*[:=]?\\s*\\d+"), "[PIN_REDACTED]")  // PINs
}
