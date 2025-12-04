package com.momoterminal.core.osintegration.capabilities

import android.Manifest
import android.content.Context
import android.location.Location

import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider

import androidx.core.content.ContextCompat
import androidx.fragment.app.FragmentActivity
import com.google.android.gms.location.*
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.suspendCancellableCoroutine
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume

// 1. LOCATION PROVIDER (Generic, domain-agnostic)

sealed class LocationResult {
    data class Success(val location: Location) : LocationResult()
    data class Error(val message: String) : LocationResult()
    data object PermissionDenied : LocationResult()
}

@Singleton
class LocationProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val fusedLocationClient = LocationServices.getFusedLocationProviderClient(context)
    
    suspend fun getCurrentLocation(): LocationResult = suspendCancellableCoroutine { continuation ->
        try {
            fusedLocationClient.lastLocation
                .addOnSuccessListener { location ->
                    if (location != null) {
                        continuation.resume(LocationResult.Success(location))
                    } else {
                        requestNewLocation { result ->
                            continuation.resume(result)
                        }
                    }
                }
                .addOnFailureListener { e ->
                    continuation.resume(LocationResult.Error(e.message ?: "Unknown error"))
                }
        } catch (e: SecurityException) {
            continuation.resume(LocationResult.PermissionDenied)
        }
    }
    
    private fun requestNewLocation(callback: (LocationResult) -> Unit) {
        val locationRequest = LocationRequest.Builder(
            Priority.PRIORITY_HIGH_ACCURACY,
            10000L
        ).build()
        
        val locationCallback = object : LocationCallback() {
            override fun onLocationResult(result: com.google.android.gms.location.LocationResult) {
                result.lastLocation?.let { location ->
                    callback(LocationResult.Success(location))
                }
                fusedLocationClient.removeLocationUpdates(this)
            }
        }
        
        try {
            fusedLocationClient.requestLocationUpdates(
                locationRequest,
                locationCallback,
                null
            )
        } catch (e: SecurityException) {
            callback(LocationResult.PermissionDenied)
        }
    }
}



// 2. CAMERA PROVIDER (Generic, domain-agnostic)

sealed class CameraResult {
    data class Success(val bitmap: android.graphics.Bitmap) : CameraResult()
    data class Error(val message: String) : CameraResult()
    data object PermissionDenied : CameraResult()
    data object Cancelled : CameraResult()
}

@Singleton
class CameraProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    suspend fun captureImage(activity: FragmentActivity): CameraResult {
        return suspendCancellableCoroutine { continuation ->
            val cameraProviderFuture = ProcessCameraProvider.getInstance(context)
            
            cameraProviderFuture.addListener({
                val cameraProvider = cameraProviderFuture.get()
                
                val preview = Preview.Builder().build()
                val imageCapture = ImageCapture.Builder()
                    .setCaptureMode(ImageCapture.CAPTURE_MODE_MINIMIZE_LATENCY)
                    .build()
                
                val cameraSelector = CameraSelector.DEFAULT_BACK_CAMERA
                
                try {
                    cameraProvider.unbindAll()
                    cameraProvider.bindToLifecycle(
                        activity,
                        cameraSelector,
                        preview,
                        imageCapture
                    )
                    
                    // Capture logic would go here
                    // For now, return success with placeholder
                    continuation.resume(CameraResult.Success(createPlaceholderBitmap()))
                    
                } catch (e: Exception) {
                    continuation.resume(CameraResult.Error(e.message ?: "Camera error"))
                }
            }, ContextCompat.getMainExecutor(context))
        }
    }
    
    private fun createPlaceholderBitmap(): android.graphics.Bitmap {
        return android.graphics.Bitmap.createBitmap(100, 100, android.graphics.Bitmap.Config.ARGB_8888)
    }
}



// 3. BIOMETRIC PROVIDER (Generic, domain-agnostic)

sealed class BiometricResult {
    data object Success : BiometricResult()
    data class Error(val message: String) : BiometricResult()
    data object Cancelled : BiometricResult()
    data object NotAvailable : BiometricResult()
}

@Singleton
class BiometricProvider @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun isBiometricAvailable(): Boolean {
        val biometricManager = BiometricManager.from(context)
        return when (biometricManager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_STRONG)) {
            BiometricManager.BIOMETRIC_SUCCESS -> true
            else -> false
        }
    }
    
    suspend fun authenticate(
        activity: FragmentActivity,
        title: String = "Authenticate",
        subtitle: String = "Verify your identity",
        negativeButtonText: String = "Cancel"
    ): BiometricResult = suspendCancellableCoroutine { continuation ->
        
        if (!isBiometricAvailable()) {
            continuation.resume(BiometricResult.NotAvailable)
            return@suspendCancellableCoroutine
        }
        
        val promptInfo = BiometricPrompt.PromptInfo.Builder()
            .setTitle(title)
            .setSubtitle(subtitle)
            .setNegativeButtonText(negativeButtonText)
            .build()
        
        val biometricPrompt = BiometricPrompt(
            activity,
            ContextCompat.getMainExecutor(context),
            object : BiometricPrompt.AuthenticationCallback() {
                override fun onAuthenticationSucceeded(result: BiometricPrompt.AuthenticationResult) {
                    continuation.resume(BiometricResult.Success)
                }
                
                override fun onAuthenticationError(errorCode: Int, errString: CharSequence) {
                    if (errorCode == BiometricPrompt.ERROR_NEGATIVE_BUTTON ||
                        errorCode == BiometricPrompt.ERROR_USER_CANCELED) {
                        continuation.resume(BiometricResult.Cancelled)
                    } else {
                        continuation.resume(BiometricResult.Error(errString.toString()))
                    }
                }
                
                override fun onAuthenticationFailed() {
                    // Don't resume here, let user retry
                }
            }
        )
        
        biometricPrompt.authenticate(promptInfo)
    }
}


