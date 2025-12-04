package com.momoterminal.core.osintegration.widgets

import android.app.PendingIntent
import android.appwidget.AppWidgetManager
import android.appwidget.AppWidgetProvider
import android.content.Context
import android.content.Intent
import android.widget.RemoteViews
import androidx.glance.*
import androidx.glance.appwidget.*
import androidx.glance.action.clickable
import androidx.glance.layout.*
import androidx.glance.text.Text

// 1. Quick Actions Widget (Glance-based)
class QuickActionsWidget : GlanceAppWidget() {
    
    override suspend fun provideGlance(context: Context, id: GlanceId) {
        provideContent {
            QuickActionsContent()
        }
    }
    
    @Composable
    private fun QuickActionsContent() {
        Column(
            modifier = GlanceModifier
                .fillMaxSize()
                .padding(16.dp)
                .background(GlanceTheme.colors.background)
        ) {
            Text(
                text = "Quick Actions",
                style = TextStyle(
                    fontSize = 18.sp,
                    fontWeight = FontWeight.Bold
                )
            )
            
            Spacer(modifier = GlanceModifier.height(8.dp))
            
            // Generic action buttons
            QuickActionButton(
                text = "Feature A",
                deepLink = "app://featureA"
            )
            
            QuickActionButton(
                text = "Feature B",
                deepLink = "app://featureB"
            )
            
            QuickActionButton(
                text = "Settings",
                deepLink = "app://settings"
            )
        }
    }
    
    @Composable
    private fun QuickActionButton(text: String, deepLink: String) {
        Box(
            modifier = GlanceModifier
                .fillMaxWidth()
                .padding(vertical = 4.dp)
                .background(GlanceTheme.colors.primary)
                .cornerRadius(8.dp)
                .clickable(actionStartActivity(deepLink))
                .padding(12.dp)
        ) {
            Text(
                text = text,
                style = TextStyle(color = GlanceTheme.colors.onPrimary)
            )
        }
    }
}

class QuickActionsWidgetReceiver : GlanceAppWidgetReceiver() {
    override val glanceAppWidget: GlanceAppWidget = QuickActionsWidget()
}

// 2. Recent Items Widget (Traditional RemoteViews)
class RecentItemsWidget : AppWidgetProvider() {
    
    override fun onUpdate(
        context: Context,
        appWidgetManager: AppWidgetManager,
        appWidgetIds: IntArray
    ) {
        appWidgetIds.forEach { widgetId ->
            val views = RemoteViews(context.packageName, R.layout.widget_recent_items)
            
            // Set up list view with RemoteViewsService
            val intent = Intent(context, RecentItemsRemoteViewsService::class.java)
            views.setRemoteAdapter(R.id.list_view, intent)
            
            // Set up click intent template
            val clickIntent = Intent(context, MainActivity::class.java)
            val clickPendingIntent = PendingIntent.getActivity(
                context,
                0,
                clickIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
            views.setPendingIntentTemplate(R.id.list_view, clickPendingIntent)
            
            appWidgetManager.updateAppWidget(widgetId, views)
        }
    }
}

// RemoteViewsService for list data
class RecentItemsRemoteViewsService : RemoteViewsService() {
    override fun onGetViewFactory(intent: Intent): RemoteViewsFactory {
        return RecentItemsRemoteViewsFactory(applicationContext)
    }
}

class RecentItemsRemoteViewsFactory(
    private val context: Context
) : RemoteViewsService.RemoteViewsFactory {
    
    private var items: List<RecentItem> = emptyList()
    
    override fun onCreate() {
        // Initialize
    }
    
    override fun onDataSetChanged() {
        // Fetch recent items from repository
        // This runs on the main thread, so use runBlocking or cached data
        items = fetchRecentItems()
    }
    
    override fun getCount(): Int = items.size
    
    override fun getViewAt(position: Int): RemoteViews {
        val item = items[position]
        val views = RemoteViews(context.packageName, R.layout.widget_recent_item)
        
        views.setTextViewText(R.id.item_title, item.title)
        views.setTextViewText(R.id.item_subtitle, item.subtitle)
        
        // Set fill-in intent for click
        val fillInIntent = Intent().apply {
            data = android.net.Uri.parse("app://feature/item/${item.id}")
        }
        views.setOnClickFillInIntent(R.id.item_container, fillInIntent)
        
        return views
    }
    
    override fun getLoadingView(): RemoteViews? = null
    override fun getViewTypeCount(): Int = 1
    override fun getItemId(position: Int): Long = position.toLong()
    override fun hasStableIds(): Boolean = true
    override fun onDestroy() {}
    
    private fun fetchRecentItems(): List<RecentItem> {
        // Fetch from repository or cache
        return emptyList()
    }
}

// Generic data model for widgets
data class RecentItem(
    val id: String,
    val title: String,
    val subtitle: String
)

// Widget data provider interface
interface WidgetDataProvider {
    suspend fun getRecentItems(limit: Int): List<RecentItem>
    suspend fun getQuickActions(): List<QuickAction>
}

data class QuickAction(
    val id: String,
    val label: String,
    val deepLink: String
)

// Widget update manager
class WidgetUpdateManager @Inject constructor(
    @ApplicationContext private val context: Context,
    private val dataProvider: WidgetDataProvider
) {
    
    fun updateAllWidgets() {
        // Update Quick Actions Widget
        updateQuickActionsWidget()
        
        // Update Recent Items Widget
        updateRecentItemsWidget()
    }
    
    private fun updateQuickActionsWidget() {
        val glanceId = GlanceAppWidgetManager(context)
            .getGlanceIds(QuickActionsWidget::class.java)
        
        glanceId.forEach { id ->
            QuickActionsWidget().update(context, id)
        }
    }
    
    private fun updateRecentItemsWidget() {
        val appWidgetManager = AppWidgetManager.getInstance(context)
        val widgetIds = appWidgetManager.getAppWidgetIds(
            ComponentName(context, RecentItemsWidget::class.java)
        )
        
        appWidgetManager.notifyAppWidgetViewDataChanged(widgetIds, R.id.list_view)
    }
}

// Widget layouts (XML)
/*
res/layout/widget_recent_items.xml:
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:layout_width="match_parent"
    android:layout_height="match_parent"
    android:orientation="vertical"
    android:padding="8dp">
    
    <TextView
        android:id="@+id/widget_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:text="Recent Items"
        android:textSize="16sp"
        android:textStyle="bold" />
    
    <ListView
        android:id="@+id/list_view"
        android:layout_width="match_parent"
        android:layout_height="match_parent" />
</LinearLayout>

res/layout/widget_recent_item.xml:
<?xml version="1.0" encoding="utf-8"?>
<LinearLayout xmlns:android="http://schemas.android.com/apk/res/android"
    android:id="@+id/item_container"
    android:layout_width="match_parent"
    android:layout_height="wrap_content"
    android:orientation="vertical"
    android:padding="8dp">
    
    <TextView
        android:id="@+id/item_title"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="14sp" />
    
    <TextView
        android:id="@+id/item_subtitle"
        android:layout_width="match_parent"
        android:layout_height="wrap_content"
        android:textSize="12sp"
        android:textColor="#666666" />
</LinearLayout>

res/xml/widget_info_quick_actions.xml:
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="180dp"
    android:minHeight="110dp"
    android:updatePeriodMillis="0"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />

res/xml/widget_info_recent_items.xml:
<?xml version="1.0" encoding="utf-8"?>
<appwidget-provider xmlns:android="http://schemas.android.com/apk/res/android"
    android:minWidth="250dp"
    android:minHeight="180dp"
    android:updatePeriodMillis="1800000"
    android:initialLayout="@layout/widget_recent_items"
    android:resizeMode="horizontal|vertical"
    android:widgetCategory="home_screen" />
*/
