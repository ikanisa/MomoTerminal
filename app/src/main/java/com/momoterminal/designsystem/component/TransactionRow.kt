package com.momoterminal.designsystem.component

import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.CallReceived
import androidx.compose.material.icons.automirrored.filled.CallMade
import androidx.compose.material.icons.filled.Nfc
import androidx.compose.material.icons.filled.Sms
import androidx.compose.material.icons.filled.Edit
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.text.style.TextOverflow
import androidx.compose.ui.unit.dp
import com.momoterminal.designsystem.theme.AmountTextStyle
import com.momoterminal.designsystem.theme.MomoTheme
import com.momoterminal.designsystem.theme.TransactionIdTextStyle

enum class TransactionDirection { CREDIT, DEBIT }
enum class TransactionSource { NFC, SMS, MANUAL }

@Composable
fun TransactionRow(
    amount: String,
    currencySymbol: String,
    direction: TransactionDirection,
    source: TransactionSource,
    title: String,
    subtitle: String,
    timestamp: String,
    transactionId: String? = null,
    onClick: (() -> Unit)? = null,
    modifier: Modifier = Modifier
) {
    val colors = MomoTheme.colors
    val amountColor = when (direction) {
        TransactionDirection.CREDIT -> colors.credit
        TransactionDirection.DEBIT -> colors.debit
    }
    val amountPrefix = when (direction) {
        TransactionDirection.CREDIT -> "+"
        TransactionDirection.DEBIT -> "-"
    }
    val sourceIcon: ImageVector = when (source) {
        TransactionSource.NFC -> Icons.Default.Nfc
        TransactionSource.SMS -> Icons.Default.Sms
        TransactionSource.MANUAL -> Icons.Default.Edit
    }
    val directionIcon = when (direction) {
        TransactionDirection.CREDIT -> Icons.AutoMirrored.Filled.CallReceived
        TransactionDirection.DEBIT -> Icons.AutoMirrored.Filled.CallMade
    }
    
    Row(
        modifier = modifier
            .fillMaxWidth()
            .then(if (onClick != null) Modifier.clickable(onClick = onClick) else Modifier)
            .padding(horizontal = MomoTheme.spacing.lg, vertical = MomoTheme.spacing.md),
        verticalAlignment = Alignment.CenterVertically,
        horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.md)
    ) {
        // Direction indicator
        Box(
            modifier = Modifier
                .size(44.dp)
                .clip(CircleShape)
                .background(amountColor.copy(alpha = 0.12f)),
            contentAlignment = Alignment.Center
        ) {
            Icon(
                imageVector = directionIcon,
                contentDescription = null,
                tint = amountColor,
                modifier = Modifier.size(22.dp)
            )
        }
        
        // Details
        Column(modifier = Modifier.weight(1f)) {
            Row(
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.spacedBy(MomoTheme.spacing.xs)
            ) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleSmall,
                    maxLines = 1,
                    overflow = TextOverflow.Ellipsis
                )
                Icon(
                    imageVector = sourceIcon,
                    contentDescription = source.name,
                    tint = MaterialTheme.colorScheme.onSurfaceVariant,
                    modifier = Modifier.size(14.dp)
                )
            }
            Text(
                text = subtitle,
                style = MaterialTheme.typography.bodySmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant,
                maxLines = 1,
                overflow = TextOverflow.Ellipsis
            )
            if (transactionId != null) {
                Text(
                    text = transactionId,
                    style = TransactionIdTextStyle,
                    color = MaterialTheme.colorScheme.onSurfaceVariant.copy(alpha = 0.6f)
                )
            }
        }
        
        // Amount and time
        Column(horizontalAlignment = Alignment.End) {
            Text(
                text = "$amountPrefix$currencySymbol$amount",
                style = AmountTextStyle,
                color = amountColor
            )
            Text(
                text = timestamp,
                style = MaterialTheme.typography.labelSmall,
                color = MaterialTheme.colorScheme.onSurfaceVariant
            )
        }
    }
}
