package com.momoterminal.presentation.components

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material.icons.filled.Search
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import com.momoterminal.R
import com.momoterminal.core.common.model.CountryConfig

/**
 * Country selector with search and provider display.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun CountrySelector(
    countries: List<CountryConfig>,
    selectedCountryCode: String?,
    onCountrySelected: (CountryConfig) -> Unit,
    modifier: Modifier = Modifier,
    showProviderInfo: Boolean = true,
    showPrimaryMarketsFirst: Boolean = true
) {
    var searchQuery by remember { mutableStateOf("") }

    val filteredCountries = remember(countries, searchQuery) {
        val filtered = if (searchQuery.isBlank()) {
            countries
        } else {
            countries.filter { country ->
                country.name.contains(searchQuery, ignoreCase = true) ||
                country.nameLocal?.contains(searchQuery, ignoreCase = true) == true ||
                country.nameFrench?.contains(searchQuery, ignoreCase = true) == true ||
                country.code.equals(searchQuery, ignoreCase = true) ||
                country.providerName.contains(searchQuery, ignoreCase = true)
            }
        }

        if (showPrimaryMarketsFirst) {
            filtered.sortedWith(compareBy({ !it.isPrimaryMarket }, { it.launchPriority }, { it.name }))
        } else {
            filtered.sortedBy { it.name }
        }
    }

    Column(modifier = modifier.fillMaxWidth()) {
        OutlinedTextField(
            value = searchQuery,
            onValueChange = { searchQuery = it },
            modifier = Modifier
                .fillMaxWidth()
                .padding(horizontal = 16.dp, vertical = 8.dp),
            placeholder = { Text(stringResource(R.string.search_country)) },
            leadingIcon = { Icon(Icons.Default.Search, contentDescription = null) },
            singleLine = true,
            shape = RoundedCornerShape(12.dp)
        )

        LazyColumn(
            modifier = Modifier.fillMaxWidth(),
            contentPadding = PaddingValues(horizontal = 16.dp, vertical = 8.dp),
            verticalArrangement = Arrangement.spacedBy(4.dp)
        ) {
            if (showPrimaryMarketsFirst && filteredCountries.any { it.isPrimaryMarket }) {
                item {
                    Text(
                        text = stringResource(R.string.primary_markets),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.primary,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            items(filteredCountries) { country ->
                CountryListItem(
                    country = country,
                    isSelected = country.code == selectedCountryCode,
                    showProviderInfo = showProviderInfo,
                    onClick = { onCountrySelected(country) }
                )

                if (country.isPrimaryMarket &&
                    filteredCountries.getOrNull(filteredCountries.indexOf(country) + 1)?.isPrimaryMarket == false
                ) {
                    Spacer(modifier = Modifier.height(8.dp))
                    Text(
                        text = stringResource(R.string.all_countries),
                        style = MaterialTheme.typography.labelMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant,
                        modifier = Modifier.padding(vertical = 8.dp)
                    )
                }
            }

            if (filteredCountries.isEmpty()) {
                item {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(32.dp),
                        contentAlignment = Alignment.Center
                    ) {
                        Text(
                            text = stringResource(R.string.no_countries_found),
                            style = MaterialTheme.typography.bodyLarge,
                            color = MaterialTheme.colorScheme.onSurfaceVariant
                        )
                    }
                }
            }
        }
    }
}

@Composable
private fun CountryListItem(
    country: CountryConfig,
    isSelected: Boolean,
    showProviderInfo: Boolean,
    onClick: () -> Unit
) {
    val backgroundColor = if (isSelected) {
        MaterialTheme.colorScheme.primaryContainer
    } else {
        MaterialTheme.colorScheme.surface
    }

    Card(
        modifier = Modifier
            .fillMaxWidth()
            .clip(RoundedCornerShape(12.dp))
            .clickable(onClick = onClick),
        colors = CardDefaults.cardColors(containerColor = backgroundColor),
        elevation = CardDefaults.cardElevation(defaultElevation = if (isSelected) 2.dp else 0.dp)
    ) {
        Row(
            modifier = Modifier
                .fillMaxWidth()
                .padding(12.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = country.flagEmoji,
                style = MaterialTheme.typography.headlineMedium,
                modifier = Modifier.padding(end = 12.dp)
            )

            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = country.name,
                    style = MaterialTheme.typography.bodyLarge,
                    fontWeight = FontWeight.Medium
                )

                if (showProviderInfo) {
                    Row(
                        verticalAlignment = Alignment.CenterVertically,
                        horizontalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Row(verticalAlignment = Alignment.CenterVertically) {
                            Box(
                                modifier = Modifier
                                    .size(8.dp)
                                    .clip(RoundedCornerShape(4.dp))
                                    .background(
                                        try {
                                            Color(android.graphics.Color.parseColor(country.providerColor))
                                        } catch (e: Exception) {
                                            MaterialTheme.colorScheme.primary
                                        }
                                    )
                            )
                            Spacer(modifier = Modifier.width(4.dp))
                            Text(
                                text = country.providerName,
                                style = MaterialTheme.typography.bodySmall,
                                color = MaterialTheme.colorScheme.onSurfaceVariant
                            )
                        }
                        PaymentMethodBadge(country = country)
                    }
                }
            }

            Column(horizontalAlignment = Alignment.End) {
                Text(
                    text = country.currency,
                    style = MaterialTheme.typography.bodyMedium,
                    fontWeight = FontWeight.Medium
                )
                Text(
                    text = country.phonePrefix,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }

            if (isSelected) {
                Icon(
                    imageVector = Icons.Default.Check,
                    contentDescription = null,
                    tint = MaterialTheme.colorScheme.primary,
                    modifier = Modifier.padding(start = 8.dp)
                )
            }
        }
    }
}

@Composable
private fun PaymentMethodBadge(country: CountryConfig) {
    val (text, color) = when {
        country.hasUssdSupport -> "USSD" to MaterialTheme.colorScheme.tertiary
        country.hasAppSupport -> "APP" to MaterialTheme.colorScheme.secondary
        country.hasQrSupport -> "QR" to MaterialTheme.colorScheme.primary
        else -> return
    }

    Surface(
        shape = RoundedCornerShape(4.dp),
        color = color.copy(alpha = 0.1f)
    ) {
        Text(
            text = text,
            style = MaterialTheme.typography.labelSmall,
            color = color,
            modifier = Modifier.padding(horizontal = 6.dp, vertical = 2.dp)
        )
    }
}
