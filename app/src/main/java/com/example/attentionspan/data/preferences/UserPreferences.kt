package com.example.attentionspan.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class UserPreferences(private val context: Context) {

    companion object {
        val PERIOD_DAYS_KEY = intPreferencesKey("period_days")
    }

    val periodDays: Flow<Int> = context.dataStore.data.map { preferences ->
        preferences[PERIOD_DAYS_KEY] ?: 7 // Default to 7 days
    }

    suspend fun setPeriodDays(days: Int) {
        context.dataStore.edit { preferences ->
            preferences[PERIOD_DAYS_KEY] = days
        }
    }
}
