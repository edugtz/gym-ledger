package com.edu.gymledger.data.repository

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.settingsDataStore: DataStore<Preferences> by preferencesDataStore(
    name = "gym_ledger_settings"
)

class SettingsRepository(private val context: Context) {

    private object Keys {
        val ONLINE_FOOD_LOOKUP_ENABLED = booleanPreferencesKey("online_food_lookup_enabled")
        val FOOD_LOOKUP_ENDPOINT = stringPreferencesKey("food_lookup_endpoint")
        val FOOD_LOOKUP_API_KEY = stringPreferencesKey("food_lookup_api_key")
        val USDA_ENABLED = booleanPreferencesKey("usda_enabled")
        val OPEN_FOOD_FACTS_ENABLED = booleanPreferencesKey("open_food_facts_enabled")
        val SAFE_MODE_ENABLED = booleanPreferencesKey("safe_mode_enabled")
    }

    val onlineAssistanceSettings: Flow<OnlineAssistanceSettings> =
        context.settingsDataStore.data.map { prefs ->
            OnlineAssistanceSettings(
                onlineFoodLookupEnabled = prefs[Keys.ONLINE_FOOD_LOOKUP_ENABLED] ?: false,
                foodLookupEndpoint = prefs[Keys.FOOD_LOOKUP_ENDPOINT] ?: "",
                foodLookupApiKey = prefs[Keys.FOOD_LOOKUP_API_KEY] ?: "",
                usdaEnabled = prefs[Keys.USDA_ENABLED] ?: true,
                openFoodFactsEnabled = prefs[Keys.OPEN_FOOD_FACTS_ENABLED] ?: true,
                safeModeEnabled = prefs[Keys.SAFE_MODE_ENABLED] ?: true
            )
        }

    suspend fun updateOnlineFoodLookupEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.ONLINE_FOOD_LOOKUP_ENABLED] = enabled
        }
    }

    suspend fun updateFoodLookupEndpoint(endpoint: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FOOD_LOOKUP_ENDPOINT] = endpoint
        }
    }

    suspend fun updateFoodLookupApiKey(apiKey: String) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.FOOD_LOOKUP_API_KEY] = apiKey
        }
    }

    suspend fun updateUsdaEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.USDA_ENABLED] = enabled
        }
    }

    suspend fun updateOpenFoodFactsEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.OPEN_FOOD_FACTS_ENABLED] = enabled
        }
    }

    suspend fun updateSafeModeEnabled(enabled: Boolean) {
        context.settingsDataStore.edit { prefs ->
            prefs[Keys.SAFE_MODE_ENABLED] = enabled
        }
    }
}
