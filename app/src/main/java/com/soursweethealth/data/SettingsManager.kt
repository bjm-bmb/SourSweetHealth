package com.soursweethealth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    companion object {
        val CURRENT_USER_ID = longPreferencesKey("current_user_id")
        val API_URL = stringPreferencesKey("api_url")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_NAME = stringPreferencesKey("model_name")
    }

    val currentUserId: Flow<Long> = context.dataStore.data.map { it[CURRENT_USER_ID] ?: -1L }
    val apiUrl: Flow<String> = context.dataStore.data.map { it[API_URL] ?: "https://api.siliconflow.cn/v1/chat/completions" }
    val apiKey: Flow<String> = context.dataStore.data.map { it[API_KEY] ?: "" }
    val modelName: Flow<String> = context.dataStore.data.map { it[MODEL_NAME] ?: "Pro/zai-org/GLM-4.7" }

    suspend fun setCurrentUserId(id: Long) {
        context.dataStore.edit { it[CURRENT_USER_ID] = id }
    }

    suspend fun setApiConfig(url: String, key: String, model: String) {
        context.dataStore.edit {
            it[API_URL] = url
            it[API_KEY] = key
            it[MODEL_NAME] = model
        }
    }
}
