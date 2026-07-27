package com.example.ui

import android.app.Application
import android.speech.tts.TextToSpeech
import androidx.lifecycle.AndroidViewModel
import androidx.lifecycle.viewModelScope
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import com.example.data.WikipediaRepository
import com.example.data.db.BookmarkedArticle
import com.example.data.db.ScrollBreakDatabase
import com.example.data.models.WikiSummaryResponse
import kotlinx.coroutines.flow.Flow
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.StateFlow
import kotlinx.coroutines.flow.asStateFlow
import kotlinx.coroutines.flow.collectLatest
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.launch
import java.util.Locale

sealed class NavigationScreen {
    object Dashboard : NavigationScreen()
    object BreakArticle : NavigationScreen()
    object SavedArticles : NavigationScreen()
    object Statistics : NavigationScreen()
}

class MainViewModel(application: Application) : AndroidViewModel(application), TextToSpeech.OnInitListener {

    private val prefsRepo = UserPreferencesRepository(application)
    private val wikiRepo = WikipediaRepository()
    private val articleDao = ScrollBreakDatabase.getDatabase(application).articleDao()

    val userPreferences: Flow<UserPreferences> = prefsRepo.userPreferencesFlow

    private val _currentScreen = MutableStateFlow<NavigationScreen>(NavigationScreen.Dashboard)
    val currentScreen: StateFlow<NavigationScreen> = _currentScreen.asStateFlow()

    private val _currentArticle = MutableStateFlow<WikiSummaryResponse?>(null)
    val currentArticle: StateFlow<WikiSummaryResponse?> = _currentArticle.asStateFlow()

    private val _isLoadingArticle = MutableStateFlow(false)
    val isLoadingArticle: StateFlow<Boolean> = _isLoadingArticle.asStateFlow()

    private val _articleErrorMessage = MutableStateFlow<String?>(null)
    val articleErrorMessage: StateFlow<String?> = _articleErrorMessage.asStateFlow()

    private val _isCurrentArticleBookmarked = MutableStateFlow(false)
    val isCurrentArticleBookmarked: StateFlow<Boolean> = _isCurrentArticleBookmarked.asStateFlow()

    val bookmarkedArticles = articleDao.getAllBookmarks()

    // Text to Speech
    private var tts: TextToSpeech? = null
    private val _isTtsSpeaking = MutableStateFlow(false)
    val isTtsSpeaking: StateFlow<Boolean> = _isTtsSpeaking.asStateFlow()
    private var isTtsInitialized = false

    init {
        tts = TextToSpeech(application, this)
        viewModelScope.launch {
            userPreferences.collectLatest { prefs ->
                // Keep TTS language updated when language preference changes
                updateTtsLocale(prefs.language)
            }
        }
    }

    override fun onInit(status: Int) {
        if (status == TextToSpeech.SUCCESS) {
            isTtsInitialized = true
        }
    }

    fun navigateTo(screen: NavigationScreen) {
        if (screen != NavigationScreen.BreakArticle) {
            stopTts()
        }
        _currentScreen.value = screen
    }

    fun setPreloadedArticle(summary: WikiSummaryResponse) {
        stopTts()
        _currentArticle.value = summary
        checkIsBookmarked(summary.title)
    }

    fun loadArticleByTitle(title: String, lang: String) {
        viewModelScope.launch {
            stopTts()
            _isLoadingArticle.value = true
            _articleErrorMessage.value = null

            val result = wikiRepo.fetchArticleByTitle(lang, title)
            result.onSuccess { summary ->
                _currentArticle.value = summary
                checkIsBookmarked(summary.title)
            }.onFailure { err ->
                _articleErrorMessage.value = err.message ?: "Could not load article"
            }
            _isLoadingArticle.value = false
        }
    }

    fun loadNewArticle(forcedLang: String? = null, forcedTopic: String? = null) {
        viewModelScope.launch {
            stopTts()
            _isLoadingArticle.value = true
            _articleErrorMessage.value = null

            val currentPrefs = userPreferences.first()

            val targetLang = forcedLang ?: currentPrefs.language
            val targetTopic = forcedTopic ?: currentPrefs.themeKeyword
            val targetSource = currentPrefs.contentSource

            val result = wikiRepo.fetchArticle(targetLang, targetTopic, targetSource)
            result.onSuccess { summary ->
                _currentArticle.value = summary
                checkIsBookmarked(summary.title)
            }.onFailure { err ->
                _articleErrorMessage.value = err.message ?: "Could not load article"
            }
            _isLoadingArticle.value = false
        }
    }

    private fun checkIsBookmarked(title: String) {
        viewModelScope.launch {
            articleDao.isBookmarked(title).collectLatest { isBookmarked ->
                _isCurrentArticleBookmarked.value = isBookmarked
            }
        }
    }

    fun toggleBookmarkCurrentArticle() {
        val article = _currentArticle.value ?: return
        viewModelScope.launch {
            if (_isCurrentArticleBookmarked.value) {
                articleDao.deleteBookmarkByTitle(article.title)
            } else {
                articleDao.insertBookmark(
                    BookmarkedArticle(
                        title = article.title,
                        description = article.description,
                        extract = article.extract,
                        thumbnailUrl = article.thumbnail?.source ?: article.originalimage?.source,
                        lang = article.lang,
                        topic = article.title
                    )
                )
            }
        }
    }

    fun toggleTts() {
        if (!isTtsInitialized || tts == null) return
        val article = _currentArticle.value ?: return

        if (_isTtsSpeaking.value) {
            stopTts()
        } else {
            val textToRead = "${article.title}. ${article.description ?: ""}. ${article.extract}"
            updateTtsLocale(article.lang)
            tts?.speak(textToRead, TextToSpeech.QUEUE_FLUSH, null, "WikiArticleTTS")
            _isTtsSpeaking.value = true
        }
    }

    private fun stopTts() {
        if (_isTtsSpeaking.value) {
            tts?.stop()
            _isTtsSpeaking.value = false
        }
    }

    private fun updateTtsLocale(langCode: String) {
        if (!isTtsInitialized) return
        val locale = when (langCode.lowercase()) {
            "fr" -> Locale.FRENCH
            "es" -> Locale("es", "ES")
            "de" -> Locale.GERMAN
            else -> Locale.ENGLISH
        }
        tts?.language = locale
    }

    // User preference updates
    fun updateLanguage(lang: String) {
        viewModelScope.launch {
            prefsRepo.updateLanguage(lang)
        }
    }

    fun updateThemeKeyword(theme: String) {
        viewModelScope.launch {
            prefsRepo.updateThemeKeyword(theme)
        }
    }

    fun updateThresholdMinutes(minutes: Int) {
        viewModelScope.launch {
            prefsRepo.updateThresholdMinutes(minutes)
        }
    }

    fun updateUsageThreshold(minutes: Int) {
        updateThresholdMinutes(minutes)
    }

    fun toggleMonitoredPackage(packageName: String) {
        viewModelScope.launch {
            prefsRepo.toggleMonitoredPackage(packageName)
        }
    }

    fun updateServiceEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateServiceEnabled(enabled)
        }
    }

    fun updateOverlayEnabled(enabled: Boolean) {
        viewModelScope.launch {
            prefsRepo.updateOverlayEnabled(enabled)
        }
    }

    fun updateInterruptionStyle(style: String) {
        viewModelScope.launch {
            prefsRepo.updateInterruptionStyle(style)
        }
    }

    fun updateContentSource(source: String) {
        viewModelScope.launch {
            prefsRepo.updateContentSource(source)
        }
    }

    fun completeBreakSession() {
        viewModelScope.launch {
            prefsRepo.incrementArticleReadStats(minutesSaved = 5)
            stopTts()
            _currentScreen.value = NavigationScreen.Dashboard
        }
    }

    override fun onCleared() {
        stopTts()
        tts?.shutdown()
        super.onCleared()
    }
}
