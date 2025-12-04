package com.momoterminal.sms

import android.util.Log
import com.momoterminal.data.repository.SmsRepository
import com.momoterminal.data.repository.WalletRepository
import com.momoterminal.data.local.entity.SmsTransactionEntity
import com.momoterminal.data.local.entity.SmsTransactionType
import com.momoterminal.domain.model.ReferenceType
import com.momoterminal.domain.model.TokenTransactionType
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service that integrates SMS parsing with wallet crediting.
 * Processes incoming MoMo SMS and credits the user's token wallet.
 */
@Singleton
class SmsWalletIntegrationService @Inject constructor(
    private val smsParser: MomoSmsParser,
    private val smsRepository: SmsRepository,
    private val walletRepository: WalletRepository
) {
    companion object {
        private const val TAG = "SmsWalletIntegration"
        private const val DEFAULT_CONVERSION_RATE = 0.01 // 1 token = 100 currency units
    }

    /**
     * Process incoming SMS: parse, save, and credit wallet if applicable.
     */
    suspend fun processIncomingSms(
        userId: String,
        sender: String,
        body: String
    ): ProcessResult {
        val parsed = smsParser.parse(sender, body)
            ?: return ProcessResult.NotMomoMessage

        parsed.reference?.let { ref ->
            if (smsRepository.findByReference(ref) != null) {
                return ProcessResult.Duplicate(ref)
            }
        }

        smsRepository.insert(parsed)
        Log.d(TAG, "SMS saved: ${parsed.id}, type=${parsed.type}, amount=${parsed.amount}")

        if (parsed.type == SmsTransactionType.RECEIVED && parsed.amount > 0) {
            return creditWallet(userId, parsed)
        }

        return ProcessResult.Saved(parsed.id)
    }

    /**
     * Credit wallet from SMS transaction.
     */
    private suspend fun creditWallet(userId: String, sms: SmsTransactionEntity): ProcessResult {
        val wallet = walletRepository.getOrCreateWallet(userId)
        val tokenAmount = (sms.amount * 100 * DEFAULT_CONVERSION_RATE).toLong() // Convert to pesewas then tokens

        val result = walletRepository.applyTransaction(
            walletId = wallet.id,
            amount = tokenAmount,
            type = TokenTransactionType.SMS_CREDIT,
            reference = sms.id,
            referenceType = ReferenceType.SMS_TRANSACTION,
            description = "Credit from ${sms.currency} ${sms.amount} received",
            metadata = mapOf(
                "originalAmount" to sms.amount.toString(),
                "originalCurrency" to sms.currency,
                "sender" to sms.sender
            )
        )

        return if (result.isSuccess) {
            smsRepository.markWalletCredited(sms.id)
            Log.d(TAG, "Wallet credited: +$tokenAmount tokens")
            ProcessResult.CreditedWallet(sms.id, tokenAmount, result.getOrNull()!!.balance)
        } else {
            Log.e(TAG, "Failed to credit wallet: ${result.exceptionOrNull()?.message}")
            ProcessResult.CreditFailed(sms.id, result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    /**
     * Process any uncredited SMS transactions (recovery/retry).
     */
    suspend fun processUncreditedTransactions(userId: String): Int {
        val uncredited = smsRepository.getUncreditedReceived()
        var credited = 0
        
        for (sms in uncredited) {
            val result = creditWallet(userId, sms)
            if (result is ProcessResult.CreditedWallet) credited++
        }
        
        Log.d(TAG, "Processed $credited uncredited transactions")
        return credited
    }

    sealed class ProcessResult {
        object NotMomoMessage : ProcessResult()
        data class Duplicate(val reference: String) : ProcessResult()
        data class Saved(val id: String) : ProcessResult()
        data class CreditedWallet(val smsId: String, val tokens: Long, val newBalance: Long) : ProcessResult()
        data class CreditFailed(val smsId: String, val error: String) : ProcessResult()
    }
}
