package com.momoterminal.feature.vending.ui.event

import androidx.compose.foundation.layout.*
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.selection.selectableGroup
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.feature.vending.domain.model.*

@Composable
fun EventOrderScreen(
    event: VendingEvent,
    product: com.momoterminal.feature.vending.domain.model.VendingProduct,
    machineId: String,
    machineName: String,
    onOrderConfirm: (
        quantity: Int,
        serviceMode: ServiceMode,
        location: DeliveryLocation?,
        includeCups: Boolean,
        useEventBudget: Boolean
    ) -> Unit,
    onNavigateBack: () -> Unit,
    modifier: Modifier = Modifier
) {
    var quantity by remember { mutableIntStateOf(1) }
    var selectedServiceMode by remember { mutableStateOf(event.serviceConfig.defaultServiceMode) }
    var locationInput by remember { mutableStateOf("") }
    var includeCups by remember { mutableStateOf(true) }
    var useEventBudget by remember { mutableStateOf(true) }

    val config = event.serviceConfig
    val maxQuantity = config.sessionSettings?.maxQuantityPerSession ?: 10

    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Order for ${event.name}") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(Icons.Default.ArrowBack, "Back")
                    }
                }
            )
        }
    ) { padding ->
        Column(
            modifier = modifier
                .fillMaxSize()
                .padding(padding)
                .verticalScroll(rememberScrollState())
                .padding(16.dp),
            verticalArrangement = Arrangement.spacedBy(20.dp)
        ) {
            // Product info
            ProductInfoCard(product = product)

            // Service Mode Selector (big buttons for events)
            ServiceModeSelector(
                availableModes = config.allowedServiceModes,
                selectedMode = selectedServiceMode,
                onModeSelected = { selectedServiceMode = it },
                busyMode = config.busyMode
            )

            // Quantity Selector (huge buttons)
            QuantitySelector(
                quantity = quantity,
                maxQuantity = maxQuantity,
                onQuantityChange = { quantity = it },
                busyMode = config.busyMode
            )

            // Location Input (adaptive to event type)
            if (selectedServiceMode != ServiceMode.SELF_SERVE) {
                LocationInputSection(
                    serviceMode = selectedServiceMode,
                    locationType = config.locationType,
                    locationOptions = config.locationOptions,
                    locationInput = locationInput,
                    onLocationChange = { locationInput = it }
                )
            }

            // Cups toggle
            CupsToggle(
                includeCups = includeCups,
                onToggle = { includeCups = it }
            )

            // Payment method
            PaymentMethodSelector(
                event = event,
                useEventBudget = useEventBudget,
                onToggle = { useEventBudget = it },
                quantity = quantity,
                pricePerUnit = product.price
            )

            Spacer(modifier = Modifier.height(8.dp))

            // Confirm button
            Button(
                onClick = {
                    val location = if (selectedServiceMode != ServiceMode.SELF_SERVE && locationInput.isNotBlank()) {
                        DeliveryLocation(
                            type = config.locationType,
                            value = locationInput,
                            label = config.locationType.displayName
                        )
                    } else null

                    onOrderConfirm(quantity, selectedServiceMode, location, includeCups, useEventBudget)
                },
                modifier = Modifier
                    .fillMaxWidth()
                    .height(if (config.busyMode) 64.dp else 56.dp),
                enabled = selectedServiceMode == ServiceMode.SELF_SERVE || locationInput.isNotBlank()
            ) {
                Icon(Icons.Default.CheckCircle, contentDescription = null)
                Spacer(Modifier.width(8.dp))
                Text(
                    "Confirm Order",
                    style = if (config.busyMode) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    }
                )
            }
        }
    }
}

@Composable
private fun ProductInfoCard(product: com.momoterminal.feature.vending.domain.model.VendingProduct) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.primaryContainer
        )
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                Icons.Default.LocalCafe,
                contentDescription = null,
                modifier = Modifier.size(48.dp),
                tint = MaterialTheme.colorScheme.primary
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    product.name,
                    style = MaterialTheme.typography.titleLarge,
                    fontWeight = FontWeight.Bold
                )
                Text(
                    "${product.servingSizeML} ml",
                    style = MaterialTheme.typography.bodyMedium
                )
            }
            Text(
                "${product.pricePerServing} XAF",
                style = MaterialTheme.typography.titleLarge,
                fontWeight = FontWeight.Bold,
                color = MaterialTheme.colorScheme.primary
            )
        }
    }
}

@Composable
private fun ServiceModeSelector(
    availableModes: List<ServiceMode>,
    selectedMode: ServiceMode,
    onModeSelected: (ServiceMode) -> Unit,
    busyMode: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Service Type",
            style = if (busyMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Column(
            modifier = Modifier.selectableGroup(),
            verticalArrangement = Arrangement.spacedBy(8.dp)
        ) {
            availableModes.forEach { mode ->
                ServiceModeChip(
                    mode = mode,
                    selected = mode == selectedMode,
                    onClick = { onModeSelected(mode) },
                    busyMode = busyMode
                )
            }
        }
    }
}

@Composable
private fun ServiceModeChip(
    mode: ServiceMode,
    selected: Boolean,
    onClick: () -> Unit,
    busyMode: Boolean
) {
    Surface(
        onClick = onClick,
        modifier = Modifier
            .fillMaxWidth()
            .height(if (busyMode) 72.dp else 60.dp),
        shape = RoundedCornerShape(12.dp),
        color = if (selected) {
            MaterialTheme.colorScheme.primary
        } else {
            MaterialTheme.colorScheme.surfaceVariant
        },
        shadowElevation = if (selected) 4.dp else 0.dp
    ) {
        Row(
            modifier = Modifier.padding(16.dp),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Icon(
                when (mode) {
                    ServiceMode.SELF_SERVE -> Icons.Default.DirectionsWalk
                    ServiceMode.TABLE_SERVICE -> Icons.Default.TableRestaurant
                    ServiceMode.ZONE_SERVICE -> Icons.Default.Groups
                    ServiceMode.PICKUP -> Icons.Default.ShoppingBag
                },
                contentDescription = null,
                modifier = Modifier.size(if (busyMode) 32.dp else 24.dp),
                tint = if (selected) {
                    MaterialTheme.colorScheme.onPrimary
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                }
            )
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    mode.displayName,
                    style = if (busyMode) {
                        MaterialTheme.typography.titleLarge
                    } else {
                        MaterialTheme.typography.titleMedium
                    },
                    fontWeight = FontWeight.Bold,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant
                    }
                )
                Text(
                    mode.description,
                    style = MaterialTheme.typography.bodySmall,
                    color = if (selected) {
                        MaterialTheme.colorScheme.onPrimary.copy(alpha = 0.8f)
                    } else {
                        MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                    }
                )
            }
            if (selected) {
                Icon(
                    Icons.Default.CheckCircle,
                    contentDescription = "Selected",
                    tint = MaterialTheme.colorScheme.onPrimary
                )
            }
        }
    }
}

@Composable
private fun QuantitySelector(
    quantity: Int,
    maxQuantity: Int,
    onQuantityChange: (Int) -> Unit,
    busyMode: Boolean
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Quantity",
            style = if (busyMode) MaterialTheme.typography.titleLarge else MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Quick presets
            listOf(1, 2, 4, 6).forEach { preset ->
                if (preset <= maxQuantity) {
                    FilterChip(
                        selected = quantity == preset,
                        onClick = { onQuantityChange(preset) },
                        label = {
                            Text(
                                "$preset",
                                style = if (busyMode) {
                                    MaterialTheme.typography.titleLarge
                                } else {
                                    MaterialTheme.typography.titleMedium
                                }
                            )
                        },
                        modifier = Modifier.weight(1f)
                    )
                }
            }
        }

        // +/- buttons
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            FilledIconButton(
                onClick = { if (quantity > 1) onQuantityChange(quantity - 1) },
                modifier = Modifier.size(if (busyMode) 64.dp else 48.dp),
                enabled = quantity > 1
            ) {
                Icon(Icons.Default.Remove, contentDescription = "Decrease")
            }

            Text(
                "$quantity",
                style = if (busyMode) {
                    MaterialTheme.typography.displayMedium
                } else {
                    MaterialTheme.typography.displaySmall
                },
                fontWeight = FontWeight.Bold
            )

            FilledIconButton(
                onClick = { if (quantity < maxQuantity) onQuantityChange(quantity + 1) },
                modifier = Modifier.size(if (busyMode) 64.dp else 48.dp),
                enabled = quantity < maxQuantity
            ) {
                Icon(Icons.Default.Add, contentDescription = "Increase")
            }
        }
    }
}

@Composable
private fun LocationInputSection(
    serviceMode: ServiceMode,
    locationType: LocationType,
    locationOptions: List<String>?,
    locationInput: String,
    onLocationChange: (String) -> Unit
) {
    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            locationType.displayName,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        if (!locationOptions.isNullOrEmpty()) {
            // Quick chips for predefined options
            LocationChips(
                options = locationOptions,
                selectedValue = locationInput,
                onSelect = onLocationChange
            )
        }

        // Text input
        OutlinedTextField(
            value = locationInput,
            onValueChange = onLocationChange,
            label = { Text("Enter ${locationType.displayName.lowercase()}") },
            placeholder = { Text(locationType.placeholder) },
            leadingIcon = {
                Icon(
                    when (locationType) {
                        LocationType.TABLE -> Icons.Default.TableRestaurant
                        LocationType.ZONE -> Icons.Default.LocationOn
                        LocationType.SECTION -> Icons.Default.Stadium
                        LocationType.SEAT -> Icons.Default.Chair
                        LocationType.PICKUP_POINT -> Icons.Default.Store
                    },
                    contentDescription = null
                )
            },
            modifier = Modifier.fillMaxWidth(),
            singleLine = true
        )
    }
}

@Composable
private fun LocationChips(
    options: List<String>,
    selectedValue: String,
    onSelect: (String) -> Unit
) {
    Column(
        verticalArrangement = Arrangement.spacedBy(8.dp)
    ) {
        options.take(12).chunked(4).forEach { rowOptions ->
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                rowOptions.forEach { option ->
                    FilterChip(
                        selected = selectedValue == option,
                        onClick = { onSelect(option) },
                        label = { Text(option) },
                        modifier = Modifier.weight(1f)
                    )
                }
                // Fill remaining slots
                repeat(4 - rowOptions.size) {
                    Spacer(modifier = Modifier.weight(1f))
                }
            }
        }
    }
}

@Composable
private fun CupsToggle(
    includeCups: Boolean,
    onToggle: (Boolean) -> Unit
) {
    Card(
        modifier = Modifier.fillMaxWidth(),
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surfaceVariant
        )
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Row(
                horizontalArrangement = Arrangement.spacedBy(12.dp),
                verticalAlignment = Alignment.CenterVertically
            ) {
                Icon(Icons.Default.LocalDrink, contentDescription = null)
                Column {
                    Text(
                        "Include Disposable Cups",
                        style = MaterialTheme.typography.titleSmall,
                        fontWeight = FontWeight.Medium
                    )
                    Text(
                        if (includeCups) "Cups will be provided" else "Bring your own cup",
                        style = MaterialTheme.typography.bodySmall,
                        color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.7f)
                    )
                }
            }
            Switch(
                checked = includeCups,
                onCheckedChange = onToggle
            )
        }
    }
}

@Composable
private fun PaymentMethodSelector(
    event: VendingEvent,
    useEventBudget: Boolean,
    onToggle: (Boolean) -> Unit,
    quantity: Int,
    pricePerUnit: Long
) {
    val totalCost = quantity * pricePerUnit

    Column(verticalArrangement = Arrangement.spacedBy(12.dp)) {
        Text(
            "Payment",
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.Bold
        )

        Card(
            modifier = Modifier.fillMaxWidth(),
            colors = CardDefaults.cardColors(
                containerColor = if (useEventBudget) {
                    MaterialTheme.colorScheme.primaryContainer
                } else {
                    MaterialTheme.colorScheme.surfaceVariant
                }
            )
        ) {
            Column(modifier = Modifier.padding(16.dp)) {
                Row(
                    modifier = Modifier.fillMaxWidth(),
                    horizontalArrangement = Arrangement.SpaceBetween,
                    verticalAlignment = Alignment.CenterVertically
                ) {
                    Column(modifier = Modifier.weight(1f)) {
                        Text(
                            if (useEventBudget) "Event Budget (Free for you)" else "Personal Wallet",
                            style = MaterialTheme.typography.titleSmall,
                            fontWeight = FontWeight.Bold
                        )
                        Text(
                            if (useEventBudget) {
                                when (event.budgetConfig?.budgetType) {
                                    BudgetType.OPEN_BAR -> "Funded by host"
                                    BudgetType.GUEST_ALLOWANCE -> "From your allowance"
                                    BudgetType.HYBRID -> "Subsidized pricing"
                                    else -> "Event budget"
                                }
                            } else {
                                "$totalCost XAF will be deducted"
                            },
                            style = MaterialTheme.typography.bodySmall
                        )
                    }
                    Switch(
                        checked = useEventBudget,
                        onCheckedChange = onToggle
                    )
                }
            }
        }
    }
}
