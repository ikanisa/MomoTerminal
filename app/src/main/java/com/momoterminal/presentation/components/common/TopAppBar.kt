package com.momoterminal.presentation.components.common

import androidx.compose.foundation.layout.RowScope
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.Menu
import androidx.compose.material.icons.filled.MoreVert
import androidx.compose.material3.CenterAlignedTopAppBar
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarColors
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.tooling.preview.Preview
import com.momoterminal.presentation.theme.MomoTerminalTheme

/**
 * Standard top app bar with customizable navigation and actions.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomoTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.topAppBarColors()
) {
    TopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigation"
                    )
                }
            }
        },
        actions = actions,
        colors = colors
    )
}

/**
 * Center-aligned top app bar for prominent titles.
 */
@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun MomoCenterTopAppBar(
    title: String,
    modifier: Modifier = Modifier,
    navigationIcon: ImageVector? = null,
    onNavigationClick: (() -> Unit)? = null,
    actions: @Composable RowScope.() -> Unit = {},
    colors: TopAppBarColors = TopAppBarDefaults.centerAlignedTopAppBarColors()
) {
    CenterAlignedTopAppBar(
        title = {
            Text(
                text = title,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis,
                style = MaterialTheme.typography.titleLarge
            )
        },
        modifier = modifier,
        navigationIcon = {
            if (navigationIcon != null && onNavigationClick != null) {
                IconButton(onClick = onNavigationClick) {
                    Icon(
                        imageVector = navigationIcon,
                        contentDescription = "Navigation"
                    )
                }
            }
        },
        actions = actions,
        colors = colors
    )
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MomoTopAppBarPreview() {
    MomoTerminalTheme {
        MomoTopAppBar(
            title = "MoMo Terminal",
            navigationIcon = Icons.Filled.Menu,
            onNavigationClick = {},
            actions = {
                IconButton(onClick = {}) {
                    Icon(Icons.Filled.MoreVert, contentDescription = "More")
                }
            }
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MomoTopAppBarWithBackPreview() {
    MomoTerminalTheme {
        MomoTopAppBar(
            title = "Settings",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = {}
        )
    }
}

@OptIn(ExperimentalMaterial3Api::class)
@Preview
@Composable
private fun MomoCenterTopAppBarPreview() {
    MomoTerminalTheme {
        MomoCenterTopAppBar(
            title = "Payment Terminal",
            navigationIcon = Icons.AutoMirrored.Filled.ArrowBack,
            onNavigationClick = {}
        )
    }
}
