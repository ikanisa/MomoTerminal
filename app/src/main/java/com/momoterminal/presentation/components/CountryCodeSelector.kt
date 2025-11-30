package com.momoterminal.presentation.components

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.ArrowDropDown
import androidx.compose.material3.DropdownMenu
import androidx.compose.material3.DropdownMenuItem
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.unit.dp

data class CountryCode(
    val code: String,
    val name: String,
    val flag: String
)

@Composable
fun CountryCodeSelector(
    selectedCountryCode: String,
    onCountryCodeSelected: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var expanded by remember { mutableStateOf(false) }
    
    // Common African country codes + others
    val countries = listOf(
        CountryCode("+250", "Rwanda", "🇷🇼"),
        CountryCode("+254", "Kenya", "🇰🇪"),
        CountryCode("+256", "Uganda", "🇺🇬"),
        CountryCode("+255", "Tanzania", "🇹🇿"),
        CountryCode("+233", "Ghana", "🇬🇭"),
        CountryCode("+234", "Nigeria", "🇳🇬"),
        CountryCode("+27", "South Africa", "🇿🇦"),
        CountryCode("+1", "USA/Canada", "🇺🇸")
    )

    val selectedCountry = countries.find { it.code == selectedCountryCode } 
        ?: countries.firstOrNull() 
        ?: CountryCode("+250", "Rwanda", "🇷🇼")

    Box(modifier = modifier) {
        Surface(
            shape = MaterialTheme.shapes.small,
            color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f),
            modifier = Modifier
                .clickable { expanded = true }
        ) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween,
                modifier = Modifier.padding(horizontal = 12.dp, vertical = 16.dp)
            ) {
                Text(
                    text = "${selectedCountry.flag} ${selectedCountry.code}",
                    style = MaterialTheme.typography.bodyLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(modifier = Modifier.width(4.dp))
                Icon(
                    imageVector = Icons.Default.ArrowDropDown,
                    contentDescription = "Select Country Code",
                    tint = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }

        DropdownMenu(
            expanded = expanded,
            onDismissRequest = { expanded = false }
        ) {
            countries.forEach { country ->
                DropdownMenuItem(
                    text = {
                        Text(
                            text = "${country.flag} ${country.name} (${country.code})",
                            style = MaterialTheme.typography.bodyMedium
                        )
                    },
                    onClick = {
                        onCountryCodeSelected(country.code)
                        expanded = false
                    }
                )
            }
        }
    }
}
