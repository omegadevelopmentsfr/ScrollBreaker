package com.example

import android.content.Intent
import android.os.Bundle
import androidx.activity.ComponentActivity
import androidx.activity.compose.setContent
import androidx.activity.enableEdgeToEdge
import androidx.activity.viewModels
import androidx.compose.animation.AnimatedContent
import androidx.compose.animation.ExperimentalAnimationApi
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.animation.togetherWith
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.material3.Surface
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.ui.Modifier
import com.example.service.UsageMonitorService
import com.example.ui.MainViewModel
import com.example.ui.NavigationScreen
import com.example.ui.screens.BreakArticleScreen
import com.example.ui.screens.DashboardConfigScreen
import com.example.ui.screens.SavedArticlesScreen
import com.example.ui.screens.StatisticsScreen
import com.example.ui.theme.ScrollBreakTheme

class MainActivity : ComponentActivity() {

    private val viewModel: MainViewModel by viewModels()

    override fun onCreate(savedInstanceState: Bundle?) {
        super.onCreate(savedInstanceState)
        enableEdgeToEdge()

        handleIntent(intent)

        setContent {
            ScrollBreakTheme {
                Surface(modifier = Modifier.fillMaxSize()) {
                    ScrollBreakApp(viewModel = viewModel)
                }
            }
        }
    }

    override fun onNewIntent(intent: Intent) {
        super.onNewIntent(intent)
        setIntent(intent)
        handleIntent(intent)
    }

    private fun handleIntent(intent: Intent?) {
        if (intent == null) return
        val isBreakLaunch = intent.getBooleanExtra(UsageMonitorService.EXTRA_LAUNCH_BREAK, false)
        val title = intent.getStringExtra("EXTRA_ARTICLE_TITLE")
        val lang = intent.getStringExtra("EXTRA_ARTICLE_LANG")
        val extract = intent.getStringExtra("EXTRA_ARTICLE_EXTRACT")
        val desc = intent.getStringExtra("EXTRA_ARTICLE_DESC")
        val thumb = intent.getStringExtra("EXTRA_ARTICLE_THUMB")
        val url = intent.getStringExtra("EXTRA_ARTICLE_URL")

        if (!title.isNullOrEmpty() && !lang.isNullOrEmpty() && !extract.isNullOrEmpty()) {
            val preloadedArticle = com.example.data.models.WikiSummaryResponse(
                title = title,
                displaytitle = title,
                extract = extract,
                description = desc,
                lang = lang,
                thumbnail = if (!thumb.isNullOrEmpty()) com.example.data.models.WikiImage(source = thumb) else null,
                contentUrls = if (!url.isNullOrEmpty()) com.example.data.models.WikiContentUrls(
                    desktop = com.example.data.models.WikiUrl(page = url),
                    mobile = com.example.data.models.WikiUrl(page = url)
                ) else null
            )
            viewModel.setPreloadedArticle(preloadedArticle)
            viewModel.navigateTo(NavigationScreen.BreakArticle)
        } else if (!title.isNullOrEmpty() && !lang.isNullOrEmpty()) {
            viewModel.loadArticleByTitle(title, lang)
            viewModel.navigateTo(NavigationScreen.BreakArticle)
        } else if (isBreakLaunch) {
            if (viewModel.currentArticle.value == null) {
                viewModel.loadNewArticle()
            }
            viewModel.navigateTo(NavigationScreen.BreakArticle)
        }
    }
}

@OptIn(ExperimentalAnimationApi::class)
@Composable
fun ScrollBreakApp(viewModel: MainViewModel) {
    val currentScreen by viewModel.currentScreen.collectAsState()

    AnimatedContent(
        targetState = currentScreen,
        transitionSpec = {
            fadeIn() togetherWith fadeOut()
        },
        label = "ScreenTransition"
    ) { screen ->
        when (screen) {
            is NavigationScreen.Dashboard -> DashboardConfigScreen(viewModel = viewModel)
            is NavigationScreen.BreakArticle -> BreakArticleScreen(viewModel = viewModel)
            is NavigationScreen.SavedArticles -> SavedArticlesScreen(viewModel = viewModel)
            is NavigationScreen.Statistics -> StatisticsScreen(viewModel = viewModel)
        }
    }
}
