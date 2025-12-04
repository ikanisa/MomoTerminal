package com.momoterminal.presentation.components

import android.graphics.Bitmap
import android.graphics.Color
import androidx.compose.foundation.Image
import androidx.compose.foundation.layout.*
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Text
import androidx.compose.runtime.*
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.graphics.asImageBitmap
import androidx.compose.ui.unit.dp
import com.google.zxing.BarcodeFormat
import com.google.zxing.EncodeHintType
import com.google.zxing.qrcode.QRCodeWriter
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.withContext

/**
 * Composable that displays a QR code for a given text/URL.
 * Generates QR code asynchronously and displays it as an image.
 */
@Composable
fun QrCodeDisplay(
    data: String,
    modifier: Modifier = Modifier,
    size: Int = 512,
    title: String? = null,
    subtitle: String? = null
) {
    var qrBitmap by remember(data) { mutableStateOf<Bitmap?>(null) }
    
    LaunchedEffect(data) {
        qrBitmap = withContext(Dispatchers.Default) {
            generateQrCode(data, size)
        }
    }
    
    Card(
        modifier = modifier,
        colors = CardDefaults.cardColors(
            containerColor = MaterialTheme.colorScheme.surface
        ),
        elevation = CardDefaults.cardElevation(defaultElevation = 4.dp)
    ) {
        Column(
            modifier = Modifier
                .fillMaxWidth()
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally,
            verticalArrangement = Arrangement.spacedBy(12.dp)
        ) {
            if (title != null) {
                Text(
                    text = title,
                    style = MaterialTheme.typography.titleMedium,
                    color = MaterialTheme.colorScheme.onSurface
                )
            }
            
            qrBitmap?.let { bitmap ->
                Image(
                    bitmap = bitmap.asImageBitmap(),
                    contentDescription = "QR Code",
                    modifier = Modifier
                        .size((size / 2).dp)
                        .padding(8.dp)
                )
            } ?: Box(
                modifier = Modifier
                    .size((size / 2).dp)
                    .padding(8.dp),
                contentAlignment = Alignment.Center
            ) {
                Text("Generating QR...")
            }
            
            if (subtitle != null) {
                Text(
                    text = subtitle,
                    style = MaterialTheme.typography.bodySmall,
                    color = MaterialTheme.colorScheme.onSurfaceVariant
                )
            }
        }
    }
}

/**
 * Generates a QR code bitmap from the given data.
 */
private fun generateQrCode(data: String, size: Int): Bitmap {
    val hints = hashMapOf<EncodeHintType, Any>().apply {
        put(EncodeHintType.MARGIN, 1)
    }
    
    val qrCodeWriter = QRCodeWriter()
    val bitMatrix = qrCodeWriter.encode(data, BarcodeFormat.QR_CODE, size, size, hints)
    
    val bitmap = Bitmap.createBitmap(size, size, Bitmap.Config.RGB_565)
    for (x in 0 until size) {
        for (y in 0 until size) {
            bitmap.setPixel(x, y, if (bitMatrix[x, y]) Color.BLACK else Color.WHITE)
        }
    }
    return bitmap
}
