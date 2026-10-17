package com.soursweethealth.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.*
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import kotlinx.coroutines.flow.first

private val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "settings")

class SettingsManager(private val context: Context) {
    private val apiKeyCipher = ApiKeyCipher()

    companion object {
        val CURRENT_USER_ID = longPreferencesKey("current_user_id")
        val API_URL = stringPreferencesKey("api_url")
        val API_KEY = stringPreferencesKey("api_key")
        val MODEL_NAME = stringPreferencesKey("model_name")
        val AI_PRIVACY_ACCEPTED = booleanPreferencesKey("ai_privacy_accepted")
    }

    val currentUserId: Flow<Long> = context.dataStore.data.map { it[CURRENT_USER_ID] ?: -1L }
    val apiUrl: Flow<String> = context.dataStore.data.map { it[API_URL] ?: "https://api.siliconflow.cn/v1/chat/completions" }
    val apiKey: Flow<String> = context.dataStore.data.map { apiKeyCipher.decrypt(it[API_KEY] ?: "") }
    val modelName: Flow<String> = context.dataStore.data.map { it[MODEL_NAME] ?: "Pro/zai-org/GLM-4.7" }
    val aiPrivacyAccepted: Flow<Boolean> = context.dataStore.data.map { it[AI_PRIVACY_ACCEPTED] ?: false }

    suspend fun setCurrentUserId(id: Long) {
        context.dataStore.edit { it[CURRENT_USER_ID] = id }
    }

    suspend fun setApiConfig(url: String, key: String, model: String) {
        context.dataStore.edit {
            it[API_URL] = url
            it[API_KEY] = apiKeyCipher.encrypt(key.trim())
            it[MODEL_NAME] = model
            it[AI_PRIVACY_ACCEPTED] = false
        }
    }

    suspend fun setAiPrivacyAccepted(accepted: Boolean) {
        context.dataStore.edit { it[AI_PRIVACY_ACCEPTED] = accepted }
    }

    suspend fun migrateLegacyApiKey() {
        val stored = context.dataStore.data.first()[API_KEY].orEmpty()
        if (stored.isNotBlank() && !apiKeyCipher.isEncrypted(stored)) {
            context.dataStore.edit { it[API_KEY] = apiKeyCipher.encrypt(stored) }
        }
    }
}
