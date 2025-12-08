package com.momoterminal.feature.vending.domain.model

data class VendingCode(
    val code: String,
    val orderId: String,
    val machineId: String,
    val expiresAt: Long,
    val usedAt: Long? = null,
    val totalServes: Int = 1,
    val remainingServes: Int = 1,
    val closedAt: Long? = null
) {
    fun isExpired(): Boolean {
        return System.currentTimeMillis() > expiresAt
    }
    
    fun isUsed(): Boolean {
        return remainingServes <= 0 || usedAt != null
    }
    
    fun isInProgress(): Boolean {
        return !isExpired() && !isUsed() && usedAt != null && remainingServes > 0
    }
    
    fun remainingSeconds(): Long {
        val remaining = (expiresAt - System.currentTimeMillis()) / 1000
        return if (remaining > 0) remaining else 0
    }
    
    fun formattedCode(): String {
        return code.chunked(2).joinToString(" ")
    }
    
    fun servesUsed(): Int = totalServes - remainingServes
}
