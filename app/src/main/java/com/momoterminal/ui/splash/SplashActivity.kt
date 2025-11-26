package com.momoterminal.ui.splash

import android.animation.ObjectAnimator
import android.content.Intent
import android.os.Bundle
import android.view.View
import android.view.animation.AnticipateInterpolator
import androidx.activity.ComponentActivity
import androidx.core.animation.doOnEnd
import androidx.core.splashscreen.SplashScreen.Companion.installSplashScreen
import com.momoterminal.presentation.ComposeMainActivity
import dagger.hilt.android.AndroidEntryPoint

/**
 * Splash screen activity using Android 12+ SplashScreen API.
 * Provides animated icon with scaling and fade effects,
 * security checks during splash display, and smooth exit animation.
 */
@AndroidEntryPoint
class SplashActivity : ComponentActivity() {

    private var isReady = false
    private val minimumDisplayDurationMs = 1000L

    override fun onCreate(savedInstanceState: Bundle?) {
        val splashScreen = installSplashScreen()
        super.onCreate(savedInstanceState)

        // Keep splash screen visible until ready
        splashScreen.setKeepOnScreenCondition { !isReady }

        // Configure exit animation
        splashScreen.setOnExitAnimationListener { splashScreenView ->
            // Create scale and fade animations
            val scaleX = ObjectAnimator.ofFloat(
                splashScreenView.iconView,
                View.SCALE_X,
                1f,
                0f
            )
            val scaleY = ObjectAnimator.ofFloat(
                splashScreenView.iconView,
                View.SCALE_Y,
                1f,
                0f
            )
            val alpha = ObjectAnimator.ofFloat(
                splashScreenView.view,
                View.ALPHA,
                1f,
                0f
            )

            // Apply anticipate interpolator for smooth exit
            val interpolator = AnticipateInterpolator()
            scaleX.interpolator = interpolator
            scaleY.interpolator = interpolator
            alpha.interpolator = interpolator

            // Set animation duration
            scaleX.duration = 300L
            scaleY.duration = 300L
            alpha.duration = 400L

            // Remove splash screen when animation ends
            alpha.doOnEnd { splashScreenView.remove() }

            // Start animations
            scaleX.start()
            scaleY.start()
            alpha.start()
        }

        // Perform initialization and security checks
        performInitialization()
    }

    private fun performInitialization() {
        // Simulate minimum display duration for branding
        // In a real app, this would include security checks, data loading, etc.
        window.decorView.postDelayed({
            performSecurityChecks()
        }, minimumDisplayDurationMs)
    }

    private fun performSecurityChecks() {
        // Perform any security checks here
        // For now, we just mark as ready and navigate
        isReady = true
        navigateToMain()
    }

    private fun navigateToMain() {
        val intent = Intent(this, ComposeMainActivity::class.java)
        startActivity(intent)
        finish()
    }
}
