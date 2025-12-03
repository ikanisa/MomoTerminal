package com.momoterminal.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.*
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Brush
import com.momoterminal.designsystem.theme.BottomSheetShape
import com.momoterminal.designsystem.theme.MomoTheme

/**
 * Full-screen scaffold with gradient background and glass content panel.
 * Use for main screens with a hero header area.
 */
@Composable
fun SurfaceScaffold(
    modifier: Modifier = Modifier,
    header: @Composable (BoxScope.() -> Unit)? = null,
    floatingContent: @Composable (BoxScope.() -> Unit)? = null,
    content: @Composable ColumnScope.() -> Unit
) {
    val colors = MomoTheme.colors
    val gradient = Brush.verticalGradient(
        colors = listOf(colors.gradientStart, colors.gradientEnd),
        startY = 0f,
        endY = 500f
    )

    Box(modifier = modifier.fillMaxSize().background(gradient)) {
        Column(modifier = Modifier.fillMaxSize()) {
            // Header with gradient background
            if (header != null) {
                Box(
                    modifier = Modifier
                        .fillMaxWidth()
                        .statusBarsPadding()
                        .padding(MomoTheme.spacing.lg)
                ) { header() }
            }

            // Main glass surface
            Box(modifier = Modifier.fillMaxSize()) {
                Column(
                    modifier = Modifier
                        .fillMaxSize()
                        .clip(BottomSheetShape)
                        .background(MaterialTheme.colorScheme.surface)
                        .padding(top = MomoTheme.spacing.lg)
                ) { content() }

                // Floating card overlapping header/content
                if (floatingContent != null) {
                    Box(
                        modifier = Modifier
                            .fillMaxWidth()
                            .offset(y = -MomoTheme.spacing.xxl)
                            .padding(horizontal = MomoTheme.spacing.lg)
                    ) { floatingContent() }
                }
            }
        }
    }
}
