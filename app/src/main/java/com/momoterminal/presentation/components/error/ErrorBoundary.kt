package com.momoterminal.presentation.components.error

import android.content.Intent
import android.os.Process
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.runtime.Composable
import androidx.compose.runtime.DisposableEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Modifier
import androidx.compose.ui.platform.LocalContext
import com.momoterminal.error.AppError
import timber.log.Timber
import kotlin.system.exitProcess

/**
 * Error boundary state for tracking caught exceptions.
 */
data class ErrorBoundaryState(
    val hasError: Boolean = false,
    val error: Throwable? = null,
    val errorInfo: String? = null
)

/**
 * Composable error boundary that catches and handles errors in the composition.
 *
 * This provides a global error handling wrapper similar to React's Error Boundary.
 * When an error occurs during composition:
 * 1. The error is logged via Timber/Crashlytics
 * 2. A fallback error UI is displayed
 * 3. User can retry or restart the app
 *
 * Usage:
 * ```kotlin
 * ErrorBoundary(
 *     onError = { error -> /* Handle error */ },
 *     fallback = { error, onRetry -> ErrorScreen(error, onRetry) }
 * ) {
 *     // Your composable content that might throw
 *     MyScreen()
 * }
 * ```
 *
 * Note: This is a best-effort error boundary. Some types of errors
 * (particularly those in event handlers) may not be catchable.
 */
@Composable
fun ErrorBoundary(
    modifier: Modifier = Modifier,
    onError: ((Throwable) -> Unit)? = null,
    fallback: @Composable (AppError, () -> Unit) -> Unit = { error, onRetry ->
        ErrorScreen(
            error = error,
            onRetry = onRetry,
            modifier = Modifier.fillMaxSize()
        )
    },
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    var errorState by remember { mutableStateOf(ErrorBoundaryState()) }

    // Set up uncaught exception handler for this scope
    DisposableEffect(Unit) {
        val defaultHandler = Thread.getDefaultUncaughtExceptionHandler()
        
        Thread.setDefaultUncaughtExceptionHandler { thread, throwable ->
            Timber.e(throwable, "Uncaught exception in ErrorBoundary: ${throwable.message}")
            
            // Log to Crashlytics if available
            try {
                com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                    .recordException(throwable)
            } catch (e: Exception) {
                Timber.w(e, "Failed to log to Crashlytics")
            }
            
            errorState = ErrorBoundaryState(
                hasError = true,
                error = throwable,
                errorInfo = "Thread: ${thread.name}"
            )
            
            onError?.invoke(throwable)
            
            // For fatal errors, let the default handler manage them
            if (throwable is Error && throwable !is AssertionError) {
                defaultHandler?.uncaughtException(thread, throwable)
            }
        }
        
        onDispose {
            Thread.setDefaultUncaughtExceptionHandler(defaultHandler)
        }
    }

    Box(modifier = modifier) {
        if (errorState.hasError) {
            val appError = errorState.error?.let { throwable ->
                when (throwable) {
                    is OutOfMemoryError -> AppError.Unknown(
                        message = "The app ran out of memory. Please restart the app.",
                        cause = throwable
                    )
                    is SecurityException -> AppError.Security.EncryptionFailed(
                        cause = throwable
                    )
                    is IllegalStateException -> AppError.Unknown(
                        message = "An unexpected error occurred: ${throwable.message}",
                        cause = throwable
                    )
                    is IllegalArgumentException -> AppError.Validation.InvalidInput(
                        field = "data",
                        message = "Invalid data: ${throwable.message}"
                    )
                    else -> AppError.Unknown(
                        message = throwable.message ?: "An unexpected error occurred",
                        cause = throwable
                    )
                }
            } ?: AppError.Unknown(
                message = "An unexpected error occurred",
                cause = null
            )

            fallback(appError) {
                // Retry callback - reset error state to retry rendering
                errorState = ErrorBoundaryState()
            }
        } else {
            // Wrap content in try-catch for composition errors
            try {
                content()
            } catch (e: Exception) {
                Timber.e(e, "Error during composition: ${e.message}")
                
                // Log to Crashlytics
                try {
                    com.google.firebase.crashlytics.FirebaseCrashlytics.getInstance()
                        .recordException(e)
                } catch (ex: Exception) {
                    Timber.w(ex, "Failed to log to Crashlytics")
                }
                
                errorState = ErrorBoundaryState(
                    hasError = true,
                    error = e,
                    errorInfo = "Composition error"
                )
                
                onError?.invoke(e)
            }
        }
    }
}

/**
 * Application-level error boundary with restart capability.
 *
 * This is designed to be used at the root of the app's composition,
 * providing crash recovery options including app restart.
 *
 * Usage:
 * ```kotlin
 * setContent {
 *     AppErrorBoundary {
 *         MomoTerminalApp()
 *     }
 * }
 * ```
 */
@Composable
fun AppErrorBoundary(
    modifier: Modifier = Modifier,
    onError: ((Throwable) -> Unit)? = null,
    content: @Composable () -> Unit
) {
    val context = LocalContext.current
    
    ErrorBoundary(
        modifier = modifier,
        onError = { throwable ->
            Timber.e(throwable, "App-level error caught")
            onError?.invoke(throwable)
        },
        fallback = { error, onRetry ->
            CrashRecoveryScreen(
                error = error,
                onRetry = onRetry,
                onRestart = {
                    // Restart the app
                    restartApp(context)
                }
            )
        },
        content = content
    )
}

/**
 * Crash recovery screen with retry and restart options.
 */
@Composable
private fun CrashRecoveryScreen(
    error: AppError,
    onRetry: () -> Unit,
    onRestart: () -> Unit,
    modifier: Modifier = Modifier
) {
    ErrorScreen(
        error = error,
        onRetry = if (error.isRecoverable) onRetry else null,
        onAction = { onRestart() },
        action = com.momoterminal.error.ErrorAction.Retry,
        actionLabel = "Restart App",
        modifier = modifier
    )
}

/**
 * Restart the application.
 */
private fun restartApp(context: android.content.Context) {
    try {
        val packageManager = context.packageManager
        val intent = packageManager.getLaunchIntentForPackage(context.packageName)
        
        if (intent != null) {
            val mainIntent = Intent.makeRestartActivityTask(intent.component)
            context.startActivity(mainIntent)
            
            // Kill current process
            Process.killProcess(Process.myPid())
            exitProcess(0)
        }
    } catch (e: Exception) {
        Timber.e(e, "Failed to restart app")
    }
}
