package com.momoterminal.baselineprofile

import androidx.benchmark.macro.junit4.BaselineProfileRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import androidx.test.uiautomator.By
import androidx.test.uiautomator.Direction
import androidx.test.uiautomator.Until
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Generates Baseline Profiles for the MomoTerminal app.
 * 
 * Baseline Profiles improve app startup by 30%+ through AOT compilation
 * of critical code paths. This generator exercises the most common user
 * journeys to optimize those paths.
 * 
 * Run this test on a physical device or emulator to generate profiles:
 * ./gradlew :baselineprofile:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=BaselineProfile
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class BaselineProfileGenerator {

    @get:Rule
    val rule = BaselineProfileRule()

    /**
     * Generate baseline profiles for cold startup.
     * This captures the critical startup path from launch to first frame.
     */
    @Test
    fun generateStartupProfile() {
        rule.collect(
            packageName = PACKAGE_NAME,
            includeInStartupProfile = true
        ) {
            // Start from a fresh process
            pressHome()
            startActivityAndWait()
            
            // Wait for the splash screen to complete
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT)
        }
    }

    /**
     * Generate baseline profiles for critical user journeys.
     * This covers the main user flows in the app.
     */
    @Test
    fun generateCriticalJourneysProfile() {
        rule.collect(
            packageName = PACKAGE_NAME,
            includeInStartupProfile = true
        ) {
            pressHome()
            startActivityAndWait()
            
            // Wait for main UI to be ready
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT)
            
            // Exercise transaction flow
            exerciseTransactionFlow()
            
            // Exercise settings
            exerciseSettingsFlow()
            
            // Exercise NFC terminal if available
            exerciseNfcTerminalFlow()
        }
    }

    /**
     * Generate baseline profiles for scrolling content.
     * Optimizes list scrolling performance.
     */
    @Test
    fun generateScrollingProfile() {
        rule.collect(
            packageName = PACKAGE_NAME,
            includeInStartupProfile = false
        ) {
            pressHome()
            startActivityAndWait()
            
            // Wait for content to load
            device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), TIMEOUT)
            
            // Scroll transaction lists
            scrollTransactionList()
        }
    }

    private fun exerciseTransactionFlow() {
        // Navigate to transactions if available
        device.findObject(By.text("Transactions"))?.click()
        device.waitForIdle()
        
        // Wait for transaction list to load
        device.wait(Until.hasObject(By.scrollable(true)), SHORT_TIMEOUT)
        
        // Scroll through transactions
        device.findObject(By.scrollable(true))?.let { list ->
            list.setGestureMargin(device.displayWidth / 5)
            list.fling(Direction.DOWN)
            device.waitForIdle()
            list.fling(Direction.UP)
            device.waitForIdle()
        }
    }

    private fun exerciseSettingsFlow() {
        // Navigate to settings
        device.findObject(By.desc("Settings"))?.click()
            ?: device.findObject(By.text("Settings"))?.click()
        device.waitForIdle()
        
        // Wait for settings to load
        device.wait(Until.hasObject(By.scrollable(true)), SHORT_TIMEOUT)
        
        // Scroll through settings
        device.findObject(By.scrollable(true))?.let { list ->
            list.setGestureMargin(device.displayWidth / 5)
            list.scroll(Direction.DOWN, 0.8f)
            device.waitForIdle()
        }
        
        // Go back
        device.pressBack()
        device.waitForIdle()
    }

    private fun exerciseNfcTerminalFlow() {
        // Navigate to NFC terminal if available
        device.findObject(By.text("NFC Terminal"))?.click()
            ?: device.findObject(By.desc("NFC Terminal"))?.click()
        device.waitForIdle()
        
        // Wait for NFC screen
        device.wait(Until.hasObject(By.pkg(PACKAGE_NAME).depth(0)), SHORT_TIMEOUT)
        
        // Go back
        device.pressBack()
        device.waitForIdle()
    }

    private fun scrollTransactionList() {
        // Find and scroll any scrollable content
        device.findObject(By.scrollable(true))?.let { list ->
            list.setGestureMargin(device.displayWidth / 5)
            
            // Scroll down multiple times
            repeat(3) {
                list.scroll(Direction.DOWN, 0.8f)
                device.waitForIdle()
            }
            
            // Scroll back up
            repeat(3) {
                list.scroll(Direction.UP, 0.8f)
                device.waitForIdle()
            }
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.momoterminal"
        private const val TIMEOUT = 10_000L
        private const val SHORT_TIMEOUT = 5_000L
    }
}
