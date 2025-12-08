package com.momoterminal.feature.vending.domain.model

enum class ServiceMode(val displayName: String, val description: String) {
    SELF_SERVE("Self-Serve", "Walk to machine yourself"),
    TABLE_SERVICE("Table Service", "Waiter delivers to your table"),
    ZONE_SERVICE("Zone Service", "Waiter delivers to your zone"),
    PICKUP("Pickup", "Collect at pickup point")
}

enum class LocationType(val displayName: String, val placeholder: String) {
    TABLE("Table Number", "e.g., 12"),
    ZONE("Zone", "e.g., Zone A"),
    SECTION("Section", "e.g., VIP West"),
    SEAT("Seat Number", "e.g., A-23"),
    PICKUP_POINT("Pickup Point", "e.g., North Kiosk")
}

data class DeliveryLocation(
    val type: LocationType,
    val label: String,      // "Table", "Zone", "Section", etc.
    val value: String       // "12", "VIP West", "North A", etc.
) {
    fun formattedDisplay(): String = when (type) {
        LocationType.TABLE -> "T$value"
        LocationType.ZONE -> "Zone $value"
        LocationType.SECTION -> "Section $value"
        LocationType.SEAT -> "Seat $value"
        LocationType.PICKUP_POINT -> value
    }
}

data class VendingEvent(
    val id: String,
    val name: String,               // "John & Mary's Wedding"
    val venueName: String,
    val eventType: EventType,
    val startTime: Long,            // Unix timestamp millis
    val endTime: Long,
    val status: EventStatus,
    
    // Service configuration
    val serviceConfig: EventServiceConfig,
    
    // Budget & payment
    val budgetConfig: EventBudgetConfig?,
    
    // Linked machines
    val machineIds: List<String>,
    
    // Branding
    val brandingImageUrl: String? = null,
    val primaryColor: String? = null,
    
    val createdAt: Long,
    val hostUserId: String
)

@Serializable
enum class EventType {
    WEDDING,
    CONFERENCE,
    CORPORATE,
    STADIUM,
    CONCERT,
    PRIVATE_PARTY,
    OTHER
}

@Serializable
enum class EventStatus {
    UPCOMING,
    ACTIVE,
    PAUSED,
    COMPLETED,
    CANCELLED
}

@Serializable
data class EventServiceConfig(
    val defaultServiceMode: ServiceMode,
    val allowedServiceModes: List<ServiceMode>,
    
    // Location settings
    val locationType: LocationType,
    val locationRequired: Boolean = false,
    val locationLabel: String,          // "Table Number", "Zone", etc.
    val locationPlaceholder: String,    // "e.g., 12", "e.g., VIP West"
    val locationOptions: List<String>? = null,  // Predefined options
    
    // Session settings (overrides normal mode)
    val codeExpiryMinutes: Int = 15,    // Longer than normal 5 min
    val maxCupsPerSession: Int = 10,
    val maxAlcoholPerSession: Int = 6,
    
    // UI settings
    val showActiveOrdersBoard: Boolean = true,
    val busyModeEnabled: Boolean = true,  // Huge fonts, minimal steps
    
    // Staff
    val requireStaffAuth: Boolean = false,
    val staffPinRequired: Boolean = false
)

@Serializable
data class EventBudgetConfig(
    val budgetType: BudgetType,
    val totalBudget: Long,              // In cents
    val spentAmount: Long = 0,
    
    // Per-guest limits (optional)
    val guestAllowance: GuestAllowance? = null,
    
    // Auto-disable when depleted
    val pauseWhenDepleted: Boolean = true
)

@Serializable
enum class BudgetType {
    OPEN_BAR,           // Unlimited until budget cap
    GUEST_ALLOWANCE,    // Each guest gets X free drinks
    HYBRID              // Allowance + top-up allowed
}

@Serializable
data class GuestAllowance(
    val maxDrinks: Int,             // Total drinks per guest
    val maxAlcohol: Int,            // Max alcoholic drinks per guest
    val categories: Map<String, Int> // e.g., {"beer": 2, "cocktail": 1}
)

@Serializable
data class EventGuest(
    val id: String,
    val eventId: String,
    val userId: String,
    val guestName: String,
    val phoneLastFour: String,
    
    // Consumption tracking
    val drinksConsumed: Int = 0,
    val alcoholConsumed: Int = 0,
    val amountSpent: Long = 0,
    val freeAllowanceUsed: Long = 0,
    
    // Status
    val status: GuestStatus = GuestStatus.ACTIVE,
    val joinedAt: Long,
    val ageVerified: Boolean = false
)

@Serializable
enum class GuestStatus {
    ACTIVE,
    SUSPENDED,
    COMPLETED
}

@Serializable
data class EventStaff(
    val id: String,
    val eventId: String,
    val userId: String,
    val role: StaffRole,
    val pin: String? = null,
    val allowedCategories: List<String>? = null,  // null = all
    val isActive: Boolean = true
)

@Serializable
enum class StaffRole {
    WAITER,
    SUPERVISOR,
    BARTENDER,
    RUNNER
}

// Enhanced VendingOrder for event mode
fun VendingOrder.toEventOrder(
    deliveryLocation: DeliveryLocation? = null,
    serviceMode: ServiceMode = ServiceMode.SELF_SERVE,
    eventId: String? = null,
    staffId: String? = null,
    remainingServes: Int = 0,
    cupQuantity: Int = 0,
    cupsIncluded: Boolean = true
): EventVendingOrder = EventVendingOrder(
    orderId = id,
    eventId = eventId,
    userId = userId,
    machineId = machineId,
    machineName = machineName,
    machineLocation = machineLocation,
    productName = productName,
    productSizeML = productSizeML,
    amount = amount,
    status = status,
    serviceMode = serviceMode,
    deliveryLocation = deliveryLocation,
    staffId = staffId,
    quantity = cupQuantity,
    remainingServes = remainingServes,
    cupsIncluded = cupsIncluded,
    createdAt = createdAt,
    code = code
)

@Serializable
data class EventVendingOrder(
    val orderId: String,
    val eventId: String?,
    val userId: String,
    val machineId: String,
    val machineName: String,
    val machineLocation: String,
    val productName: String,
    val productSizeML: Int,
    val amount: Long,
    val status: OrderStatus,
    
    // Event-specific fields
    val serviceMode: ServiceMode,
    val deliveryLocation: DeliveryLocation?,
    val staffId: String?,              // Who served it
    val quantity: Int,                 // Number of cups
    val remainingServes: Int,          // For partial fulfillment
    val cupsIncluded: Boolean,
    
    val createdAt: Long,
    val code: VendingCode?
) {
    fun formattedOrderNumber(): String = "#E${orderId.takeLast(4).uppercase()}"
    
    fun formattedPhoneLastFour(): String = "****"  // Will be populated from user data
    
    fun displayRow(): String = buildString {
        append(formattedOrderNumber())
        deliveryLocation?.let {
            append(" — ${it.formattedDisplay()}")
        }
        append(" — $formattedPhoneLastFour")
        append(" — $productName x$quantity")
        if (cupsIncluded) append(" — Cups ✓")
    }
}
