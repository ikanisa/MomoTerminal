package com.momoterminal.baselineprofile

import androidx.benchmark.macro.CompilationMode
import androidx.benchmark.macro.ExperimentalMetricApi
import androidx.benchmark.macro.FrameTimingMetric
import androidx.benchmark.macro.StartupMode
import androidx.benchmark.macro.StartupTimingMetric
import androidx.benchmark.macro.junit4.MacrobenchmarkRule
import androidx.test.ext.junit.runners.AndroidJUnit4
import androidx.test.filters.LargeTest
import org.junit.Rule
import org.junit.Test
import org.junit.runner.RunWith

/**
 * Benchmarks for measuring app startup performance.
 * 
 * Compares startup times across different compilation modes:
 * - None: No ahead-of-time compilation (worst case)
 * - BaselineProfile: With baseline profile (optimized)
 * - Full: Full AOT compilation (best case)
 * 
 * Run benchmarks with:
 * ./gradlew :baselineprofile:connectedAndroidTest -Pandroid.testInstrumentationRunnerArguments.androidx.benchmark.enabledRules=Macrobenchmark
 */
@RunWith(AndroidJUnit4::class)
@LargeTest
class StartupBenchmark {

    @get:Rule
    val rule = MacrobenchmarkRule()

    /**
     * Cold startup with no compilation.
     * This represents the worst-case startup scenario for new users.
     */
    @Test
    fun startupNoCompilation() {
        measureStartup(CompilationMode.None())
    }

    /**
     * Cold startup with baseline profile.
     * This represents typical startup after baseline profiles are installed.
     */
    @Test
    fun startupBaselineProfile() {
        measureStartup(CompilationMode.Partial())
    }

    /**
     * Cold startup with full compilation.
     * This represents the best-case startup after full AOT compilation.
     */
    @Test
    fun startupFullCompilation() {
        measureStartup(CompilationMode.Full())
    }

    /**
     * Warm startup benchmark.
     * Measures startup when the app process already exists.
     */
    @Test
    fun startupWarm() {
        measureStartup(
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.WARM
        )
    }

    /**
     * Hot startup benchmark.
     * Measures startup when the activity already exists.
     */
    @Test
    fun startupHot() {
        measureStartup(
            compilationMode = CompilationMode.Partial(),
            startupMode = StartupMode.HOT
        )
    }

    @OptIn(ExperimentalMetricApi::class)
    private fun measureStartup(
        compilationMode: CompilationMode,
        startupMode: StartupMode = StartupMode.COLD
    ) {
        rule.measureRepeated(
            packageName = PACKAGE_NAME,
            metrics = listOf(
                StartupTimingMetric(),
                FrameTimingMetric()
            ),
            compilationMode = compilationMode,
            startupMode = startupMode,
            iterations = ITERATIONS
        ) {
            pressHome()
            startActivityAndWait()
        }
    }

    companion object {
        private const val PACKAGE_NAME = "com.momoterminal"
        private const val ITERATIONS = 5
    }
}
