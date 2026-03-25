package com.orbital.app.data

import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import com.orbital.app.domain.AppearanceSettings
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map
import javax.inject.Inject
import javax.inject.Singleton

@Singleton
class AppearanceStore @Inject constructor(private val dataStore: DataStore<Preferences>) {

    companion object {
        val KEY_THEME     = stringPreferencesKey("theme")
        val KEY_ACCENT    = stringPreferencesKey("accent")
        val KEY_FONT      = stringPreferencesKey("font")
        val KEY_FONT_SIZE = intPreferencesKey("font_size")
        val KEY_BUBBLE    = stringPreferencesKey("bubble")
        val KEY_DENSITY   = stringPreferencesKey("density")
    }

    val appearance: Flow<AppearanceSettings> = dataStore.data.map { prefs ->
        AppearanceSettings(
            themeName   = prefs[KEY_THEME]     ?: "dark",
            accentName  = prefs[KEY_ACCENT]    ?: "indigo",
            fontName    = prefs[KEY_FONT]      ?: "Syne / JetBrains",
            fontSize    = prefs[KEY_FONT_SIZE] ?: 11,
            bubbleStyle = prefs[KEY_BUBBLE]    ?: "rounded",
            density     = prefs[KEY_DENSITY]   ?: "normal"
        )
    }

    suspend fun save(settings: AppearanceSettings) {
        dataStore.edit { prefs ->
            prefs[KEY_THEME]     = settings.themeName
            prefs[KEY_ACCENT]    = settings.accentName
            prefs[KEY_FONT]      = settings.fontName
            prefs[KEY_FONT_SIZE] = settings.fontSize
            prefs[KEY_BUBBLE]    = settings.bubbleStyle
            prefs[KEY_DENSITY]   = settings.density
        }
    }
}
