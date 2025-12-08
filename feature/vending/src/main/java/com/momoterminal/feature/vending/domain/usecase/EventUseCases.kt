package com.momoterminal.feature.vending.domain.usecase

import com.momoterminal.feature.vending.domain.model.*
import com.momoterminal.feature.vending.domain.repository.VendingRepository
import kotlinx.coroutines.flow.Flow
import javax.inject.Inject

class GetEventDetailsUseCase @Inject constructor(
    private val repository: VendingRepository
) {
    operator fun invoke(eventId: String): Flow<Result<VendingEvent>> {
        return repository.getEventDetails(eventId)
    }
}

class JoinEventUseCase @Inject constructor(
    private val repository: VendingRepository
) {
    suspend operator fun invoke(eventCode: String): Result<EventGuest> {
        return repository.joinEvent(eventCode)
    }
}

class CreateEventOrderUseCase @Inject constructor(
    private val repository: VendingRepository,
    private val getWalletBalanceUseCase: com.momoterminal.feature.wallet.domain.usecase.GetWalletBalanceUseCase
) {
    suspend operator fun invoke(
        eventId: String,
        machineId: String,
        productId: String,
        quantity: Int,
        serviceMode: ServiceMode,
        deliveryLocation: DeliveryLocation?,
        cupsIncluded: Boolean,
        useEventBudget: Boolean
    ): Result<EventVendingOrder> {
        // Validate quantity limits based on product type
        if (quantity > 10) {
            return Result.failure(Exception("Maximum 10 cups per order"))
        }
        
        // Get event details to check budget and limits
        val eventResult = repository.getEventDetails(eventId)
        // ... validation logic
        
        // If using personal wallet, check balance
        if (!useEventBudget) {
            // Use existing wallet validation
        }
        
        return repository.createEventOrder(
            eventId = eventId,
            machineId = machineId,
            productId = productId,
            quantity = quantity,
            serviceMode = serviceMode,
            deliveryLocation = deliveryLocation,
            cupsIncluded = cupsIncluded,
            useEventBudget = useEventBudget
        )
    }
}

class GetActiveEventOrdersUseCase @Inject constructor(
    private val repository: VendingRepository
) {
    operator fun invoke(
        eventId: String,
        machineId: String,
        filterByServiceMode: ServiceMode? = null,
        filterByLocation: String? = null
    ): Flow<Result<List<EventVendingOrder>>> {
        return repository.getActiveEventOrders(eventId, machineId, filterByServiceMode, filterByLocation)
    }
}

class ServeOrderUseCase @Inject constructor(
    private val repository: VendingRepository
) {
    suspend operator fun invoke(
        orderId: String,
        staffId: String,
        staffPin: String?,
        servesCount: Int = 1
    ): Result<EventVendingOrder> {
        // Validate staff auth if required
        if (staffPin != null) {
            val staffValid = repository.validateStaffPin(staffId, staffPin)
            if (!staffValid) {
                return Result.failure(Exception("Invalid staff PIN"))
            }
        }
        
        return repository.serveOrder(orderId, staffId, servesCount)
    }
}

class GetEventDashboardUseCase @Inject constructor(
    private val repository: VendingRepository
) {
    operator fun invoke(eventId: String): Flow<Result<EventDashboard>> {
        return repository.getEventDashboard(eventId)
    }
}

data class EventDashboard(
    val event: VendingEvent,
    val stats: EventStats,
    val activeMachines: List<VendingMachine>,
    val recentOrders: List<EventVendingOrder>,
    val alerts: List<EventAlert>
)

data class EventStats(
    val totalOrders: Int,
    val totalRevenue: Long,
    val budgetSpent: Long,
    val budgetRemaining: Long,
    val guestsServed: Int,
    val averageOrderValue: Long,
    val topProducts: List<Pair<String, Int>>,
    val ordersPerHour: Map<Int, Int>
)

data class EventAlert(
    val id: String,
    val type: AlertType,
    val message: String,
    val severity: AlertSeverity,
    val timestamp: Long
)

enum class AlertType {
    LOW_BUDGET,
    MACHINE_OFFLINE,
    LOW_STOCK,
    HIGH_DEMAND,
    STAFF_NEEDED
}

enum class AlertSeverity {
    INFO,
    WARNING,
    CRITICAL
}
