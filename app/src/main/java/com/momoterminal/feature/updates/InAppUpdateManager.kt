package com.momoterminal.feature.updates

import android.app.Activity
import android.content.Context
import androidx.activity.result.ActivityResultLauncher
import androidx.activity.result.IntentSenderRequest
import com.google.android.play.core.appupdate.AppUpdateInfo
import com.google.android.play.core.appupdate.AppUpdateManager
import com.google.android.play.core.appupdate.AppUpdateManagerFactory
import com.google.android.play.core.appupdate.AppUpdateOptions
import com.google.android.play.core.install.InstallStateUpdatedListener
import com.google.android.play.core.install.model.AppUpdateType
import com.google.android.play.core.install.model.InstallStatus
import com.google.android.play.core.install.model.UpdateAvailability
import com.google.android.play.core.ktx.installStatus
import com.google.android.play.core.ktx.isFlexibleUpdateAllowed
import com.google.android.play.core.ktx.isImmediateUpdateAllowed
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.channels.awaitClose
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.callbackFlow
import kotlinx.coroutines.suspendCancellableCoroutine
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton
import kotlin.coroutines.resume
import kotlin.coroutines.resumeWithException

/**
 * Sealed class representing the current update state.
 */
sealed class UpdateState {
    /**
     * No update is available.
     */
    data object NoUpdate : UpdateState()
    
    /**
     * An update is available.
     */
    data class UpdateAvailable(
        val versionCode: Int,
        val priority: Int,
        val isImmediate: Boolean,
        val isFlexible: Boolean
    ) : UpdateState()
    
    /**
     * Update is being downloaded.
     */
    data class Downloading(
        val bytesDownloaded: Long,
        val totalBytesToDownload: Long
    ) : UpdateState() {
        val progress: Float
            get() = if (totalBytesToDownload > 0) {
                bytesDownloaded.toFloat() / totalBytesToDownload
            } else {
                0f
            }
    }
    
    /**
     * Update has been downloaded and is ready to install.
     */
    data object Downloaded : UpdateState()
    
    /**
     * Update check or download failed.
     */
    data class Failed(val exception: Exception) : UpdateState()
}

/**
 * Manager class for handling in-app updates using Play Core library.
 */
@Singleton
class InAppUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val appUpdateManager: AppUpdateManager = AppUpdateManagerFactory.create(context)
    
    companion object {
        /**
         * Priority threshold for immediate updates.
         * Updates with priority >= 4 will trigger immediate update flow.
         */
        private const val IMMEDIATE_UPDATE_PRIORITY_THRESHOLD = 4
    }
    
    /**
     * Check for available updates.
     * Returns UpdateState indicating if an update is available and its type.
     */
    suspend fun checkForUpdates(): UpdateState = suspendCancellableCoroutine { continuation ->
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                val updateState = when (appUpdateInfo.updateAvailability()) {
                    UpdateAvailability.UPDATE_AVAILABLE -> {
                        val priority = appUpdateInfo.updatePriority()
                        val isImmediate = appUpdateInfo.isImmediateUpdateAllowed
                        val isFlexible = appUpdateInfo.isFlexibleUpdateAllowed
                        
                        UpdateState.UpdateAvailable(
                            versionCode = appUpdateInfo.availableVersionCode(),
                            priority = priority,
                            isImmediate = isImmediate && priority >= IMMEDIATE_UPDATE_PRIORITY_THRESHOLD,
                            isFlexible = isFlexible
                        )
                    }
                    UpdateAvailability.UPDATE_NOT_AVAILABLE -> UpdateState.NoUpdate
                    UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS -> {
                        // Update already in progress
                        when (appUpdateInfo.installStatus) {
                            InstallStatus.DOWNLOADING -> UpdateState.Downloading(
                                bytesDownloaded = appUpdateInfo.bytesDownloaded(),
                                totalBytesToDownload = appUpdateInfo.totalBytesToDownload()
                            )
                            InstallStatus.DOWNLOADED -> UpdateState.Downloaded
                            else -> UpdateState.NoUpdate
                        }
                    }
                    else -> UpdateState.NoUpdate
                }
                
                Timber.d("Update check result: $updateState")
                continuation.resume(updateState)
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to check for updates")
                continuation.resumeWithException(exception)
            }
    }
    
    /**
     * Start an immediate update flow.
     * This will block the user from using the app until the update is installed.
     *
     * @param activity The activity to use for the update flow
     * @param launcher ActivityResultLauncher for handling the update result
     */
    suspend fun startImmediateUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): Boolean = suspendCancellableCoroutine { continuation ->
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isImmediateUpdateAllowed
                ) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.IMMEDIATE).build()
                        )
                        continuation.resume(true)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to start immediate update")
                        continuation.resume(false)
                    }
                } else {
                    continuation.resume(false)
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to get app update info for immediate update")
                continuation.resume(false)
            }
    }
    
    /**
     * Start a flexible update flow.
     * This downloads the update in the background while the user continues using the app.
     *
     * @param activity The activity to use for the update flow
     * @param launcher ActivityResultLauncher for handling the update result
     */
    suspend fun startFlexibleUpdate(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ): Boolean = suspendCancellableCoroutine { continuation ->
        appUpdateManager.appUpdateInfo
            .addOnSuccessListener { appUpdateInfo ->
                if (appUpdateInfo.updateAvailability() == UpdateAvailability.UPDATE_AVAILABLE &&
                    appUpdateInfo.isFlexibleUpdateAllowed
                ) {
                    try {
                        appUpdateManager.startUpdateFlowForResult(
                            appUpdateInfo,
                            launcher,
                            AppUpdateOptions.newBuilder(AppUpdateType.FLEXIBLE).build()
                        )
                        continuation.resume(true)
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to start flexible update")
                        continuation.resume(false)
                    }
                } else {
                    continuation.resume(false)
                }
            }
            .addOnFailureListener { exception ->
                Timber.e(exception, "Failed to get app update info for flexible update")
                continuation.resume(false)
            }
    }
    
    /**
     * Observe install state updates for flexible updates.
     * Returns a Flow that emits UpdateState changes during download.
     */
    fun observeInstallState(): Flow<UpdateState> = callbackFlow {
        val listener = InstallStateUpdatedListener { state ->
            val updateState = when (state.installStatus()) {
                InstallStatus.DOWNLOADING -> UpdateState.Downloading(
                    bytesDownloaded = state.bytesDownloaded(),
                    totalBytesToDownload = state.totalBytesToDownload()
                )
                InstallStatus.DOWNLOADED -> UpdateState.Downloaded
                InstallStatus.FAILED -> UpdateState.Failed(
                    Exception("Update download failed with error code: ${state.installErrorCode()}")
                )
                InstallStatus.CANCELED -> UpdateState.NoUpdate
                else -> null
            }
            
            updateState?.let { trySend(it) }
        }
        
        appUpdateManager.registerListener(listener)
        
        awaitClose {
            appUpdateManager.unregisterListener(listener)
        }
    }
    
    /**
     * Complete a flexible update by installing the downloaded update.
     * This will restart the app.
     */
    fun completeUpdate() {
        Timber.d("Completing flexible update - app will restart")
        appUpdateManager.completeUpdate()
    }
    
    /**
     * Resume an in-progress update if the app was closed during update.
     * Should be called in onResume of the main activity.
     */
    suspend fun resumeUpdateIfNeeded(
        activity: Activity,
        launcher: ActivityResultLauncher<IntentSenderRequest>
    ) {
        try {
            val appUpdateInfo = suspendCancellableCoroutine<AppUpdateInfo> { continuation ->
                appUpdateManager.appUpdateInfo
                    .addOnSuccessListener { continuation.resume(it) }
                    .addOnFailureListener { continuation.resumeWithException(it) }
            }
            
            when {
                // Immediate update was interrupted
                appUpdateInfo.updateAvailability() == UpdateAvailability.DEVELOPER_TRIGGERED_UPDATE_IN_PROGRESS &&
                appUpdateInfo.isImmediateUpdateAllowed -> {
                    startImmediateUpdate(activity, launcher)
                }
                
                // Flexible update was downloaded but not installed
                appUpdateInfo.installStatus == InstallStatus.DOWNLOADED -> {
                    // Update is ready to install
                    Timber.d("Downloaded update found, ready to install")
                }
            }
        } catch (e: Exception) {
            Timber.e(e, "Failed to check for in-progress updates")
        }
    }
}
