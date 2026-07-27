package com.example.data

import android.content.Context
import androidx.datastore.core.DataStore
import androidx.datastore.preferences.core.Preferences
import androidx.datastore.preferences.core.booleanPreferencesKey
import androidx.datastore.preferences.core.edit
import androidx.datastore.preferences.core.intPreferencesKey
import androidx.datastore.preferences.core.stringPreferencesKey
import androidx.datastore.preferences.core.stringSetPreferencesKey
import androidx.datastore.preferences.preferencesDataStore
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.map

val Context.dataStore: DataStore<Preferences> by preferencesDataStore(name = "scrollbreak_preferences")

data class UserPreferences(
    val language: String = "fr",
    val themeKeyword: String = "Science",
    val usageThresholdMinutes: Int = 5,
    val monitoredPackages: Set<String> = setOf(
        "com.instagram.android",
        "com.zhiliaoapp.musically",
        "com.twitter.android",
        "com.google.android.youtube",
        "com.reddit.frontpage",
        "com.facebook.katana"
    ),
    val serviceEnabled: Boolean = true,
    val overlayEnabled: Boolean = true,
    val interruptionStyle: String = "overlay", // "notification", "overlay", "immersive"
    val contentSource: String = "all", // "wikipedia", "wikinews", "wikiquote", "wikivoyage", "wikibooks", "devto", "all"
    val articlesReadCount: Int = 0,
    val minutesSavedCount: Int = 0
)

class UserPreferencesRepository(private val context: Context) {

    private object Keys {
        val LANGUAGE = stringPreferencesKey("language")
        val THEME_KEYWORD = stringPreferencesKey("theme_keyword")
        val THRESHOLD_MINUTES = intPreferencesKey("threshold_minutes")
        val MONITORED_PACKAGES = stringSetPreferencesKey("monitored_packages")
        val SERVICE_ENABLED = booleanPreferencesKey("service_enabled")
        val OVERLAY_ENABLED = booleanPreferencesKey("overlay_enabled")
        val INTERRUPTION_STYLE = stringPreferencesKey("interruption_style")
        val CONTENT_SOURCE = stringPreferencesKey("content_source")
        val ARTICLES_READ = intPreferencesKey("articles_read")
        val MINUTES_SAVED = intPreferencesKey("minutes_saved")
    }

    val userPreferencesFlow: Flow<UserPreferences> = context.dataStore.data.map { prefs ->
        val overlayPref = prefs[Keys.OVERLAY_ENABLED] ?: true
        val defaultStyle = if (!overlayPref) "notification" else "overlay"
        UserPreferences(
            language = prefs[Keys.LANGUAGE] ?: "fr",
            themeKeyword = prefs[Keys.THEME_KEYWORD] ?: "Science",
            usageThresholdMinutes = prefs[Keys.THRESHOLD_MINUTES] ?: 5,
            monitoredPackages = prefs[Keys.MONITORED_PACKAGES] ?: setOf(
                "com.instagram.android",
                "com.zhiliaoapp.musically",
                "com.twitter.android",
                "com.google.android.youtube",
                "com.reddit.frontpage",
                "com.facebook.katana"
            ),
            serviceEnabled = prefs[Keys.SERVICE_ENABLED] ?: true,
            overlayEnabled = overlayPref,
            interruptionStyle = prefs[Keys.INTERRUPTION_STYLE] ?: defaultStyle,
            contentSource = prefs[Keys.CONTENT_SOURCE] ?: "all",
            articlesReadCount = prefs[Keys.ARTICLES_READ] ?: 0,
            minutesSavedCount = prefs[Keys.MINUTES_SAVED] ?: 0
        )
    }

    suspend fun updateLanguage(language: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.LANGUAGE] = language.lowercase()
        }
    }

    suspend fun updateThemeKeyword(theme: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THEME_KEYWORD] = theme
        }
    }

    suspend fun updateThresholdMinutes(minutes: Int) {
        context.dataStore.edit { prefs ->
            prefs[Keys.THRESHOLD_MINUTES] = minutes
        }
    }

    suspend fun updateMonitoredPackages(packages: Set<String>) {
        context.dataStore.edit { prefs ->
            prefs[Keys.MONITORED_PACKAGES] = packages
        }
    }

    suspend fun toggleMonitoredPackage(packageName: String) {
        context.dataStore.edit { prefs ->
            val current = prefs[Keys.MONITORED_PACKAGES] ?: setOf()
            val updated = if (current.contains(packageName)) {
                current - packageName
            } else {
                current + packageName
            }
            prefs[Keys.MONITORED_PACKAGES] = updated
        }
    }

    suspend fun updateServiceEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.SERVICE_ENABLED] = enabled
        }
    }

    suspend fun updateOverlayEnabled(enabled: Boolean) {
        context.dataStore.edit { prefs ->
            prefs[Keys.OVERLAY_ENABLED] = enabled
        }
    }

    suspend fun updateInterruptionStyle(style: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.INTERRUPTION_STYLE] = style
            prefs[Keys.OVERLAY_ENABLED] = (style == "overlay")
        }
    }

    suspend fun updateContentSource(source: String) {
        context.dataStore.edit { prefs ->
            prefs[Keys.CONTENT_SOURCE] = source
        }
    }

    suspend fun incrementArticleReadStats(minutesSaved: Int = 5) {
        context.dataStore.edit { prefs ->
            val currentArticles = prefs[Keys.ARTICLES_READ] ?: 0
            val currentMinutes = prefs[Keys.MINUTES_SAVED] ?: 0
            prefs[Keys.ARTICLES_READ] = currentArticles + 1
            prefs[Keys.MINUTES_SAVED] = currentMinutes + minutesSaved
        }
    }
}
