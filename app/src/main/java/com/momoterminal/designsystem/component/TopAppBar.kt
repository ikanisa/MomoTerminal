package com.momoterminal.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.rounded.ArrowBack
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun MomoTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    backgroundColor: Color = Color.Transparent,
    actions: @Composable RowScope.() -> Unit = {}
) {
    Row(
        modifier = modifier
            .fillMaxWidth()
            .background(backgroundColor)
            .statusBarsPadding()
            .padding(horizontal = MomoTheme.spacing.sm, vertical = MomoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically
    ) {
        if (onNavigateBack != null) {
            MomoIconButton(
                icon = Icons.AutoMirrored.Rounded.ArrowBack,
                onClick = onNavigateBack,
                style = IconButtonStyle.Ghost,
                contentDescription = "Back"
            )
        } else {
            Spacer(Modifier.width(MomoTheme.spacing.lg))
        }
        Text(
            text = title,
            style = MaterialTheme.typography.titleLarge,
            color = MaterialTheme.colorScheme.onSurface,
            modifier = Modifier.weight(1f).padding(horizontal = MomoTheme.spacing.sm)
        )
        actions()
    }
}

@Composable
fun GlassTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    onNavigateBack: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {}
) {
    MomoTopAppBar(
        title = title,
        modifier = modifier,
        onNavigateBack = onNavigateBack,
        backgroundColor = MomoTheme.colors.surfaceGlass,
        actions = actions
    )
}
