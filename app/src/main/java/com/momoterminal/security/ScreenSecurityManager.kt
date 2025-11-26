package com.momoterminal.security

import android.app.Activity
import android.view.Window
import android.view.WindowManager
import com.momoterminal.BuildConfig
import timber.log.Timber
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for screen security features to prevent screenshots
 * and screen recording of sensitive financial information.
 * 
 * Uses FLAG_SECURE to prevent:
 * - Screenshots
 * - Screen recording
 * - Screen sharing
 * - Display in recent apps thumbnail
 */
@Singleton
class ScreenSecurityManager @Inject constructor() {

    /**
     * Enables screen security on the given window.
     * 
     * In debug builds, security is disabled to allow screenshots for
     * testing and debugging purposes.
     * 
     * @param window The window to secure
     * @param force If true, enables security even in debug builds
     */
    fun enableScreenSecurity(window: Window, force: Boolean = false) {
        if (!shouldEnableSecurity() && !force) {
            Timber.d("Screen security disabled in debug mode")
            return
        }

        try {
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Timber.d("Screen security enabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to enable screen security")
        }
    }

    /**
     * Disables screen security on the given window.
     * 
     * Use with caution - only disable for non-sensitive screens.
     * 
     * @param window The window to unsecure
     */
    fun disableScreenSecurity(window: Window) {
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Timber.d("Screen security disabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to disable screen security")
        }
    }

    /**
     * Enables screen security on an Activity.
     * 
     * @param activity The activity to secure
     * @param force If true, enables security even in debug builds
     */
    fun enableScreenSecurity(activity: Activity, force: Boolean = false) {
        enableScreenSecurity(activity.window, force)
    }

    /**
     * Disables screen security on an Activity.
     * 
     * @param activity The activity to unsecure
     */
    fun disableScreenSecurity(activity: Activity) {
        disableScreenSecurity(activity.window)
    }

    /**
     * Checks if screen security is currently enabled on the window.
     * 
     * @param window The window to check
     * @return true if FLAG_SECURE is set
     */
    fun isScreenSecurityEnabled(window: Window): Boolean {
        return try {
            val flags = window.attributes.flags
            (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to check screen security status")
            false
        }
    }

    /**
     * Checks if screen security is enabled on an Activity.
     * 
     * @param activity The activity to check
     * @return true if FLAG_SECURE is set
     */
    fun isScreenSecurityEnabled(activity: Activity): Boolean {
        return isScreenSecurityEnabled(activity.window)
    }

    /**
     * Toggles screen security on the window.
     * 
     * @param window The window to toggle security on
     * @return true if security is now enabled, false otherwise
     */
    fun toggleScreenSecurity(window: Window): Boolean {
        return if (isScreenSecurityEnabled(window)) {
            disableScreenSecurity(window)
            false
        } else {
            enableScreenSecurity(window, force = true)
            true
        }
    }

    /**
     * Determines if screen security should be enabled based on build type.
     * 
     * In debug builds, security is typically disabled for testing.
     * In release builds, security is always enabled.
     */
    private fun shouldEnableSecurity(): Boolean {
        return !BuildConfig.DEBUG
    }

    /**
     * Applies recommended security flags to a window.
     * 
     * This includes:
     * - FLAG_SECURE for screenshot prevention
     * - Additional window flags for enhanced security
     * 
     * @param window The window to secure
     * @param force If true, applies security even in debug builds
     */
    fun applyFullSecurityFlags(window: Window, force: Boolean = false) {
        if (!shouldEnableSecurity() && !force) {
            Timber.d("Full security flags disabled in debug mode")
            return
        }

        try {
            // Prevent screenshots and screen recording
            window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)

            Timber.d("Full security flags applied")
        } catch (e: Exception) {
            Timber.e(e, "Failed to apply full security flags")
        }
    }

    companion object {
        /**
         * Extension function to easily secure an Activity.
         */
        fun Activity.enableSecureMode(force: Boolean = false) {
            val flags = WindowManager.LayoutParams.FLAG_SECURE
            if (!BuildConfig.DEBUG || force) {
                window.addFlags(flags)
            }
        }

        /**
         * Extension function to unsecure an Activity.
         */
        fun Activity.disableSecureMode() {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
        }
    }
}
