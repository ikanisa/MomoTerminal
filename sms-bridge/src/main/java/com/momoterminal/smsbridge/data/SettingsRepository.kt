package com.momoterminal.smsbridge.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import dagger.hilt.android.qualifiers.ApplicationContext
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "sms_bridge_settings")

@Singleton
class SettingsRepository @Inject constructor(
    @ApplicationContext private val context: Context
) {
    private val WEBHOOK_URL_KEY = stringPreferencesKey("webhook_url")
    private val SECRET_TOKEN_KEY = stringPreferencesKey("secret_token")
    private val DEVICE_NAME_KEY = stringPreferencesKey("device_name")
    private val FORWARDING_ENABLED_KEY = booleanPreferencesKey("forwarding_enabled")

    val webhookUrl: Flow<String> = context.dataStore.data.map { it[WEBHOOK_URL_KEY] ?: "" }
    val secretToken: Flow<String> = context.dataStore.data.map { it[SECRET_TOKEN_KEY] ?: "" }
    val deviceName: Flow<String> = context.dataStore.data.map { it[DEVICE_NAME_KEY] ?: "My Gateway Phone" }
    val isForwardingEnabled: Flow<Boolean> = context.dataStore.data.map { it[FORWARDING_ENABLED_KEY] ?: false }

    suspend fun saveSettings(url: String, secret: String, name: String) {
        context.dataStore.edit { preferences ->
            preferences[WEBHOOK_URL_KEY] = url
            preferences[SECRET_TOKEN_KEY] = secret
            preferences[DEVICE_NAME_KEY] = name
        }
    }

    suspend fun setForwardingEnabled(enabled: Boolean) {
        context.dataStore.edit { preferences ->
            preferences[FORWARDING_ENABLED_KEY] = enabled
        }
    }
}
