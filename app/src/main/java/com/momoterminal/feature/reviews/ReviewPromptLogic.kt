package com.momoterminal.feature.reviews

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.longPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.flow.map
import timber.log.Timber
import java.util.concurrent.TimeUnit
import javax.inject.Inject
import javax.inject.Singleton

/**
 * Extension property to create DataStore instance for review preferences.
 */
private val Context.reviewDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "review_preferences"
)

/**
 * Manager class for handling review prompt logic and timing.
 *
 * Criteria for showing review prompt:
 * - App used for 7+ days
 * - 10+ successful transactions
 * - Not shown in last 30 days
 * - Max 3 prompts total
 */
@Singleton
class ReviewPromptManager @Inject constructor(
    @ApplicationContext private val context: Context
) {
    
    private val dataStore = context.reviewDataStore
    
    companion object {
        // Preference keys
        private val KEY_FIRST_LAUNCH_TIME = longPreferencesKey("first_launch_time")
        private val KEY_SUCCESSFUL_TRANSACTIONS = intPreferencesKey("successful_transactions")
        private val KEY_LAST_PROMPT_TIME = longPreferencesKey("last_prompt_time")
        private val KEY_TOTAL_PROMPTS = intPreferencesKey("total_prompts")
        
        // Thresholds
        private const val MIN_DAYS_SINCE_INSTALL = 7
        private const val MIN_SUCCESSFUL_TRANSACTIONS = 10
        private const val MIN_DAYS_BETWEEN_PROMPTS = 30
        private const val MAX_TOTAL_PROMPTS = 3
    }
    
    /**
     * Check if the review prompt should be shown based on all criteria.
     *
     * @return true if all conditions are met to show the review prompt
     */
    suspend fun shouldShowReviewPrompt(): Boolean {
        val preferences = dataStore.data.first()
        val now = System.currentTimeMillis()
        
        // Check first launch time
        val firstLaunchTime = preferences[KEY_FIRST_LAUNCH_TIME] ?: run {
            // First time - set the launch time and return false
            recordFirstLaunch()
            return false
        }
        
        val daysSinceInstall = TimeUnit.MILLISECONDS.toDays(now - firstLaunchTime)
        if (daysSinceInstall < MIN_DAYS_SINCE_INSTALL) {
            Timber.d("Review prompt: Not enough days since install ($daysSinceInstall < $MIN_DAYS_SINCE_INSTALL)")
            return false
        }
        
        // Check successful transactions
        val successfulTransactions = preferences[KEY_SUCCESSFUL_TRANSACTIONS] ?: 0
        if (successfulTransactions < MIN_SUCCESSFUL_TRANSACTIONS) {
            Timber.d("Review prompt: Not enough transactions ($successfulTransactions < $MIN_SUCCESSFUL_TRANSACTIONS)")
            return false
        }
        
        // Check last prompt time
        val lastPromptTime = preferences[KEY_LAST_PROMPT_TIME] ?: 0L
        val daysSinceLastPrompt = TimeUnit.MILLISECONDS.toDays(now - lastPromptTime)
        if (lastPromptTime > 0 && daysSinceLastPrompt < MIN_DAYS_BETWEEN_PROMPTS) {
            Timber.d("Review prompt: Not enough days since last prompt ($daysSinceLastPrompt < $MIN_DAYS_BETWEEN_PROMPTS)")
            return false
        }
        
        // Check total prompts
        val totalPrompts = preferences[KEY_TOTAL_PROMPTS] ?: 0
        if (totalPrompts >= MAX_TOTAL_PROMPTS) {
            Timber.d("Review prompt: Max prompts reached ($totalPrompts >= $MAX_TOTAL_PROMPTS)")
            return false
        }
        
        Timber.d("Review prompt: All conditions met, should show prompt")
        return true
    }
    
    /**
     * Record that the review prompt was shown.
     * Updates the last prompt time and increments the total prompts counter.
     */
    suspend fun recordReviewPromptShown() {
        dataStore.edit { preferences ->
            preferences[KEY_LAST_PROMPT_TIME] = System.currentTimeMillis()
            preferences[KEY_TOTAL_PROMPTS] = (preferences[KEY_TOTAL_PROMPTS] ?: 0) + 1
        }
        Timber.d("Review prompt shown recorded")
    }
    
    /**
     * Increment the successful transactions counter.
     * Call this after each successful transaction.
     */
    suspend fun recordSuccessfulTransaction() {
        dataStore.edit { preferences ->
            preferences[KEY_SUCCESSFUL_TRANSACTIONS] = 
                (preferences[KEY_SUCCESSFUL_TRANSACTIONS] ?: 0) + 1
        }
    }
    
    /**
     * Record the first launch time if not already set.
     */
    private suspend fun recordFirstLaunch() {
        dataStore.edit { preferences ->
            if (preferences[KEY_FIRST_LAUNCH_TIME] == null) {
                preferences[KEY_FIRST_LAUNCH_TIME] = System.currentTimeMillis()
                Timber.d("First launch time recorded")
            }
        }
    }
    
    /**
     * Initialize the manager - call on app startup.
     */
    suspend fun initialize() {
        recordFirstLaunch()
    }
    
    /**
     * Get the current review statistics for debugging.
     */
    suspend fun getReviewStats(): ReviewStats {
        val preferences = dataStore.data.first()
        val now = System.currentTimeMillis()
        
        val firstLaunchTime = preferences[KEY_FIRST_LAUNCH_TIME] ?: now
        
        return ReviewStats(
            daysSinceInstall = TimeUnit.MILLISECONDS.toDays(now - firstLaunchTime).toInt(),
            successfulTransactions = preferences[KEY_SUCCESSFUL_TRANSACTIONS] ?: 0,
            daysSinceLastPrompt = preferences[KEY_LAST_PROMPT_TIME]?.let {
                TimeUnit.MILLISECONDS.toDays(now - it).toInt()
            },
            totalPrompts = preferences[KEY_TOTAL_PROMPTS] ?: 0
        )
    }
    
    /**
     * Reset all review tracking data.
     * Useful for testing or when the user reinstalls.
     */
    suspend fun reset() {
        dataStore.edit { preferences ->
            preferences.clear()
        }
        Timber.d("Review tracking data reset")
    }
}

/**
 * Data class containing review statistics.
 */
data class ReviewStats(
    val daysSinceInstall: Int,
    val successfulTransactions: Int,
    val daysSinceLastPrompt: Int?,
    val totalPrompts: Int
) {
    val canShowPrompt: Boolean
        get() = daysSinceInstall >= 7 &&
                successfulTransactions >= 10 &&
                (daysSinceLastPrompt == null || daysSinceLastPrompt >= 30) &&
                totalPrompts < 3
}
