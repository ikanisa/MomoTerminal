package com.momoterminal.feature.vending.domain.usecase

import com.momoterminal.feature.vending.domain.model.VendingMachine
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetMachinesUseCase @Inject constructor(
    private val repository: VendingRepository
) {
    operator fun invoke(
        latitude: Double? = null,
        longitude: Double? = null,
        radiusKm: Int? = null
    ): Flow<Result<List<VendingMachine>>> {
        return repository.getMachines(latitude, longitude, radiusKm)
    }
}
