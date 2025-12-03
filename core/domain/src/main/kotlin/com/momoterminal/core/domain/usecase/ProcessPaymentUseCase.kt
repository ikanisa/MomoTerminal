package com.momoterminal.core.domain.usecase

import com.momoterminal.core.domain.model.Provider
import com.momoterminal.core.common.Result
import javax.inject.Inject

/**
 * Use case for processing payment by generating USSD code.
 */
class ProcessPaymentUseCase @Inject constructor() {
    
    /**
     * Process a payment by generating the appropriate USSD code.
     * 
     * @param provider The mobile money provider
     * @param merchantCode The merchant's code/phone number
     * @param amount The payment amount
     * @return Result containing the USSD dial string
     */
    operator fun invoke(
        provider: Provider,
        merchantCode: String,
        amount: Double
    ): Result<PaymentData> {
        // Validate inputs
        if (merchantCode.isBlank()) {
            return Result.Error(IllegalArgumentException("Merchant code cannot be empty"))
        }
        
        if (amount <= 0) {
            return Result.Error(IllegalArgumentException("Amount must be greater than zero"))
        }
        
        // Generate USSD code
        val ussdCode = provider.generateUssdCode(merchantCode, amount)
        val dialString = "tel:$ussdCode"
        
        return Result.Success(
            PaymentData(
                ussdCode = ussdCode,
                dialString = dialString,
                provider = provider,
                merchantCode = merchantCode,
                amount = amount
            )
        )
    }
}

/**
 * Data class containing payment information.
 */
data class PaymentData(
    val ussdCode: String,
    val dialString: String,
    val provider: Provider,
    val merchantCode: String,
    val amount: Double
)
