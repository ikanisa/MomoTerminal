package com.momoterminal.core.i18n.rtl

import androidx.compose.foundation.layout.*
import androidx.compose.material3.*
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalLayoutDirection
import androidx.compose.ui.unit.LayoutDirection
import androidx.compose.ui.unit.dp
import com.momoterminal.core.i18n.locale.rememberAppLocale

// 1. RTL UTILITIES

@Composable
fun rememberLayoutDirection(): LayoutDirection {
    val locale = rememberAppLocale()
    return if (locale.isRtl) LayoutDirection.Rtl else LayoutDirection.Ltr
}

@Composable
fun isRtl(): Boolean {
    return LocalLayoutDirection.current == LayoutDirection.Rtl
}

// 2. RTL-AWARE COMPOSABLES

@Composable
fun RtlAwareRow(
    modifier: Modifier = Modifier,
    horizontalArrangement: Arrangement.Horizontal = Arrangement.Start,
    verticalAlignment: Alignment.Vertical = Alignment.Top,
    content: @Composable RowScope.() -> Unit
) {
    val layoutDirection = rememberLayoutDirection()
    
    CompositionLocalProvider(LocalLayoutDirection provides layoutDirection) {
        Row(
            modifier = modifier,
            horizontalArrangement = horizontalArrangement,
            verticalAlignment = verticalAlignment,
            content = content
        )
    }
}

// 3. EXAMPLE: LIST ITEM WITH RTL SUPPORT

@Composable
fun GenericListItem(
    title: String,
    subtitle: String? = null,
    leadingIcon: @Composable (() -> Unit)? = null,
    trailingIcon: @Composable (() -> Unit)? = null,
    onClick: () -> Unit = {}
) {
    // Automatically adapts to RTL
    ListItem(
        headlineContent = { Text(title) },
        supportingContent = subtitle?.let { { Text(it) } },
        leadingContent = leadingIcon,
        trailingContent = trailingIcon,
        modifier = Modifier.clickable(onClick = onClick)
    )
}

// 4. EXAMPLE: CARD WITH ICON AND TEXT

@Composable
fun InfoCard(
    title: String,
    description: String,
    icon: @Composable () -> Unit,
    modifier: Modifier = Modifier
) {
    Card(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Icon on start (left in LTR, right in RTL)
            icon()
            
            // Text fills remaining space
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium
                )
                Text(
                    text = description,
                    style = MaterialTheme.typography.bodyMedium
                )
            }
        }
    }
}

// 5. EXAMPLE: NAVIGATION WITH RTL

@Composable
fun NavigationExample() {
    val isRtl = isRtl()
    
    Row(
        modifier = Modifier.fillMaxWidth(),
        horizontalArrangement = Arrangement.SpaceBetween
    ) {
        // Back button (automatically flips in RTL)
        IconButton(onClick = { /* Navigate back */ }) {
            Icon(
                imageVector = if (isRtl) Icons.Default.ArrowForward else Icons.Default.ArrowBack,
                contentDescription = "Back"
            )
        }
        
        Text("Screen Title")
        
        // Forward button (automatically flips in RTL)
        IconButton(onClick = { /* Navigate forward */ }) {
            Icon(
                imageVector = if (isRtl) Icons.Default.ArrowBack else Icons.Default.ArrowForward,
                contentDescription = "Forward"
            )
        }
    }
}

// 6. PADDING HELPERS (start/end instead of left/right)

fun Modifier.paddingStart(dp: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.padding(start = dp)
)

fun Modifier.paddingEnd(dp: androidx.compose.ui.unit.Dp) = this.then(
    Modifier.padding(end = dp)
)

// 7. COMPLETE EXAMPLE: PROFILE CARD

@Composable
fun ProfileCard(
    name: String,
    email: String,
    avatarUrl: String?,
    onEditClick: () -> Unit
) {
    Card(
        modifier = Modifier
            .fillMaxWidth()
            .padding(16.dp)
    ) {
        Row(
            modifier = Modifier
                .padding(16.dp)
                .fillMaxWidth(),
            horizontalArrangement = Arrangement.spacedBy(16.dp),
            verticalAlignment = Alignment.CenterVertically
        ) {
            // Avatar (start side)
            AsyncImage(
                model = avatarUrl,
                contentDescription = null,
                modifier = Modifier.size(64.dp)
            )
            
            // Info (middle, fills space)
            Column(modifier = Modifier.weight(1f)) {
                Text(
                    text = name,
                    style = MaterialTheme.typography.titleLarge
                )
                Text(
                    text = email,
                    style = MaterialTheme.typography.bodyMedium,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
            
            // Edit button (end side)
            IconButton(onClick = onEditClick) {
                Icon(Icons.Default.Edit, contentDescription = "Edit")
            }
        }
    }
}

/*
STRING RESOURCES STRUCTURE

res/values/strings.xml (Default - English):
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <!-- Screen titles -->
    <string name="home_screen_title">Home</string>
    <string name="settings_screen_title">Settings</string>
    <string name="profile_screen_title">Profile</string>
    
    <!-- Common actions -->
    <string name="button_save_text">Save</string>
    <string name="button_cancel_text">Cancel</string>
    <string name="button_delete_text">Delete</string>
    <string name="button_edit_text">Edit</string>
    <string name="button_retry_text">Retry</string>
    
    <!-- Form labels -->
    <string name="form_name_label">Name</string>
    <string name="form_email_label">Email</string>
    <string name="form_phone_label">Phone</string>
    
    <!-- Form hints -->
    <string name="form_name_hint">Enter your name</string>
    <string name="form_email_hint">Enter your email</string>
    
    <!-- Validation errors -->
    <string name="error_field_required">This field is required</string>
    <string name="error_email_invalid">Invalid email address</string>
    <string name="error_network_message">Connection failed. Please try again.</string>
    
    <!-- Empty states -->
    <string name="list_empty_message">No items found</string>
    <string name="list_empty_action">Add your first item</string>
    
    <!-- Plurals -->
    <plurals name="item_count">
        <item quantity="one">%d item</item>
        <item quantity="other">%d items</item>
    </plurals>
    
    <!-- Formatted strings -->
    <string name="welcome_message">Welcome, %1$s!</string>
    <string name="item_detail_info">%1$s • %2$s</string>
    <string name="price_with_currency">%1$s %2$.2f</string>
    
    <!-- Language settings -->
    <string name="settings_language_title">Language</string>
    <string name="settings_language_subtitle">Choose your preferred language</string>
    <string name="settings_region_title">Region</string>
    
    <!-- Dialogs -->
    <string name="dialog_confirm_title">Confirm</string>
    <string name="dialog_confirm_message">Are you sure?</string>
    <string name="dialog_delete_title">Delete Item</string>
    <string name="dialog_delete_message">This action cannot be undone.</string>
</resources>

res/values-es/strings.xml (Spanish):
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="home_screen_title">Inicio</string>
    <string name="settings_screen_title">Configuración</string>
    <string name="profile_screen_title">Perfil</string>
    
    <string name="button_save_text">Guardar</string>
    <string name="button_cancel_text">Cancelar</string>
    <string name="button_delete_text">Eliminar</string>
    <string name="button_edit_text">Editar</string>
    <string name="button_retry_text">Reintentar</string>
    
    <string name="form_name_label">Nombre</string>
    <string name="form_email_label">Correo electrónico</string>
    <string name="form_phone_label">Teléfono</string>
    
    <string name="form_name_hint">Ingresa tu nombre</string>
    <string name="form_email_hint">Ingresa tu correo</string>
    
    <string name="error_field_required">Este campo es obligatorio</string>
    <string name="error_email_invalid">Correo electrónico inválido</string>
    <string name="error_network_message">Conexión fallida. Inténtalo de nuevo.</string>
    
    <string name="list_empty_message">No se encontraron elementos</string>
    <string name="list_empty_action">Agrega tu primer elemento</string>
    
    <plurals name="item_count">
        <item quantity="one">%d elemento</item>
        <item quantity="other">%d elementos</item>
    </plurals>
    
    <string name="welcome_message">¡Bienvenido, %1$s!</string>
    <string name="item_detail_info">%1$s • %2$s</string>
    <string name="price_with_currency">%1$s %2$.2f</string>
    
    <string name="settings_language_title">Idioma</string>
    <string name="settings_language_subtitle">Elige tu idioma preferido</string>
    <string name="settings_region_title">Región</string>
    
    <string name="dialog_confirm_title">Confirmar</string>
    <string name="dialog_confirm_message">¿Estás seguro?</string>
    <string name="dialog_delete_title">Eliminar elemento</string>
    <string name="dialog_delete_message">Esta acción no se puede deshacer.</string>
</resources>

res/values-ar/strings.xml (Arabic - RTL):
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="home_screen_title">الرئيسية</string>
    <string name="settings_screen_title">الإعدادات</string>
    <string name="profile_screen_title">الملف الشخصي</string>
    
    <string name="button_save_text">حفظ</string>
    <string name="button_cancel_text">إلغاء</string>
    <string name="button_delete_text">حذف</string>
    <string name="button_edit_text">تعديل</string>
    <string name="button_retry_text">إعادة المحاولة</string>
    
    <!-- ... more translations -->
</resources>

res/values-fr/strings.xml (French):
<?xml version="1.0" encoding="utf-8"?>
<resources>
    <string name="home_screen_title">Accueil</string>
    <string name="settings_screen_title">Paramètres</string>
    <string name="profile_screen_title">Profil</string>
    
    <string name="button_save_text">Enregistrer</string>
    <string name="button_cancel_text">Annuler</string>
    <string name="button_delete_text">Supprimer</string>
    <string name="button_edit_text">Modifier</string>
    <string name="button_retry_text">Réessayer</string>
    
    <!-- ... more translations -->
</resources>
*/
