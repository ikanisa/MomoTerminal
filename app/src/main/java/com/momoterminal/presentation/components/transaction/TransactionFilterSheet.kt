package com.momoterminal.presentation.components.transaction

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.sizeIn
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyRow
import androidx.compose.foundation.lazy.items
import androidx.compose.material3.Button
import androidx.compose.material3.DatePicker
import androidx.compose.material3.DatePickerDialog
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.FilterChip
import androidx.compose.material3.HorizontalDivider
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.ModalBottomSheet
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.material3.rememberDatePickerState
import androidx.compose.material3.rememberModalBottomSheetState
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.momoterminal.R
import com.momoterminal.domain.model.Provider
import com.momoterminal.domain.model.SyncStatus
import com.momoterminal.domain.model.TransactionFilter
import com.momoterminal.presentation.accessibility.minTouchTarget
import java.text.SimpleDateFormat
import java.util.Date
import java.util.Locale

/**
 * Bottom sheet for filtering transactions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun TransactionFilterSheet(
    currentFilter: TransactionFilter,
    onFilterChange: (TransactionFilter) -> Unit,
    onDismiss: () -> Unit,
    modifier: Modifier = Modifier
) {
    val sheetState = rememberModalBottomSheetState(skipPartiallyExpanded = true)
    var tempFilter by remember { mutableStateOf(currentFilter) }
    var showStartDatePicker by remember { mutableStateOf(false) }
    var showEndDatePicker by remember { mutableStateOf(false) }
    
    ModalBottomSheet(
        onDismissRequest = onDismiss,
        sheetState = sheetState,
        modifier = modifier
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp)
        ) {
            // Header
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.SpaceBetween,
                verticalAlignment = Alignment.CenterVertically
            ) {
                Text(
                    text = stringResource(R.string.filter_transactions),
                    style = MaterialTheme.typography.titleLarge
                )
                
                if (tempFilter.hasActiveFilters) {
                    TextButton(onClick = { tempFilter = TransactionFilter() }) {
                        Text(stringResource(R.string.filter_all))
                    }
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            
            // Provider filter
            Text(
                text = stringResource(R.string.provider),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(Provider.entries) { provider ->
                    FilterChip(
                        selected = tempFilter.provider == provider,
                        onClick = {
                            tempFilter = if (tempFilter.provider == provider) {
                                tempFilter.withProvider(null)
                            } else {
                                tempFilter.withProvider(provider)
                            }
                        },
                        label = { Text(provider.displayName) },
                        modifier = Modifier
                            .minTouchTarget()
                            .semantics {
                                contentDescription = "${provider.displayName} filter, ${if (tempFilter.provider == provider) "selected" else "not selected"}"
                            }
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Status filter
            Text(
                text = stringResource(R.string.sync_status),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            LazyRow(
                horizontalArrangement = Arrangement.spacedBy(8.dp)
            ) {
                items(SyncStatus.entries) { status ->
                    FilterChip(
                        selected = tempFilter.status == status,
                        onClick = {
                            tempFilter = if (tempFilter.status == status) {
                                tempFilter.withStatus(null)
                            } else {
                                tempFilter.withStatus(status)
                            }
                        },
                        label = { Text(status.value) },
                        modifier = Modifier.minTouchTarget()
                    )
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
            HorizontalDivider()
            Spacer(modifier = Modifier.height(16.dp))
            
            // Date range filter
            Text(
                text = stringResource(R.string.date_range),
                style = MaterialTheme.typography.titleSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
            
            Spacer(modifier = Modifier.height(8.dp))
            
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                // Start date
                DatePickerButton(
                    label = "From",
                    date = tempFilter.startDate,
                    onClick = { showStartDatePicker = true },
                    onClear = { tempFilter = tempFilter.withDateRange(null, tempFilter.endDate) },
                    modifier = Modifier.weight(1f)
                )
                
                // End date
                DatePickerButton(
                    label = "To",
                    date = tempFilter.endDate,
                    onClick = { showEndDatePicker = true },
                    onClear = { tempFilter = tempFilter.withDateRange(tempFilter.startDate, null) },
                    modifier = Modifier.weight(1f)
                )
            }
            
            Spacer(modifier = Modifier.height(24.dp))
            
            // Action buttons
            Row(
                modifier = Modifier.fillMaxWidth(),
                horizontalArrangement = Arrangement.spacedBy(12.dp)
            ) {
                OutlinedButton(
                    onClick = onDismiss,
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 48.dp)
                ) {
                    Text("Cancel")
                }
                
                Button(
                    onClick = {
                        onFilterChange(tempFilter)
                        onDismiss()
                    },
                    modifier = Modifier
                        .weight(1f)
                        .sizeIn(minHeight = 48.dp)
                ) {
                    Text("Apply")
                }
            }
            
            Spacer(modifier = Modifier.height(16.dp))
        }
    }
    
    // Date picker dialogs
    if (showStartDatePicker) {
        DatePickerDialogWrapper(
            initialDate = tempFilter.startDate,
            onDateSelected = { date ->
                tempFilter = tempFilter.withDateRange(date, tempFilter.endDate)
                showStartDatePicker = false
            },
            onDismiss = { showStartDatePicker = false }
        )
    }
    
    if (showEndDatePicker) {
        DatePickerDialogWrapper(
            initialDate = tempFilter.endDate,
            onDateSelected = { date ->
                tempFilter = tempFilter.withDateRange(tempFilter.startDate, date)
                showEndDatePicker = false
            },
            onDismiss = { showEndDatePicker = false }
        )
    }
}

/**
 * Button for selecting a date.
 */
@Composable
private fun DatePickerButton(
    label: String,
    date: Date?,
    onClick: () -> Unit,
    onClear: () -> Unit,
    modifier: Modifier = Modifier
) {
    val dateFormat = remember { SimpleDateFormat("MMM d, yyyy", Locale.getDefault()) }
    
    Column(modifier = modifier) {
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(4.dp))
        
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .clickable(onClick = onClick)
                .padding(vertical = 12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = date?.let { dateFormat.format(it) } ?: "Select date",
                style = MaterialTheme.typography.bodyMedium,
                color = if (date != null) {
                    MaterialTheme.colorScheme.onSurface
                } else {
                    MaterialTheme.colorScheme.onSurfaceVariant
                },
                modifier = Modifier.weight(1f)
            )
            
            if (date != null) {
                TextButton(
                    onClick = onClear,
                    modifier = Modifier.sizeIn(minWidth = 48.dp, minHeight = 48.dp)
                ) {
                    Text("Clear")
                }
            }
        }
    }
}

/**
 * Wrapper for Material3 DatePickerDialog.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
private fun DatePickerDialogWrapper(
    initialDate: Date?,
    onDateSelected: (Date) -> Unit,
    onDismiss: () -> Unit
) {
    val datePickerState = rememberDatePickerState(
        initialSelectedDateMillis = initialDate?.time
    )
    
    DatePickerDialog(
        onDismissRequest = onDismiss,
        confirmButton = {
            TextButton(
                onClick = {
                    datePickerState.selectedDateMillis?.let { millis ->
                        onDateSelected(Date(millis))
                    }
                }
            ) {
                Text("OK")
            }
        },
        dismissButton = {
            TextButton(onClick = onDismiss) {
                Text("Cancel")
            }
        }
    ) {
        DatePicker(state = datePickerState)
    }
}
