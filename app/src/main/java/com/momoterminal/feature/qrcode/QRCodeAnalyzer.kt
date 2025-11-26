package com.momoterminal.feature.qrcode

import androidx.annotation.OptIn
import androidx.camera.core.ExperimentalGetImage
import androidx.camera.core.ImageAnalysis
import androidx.camera.core.ImageProxy
import com.google.mlkit.vision.barcode.BarcodeScanner
import com.google.mlkit.vision.barcode.BarcodeScannerOptions
import com.google.mlkit.vision.barcode.BarcodeScanning
import com.google.mlkit.vision.barcode.common.Barcode
import com.google.mlkit.vision.common.InputImage
import timber.log.Timber

/**
 * ImageAnalysis.Analyzer implementation for CameraX that uses ML Kit
 * to scan QR codes and other barcode formats.
 */
class QRCodeAnalyzer(
    private val onQRCodeDetected: (QRCodeResult) -> Unit
) : ImageAnalysis.Analyzer {
    
    private val options = BarcodeScannerOptions.Builder()
        .setBarcodeFormats(
            Barcode.FORMAT_QR_CODE,
            Barcode.FORMAT_AZTEC,
            Barcode.FORMAT_DATA_MATRIX,
            Barcode.FORMAT_PDF417
        )
        .build()
    
    private val scanner: BarcodeScanner = BarcodeScanning.getClient(options)
    
    private var lastAnalyzedTimestamp = 0L
    
    companion object {
        /**
         * Minimum interval between analyses in milliseconds.
         * Prevents excessive processing and battery drain.
         */
        private const val ANALYSIS_INTERVAL_MS = 100L
    }
    
    @OptIn(ExperimentalGetImage::class)
    override fun analyze(imageProxy: ImageProxy) {
        val currentTimestamp = System.currentTimeMillis()
        
        // Throttle analysis to prevent excessive processing
        if (currentTimestamp - lastAnalyzedTimestamp < ANALYSIS_INTERVAL_MS) {
            imageProxy.close()
            return
        }
        
        lastAnalyzedTimestamp = currentTimestamp
        
        val mediaImage = imageProxy.image
        if (mediaImage == null) {
            imageProxy.close()
            return
        }
        
        val inputImage = InputImage.fromMediaImage(
            mediaImage,
            imageProxy.imageInfo.rotationDegrees
        )
        
        scanner.process(inputImage)
            .addOnSuccessListener { barcodes ->
                if (barcodes.isNotEmpty()) {
                    // Get the first barcode found
                    val barcode = barcodes.first()
                    Timber.d("QR Code detected: ${barcode.rawValue}")
                    
                    val parsedData = QRCodeParser.parse(barcode)
                    onQRCodeDetected(QRCodeResult.Success(parsedData))
                }
                // Note: We don't send NotFound for each frame without a code
                // to avoid flooding the callback
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "QR Code scanning failed")
                onQRCodeDetected(QRCodeResult.Error(exception as Exception))
            }
            .addOnCompleteListener {
                imageProxy.close()
            }
    }
    
    /**
     * Release scanner resources when done.
     */
    fun close() {
        scanner.close()
    }
}
