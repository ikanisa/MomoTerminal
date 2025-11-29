package com.momoterminal.util.image

import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.AccountBalance
import androidx.compose.material.icons.filled.AttachMoney
import androidx.compose.material.icons.filled.CallMade
import androidx.compose.material.icons.filled.CallReceived
import androidx.compose.material.icons.filled.Person
import androidx.compose.material.icons.filled.Receipt
import androidx.compose.material.icons.filled.Store
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.Icon
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.Composable
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.vector.ImageVector
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.Dp
import androidx.compose.ui.unit.dp
import coil.compose.SubcomposeAsyncImage
import coil.imageLoader
import coil.request.ImageRequest

/**
 * Generic async image composable with loading and error states.
 *
 * @param imageUrl URL of the image to load
 * @param contentDescription Content description for accessibility
 * @param modifier Modifier for the image
 * @param placeholder Composable to show while loading
 * @param error Composable to show on error
 */
@Composable
fun MomoAsyncImage(
    imageUrl: String?,
    contentDescription: String?,
    modifier: Modifier = Modifier,
    placeholder: @Composable () -> Unit = { DefaultPlaceholder() },
    error: @Composable () -> Unit = { DefaultError() },
    contentScale: ContentScale = ContentScale.Fit
) {
    SubcomposeAsyncImage(
        model = ImageRequest.Builder(LocalContext.current)
            .data(imageUrl)
            .crossfade(true)
            .build(),
        contentDescription = contentDescription,
        modifier = modifier,
        contentScale = contentScale,
        loading = { placeholder() },
        error = { error() }
    )
}

/**
 * Composable for displaying mobile money provider logos.
 *
 * @param providerCode Code identifying the mobile money provider (e.g., "MTN", "VODAFONE", "AIRTEL")
 * @param modifier Modifier for the image
 * @param size Size of the logo
 */
@Composable
fun ProviderLogo(
    providerCode: String,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val (backgroundColor, textColor, displayText) = when (providerCode.uppercase()) {
        "MTN", "MTNMOMO" -> Triple(Color(0xFFFFCC00), Color.Black, "MTN")
        "VODAFONE", "VODACASH" -> Triple(Color(0xFFE60000), Color.White, "VOD")
        "AIRTEL", "AIRTELMONEY" -> Triple(Color(0xFFFF0000), Color.White, "AIR")
        "TIGO", "TIGOCASH" -> Triple(Color(0xFF003366), Color.White, "TIGO")
        else -> Triple(MaterialTheme.colorScheme.primary, Color.White, providerCode.take(3))
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = displayText,
            color = textColor,
            style = MaterialTheme.typography.labelSmall,
            fontWeight = FontWeight.Bold
        )
    }
}

/**
 * Composable for displaying merchant avatars with fallback to initials.
 *
 * @param merchantName Name of the merchant
 * @param imageUrl Optional URL for the merchant's logo
 * @param modifier Modifier for the avatar
 * @param size Size of the avatar
 */
@Composable
fun MerchantAvatar(
    merchantName: String,
    imageUrl: String? = null,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp
) {
    if (imageUrl.isNullOrBlank()) {
        // Show initials fallback
        InitialsAvatar(
            name = merchantName,
            modifier = modifier,
            size = size
        )
    } else {
        SubcomposeAsyncImage(
            model = ImageRequest.Builder(LocalContext.current)
                .data(imageUrl)
                .crossfade(true)
                .build(),
            contentDescription = "Logo for $merchantName",
            modifier = modifier
                .size(size)
                .clip(CircleShape),
            contentScale = ContentScale.Crop,
            loading = {
                InitialsAvatar(name = merchantName, size = size)
            },
            error = {
                InitialsAvatar(name = merchantName, size = size)
            }
        )
    }
}

/**
 * Avatar showing initials from a name.
 */
@Composable
fun InitialsAvatar(
    name: String,
    modifier: Modifier = Modifier,
    size: Dp = 48.dp,
    backgroundColor: Color = MaterialTheme.colorScheme.primaryContainer,
    textColor: Color = MaterialTheme.colorScheme.onPrimaryContainer
) {
    val initials = name
        .split(" ")
        .take(2)
        .mapNotNull { it.firstOrNull()?.uppercase() }
        .joinToString("")
        .ifEmpty { "?" }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Text(
            text = initials,
            color = textColor,
            style = MaterialTheme.typography.titleMedium,
            fontWeight = FontWeight.SemiBold
        )
    }
}

/**
 * Icon composable for different transaction types.
 *
 * @param transactionType Type of transaction
 * @param modifier Modifier for the icon
 * @param size Size of the icon container
 */
@Composable
fun TransactionIcon(
    transactionType: TransactionIconType,
    modifier: Modifier = Modifier,
    size: Dp = 40.dp
) {
    val (icon, backgroundColor, iconColor) = when (transactionType) {
        TransactionIconType.PAYMENT -> Triple(
            Icons.Default.AttachMoney,
            Color(0xFF4CAF50).copy(alpha = 0.1f),
            Color(0xFF4CAF50)
        )
        TransactionIconType.TRANSFER_OUT -> Triple(
            Icons.Default.CallMade,
            Color(0xFFF44336).copy(alpha = 0.1f),
            Color(0xFFF44336)
        )
        TransactionIconType.TRANSFER_IN -> Triple(
            Icons.Default.CallReceived,
            Color(0xFF4CAF50).copy(alpha = 0.1f),
            Color(0xFF4CAF50)
        )
        TransactionIconType.WITHDRAWAL -> Triple(
            Icons.Default.AccountBalance,
            Color(0xFFFF9800).copy(alpha = 0.1f),
            Color(0xFFFF9800)
        )
        TransactionIconType.MERCHANT -> Triple(
            Icons.Default.Store,
            Color(0xFF2196F3).copy(alpha = 0.1f),
            Color(0xFF2196F3)
        )
        TransactionIconType.BILL -> Triple(
            Icons.Default.Receipt,
            Color(0xFF9C27B0).copy(alpha = 0.1f),
            Color(0xFF9C27B0)
        )
        TransactionIconType.PERSON -> Triple(
            Icons.Default.Person,
            MaterialTheme.colorScheme.primaryContainer,
            MaterialTheme.colorScheme.onPrimaryContainer
        )
    }
    
    Box(
        modifier = modifier
            .size(size)
            .clip(CircleShape)
            .background(backgroundColor),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = icon,
            contentDescription = null,
            tint = iconColor,
            modifier = Modifier.size(size * 0.5f)
        )
    }
}

/**
 * Enum for different transaction icon types.
 */
enum class TransactionIconType {
    PAYMENT,
    TRANSFER_OUT,
    TRANSFER_IN,
    WITHDRAWAL,
    MERCHANT,
    BILL,
    PERSON
}

/**
 * Default placeholder shown while images are loading.
 */
@Composable
private fun DefaultPlaceholder(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier,
        contentAlignment = Alignment.Center
    ) {
        CircularProgressIndicator(
            modifier = Modifier.size(24.dp),
            strokeWidth = 2.dp
        )
    }
}

/**
 * Default error state shown when image loading fails.
 */
@Composable
private fun DefaultError(
    modifier: Modifier = Modifier
) {
    Box(
        modifier = modifier
            .background(
                color = MaterialTheme.colorScheme.surfaceVariant,
                shape = CircleShape
            ),
        contentAlignment = Alignment.Center
    ) {
        Icon(
            imageVector = Icons.Default.Person,
            contentDescription = null,
            tint = MaterialTheme.colorScheme.onSurfaceVariant
        )
    }
}

/**
 * Preload an image URL into the cache.
 * Uses the app's ImageLoader instance through Coil's singleton.
 */
@Composable
fun PreloadImage(imageUrl: String?) {
    if (imageUrl.isNullOrBlank()) return
    
    val context = LocalContext.current
    
    // Use Coil's imageLoader extension which returns the singleton ImageLoader
    // configured by the app (via Hilt in our case through Coil.setImageLoader)
    context.imageLoader.enqueue(
        ImageRequest.Builder(context)
            .data(imageUrl)
            .build()
    )
}
