package com.momoterminal.core.designsystem.component

import androidx.compose.animation.animateColorAsState
import androidx.compose.animation.core.animateDpAsState
import androidx.compose.animation.core.tween
import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.*
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.material3.MaterialTheme
import androidx.compose.runtime.Composable
import androidx.compose.runtime.getValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.theme.MomoTheme

@Composable
fun MomoSwitch(
    checked: Boolean,
    onCheckedChange: (Boolean) -> Unit,
    modifier: Modifier = Modifier,
    enabled: Boolean = true
) {
    val colors = MomoTheme.colors
    val trackColor by animateColorAsState(
        targetValue = if (checked) MaterialTheme.colorScheme.primary else colors.surfaceGlass,
        animationSpec = tween(200),
        label = "track"
    )
    val thumbOffset by animateDpAsState(
        targetValue = if (checked) 20.dp else 0.dp,
        animationSpec = tween(200),
        label = "thumb"
    )

    Box(
        modifier = modifier
            .width(48.dp)
            .height(28.dp)
            .clip(RoundedCornerShape(14.dp))
            .background(trackColor)
            .border(1.dp, colors.glassBorder, RoundedCornerShape(14.dp))
            .clickable(enabled = enabled) { onCheckedChange(!checked) }
            .padding(4.dp)
    ) {
        Box(
            modifier = Modifier
                .offset(x = thumbOffset)
                .size(20.dp)
                .clip(CircleShape)
                .background(MaterialTheme.colorScheme.surface)
                .align(Alignment.CenterStart)
        )
    }
}
