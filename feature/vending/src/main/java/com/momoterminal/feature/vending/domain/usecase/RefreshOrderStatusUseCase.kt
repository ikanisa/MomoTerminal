package com.momoterminal.feature.vending.domain.usecase
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import javax.inject.Inject
class RefreshOrderStatusUseCase @Inject constructor(private val repository: VendingRepository) {
    suspend operator fun invoke(orderId: String) = repository.refreshOrderStatus(orderId)
}
