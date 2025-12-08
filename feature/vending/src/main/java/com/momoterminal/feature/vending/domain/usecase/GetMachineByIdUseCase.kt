package com.momoterminal.feature.vending.domain.usecase

import com.momoterminal.feature.vending.domain.model.VendingMachine
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import javax.inject.Inject

class GetMachineByIdUseCase @Inject constructor(
    private val repository: VendingRepository
) {
    suspend operator fun invoke(machineId: String): Result<VendingMachine> {
        return repository.getMachineById(machineId)
    }
}
