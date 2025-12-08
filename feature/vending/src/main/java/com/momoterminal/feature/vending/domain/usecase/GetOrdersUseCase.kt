package com.momoterminal.feature.vending.domain.usecase

import com.momoterminal.feature.vending.domain.model.VendingOrder
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetOrdersUseCase @Inject constructor(
    private val repository: VendingRepository
) {
    operator fun invoke(): Flow<Result<List<VendingOrder>>> {
        return repository.getOrders()
    }
}
