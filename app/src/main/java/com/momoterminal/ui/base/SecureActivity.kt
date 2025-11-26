package com.momoterminal.ui.base

import android.os.Bundle
import android.view.WindowManager
import androidx.appcompat.app.AppCompatActivity
import com.momoterminal.BuildConfig
import com.momoterminal.security.ScreenSecurityManager
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

/**
 * Base Activity class with automatic screen security enabled.
 * 
 * All activities that display sensitive financial information should
 * extend this class to automatically prevent screenshots and screen recording.
 * 
 * Features:
 * - Automatic FLAG_SECURE application in release builds
 * - Easy toggle for screen security per-activity
 * - Integration with ScreenSecurityManager
 */
@AndroidEntryPoint
open class SecureActivity : AppCompatActivity() {

    @Inject
    lateinit var screenSecurityManager: ScreenSecurityManager

    /**
     * Override to disable screen security for specific activities.
     * Default is true (security enabled).
     */
    protected open val enableScreenSecurity: Boolean = true

    /**
     * Override to force enable screen security even in debug builds.
     * Default is false (follows build type).
     */
    protected open val forceScreenSecurity: Boolean = false

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        applyScreenSecurity()
    }

    /**
     * Applies screen security based on activity configuration.
     */
    private fun applyScreenSecurity() {
        if (!enableScreenSecurity) {
            Timber.d("Screen security disabled for ${this::class.simpleName}")
            return
        }

        // In release builds, always enable security
        // In debug builds, only enable if forced
        val shouldEnable = !BuildConfig.DEBUG || forceScreenSecurity

        if (shouldEnable) {
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                Timber.d("Screen security enabled for ${this::class.simpleName}")
            } catch (e: Exception) {
                Timber.e(e, "Failed to enable screen security")
            }
        } else {
            Timber.d("Screen security skipped in debug mode for ${this::class.simpleName}")
        }
    }

    /**
     * Temporarily disables screen security.
     * Use with caution - only for non-sensitive operations.
     */
    protected fun temporarilyDisableScreenSecurity() {
        try {
            window.clearFlags(WindowManager.LayoutParams.FLAG_SECURE)
            Timber.d("Screen security temporarily disabled")
        } catch (e: Exception) {
            Timber.e(e, "Failed to disable screen security")
        }
    }

    /**
     * Re-enables screen security after temporary disable.
     */
    protected fun reEnableScreenSecurity() {
        if (enableScreenSecurity && (!BuildConfig.DEBUG || forceScreenSecurity)) {
            try {
                window.addFlags(WindowManager.LayoutParams.FLAG_SECURE)
                Timber.d("Screen security re-enabled")
            } catch (e: Exception) {
                Timber.e(e, "Failed to re-enable screen security")
            }
        }
    }

    /**
     * Checks if screen security is currently enabled.
     */
    protected fun isScreenSecurityEnabled(): Boolean {
        return try {
            val flags = window.attributes.flags
            (flags and WindowManager.LayoutParams.FLAG_SECURE) != 0
        } catch (e: Exception) {
            Timber.e(e, "Failed to check screen security status")
            false
        }
    }
}
