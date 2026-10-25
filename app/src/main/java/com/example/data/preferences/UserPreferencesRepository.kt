package com.example.data.preferences

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "user_preferences")

enum class ThemeMode {
    SYSTEM,
    LIGHT,
    DARK
}

enum class ChatLanguage(val displayName: String, val code: String) {
    AUTO("Auto / উভয় ভাষা", "auto"),
    BANGLA("বাংলা (Bengali)", "bn"),
    ENGLISH("English", "en")
}

class UserPreferencesRepository(private val context: Context) {

    private object PreferencesKeys {
        val THEME_MODE = stringPreferencesKey("theme_mode")
        val CHAT_LANGUAGE = stringPreferencesKey("chat_language")
        val CUSTOM_API_KEY = stringPreferencesKey("custom_api_key")
    }

    val themeModeFlow: Flow<ThemeMode> = context.dataStore.data.map { preferences ->
        val modeStr = preferences[PreferencesKeys.THEME_MODE] ?: ThemeMode.SYSTEM.name
        try {
            ThemeMode.valueOf(modeStr)
        } catch (e: Exception) {
            ThemeMode.SYSTEM
        }
    }

    val chatLanguageFlow: Flow<ChatLanguage> = context.dataStore.data.map { preferences ->
        val langStr = preferences[PreferencesKeys.CHAT_LANGUAGE] ?: ChatLanguage.AUTO.name
        try {
            ChatLanguage.valueOf(langStr)
        } catch (e: Exception) {
            ChatLanguage.AUTO
        }
    }

    val customApiKeyFlow: Flow<String> = context.dataStore.data.map { preferences ->
        preferences[PreferencesKeys.CUSTOM_API_KEY] ?: ""
    }

    suspend fun setThemeMode(mode: ThemeMode) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.THEME_MODE] = mode.name
        }
    }

    suspend fun setChatLanguage(language: ChatLanguage) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CHAT_LANGUAGE] = language.name
        }
    }

    suspend fun setCustomApiKey(key: String) {
        context.dataStore.edit { preferences ->
            preferences[PreferencesKeys.CUSTOM_API_KEY] = key.trim()
        }
    }
}
