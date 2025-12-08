package com.momoterminal.feature.vending.domain.usecase
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import javax.inject.Inject
class GetOrdersUseCase @Inject constructor(private val repository: VendingRepository) {
    operator fun invoke() = repository.getOrders()
}
