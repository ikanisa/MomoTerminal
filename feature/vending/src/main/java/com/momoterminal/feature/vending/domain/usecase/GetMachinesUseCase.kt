package com.momoterminal.feature.vending.domain.usecase
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import javax.inject.Inject
class GetMachinesUseCase @Inject constructor(private val repository: VendingRepository) {
    operator fun invoke(latitude: Double? = null, longitude: Double? = null, radiusKm: Int? = null) =
        repository.getMachines(latitude, longitude, radiusKm)
}
