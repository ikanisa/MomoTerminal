package com.momoterminal.security

import android.content.Context
import android.content.pm.PackageManager
import android.os.Build
import android.provider.Settings
import dagger.hilt.android.qualifiers.ApplicationContext
import timber.log.Timber
import java.io.File
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Manager for device security checks including root detection,
 * emulator detection, and instrumentation framework detection.
 * 
 * This class provides comprehensive security checks to ensure
 * the application runs on a trusted, uncompromised device.
 */
@Singleton
class DeviceSecurityManager @Inject constructor(
    @ApplicationContext private val context: Context
) {

    /**
     * Result of security check containing detailed information.
     */
    data class SecurityCheckResult(
        val isSecure: Boolean,
        val isRooted: Boolean,
        val isEmulator: Boolean,
        val hasInstrumentation: Boolean,
        val isDebuggable: Boolean,
        val failureReasons: List<String>
    )

    /**
     * Performs comprehensive device security checks.
     * Returns a detailed result with all security concerns.
     */
    fun performSecurityCheck(): SecurityCheckResult {
        val failureReasons = mutableListOf<String>()
        
        val isRooted = checkRootStatus(failureReasons)
        val isEmulator = checkEmulatorStatus(failureReasons)
        val hasInstrumentation = checkInstrumentationStatus(failureReasons)
        val isDebuggable = checkDebuggableStatus(failureReasons)
        
        val isSecure = !isRooted && !isEmulator && !hasInstrumentation && !isDebuggable
        
        return SecurityCheckResult(
            isSecure = isSecure,
            isRooted = isRooted,
            isEmulator = isEmulator,
            hasInstrumentation = hasInstrumentation,
            isDebuggable = isDebuggable,
            failureReasons = failureReasons
        )
    }

    /**
     * Quick check if device is considered safe for financial transactions.
     */
    fun isDeviceSecure(): Boolean {
        return performSecurityCheck().isSecure
    }

    /**
     * Checks for root access on the device.
     */
    fun isDeviceRooted(): Boolean {
        return checkRootStatus(mutableListOf())
    }

    /**
     * Checks if running on an emulator.
     */
    fun isRunningOnEmulator(): Boolean {
        return checkEmulatorStatus(mutableListOf())
    }

    /**
     * Checks if instrumentation frameworks are present.
     */
    fun hasInstrumentationFramework(): Boolean {
        return checkInstrumentationStatus(mutableListOf())
    }

    // ==================== Root Detection ====================

    private fun checkRootStatus(reasons: MutableList<String>): Boolean {
        var isRooted = false

        if (checkRootBinaries()) {
            reasons.add("Root binaries detected")
            isRooted = true
        }

        if (checkRootPackages()) {
            reasons.add("Root management packages detected")
            isRooted = true
        }

        if (checkSuExists()) {
            reasons.add("SU command available")
            isRooted = true
        }

        if (checkRootCloaking()) {
            reasons.add("Root cloaking detected")
            isRooted = true
        }

        if (checkSystemProperties()) {
            reasons.add("Suspicious system properties detected")
            isRooted = true
        }

        if (checkRWSystem()) {
            reasons.add("System partition is writable")
            isRooted = true
        }

        return isRooted
    }

    /**
     * Checks for common root binary paths (15+ paths).
     */
    private fun checkRootBinaries(): Boolean {
        val binaryPaths = listOf(
            "/system/bin/su",
            "/system/xbin/su",
            "/sbin/su",
            "/system/su",
            "/system/bin/.ext/.su",
            "/system/usr/we-need-root/su-backup",
            "/system/xbin/mu",
            "/data/local/su",
            "/data/local/bin/su",
            "/data/local/xbin/su",
            "/su/bin/su",
            "/su/xbin/su",
            "/magisk/.core/bin/su",
            "/system/app/Superuser.apk",
            "/system/etc/.installed_su_daemon",
            "/dev/com.koushikdutta.superuser.daemon/",
            "/system/etc/init.d/99SuperSUDaemon"
        )

        return binaryPaths.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Checks for root management packages (Magisk, SuperSU, etc.).
     */
    private fun checkRootPackages(): Boolean {
        val rootPackages = listOf(
            // Magisk and derivatives
            "com.topjohnwu.magisk",
            "io.github.vvb2060.magisk",
            "io.github.huskydg.magisk",
            // SuperSU
            "eu.chainfire.supersu",
            "com.noshufou.android.su",
            "com.noshufou.android.su.elite",
            "com.koushikdutta.superuser",
            "com.thirdparty.superuser",
            // Root checkers and explorers
            "com.yellowes.su",
            "com.zachspong.temprootremovejb",
            "com.ramdroid.appquarantine",
            "com.amphoras.hidemyroot",
            "com.amphoras.hidemyrootadfree",
            "com.formyhm.hideroot",
            "com.saurik.substrate",
            "de.robv.android.xposed.installer",
            // Root file managers
            "stericson.busybox",
            "com.jrummy.root.browserfree",
            "com.jrummy.busybox.installer"
        )

        val packageManager = context.packageManager
        return rootPackages.any { packageName ->
            try {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Checks if su command is available.
     */
    private fun checkSuExists(): Boolean {
        val pathDirs = System.getenv("PATH")?.split(":") ?: return false
        
        return pathDirs.any { dir ->
            try {
                File(dir, "su").exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Checks for root cloaking mechanisms.
     */
    private fun checkRootCloaking(): Boolean {
        val cloakingPackages = listOf(
            "com.devadvance.rootcloak",
            "com.devadvance.rootcloakplus",
            "de.robv.android.xposed.installer",
            "com.saurik.substrate",
            "com.formyhm.hideroot"
        )

        val packageManager = context.packageManager
        return cloakingPackages.any { packageName ->
            try {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }
    }

    /**
     * Checks system properties for signs of rooting.
     */
    private fun checkSystemProperties(): Boolean {
        val dangerousProps = mapOf(
            "ro.debuggable" to "1",
            "ro.secure" to "0"
        )

        return dangerousProps.any { (key, badValue) ->
            try {
                val value = getSystemProperty(key)
                value == badValue
            } catch (e: Exception) {
                false
            }
        }
    }

    private fun getSystemProperty(key: String): String? {
        return try {
            val process = Runtime.getRuntime().exec("getprop $key")
            process.inputStream.bufferedReader().readLine()
        } catch (e: Exception) {
            null
        }
    }

    /**
     * Checks if system partition is writable.
     */
    private fun checkRWSystem(): Boolean {
        return try {
            val mounts = File("/proc/mounts").readText()
            mounts.contains("/system") && mounts.contains("rw,")
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Emulator Detection ====================

    private fun checkEmulatorStatus(reasons: MutableList<String>): Boolean {
        var isEmulator = false

        if (checkEmulatorBuild()) {
            reasons.add("Emulator build properties detected")
            isEmulator = true
        }

        if (checkEmulatorHardware()) {
            reasons.add("Emulator hardware characteristics detected")
            isEmulator = true
        }

        if (checkEmulatorFiles()) {
            reasons.add("Emulator files detected")
            isEmulator = true
        }

        if (checkEmulatorOperatorName()) {
            reasons.add("Emulator operator name detected")
            isEmulator = true
        }

        return isEmulator
    }

    /**
     * Checks build properties for emulator signatures.
     */
    private fun checkEmulatorBuild(): Boolean {
        val suspiciousProperties = listOf(
            Build.FINGERPRINT.startsWith("generic"),
            Build.FINGERPRINT.startsWith("unknown"),
            Build.MODEL.contains("google_sdk"),
            Build.MODEL.contains("Emulator"),
            Build.MODEL.contains("Android SDK built for x86"),
            Build.MANUFACTURER.contains("Genymotion"),
            Build.BRAND.startsWith("generic") && Build.DEVICE.startsWith("generic"),
            Build.PRODUCT == "sdk",
            Build.PRODUCT == "google_sdk",
            Build.PRODUCT == "sdk_x86",
            Build.PRODUCT == "vbox86p",
            Build.PRODUCT.contains("emulator"),
            Build.HARDWARE == "goldfish",
            Build.HARDWARE == "ranchu",
            Build.HARDWARE.contains("nox"),
            Build.HARDWARE.contains("vbox"),
            Build.BOARD == "unknown",
            Build.ID == "FRF91"
        )

        return suspiciousProperties.any { it }
    }

    /**
     * Checks hardware characteristics for emulator indicators.
     */
    private fun checkEmulatorHardware(): Boolean {
        val suspiciousHardware = listOf(
            Build.HARDWARE.contains("goldfish"),
            Build.HARDWARE.contains("ranchu"),
            Build.HARDWARE.contains("vbox"),
            Build.HARDWARE.contains("nox"),
            Build.HARDWARE == "unknown"
        )

        return suspiciousHardware.any { it }
    }

    /**
     * Checks for emulator-specific files.
     */
    private fun checkEmulatorFiles(): Boolean {
        val emulatorFiles = listOf(
            "/dev/socket/qemud",
            "/dev/qemu_pipe",
            "/system/lib/libc_malloc_debug_qemu.so",
            "/sys/qemu_trace",
            "/system/bin/qemu-props",
            "/dev/socket/genyd",
            "/dev/socket/baseband_genyd",
            "/system/bin/nox",
            "/system/bin/nox-prop",
            "/system/lib/libnoxd.so"
        )

        return emulatorFiles.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    /**
     * Checks network operator name for emulator indicators.
     */
    private fun checkEmulatorOperatorName(): Boolean {
        val suspiciousOperators = listOf(
            "android",
            "emulator"
        )

        return try {
            val telephonyManager = context.getSystemService(Context.TELEPHONY_SERVICE)
            val operatorName = (telephonyManager as? android.telephony.TelephonyManager)
                ?.networkOperatorName?.lowercase() ?: ""
            suspiciousOperators.any { operatorName.contains(it) }
        } catch (e: Exception) {
            false
        }
    }

    // ==================== Instrumentation Detection ====================

    private fun checkInstrumentationStatus(reasons: MutableList<String>): Boolean {
        var hasInstrumentation = false

        if (checkXposedPresent()) {
            reasons.add("Xposed framework detected")
            hasInstrumentation = true
        }

        if (checkFridaPresent()) {
            reasons.add("Frida instrumentation detected")
            hasInstrumentation = true
        }

        if (checkSubstratePresent()) {
            reasons.add("Cydia Substrate detected")
            hasInstrumentation = true
        }

        return hasInstrumentation
    }

    /**
     * Checks for Xposed framework presence.
     */
    private fun checkXposedPresent(): Boolean {
        // Check for Xposed-related stack traces
        val stackTrace = Thread.currentThread().stackTrace
        val xposedClasses = listOf(
            "de.robv.android.xposed.XposedBridge",
            "de.robv.android.xposed.XposedHelpers"
        )

        val hasXposedInStack = stackTrace.any { element ->
            xposedClasses.any { xposedClass ->
                element.className.contains(xposedClass)
            }
        }

        // Check for Xposed installer packages
        val xposedPackages = listOf(
            "de.robv.android.xposed.installer",
            "org.meowcat.edxposed.manager",
            "org.lsposed.manager"
        )

        val packageManager = context.packageManager
        val hasXposedPackage = xposedPackages.any { packageName ->
            try {
                @Suppress("DEPRECATION")
                packageManager.getPackageInfo(packageName, 0)
                true
            } catch (e: PackageManager.NameNotFoundException) {
                false
            }
        }

        return hasXposedInStack || hasXposedPackage
    }

    /**
     * Checks for Frida instrumentation framework.
     */
    private fun checkFridaPresent(): Boolean {
        // Check for Frida server binary
        val fridaPaths = listOf(
            "/data/local/tmp/frida-server",
            "/data/local/tmp/re.frida.server",
            "/sdcard/frida-server"
        )

        val hasFridaFile = fridaPaths.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }

        // Check for Frida libraries in maps
        val hasFridaLib = try {
            val maps = File("/proc/self/maps").readText()
            maps.contains("frida") || maps.contains("linjector")
        } catch (e: Exception) {
            false
        }

        // Check for default Frida port
        val hasFridaPort = try {
            val tcpFile = File("/proc/net/tcp")
            if (tcpFile.exists()) {
                val content = tcpFile.readText()
                // Default Frida port 27042 in hex is 69B2
                content.contains("69B2")
            } else {
                false
            }
        } catch (e: Exception) {
            false
        }

        return hasFridaFile || hasFridaLib || hasFridaPort
    }

    /**
     * Checks for Cydia Substrate presence.
     */
    private fun checkSubstratePresent(): Boolean {
        val substratePaths = listOf(
            "/data/data/com.saurik.substrate",
            "/system/lib/libsubstrate.so",
            "/system/lib64/libsubstrate.so"
        )

        return substratePaths.any { path ->
            try {
                File(path).exists()
            } catch (e: Exception) {
                false
            }
        }
    }

    // ==================== Debug Detection ====================

    private fun checkDebuggableStatus(reasons: MutableList<String>): Boolean {
        var isDebuggable = false

        if (checkAdbEnabled()) {
            reasons.add("ADB debugging enabled")
            isDebuggable = true
        }

        if (checkDeveloperOptions()) {
            reasons.add("Developer options enabled")
            isDebuggable = true
        }

        return isDebuggable
    }

    /**
     * Checks if ADB debugging is enabled.
     */
    private fun checkAdbEnabled(): Boolean {
        return try {
            Settings.Global.getInt(context.contentResolver, Settings.Global.ADB_ENABLED, 0) == 1
        } catch (e: Exception) {
            false
        }
    }

    /**
     * Checks if developer options are enabled.
     */
    private fun checkDeveloperOptions(): Boolean {
        return try {
            Settings.Global.getInt(
                context.contentResolver,
                Settings.Global.DEVELOPMENT_SETTINGS_ENABLED,
                0
            ) == 1
        } catch (e: Exception) {
            false
        }
    }
}
