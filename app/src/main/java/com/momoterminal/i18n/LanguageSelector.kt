package com.momoterminal.i18n

import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.lazy.LazyColumn
import androidx.compose.foundation.lazy.items
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Check
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.RadioButton
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.res.stringResource
import androidx.compose.ui.semantics.Role
import androidx.compose.ui.semantics.contentDescription
import androidx.compose.ui.semantics.role
import androidx.compose.ui.semantics.semantics
import androidx.compose.ui.unit.dp
import com.momoterminal.R

data class LanguageOption(
    val code: String,
    val nativeName: String,
    val englishName: String
)

val SUPPORTED_LANGUAGES = listOf(
    LanguageOption("en", "English", "English"),
    LanguageOption("fr", "Français", "French"),
    LanguageOption("sw", "Kiswahili", "Swahili"),
    LanguageOption("rw", "Ikinyarwanda", "Kinyarwanda"),
    LanguageOption("pt", "Português", "Portuguese"),
    LanguageOption("ar", "العربية", "Arabic"),
    LanguageOption("es", "Español", "Spanish")
)

/**
 * Language selector dialog.
 */
@Composable
fun LanguageSelectorDialog(
    currentLanguage: String,
    onLanguageSelected: (String) -> Unit,
    onDismiss: () -> Unit
) {
    AlertDialog(
        onDismissRequest = onDismiss,
        title = { Text(stringResource(R.string.settings_language)) },
        text = {
            LazyColumn {
                items(SUPPORTED_LANGUAGES) { language ->
                    LanguageRow(
                        language = language,
                        isSelected = language.code == currentLanguage,
                        onClick = { onLanguageSelected(language.code) }
                    )
                }
            }
        },
        confirmButton = {
            TextButton(onClick = onDismiss) {
                Text(stringResource(R.string.cancel))
            }
        }
    )
}

@Composable
private fun LanguageRow(
    language: LanguageOption,
    isSelected: Boolean,
    onClick: () -> Unit
) {
    Row(
        modifier = Modifier
            .fillMaxWidth()
            .clickable(onClick = onClick)
            .padding(vertical = 12.dp, horizontal = 8.dp)
            .semantics {
                role = Role.RadioButton
                contentDescription = "${language.nativeName} (${language.englishName})"
            },
        verticalAlignment = Alignment.CenterVertically
    ) {
        RadioButton(
            selected = isSelected,
            onClick = null
        )
        Spacer(modifier = Modifier.width(12.dp))
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = language.nativeName,
                style = MaterialTheme.typography.bodyLarge
            )
            if (language.nativeName != language.englishName) {
                Text(
                    text = language.englishName,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
        if (isSelected) {
            Icon(
                imageVector = Icons.Default.Check,
                contentDescription = null,
                tint = MaterialTheme.colorScheme.primary
            )
        }
    }
}

/**
 * Settings row for language selection.
 */
@Composable
fun LanguageSettingsRow(
    currentLanguage: String,
    onLanguageChange: (String) -> Unit,
    modifier: Modifier = Modifier
) {
    var showDialog by remember { mutableStateOf(false) }
    
    val currentOption = SUPPORTED_LANGUAGES.find { it.code == currentLanguage }
        ?: SUPPORTED_LANGUAGES.first()
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .clickable { showDialog = true }
            .padding(16.dp),
        verticalAlignment = Alignment.CenterVertically
    ) {
        Column(modifier = Modifier.weight(1f)) {
            Text(
                text = stringResource(R.string.settings_language),
                style = MaterialTheme.typography.bodyLarge
            )
            Spacer(modifier = Modifier.height(4.dp))
            Text(
                text = currentOption.nativeName,
                style = MaterialTheme.typography.bodyMedium,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
    
    if (showDialog) {
        LanguageSelectorDialog(
            currentLanguage = currentLanguage,
            onLanguageSelected = { code ->
                onLanguageChange(code)
                showDialog = false
            },
            onDismiss = { showDialog = false }
        )
    }
}
