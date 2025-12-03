package com.momoterminal.core.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.border
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.unit.dp
import com.momoterminal.core.designsystem.theme.MomoGold
import com.momoterminal.core.designsystem.theme.MomoLime
import com.momoterminal.core.designsystem.theme.MomoTeal
import com.momoterminal.core.designsystem.theme.MomoTheme
import com.momoterminal.core.designsystem.theme.TokenChipShape

enum class TokenType {
    BONUS, POINTS, VOUCHER, CASHBACK, DEFAULT
}

@Composable
fun TokenChip(
    label: String,
    value: String,
    type: TokenType = TokenType.DEFAULT,
    icon: ImageVector? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val (backgroundColor, contentColor) = when (type) {
        TokenType.BONUS -> MomoGold.copy(alpha = 0.15f) to MomoGold
        TokenType.POINTS -> MomoTeal.copy(alpha = 0.15f) to MomoTeal
        TokenType.VOUCHER -> MomoLime.copy(alpha = 0.15f) to MomoLime
        TokenType.CASHBACK -> Color(0xFF8B5CF6).copy(alpha = 0.15f) to Color(0xFF8B5CF6)
        TokenType.DEFAULT -> MaterialTheme.colorScheme.surfaceVariant to MaterialTheme.colorScheme.onSurfaceVariant
    }
    
    Row(
        modifier = modifier
            .clip(TokenChipShape)
            .background(backgroundColor)
            .border(1.dp, contentColor.copy(alpha = 0.3f), TokenChipShape)
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = MomoTheme.spacing.md, vertical = MomoTheme.spacing.sm),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.xs)
    ) {
        if (icon != null) {
            Icon(
                imageVector = icon,
                contentDescription = null,
                tint = contentColor,
                modifier = Modifier.size(16.dp)
            )
        }
        Text(
            text = label,
            style = MaterialTheme.typography.labelSmall,
            color = contentColor.copy(alpha = 0.8f)
        )
        Text(
            text = value,
            style = MaterialTheme.typography.labelMedium,
            color = contentColor
        )
    }
}
