package com.momoterminal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import androidx.compose.ui.window.Dialog
import androidx.compose.ui.window.DialogProperties
import com.momoterminal.core.designsystem.theme.MomoShapes
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun MomoDialog(
    onDismissRequest: () -> Unit,
    modifier: Modifier = Modifier,
    title: String? = null,
    dismissOnBackPress: Boolean = true,
    dismissOnClickOutside: Boolean = true,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MomoTheme.colors

    Dialog(
        onDismissRequest = onDismissRequest,
        properties = DialogProperties(
            dismissOnBackPress = dismissOnBackPress,
            dismissOnClickOutside = dismissOnClickOutside
        )
    ) {
        Column(
            modifier = modifier
                .clip(MomoShapes.large)
                .background(MaterialTheme.colorScheme.surface)
                .border(1.dp, colors.glassBorder, MomoShapes.large)
                .padding(MomoTheme.spacing.lg)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleLarge,
                    color = MaterialTheme.colorScheme.onSurface
                )
                Spacer(Modifier.height(MomoTheme.spacing.md))
            }
            content()
        }
    }
}

@Composable
fun ConfirmationDialog(
    onDismissRequest: () -> Unit,
    onConfirm: () -> Unit,
    title: String,
    message: String,
    confirmLabel: String = "Confirm",
    dismissLabel: String = "Cancel"
) {
    MomoDialog(onDismissRequest = onDismissRequest, title = title) {
        Text(
            text = message,
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        Spacer(Modifier.height(MomoTheme.spacing.lg))
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.End
        ) {
            SecondaryActionButton(text = dismissLabel, onClick = onDismissRequest)
            Spacer(Modifier.width(MomoTheme.spacing.sm))
            PrimaryActionButton(text = confirmLabel, onClick = onConfirm)
        }
    }
}
