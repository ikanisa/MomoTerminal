package com.momoterminal.core.osintegration.shortcuts

import android.content.Context
import android.content.Intent
import androidx.core.content.pm.ShortcutInfoCompat
import androidx.core.content.pm.ShortcutManagerCompat
import androidx.core.graphics.drawable.IconCompat
import dagger.hilt.android.qualifiers.ApplicationContext
import javax.inject.Inject
import javax.inject.Singleton

// Generic shortcut model
data class AppShortcut(
    val id: String,
    val shortLabel: String,
    val longLabel: String,
    val iconResId: Int,
    val deepLink: String,
    val rank: Int = 0
)

@Singleton
class AppShortcutManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    fun updateShortcuts(shortcuts: List<AppShortcut>) {
        val shortcutInfos = shortcuts.map { shortcut ->
            val intent = Intent(Intent.ACTION_VIEW).apply {
                data = android.net.Uri.parse(shortcut.deepLink)
                setPackage(context.packageName)
            }
            
            ShortcutInfoCompat.Builder(context, shortcut.id)
                .setShortLabel(shortcut.shortLabel)
                .setLongLabel(shortcut.longLabel)
                .setIcon(IconCompat.createWithResource(context, shortcut.iconResId))
                .setIntent(intent)
                .setRank(shortcut.rank)
                .build()
        }
        
        ShortcutManagerCompat.setDynamicShortcuts(context, shortcutInfos)
    }
    
    fun addShortcut(shortcut: AppShortcut) {
        val intent = Intent(Intent.ACTION_VIEW).apply {
            data = android.net.Uri.parse(shortcut.deepLink)
            setPackage(context.packageName)
        }
        
        val shortcutInfo = ShortcutInfoCompat.Builder(context, shortcut.id)
            .setShortLabel(shortcut.shortLabel)
            .setLongLabel(shortcut.longLabel)
            .setIcon(IconCompat.createWithResource(context, shortcut.iconResId))
            .setIntent(intent)
            .build()
        
        ShortcutManagerCompat.pushDynamicShortcut(context, shortcutInfo)
    }
    
    fun removeShortcut(shortcutId: String) {
        ShortcutManagerCompat.removeDynamicShortcuts(context, listOf(shortcutId))
    }
    
    fun removeAllShortcuts() {
        ShortcutManagerCompat.removeAllDynamicShortcuts(context)
    }
}

// Generic shortcut provider
interface ShortcutProvider {
    suspend fun getShortcuts(): List<AppShortcut>
}

// Example: Recent items shortcut provider
class RecentItemsShortcutProvider @Inject constructor(
    private val repository: RecentItemsRepository
) : ShortcutProvider {
    
    override suspend fun getShortcuts(): List<AppShortcut> {
        return repository.getRecentItems(limit = 3).mapIndexed { index, item ->
            AppShortcut(
                id = "recent_${item.id}",
                shortLabel = item.title,
                longLabel = "Open ${item.title}",
                iconResId = android.R.drawable.ic_menu_recent_history,
                deepLink = "app://feature/item/${item.id}",
                rank = index
            )
        }
    }
}

// Shortcut update worker
class ShortcutUpdateWorker @Inject constructor(
    private val shortcutManager: AppShortcutManager,
    private val providers: Set<@JvmSuppressWildcards ShortcutProvider>
) {
    
    suspend fun updateShortcuts() {
        val allShortcuts = providers.flatMap { it.getShortcuts() }
        shortcutManager.updateShortcuts(allShortcuts.take(4)) // Max 4 dynamic shortcuts
    }
}

// Static shortcuts (defined in XML)
/*
res/xml/shortcuts.xml:

<?xml version="1.0" encoding="utf-8"?>
<shortcuts xmlns:android="http://schemas.android.com/apk/res/android">
    <shortcut
        android:shortcutId="feature_a"
        android:enabled="true"
        android:icon="@drawable/ic_feature_a"
        android:shortcutShortLabel="@string/feature_a_short"
        android:shortcutLongLabel="@string/feature_a_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:data="app://featureA" />
        <categories android:name="android.shortcut.conversation" />
    </shortcut>
    
    <shortcut
        android:shortcutId="feature_b"
        android:enabled="true"
        android:icon="@drawable/ic_feature_b"
        android:shortcutShortLabel="@string/feature_b_short"
        android:shortcutLongLabel="@string/feature_b_long">
        <intent
            android:action="android.intent.action.VIEW"
            android:data="app://featureB" />
    </shortcut>
</shortcuts>

AndroidManifest.xml:
<activity android:name=".MainActivity">
    <meta-data
        android:name="android.app.shortcuts"
        android:resource="@xml/shortcuts" />
</activity>
*/

// Usage in app initialization
class ShortcutInitializer @Inject constructor(
    private val shortcutUpdateWorker: ShortcutUpdateWorker
) {
    suspend fun initialize() {
        shortcutUpdateWorker.updateShortcuts()
    }
}
