package com.momoterminal.designsystem.component

import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.size
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextAlign
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.theme.AmountLargeTextStyle
import com.momoterminal.designsystem.theme.BalanceCardShape
import com.momoterminal.designsystem.theme.MomoTheme

@Composable
fun BalanceHeader(
    balance: String,
    currencySymbol: String,
    label: String = "Available Balance",
    icon: ImageVector? = null,
    modifier: Modifier = Modifier
) {
    GlassCardGradient(modifier = modifier.fillMaxWidth()) {
        Column(
            modifier = Modifier.fillMaxWidth(),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            if (icon != null) {
                Icon(
                    imageVector = icon,
                    contentDescription = null,
                    tint = Color.White.copy(alpha = 0.8f),
                    modifier = Modifier.size(32.dp)
                )
                Spacer(modifier = Modifier.height(MomoTheme.spacing.sm))
            }
            
            Text(
                text = label,
                style = MaterialTheme.typography.labelLarge,
                color = Color.White.copy(alpha = 0.8f)
            )
            
            Spacer(modifier = Modifier.height(MomoTheme.spacing.xs))
            
            Row(
                verticalAlignment = Alignment.Bottom,
                horizontalArrangement = Arrangement.Center
            ) {
                Text(
                    text = currencySymbol,
                    style = MaterialTheme.typography.headlineMedium,
                    color = Color.White.copy(alpha = 0.9f)
                )
                Text(
                    text = " $balance",
                    style = AmountLargeTextStyle,
                    color = Color.White
                )
            }
        }
    }
}

@Composable
fun BalanceHeaderCompact(
    balance: String,
    currencySymbol: String,
    label: String,
    modifier: Modifier = Modifier
) {
    GlassCard(modifier = modifier.fillMaxWidth()) {
        Row(
            modifier = Modifier.fillMaxWidth(),
            horizontalArrangement = Arrangement.SpaceBetween,
            verticalAlignment = Alignment.CenterVertically
        ) {
            Text(
                text = label,
                style = MaterialTheme.typography.titleMedium,
                color = MaterialTheme.colorScheme.onSurface.copy(alpha = 0.7f)
            )
            Text(
                text = "$currencySymbol $balance",
                style = AmountLargeTextStyle.copy(fontSize = MaterialTheme.typography.headlineSmall.fontSize),
                color = MaterialTheme.colorScheme.onSurface
            )
        }
    }
}
