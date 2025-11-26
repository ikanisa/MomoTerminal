package com.momoterminal.security

import android.content.Context
import com.momoterminal.BuildConfig
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Initializer for application security checks.
 * 
 * Performs comprehensive security validation at app startup to ensure
 * the device and environment are safe for financial transactions.
 */
@Singleton
class AppSecurityInitializer @Inject constructor(
    @ApplicationContext private val context: Context,
    private val deviceSecurityManager: DeviceSecurityManager
) {

    /**
     * Result of the security initialization.
     */
    sealed class InitializationResult {
        /**
         * Security checks passed successfully.
         */
        data object Success : InitializationResult()

        /**
         * Security checks failed.
         * @param warnings List of security warnings that should be addressed
         * @param criticalFailures List of critical security failures that block app usage
         */
        data class Failed(
            val warnings: List<SecurityWarning>,
            val criticalFailures: List<SecurityFailure>
        ) : InitializationResult()
    }

    /**
     * Security warning that should be shown to the user but doesn't block usage.
     */
    data class SecurityWarning(
        val type: WarningType,
        val message: String
    )

    /**
     * Critical security failure that should block app usage.
     */
    data class SecurityFailure(
        val type: FailureType,
        val message: String
    )

    /**
     * Types of security warnings.
     */
    enum class WarningType {
        DEVELOPER_OPTIONS_ENABLED,
        USB_DEBUGGING_ENABLED,
        SCREEN_OVERLAY_DETECTED
    }

    /**
     * Types of security failures.
     */
    enum class FailureType {
        DEVICE_ROOTED,
        EMULATOR_DETECTED,
        INSTRUMENTATION_DETECTED,
        TAMPERING_DETECTED
    }

    /**
     * Performs all security checks during app initialization.
     * 
     * @param strictMode If true, warnings are treated as failures
     * @return InitializationResult indicating success or failure with details
     */
    fun initialize(strictMode: Boolean = false): InitializationResult {
        Timber.d("Starting security initialization...")

        val warnings = mutableListOf<SecurityWarning>()
        val failures = mutableListOf<SecurityFailure>()

        // Perform device security check
        val securityResult = deviceSecurityManager.performSecurityCheck()

        // Check for root
        if (securityResult.isRooted) {
            if (shouldBlockOnRoot()) {
                failures.add(
                    SecurityFailure(
                        FailureType.DEVICE_ROOTED,
                        "Device appears to be rooted. For security reasons, this app cannot run on rooted devices."
                    )
                )
            } else {
                Timber.w("Device is rooted but root blocking is disabled")
            }
        }

        // Check for emulator
        if (securityResult.isEmulator) {
            if (shouldBlockOnEmulator()) {
                failures.add(
                    SecurityFailure(
                        FailureType.EMULATOR_DETECTED,
                        "Emulator detected. This app must run on a physical device for security."
                    )
                )
            } else {
                Timber.w("Running on emulator but emulator blocking is disabled")
            }
        }

        // Check for instrumentation frameworks
        if (securityResult.hasInstrumentation) {
            failures.add(
                SecurityFailure(
                    FailureType.INSTRUMENTATION_DETECTED,
                    "Security instrumentation framework detected. Please uninstall any hooking frameworks."
                )
            )
        }

        // Check for debug/developer options (warning only)
        if (securityResult.isDebuggable) {
            warnings.add(
                SecurityWarning(
                    WarningType.DEVELOPER_OPTIONS_ENABLED,
                    "Developer options or USB debugging is enabled. Consider disabling for enhanced security."
                )
            )

            if (strictMode) {
                failures.add(
                    SecurityFailure(
                        FailureType.TAMPERING_DETECTED,
                        "Debug mode detected in strict security mode."
                    )
                )
            }
        }

        // Log security check results
        logSecurityResults(securityResult, warnings, failures)

        return if (failures.isEmpty()) {
            Timber.d("Security initialization completed successfully")
            InitializationResult.Success
        } else {
            Timber.e("Security initialization failed with ${failures.size} critical failures")
            InitializationResult.Failed(warnings, failures)
        }
    }

    /**
     * Quick security check without detailed results.
     * 
     * @return true if all critical security checks pass
     */
    fun quickCheck(): Boolean {
        return when (initialize(strictMode = false)) {
            is InitializationResult.Success -> true
            is InitializationResult.Failed -> false
        }
    }

    /**
     * Determines if the app should block on rooted devices.
     * 
     * In debug builds, rooting is allowed for testing.
     * In release builds, rooted devices are blocked.
     */
    private fun shouldBlockOnRoot(): Boolean {
        return !BuildConfig.DEBUG
    }

    /**
     * Determines if the app should block on emulators.
     * 
     * In debug builds, emulators are allowed for testing.
     * In release builds, emulators are blocked.
     */
    private fun shouldBlockOnEmulator(): Boolean {
        return !BuildConfig.DEBUG
    }

    /**
     * Logs the security check results for debugging and monitoring.
     */
    private fun logSecurityResults(
        result: DeviceSecurityManager.SecurityCheckResult,
        warnings: List<SecurityWarning>,
        failures: List<SecurityFailure>
    ) {
        Timber.d("=== Security Check Results ===")
        Timber.d("Secure: ${result.isSecure}")
        Timber.d("Rooted: ${result.isRooted}")
        Timber.d("Emulator: ${result.isEmulator}")
        Timber.d("Instrumentation: ${result.hasInstrumentation}")
        Timber.d("Debuggable: ${result.isDebuggable}")

        if (result.failureReasons.isNotEmpty()) {
            Timber.d("Failure reasons:")
            result.failureReasons.forEach { reason ->
                Timber.d("  - $reason")
            }
        }

        if (warnings.isNotEmpty()) {
            Timber.w("Warnings:")
            warnings.forEach { warning ->
                Timber.w("  - [${warning.type}] ${warning.message}")
            }
        }

        if (failures.isNotEmpty()) {
            Timber.e("Critical failures:")
            failures.forEach { failure ->
                Timber.e("  - [${failure.type}] ${failure.message}")
            }
        }

        Timber.d("==============================")
    }

    /**
     * Gets a user-friendly summary of security status.
     */
    fun getSecuritySummary(): String {
        val result = deviceSecurityManager.performSecurityCheck()
        return buildString {
            append("Device Security Status:\n")
            append("• Secure: ${if (result.isSecure) "✓" else "✗"}\n")
            append("• Root Detection: ${if (!result.isRooted) "✓ Not rooted" else "✗ Rooted"}\n")
            append("• Emulator Detection: ${if (!result.isEmulator) "✓ Physical device" else "✗ Emulator"}\n")
            append("• Instrumentation: ${if (!result.hasInstrumentation) "✓ Clean" else "✗ Detected"}\n")
            append("• Debug Mode: ${if (!result.isDebuggable) "✓ Disabled" else "⚠ Enabled"}")
        }
    }
}
