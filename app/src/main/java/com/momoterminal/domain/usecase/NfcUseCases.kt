package com.momoterminal.domain.usecase

import com.momoterminal.data.local.entity.NfcTagEntity
import com.momoterminal.data.repository.NfcRepository
import com.momoterminal.nfc.NfcWalletIntegrationService
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetNfcTagsUseCase @Inject constructor(
    private val nfcRepository: NfcRepository
) {
    fun observeAll(): Flow<List<NfcTagEntity>> = nfcRepository.getAllTags()
    
    fun observeByType(type: String): Flow<List<NfcTagEntity>> = nfcRepository.getTagsByType(type)
    
    suspend fun findByTagId(tagId: String): NfcTagEntity? = nfcRepository.findByTagId(tagId)
}

class ProcessNfcTagUseCase @Inject constructor(
    private val nfcWalletService: NfcWalletIntegrationService
) {
    suspend operator fun invoke(
        userId: String,
        tagId: String,
        tagData: ByteArray? = null
    ): NfcWalletIntegrationService.ScanResult {
        return nfcWalletService.processTagScan(userId, tagId, tagData)
    }
}

class RegisterNfcTagUseCase @Inject constructor(
    private val nfcRepository: NfcRepository
) {
    suspend operator fun invoke(
        tagId: String,
        entityType: String,
        entityId: String,
        metadata: String? = null
    ): Boolean {
        return try {
            val tag = NfcTagEntity(
                tagId = tagId,
                entityType = entityType,
                entityId = entityId,
                metadata = metadata
            )
            nfcRepository.saveTag(tag)
            true
        } catch (e: Exception) {
            false
        }
    }
}

class CreateTokenPackUseCase @Inject constructor(
    private val nfcWalletService: NfcWalletIntegrationService
) {
    suspend operator fun invoke(tagId: String, tokenAmount: Long): Boolean {
        return nfcWalletService.createTokenPackTag(tagId, tokenAmount)
    }
}
