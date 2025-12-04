package com.momoterminal.nfc

import android.util.Log
import com.google.gson.Gson
import com.google.gson.reflect.TypeToken
import com.momoterminal.data.repository.NfcRepository
import com.momoterminal.data.repository.WalletRepository
import com.momoterminal.data.local.entity.NfcTagEntity
import com.momoterminal.domain.model.ReferenceType
import com.momoterminal.domain.model.TokenTransactionType
import com.momoterminal.domain.model.TokenWallet
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Service for NFC tag operations with wallet integration.
 * Handles token_pack NFC tags for crediting/debiting wallets.
 */
@Singleton
class NfcWalletIntegrationService @Inject constructor(
    private val nfcRepository: NfcRepository,
    private val walletRepository: WalletRepository
) {
    companion object {
        private const val TAG = "NfcWalletIntegration"
        const val ENTITY_TYPE_TOKEN_PACK = "token_pack"
        const val ENTITY_TYPE_MERCHANT = "merchant"
        const val ENTITY_TYPE_USER = "user"
    }

    private val gson = Gson()

    /**
     * Process NFC tag scan - handles different entity types.
     */
    suspend fun processTagScan(userId: String, tagId: String, tagData: ByteArray?): ScanResult {
        val existingTag = nfcRepository.findByTagId(tagId)
        
        return when (existingTag?.entityType) {
            ENTITY_TYPE_TOKEN_PACK -> processTokenPack(userId, existingTag)
            ENTITY_TYPE_MERCHANT -> ScanResult.MerchantTag(existingTag.entityId, existingTag.metadata)
            ENTITY_TYPE_USER -> ScanResult.UserTag(existingTag.entityId)
            null -> ScanResult.UnknownTag(tagId)
            else -> ScanResult.UnknownTag(tagId)
        }
    }

    private suspend fun processTokenPack(userId: String, tag: NfcTagEntity): ScanResult {
        val metadata = tag.metadata?.let { parseMetadata(it) } ?: emptyMap()
        val tokenAmount = metadata["amount"]?.toLongOrNull() ?: 0L
        val isRedeemed = metadata["redeemed"] == "true"

        if (isRedeemed) return ScanResult.TokenPackAlreadyRedeemed(tag.tagId)
        if (tokenAmount <= 0) return ScanResult.InvalidTokenPack(tag.tagId, "Invalid amount")

        val wallet = walletRepository.getOrCreateWallet(userId)
        val result = walletRepository.applyTransaction(
            walletId = wallet.id,
            amount = tokenAmount,
            type = TokenTransactionType.NFC_CREDIT,
            reference = tag.tagId,
            referenceType = ReferenceType.NFC_TAG,
            description = "Token pack redeemed via NFC",
            metadata = mapOf("tagId" to tag.tagId, "originalAmount" to tokenAmount.toString())
        )

        return if (result.isSuccess) {
            val updatedMetadata = metadata.toMutableMap().apply { put("redeemed", "true") }
            nfcRepository.saveTag(tag.copy(metadata = gson.toJson(updatedMetadata)))
            nfcRepository.updateLastScanned(tag.tagId)
            Log.d(TAG, "Token pack redeemed: +$tokenAmount tokens")
            ScanResult.TokenPackRedeemed(tag.tagId, tokenAmount, result.getOrNull()!!)
        } else {
            ScanResult.CreditFailed(tag.tagId, result.exceptionOrNull()?.message ?: "Unknown error")
        }
    }

    suspend fun registerTag(
        tagId: String,
        entityType: String,
        entityId: String,
        metadata: Map<String, String> = emptyMap()
    ): Boolean {
        return try {
            val tag = NfcTagEntity(
                tagId = tagId,
                entityType = entityType,
                entityId = entityId,
                metadata = if (metadata.isNotEmpty()) gson.toJson(metadata) else null
            )
            nfcRepository.saveTag(tag)
            Log.d(TAG, "Tag registered: $tagId -> $entityType:$entityId")
            true
        } catch (e: Exception) {
            Log.e(TAG, "Failed to register tag", e)
            false
        }
    }

    suspend fun createTokenPackTag(tagId: String, tokenAmount: Long): Boolean {
        return registerTag(
            tagId = tagId,
            entityType = ENTITY_TYPE_TOKEN_PACK,
            entityId = "pack_${System.currentTimeMillis()}",
            metadata = mapOf("amount" to tokenAmount.toString(), "redeemed" to "false")
        )
    }

    private fun parseMetadata(json: String): Map<String, String> {
        return try {
            val type = object : TypeToken<Map<String, String>>() {}.type
            gson.fromJson(json, type)
        } catch (e: Exception) {
            emptyMap()
        }
    }

    sealed class ScanResult {
        data class TokenPackRedeemed(val tagId: String, val tokens: Long, val wallet: TokenWallet) : ScanResult()
        data class TokenPackAlreadyRedeemed(val tagId: String) : ScanResult()
        data class InvalidTokenPack(val tagId: String, val reason: String) : ScanResult()
        data class MerchantTag(val merchantId: String, val metadata: String?) : ScanResult()
        data class UserTag(val userId: String) : ScanResult()
        data class UnknownTag(val tagId: String) : ScanResult()
        data class CreditFailed(val tagId: String, val error: String) : ScanResult()
    }
}
