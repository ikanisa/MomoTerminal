package com.momoterminal.core.osintegration.capabilities

import android.Manifest
import android.content.Context
import android.location.Location
import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.biometric.BiometricManager
import androidx.biometric.BiometricPrompt
import androidx.camera.core.*
import androidx.camera.lifecycle.ProcessCameraProvider
import androidx.compose.runtime.*
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
            override fun onLocationResult(result: LocationResult) {
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

// Compose integration for location
@Composable
fun rememberLocationProvider(): LocationProvider {
    val context = LocalContext.current
    return remember { LocationProvider(context) }
}

@Composable
fun RequestLocationPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestMultiplePermissions()
    ) { permissions ->
        when {
            permissions[Manifest.permission.ACCESS_FINE_LOCATION] == true ||
            permissions[Manifest.permission.ACCESS_COARSE_LOCATION] == true -> {
                onPermissionGranted()
            }
            else -> onPermissionDenied()
        }
    }
    
    LaunchedEffect(Unit) {
        launcher.launch(
            arrayOf(
                Manifest.permission.ACCESS_FINE_LOCATION,
                Manifest.permission.ACCESS_COARSE_LOCATION
            )
        )
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

// Compose integration for camera
@Composable
fun RequestCameraPermission(
    onPermissionGranted: () -> Unit,
    onPermissionDenied: () -> Unit
) {
    val launcher = rememberLauncherForActivityResult(
        ActivityResultContracts.RequestPermission()
    ) { isGranted ->
        if (isGranted) onPermissionGranted() else onPermissionDenied()
    }
    
    LaunchedEffect(Unit) {
        launcher.launch(Manifest.permission.CAMERA)
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

// Compose integration for biometric
@Composable
fun rememberBiometricProvider(): BiometricProvider {
    val context = LocalContext.current
    return remember { BiometricProvider(context) }
}

// Usage examples in feature modules

// Example 1: Location-aware feature (domain-agnostic)
@Composable
fun LocationAwareFeature(
    locationProvider: LocationProvider = rememberLocationProvider()
) {
    var location by remember { mutableStateOf<Location?>(null) }
    var hasPermission by remember { mutableStateOf(false) }
    
    if (!hasPermission) {
        RequestLocationPermission(
            onPermissionGranted = { hasPermission = true },
            onPermissionDenied = { /* Handle denial */ }
        )
    } else {
        LaunchedEffect(Unit) {
            when (val result = locationProvider.getCurrentLocation()) {
                is LocationResult.Success -> location = result.location
                is LocationResult.Error -> { /* Handle error */ }
                is LocationResult.PermissionDenied -> { /* Handle denial */ }
            }
        }
    }
    
    location?.let {
        Text("Lat: ${it.latitude}, Lon: ${it.longitude}")
        // Use location for any domain: nearby items, context-aware features, etc.
    }
}

// Example 2: Camera capture (domain-agnostic)
@Composable
fun CameraCaptureFeature(
    cameraProvider: CameraProvider = hiltViewModel()
) {
    var hasPermission by remember { mutableStateOf(false) }
    val activity = LocalContext.current as FragmentActivity
    
    if (!hasPermission) {
        RequestCameraPermission(
            onPermissionGranted = { hasPermission = true },
            onPermissionDenied = { /* Handle denial */ }
        )
    } else {
        Button(onClick = {
            lifecycleScope.launch {
                when (val result = cameraProvider.captureImage(activity)) {
                    is CameraResult.Success -> {
                        // Use bitmap for any domain: scan, capture, upload, etc.
                    }
                    is CameraResult.Error -> { /* Handle error */ }
                    else -> { /* Handle other cases */ }
                }
            }
        }) {
            Text("Capture")
        }
    }
}

// Example 3: Biometric authentication (domain-agnostic)
@Composable
fun SecureActionButton(
    biometricProvider: BiometricProvider = rememberBiometricProvider(),
    onAuthenticated: () -> Unit
) {
    val activity = LocalContext.current as FragmentActivity
    val scope = rememberCoroutineScope()
    
    Button(onClick = {
        scope.launch {
            when (biometricProvider.authenticate(activity, "Secure Action", "Verify to continue")) {
                is BiometricResult.Success -> onAuthenticated()
                is BiometricResult.Error -> { /* Handle error */ }
                is BiometricResult.Cancelled -> { /* Handle cancellation */ }
                is BiometricResult.NotAvailable -> { /* Fallback to PIN/password */ }
            }
        }
    }) {
        Text("Perform Secure Action")
    }
}
