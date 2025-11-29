package com.momoterminal.feature.qrcode

import android.Manifest
import android.view.HapticFeedbackConstants
import androidx.camera.core.CameraSelector
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.Preview
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.camera.view.PreviewView
import androidx.compose.animation.core.LinearEasing
import androidx.compose.animation.core.RepeatMode
import androidx.compose.animation.core.animateFloat
import androidx.compose.animation.core.infiniteRepeatable
import androidx.compose.animation.core.rememberInfiniteTransition
import androidx.compose.animation.core.tween
import androidx.compose.foundation.Canvas
import androidx.compose.foundation.background
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.automirrored.filled.ArrowBack
import androidx.compose.material.icons.filled.FlashOff
import androidx.compose.material.icons.filled.FlashOn
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.geometry.CornerRadius
import androidx.compose.ui.geometry.Offset
import androidx.compose.ui.geometry.Size
import androidx.compose.ui.graphics.BlendMode
import androidx.compose.ui.graphics.Color
import androidx.compose.ui.graphics.Path
import androidx.compose.ui.graphics.drawscope.Stroke
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.LocalLifecycleOwner
import androidx.compose.ui.platform.LocalView
import androidx.compose.ui.unit.dp
import androidx.compose.ui.viewinterop.AndroidView
import androidx.core.content.ContextCompat
import com.google.accompanist.permissions.ExperimentalPermissionsApi
import com.google.accompanist.permissions.isGranted
import com.google.accompanist.permissions.rememberPermissionState
import com.google.accompanist.permissions.shouldShowRationale
import timber.log.Timber
import java.util.concurrent.ExecutorService
import java.util.concurrent.Executors

/**
 * Full-screen QR code scanner composable with camera preview,
 * animated overlay, and ML Kit barcode detection.
 */
@OptIn(ExperimentalMaterial3Api::class, ExperimentalPermissionsApi::class)
@Composable
fun QRCodeScannerScreen(
    onNavigateBack: () -> Unit,
    onQRCodeScanned: (ParsedQRData) -> Unit,
    modifier: Modifier = Modifier
) {
    val context = LocalContext.current
    val lifecycleOwner = LocalLifecycleOwner.current
    val view = LocalView.current
    
    var isFlashEnabled by remember { mutableStateOf(false) }
    var camera by remember { mutableStateOf<androidx.camera.core.Camera?>(null) }
    var hasScanned by remember { mutableStateOf(false) }
    
    val cameraPermissionState = rememberPermissionState(Manifest.permission.CAMERA)
    
    val cameraExecutor: ExecutorService = remember {
        Executors.newSingleThreadExecutor()
    }
    
    DisposableEffect(Unit) {
        onDispose {
            cameraExecutor.shutdown()
        }
    }
    
    LaunchedEffect(Unit) {
        if (!cameraPermissionState.status.isGranted) {
            cameraPermissionState.launchPermissionRequest()
        }
    }
    
    Scaffold(
        topBar = {
            TopAppBar(
                title = { Text("Scan QR Code") },
                navigationIcon = {
                    IconButton(onClick = onNavigateBack) {
                        Icon(
                            imageVector = Icons.AutoMirrored.Filled.ArrowBack,
                            contentDescription = "Back"
                        )
                    }
                },
                actions = {
                    IconButton(
                        onClick = {
                            isFlashEnabled = !isFlashEnabled
                            camera?.cameraControl?.enableTorch(isFlashEnabled)
                        }
                    ) {
                        Icon(
                            imageVector = if (isFlashEnabled) Icons.Default.FlashOn else Icons.Default.FlashOff,
                            contentDescription = if (isFlashEnabled) "Turn off flash" else "Turn on flash"
                        )
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = Color.Transparent,
                    titleContentColor = Color.White,
                    navigationIconContentColor = Color.White,
                    actionIconContentColor = Color.White
                )
            )
        },
        modifier = modifier
    ) { paddingValues ->
        Box(
            modifier = Modifier
                .fillMaxSize()
                .padding(paddingValues)
        ) {
            when {
                cameraPermissionState.status.isGranted -> {
                    // Camera preview
                    AndroidView(
                        factory = { ctx ->
                            PreviewView(ctx).apply {
                                implementationMode = PreviewView.ImplementationMode.COMPATIBLE
                            }
                        },
                        modifier = Modifier.fillMaxSize(),
                        update = { previewView ->
                            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
                            
                            cameraProviderFuture.addListener({
                                val cameraProvider = cameraProviderFuture.get()
                                
                                val preview = Preview.Builder()
                                    .build()
                                    .also {
                                        it.setSurfaceProvider(previewView.surfaceProvider)
                                    }
                                
                                val imageAnalyzer = ImageAnalysis.Builder()
                                    .setBackpressureStrategy(ImageAnalysis.STRATEGY_KEEP_ONLY_LATEST)
                                    .build()
                                    .also { analysis ->
                                        analysis.setAnalyzer(
                                            cameraExecutor,
                                            QRCodeAnalyzer { result ->
                                                if (!hasScanned && result is QRCodeResult.Success) {
                                                    hasScanned = true
                                                    // Haptic feedback on successful scan
                                                    view.performHapticFeedback(
                                                        HapticFeedbackConstants.CONFIRM
                                                    )
                                                    onQRCodeScanned(result.data)
                                                }
                                            }
                                        )
                                    }
                                
                                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                                
                                try {
                                    cameraProvider.unbindAll()
                                    camera = cameraProvider.bindToLifecycle(
                                        lifecycleOwner,
                                        cameraSelector,
                                        preview,
                                        imageAnalyzer
                                    )
                                } catch (e: Exception) {
                                    Timber.e(e, "Camera binding failed")
                                }
                            }, ContextCompat.getMainExecutor(context))
                        }
                    )
                    
                    // Scanner overlay
                    ScannerOverlay()
                }
                
                cameraPermissionState.status.shouldShowRationale -> {
                    // Show rationale for camera permission
                    PermissionRationaleContent(
                        onRequestPermission = {
                            cameraPermissionState.launchPermissionRequest()
                        }
                    )
                }
                
                else -> {
                    // Permission denied
                    PermissionDeniedContent(
                        onNavigateBack = onNavigateBack
                    )
                }
            }
        }
    }
}

/**
 * Animated scanner overlay with corner accents and scanning line.
 */
@Composable
private fun ScannerOverlay(
    modifier: Modifier = Modifier
) {
    val infiniteTransition = rememberInfiniteTransition(label = "scanner")
    
    val scanLinePosition by infiniteTransition.animateFloat(
        initialValue = 0f,
        targetValue = 1f,
        animationSpec = infiniteRepeatable(
            animation = tween(
                durationMillis = 2000,
                easing = LinearEasing
            ),
            repeatMode = RepeatMode.Restart
        ),
        label = "scanLine"
    )
    
    val accentColor = MaterialTheme.colorScheme.primary
    val overlayColor = Color.Black.copy(alpha = 0.6f)
    
    Canvas(modifier = modifier.fillMaxSize()) {
        val canvasWidth = size.width
        val canvasHeight = size.height
        
        // Scanner frame dimensions
        val frameSize = minOf(canvasWidth, canvasHeight) * 0.7f
        val frameLeft = (canvasWidth - frameSize) / 2
        val frameTop = (canvasHeight - frameSize) / 2
        val frameRight = frameLeft + frameSize
        val frameBottom = frameTop + frameSize
        
        // Draw semi-transparent overlay with cutout
        drawRect(
            color = overlayColor,
            size = size
        )
        
        // Clear the scanning area
        drawRoundRect(
            color = Color.Transparent,
            topLeft = Offset(frameLeft, frameTop),
            size = Size(frameSize, frameSize),
            cornerRadius = CornerRadius(16.dp.toPx()),
            blendMode = BlendMode.Clear
        )
        
        // Corner accent dimensions
        val cornerLength = frameSize * 0.1f
        val cornerWidth = 4.dp.toPx()
        val cornerRadius = 8.dp.toPx()
        
        // Draw corner accents
        val cornerPath = Path().apply {
            // Top-left corner
            moveTo(frameLeft, frameTop + cornerLength)
            lineTo(frameLeft, frameTop + cornerRadius)
            quadraticTo(frameLeft, frameTop, frameLeft + cornerRadius, frameTop)
            lineTo(frameLeft + cornerLength, frameTop)
            
            // Top-right corner
            moveTo(frameRight - cornerLength, frameTop)
            lineTo(frameRight - cornerRadius, frameTop)
            quadraticTo(frameRight, frameTop, frameRight, frameTop + cornerRadius)
            lineTo(frameRight, frameTop + cornerLength)
            
            // Bottom-right corner
            moveTo(frameRight, frameBottom - cornerLength)
            lineTo(frameRight, frameBottom - cornerRadius)
            quadraticTo(frameRight, frameBottom, frameRight - cornerRadius, frameBottom)
            lineTo(frameRight - cornerLength, frameBottom)
            
            // Bottom-left corner
            moveTo(frameLeft + cornerLength, frameBottom)
            lineTo(frameLeft + cornerRadius, frameBottom)
            quadraticTo(frameLeft, frameBottom, frameLeft, frameBottom - cornerRadius)
            lineTo(frameLeft, frameBottom - cornerLength)
        }
        
        drawPath(
            path = cornerPath,
            color = accentColor,
            style = Stroke(width = cornerWidth)
        )
        
        // Draw animated scanning line
        val scanLineY = frameTop + (frameSize * scanLinePosition)
        val scanLineGradientHeight = 4.dp.toPx()
        
        drawLine(
            color = accentColor.copy(alpha = 0.8f),
            start = Offset(frameLeft + 16.dp.toPx(), scanLineY),
            end = Offset(frameRight - 16.dp.toPx(), scanLineY),
            strokeWidth = scanLineGradientHeight
        )
    }
    
    // Instruction text at the bottom
    Box(
        modifier = Modifier.fillMaxSize(),
        contentAlignment = Alignment.BottomCenter
    ) {
        Column(
            modifier = Modifier
                .padding(32.dp)
                .background(
                    color = Color.Black.copy(alpha = 0.6f),
                    shape = MaterialTheme.shapes.medium
                )
                .padding(16.dp),
            horizontalAlignment = Alignment.CenterHorizontally
        ) {
            Text(
                text = "Position QR code within the frame",
                style = MaterialTheme.typography.bodyMedium,
                color = Color.White
            )
        }
    }
}

/**
 * Content shown when camera permission rationale should be displayed.
 */
@Composable
private fun PermissionRationaleContent(
    onRequestPermission: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FlashOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.onSurface
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Camera Permission Required",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "This feature requires camera access to scan QR codes for mobile money payments.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        androidx.compose.material3.Button(onClick = onRequestPermission) {
            Text("Grant Permission")
        }
    }
}

/**
 * Content shown when camera permission is denied.
 */
@Composable
private fun PermissionDeniedContent(
    onNavigateBack: () -> Unit
) {
    Column(
        modifier = Modifier
            .fillMaxSize()
            .background(MaterialTheme.colorScheme.background)
            .padding(32.dp),
        horizontalAlignment = Alignment.CenterHorizontally,
        verticalArrangement = Arrangement.Center
    ) {
        Icon(
            imageVector = Icons.Default.FlashOff,
            contentDescription = null,
            modifier = Modifier.size(64.dp),
            tint = MaterialTheme.colorScheme.error
        )
        
        Spacer(modifier = Modifier.height(16.dp))
        
        Text(
            text = "Camera Permission Denied",
            style = MaterialTheme.typography.headlineSmall
        )
        
        Spacer(modifier = Modifier.height(8.dp))
        
        Text(
            text = "Please enable camera permission in your device settings to use the QR scanner.",
            style = MaterialTheme.typography.bodyMedium,
            color = MaterialTheme.colorScheme.onSurfaceVariant
        )
        
        Spacer(modifier = Modifier.height(24.dp))
        
        androidx.compose.material3.OutlinedButton(onClick = onNavigateBack) {
            Text("Go Back")
        }
    }
}
