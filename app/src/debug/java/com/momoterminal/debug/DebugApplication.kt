package com.momoterminal.debug

import android.app.Application
import android.content.ContentProvider
import android.content.ContentValues
import android.database.Cursor
import android.net.Uri
import com.momoterminal.BuildConfig
import timber.log.Timber

/**
 * Debug tools initialization for MomoTerminal.
 * 
 * This class provides debug-only functionality:
 * - LeakCanary for memory leak detection
 * - StrictMode for detecting policy violations
 * 
 * Note: These tools are initialized via a ContentProvider that runs
 * before Application.onCreate(), allowing early detection of issues.
 * This approach avoids needing to extend MomoTerminalApp which uses @HiltAndroidApp.
 */
class DebugToolsInitializer : ContentProvider() {

    override fun onCreate(): Boolean {
        val context = context ?: return false
        
        // Initialize StrictMode early to catch violations in Application.onCreate()
        if (BuildConfig.STRICT_MODE_ENABLED) {
            StrictModeConfig.enableStrictMode()
            Timber.d("StrictMode: ENABLED")
        }
        
        // Configure LeakCanary
        if (BuildConfig.LEAK_CANARY_ENABLED) {
            val application = context.applicationContext as? Application
            application?.let {
                LeakCanaryConfig.configure(it)
                Timber.d("LeakCanary: ENABLED")
            }
        }
        
        Timber.d("Debug tools initialized")
        return true
    }

    override fun query(
        uri: Uri,
        projection: Array<out String>?,
        selection: String?,
        selectionArgs: Array<out String>?,
        sortOrder: String?
    ): Cursor? = null

    override fun getType(uri: Uri): String? = null

    override fun insert(uri: Uri, values: ContentValues?): Uri? = null

    override fun delete(uri: Uri, selection: String?, selectionArgs: Array<out String>?): Int = 0

    override fun update(
        uri: Uri,
        values: ContentValues?,
        selection: String?,
        selectionArgs: Array<out String>?
    ): Int = 0
}
