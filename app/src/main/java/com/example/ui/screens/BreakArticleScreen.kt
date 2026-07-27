package com.example.ui.screens

import android.annotation.SuppressLint
import android.content.Intent
import android.net.Uri
import android.webkit.WebChromeClient
import android.webkit.WebSettings
import android.webkit.WebView
import android.webkit.WebViewClient
import androidx.compose.animation.AnimatedVisibility
import androidx.compose.animation.fadeIn
import androidx.compose.animation.fadeOut
import androidx.compose.foundation.background
import androidx.compose.foundation.clickable
import androidx.compose.foundation.layout.Arrangement
import androidx.compose.foundation.layout.Box
import androidx.compose.foundation.layout.Column
import androidx.compose.foundation.layout.Row
import androidx.compose.foundation.layout.Spacer
import androidx.compose.foundation.layout.fillMaxHeight
import androidx.compose.foundation.layout.fillMaxSize
import androidx.compose.foundation.layout.fillMaxWidth
import androidx.compose.foundation.layout.height
import androidx.compose.foundation.layout.padding
import androidx.compose.foundation.layout.size
import androidx.compose.foundation.layout.width
import androidx.compose.foundation.rememberScrollState
import androidx.compose.foundation.shape.CircleShape
import androidx.compose.foundation.shape.RoundedCornerShape
import androidx.compose.foundation.verticalScroll
import androidx.compose.material.icons.Icons
import androidx.compose.material.icons.filled.Add
import androidx.compose.material.icons.filled.ArrowBack
import androidx.compose.material.icons.filled.ArrowForward
import androidx.compose.material.icons.filled.AutoAwesome
import androidx.compose.material.icons.filled.Bookmark
import androidx.compose.material.icons.filled.BookmarkBorder
import androidx.compose.material.icons.filled.CheckCircle
import androidx.compose.material.icons.filled.Language
import androidx.compose.material.icons.filled.OpenInNew
import androidx.compose.material.icons.filled.Refresh
import androidx.compose.material.icons.filled.Remove
import androidx.compose.material.icons.filled.Subject
import androidx.compose.material.icons.filled.VolumeUp
import androidx.compose.material3.Button
import androidx.compose.material3.ButtonDefaults
import androidx.compose.material3.Card
import androidx.compose.material3.CardDefaults
import androidx.compose.material3.CircularProgressIndicator
import androidx.compose.material3.ExperimentalMaterial3Api
import androidx.compose.material3.Icon
import androidx.compose.material3.IconButton
import androidx.compose.material3.LinearProgressIndicator
import androidx.compose.material3.MaterialTheme
import androidx.compose.material3.OutlinedButton
import androidx.compose.material3.Scaffold
import androidx.compose.material3.Surface
import androidx.compose.material3.Text
import androidx.compose.material3.TopAppBar
import androidx.compose.material3.TopAppBarDefaults
import androidx.compose.runtime.Composable
import androidx.compose.runtime.collectAsState
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableIntStateOf
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.Alignment
import androidx.compose.ui.Modifier
import androidx.compose.ui.draw.clip
import androidx.compose.ui.layout.ContentScale
import androidx.compose.ui.platform.LocalContext
import androidx.compose.ui.platform.testTag
import androidx.compose.ui.text.font.FontFamily
import androidx.compose.ui.text.font.FontWeight
import androidx.compose.ui.unit.dp
import androidx.compose.ui.unit.sp
import androidx.compose.ui.viewinterop.AndroidView
import coil.compose.AsyncImage
import com.example.data.UserPreferences
import com.example.ui.MainViewModel
import com.example.ui.NavigationScreen

enum class ArticleViewMode {
    WEB_VIEW, // Full Wikipedia page on web
    SUMMARY   // Clean Reader Mode summary
}

@OptIn(ExperimentalMaterial3Api::class)
@Composable
fun BreakArticleScreen(
    viewModel: MainViewModel,
    modifier: Modifier = Modifier
) {
    val currentArticle by viewModel.currentArticle.collectAsState()
    val isLoading by viewModel.isLoadingArticle.collectAsState()
    val isBookmarked by viewModel.isCurrentArticleBookmarked.collectAsState()
    val isSpeaking by viewModel.isTtsSpeaking.collectAsState()
    val userPrefs by viewModel.userPreferences.collectAsState(initial = UserPreferences())

    var viewMode by remember { mutableStateOf(ArticleViewMode.WEB_VIEW) }
    var fontSizeSp by remember { mutableIntStateOf(17) }
    val context = LocalContext.current

    Scaffold(
        topBar = {
            TopAppBar(
                title = {
                    Row(verticalAlignment = Alignment.CenterVertically) {
                        Surface(
                            shape = RoundedCornerShape(8.dp),
                            color = MaterialTheme.colorScheme.primaryContainer,
                            modifier = Modifier.padding(end = 8.dp)
                        ) {
                            Text(
                                text = (currentArticle?.lang ?: userPrefs.language).uppercase(),
                                style = MaterialTheme.typography.labelMedium,
                                fontWeight = FontWeight.Bold,
                                color = MaterialTheme.colorScheme.onPrimaryContainer,
                                modifier = Modifier.padding(horizontal = 8.dp, vertical = 4.dp)
                            )
                        }
                        Text(
                            text = "Wikipedia Break",
                            style = MaterialTheme.typography.titleMedium,
                            fontWeight = FontWeight.Bold
                        )
                    }
                },
                navigationIcon = {
                    IconButton(
                        onClick = { viewModel.navigateTo(NavigationScreen.Dashboard) },
                        modifier = Modifier.testTag("back_to_dashboard_btn")
                    ) {
                        Icon(Icons.Default.ArrowBack, contentDescription = "Back")
                    }
                },
                actions = {
                    IconButton(
                        onClick = { viewModel.toggleBookmarkCurrentArticle() },
                        modifier = Modifier.testTag("bookmark_article_btn")
                    ) {
                        Icon(
                            imageVector = if (isBookmarked) Icons.Default.Bookmark else Icons.Default.BookmarkBorder,
                            contentDescription = "Bookmark",
                            tint = if (isBookmarked) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                        )
                    }
                    IconButton(
                        onClick = { viewModel.loadNewArticle() },
                        modifier = Modifier.testTag("next_article_btn")
                    ) {
                        Icon(Icons.Default.Refresh, contentDescription = "Discover Another")
                    }
                },
                colors = TopAppBarDefaults.topAppBarColors(
                    containerColor = MaterialTheme.colorScheme.surface
                )
            )
        },
        bottomBar = {
            if (!isLoading && currentArticle != null) {
                Surface(
                    shadowElevation = 8.dp,
                    color = MaterialTheme.colorScheme.surface
                ) {
                    Column(
                        modifier = Modifier
                            .fillMaxWidth()
                            .padding(16.dp),
                        verticalArrangement = Arrangement.spacedBy(8.dp)
                    ) {
                        Button(
                            onClick = {
                                viewModel.completeBreakSession()
                                try {
                                    (context as? android.app.Activity)?.moveTaskToBack(true)
                                } catch (e: Exception) {
                                    e.printStackTrace()
                                }
                            },
                            shape = RoundedCornerShape(16.dp),
                            colors = ButtonDefaults.buttonColors(
                                containerColor = MaterialTheme.colorScheme.primary
                            ),
                            modifier = Modifier
                                .fillMaxWidth()
                                .height(50.dp)
                                .testTag("complete_break_button")
                        ) {
                            Icon(Icons.Default.CheckCircle, contentDescription = null)
                            Spacer(modifier = Modifier.width(8.dp))
                            Text(
                                text = "Terminer la pause (Retour à mon écran)",
                                fontWeight = FontWeight.Bold,
                                fontSize = 15.sp
                            )
                        }

                        Row(
                            modifier = Modifier.fillMaxWidth(),
                            horizontalArrangement = Arrangement.spacedBy(8.dp)
                        ) {
                            OutlinedButton(
                                onClick = { viewModel.loadNewArticle() },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("next_article_bottom_btn")
                            ) {
                                Icon(Icons.Default.Refresh, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Autre article", fontSize = 13.sp)
                            }

                            OutlinedButton(
                                onClick = {
                                    viewModel.completeBreakSession()
                                    viewModel.navigateTo(NavigationScreen.Dashboard)
                                },
                                shape = RoundedCornerShape(16.dp),
                                modifier = Modifier
                                    .weight(1f)
                                    .testTag("open_dashboard_bottom_btn")
                            ) {
                                Icon(Icons.Default.ArrowBack, contentDescription = null, modifier = Modifier.size(16.dp))
                                Spacer(modifier = Modifier.width(6.dp))
                                Text("Tableau de bord", fontSize = 13.sp)
                            }
                        }
                    }
                }
            }
        }
    ) { innerPadding ->
        Box(
            modifier = modifier
                .fillMaxSize()
                .padding(innerPadding)
        ) {
            if (isLoading) {
                Column(
                    modifier = Modifier.fillMaxSize(),
                    verticalArrangement = Arrangement.Center,
                    horizontalAlignment = Alignment.CenterHorizontally
                ) {
                    CircularProgressIndicator(color = MaterialTheme.colorScheme.primary)
                    Spacer(modifier = Modifier.height(16.dp))
                    Text(
                        text = "Chargement de l'article Wikipédia...",
                        style = MaterialTheme.typography.bodyMedium,
                        color = MaterialTheme.colorScheme.onSurfaceVariant
                    )
                }
            } else if (currentArticle != null) {
                val article = currentArticle!!
                val articleUrl = article.getPageUrl()

                Column(modifier = Modifier.fillMaxSize()) {
                    // Mode Switcher Bar (Web Reader vs Summary)
                    Surface(
                        color = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.6f),
                        modifier = Modifier.fillMaxWidth()
                    ) {
                        Row(
                            modifier = Modifier
                                .fillMaxWidth()
                                .padding(horizontal = 16.dp, vertical = 8.dp),
                            horizontalArrangement = Arrangement.SpaceBetween,
                            verticalAlignment = Alignment.CenterVertically
                        ) {
                            // View Mode Toggle Segmented Buttons
                            Row(
                                modifier = Modifier
                                    .clip(RoundedCornerShape(12.dp))
                                    .background(MaterialTheme.colorScheme.surface)
                                    .padding(4.dp),
                                verticalAlignment = Alignment.CenterVertically
                            ) {
                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (viewMode == ArticleViewMode.WEB_VIEW)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { viewMode = ArticleViewMode.WEB_VIEW }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Language,
                                            contentDescription = null,
                                            tint = if (viewMode == ArticleViewMode.WEB_VIEW)
                                                MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Lecteur Web",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (viewMode == ArticleViewMode.WEB_VIEW)
                                                MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }

                                Box(
                                    modifier = Modifier
                                        .clip(RoundedCornerShape(8.dp))
                                        .background(
                                            if (viewMode == ArticleViewMode.SUMMARY)
                                                MaterialTheme.colorScheme.primary
                                            else MaterialTheme.colorScheme.surface
                                        )
                                        .clickable { viewMode = ArticleViewMode.SUMMARY }
                                        .padding(horizontal = 12.dp, vertical = 6.dp)
                                ) {
                                    Row(verticalAlignment = Alignment.CenterVertically) {
                                        Icon(
                                            imageVector = Icons.Default.Subject,
                                            contentDescription = null,
                                            tint = if (viewMode == ArticleViewMode.SUMMARY)
                                                MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant,
                                            modifier = Modifier.size(16.dp)
                                        )
                                        Spacer(modifier = Modifier.width(6.dp))
                                        Text(
                                            text = "Résumé",
                                            style = MaterialTheme.typography.labelMedium,
                                            fontWeight = FontWeight.Bold,
                                            color = if (viewMode == ArticleViewMode.SUMMARY)
                                                MaterialTheme.colorScheme.onPrimary
                                            else MaterialTheme.colorScheme.onSurfaceVariant
                                        )
                                    }
                                }
                            }

                            // Theme Badge
                            Surface(
                                shape = CircleShape,
                                color = MaterialTheme.colorScheme.secondaryContainer
                            ) {
                                Row(
                                    verticalAlignment = Alignment.CenterVertically,
                                    modifier = Modifier.padding(horizontal = 10.dp, vertical = 4.dp)
                                ) {
                                    Icon(
                                        imageVector = Icons.Default.AutoAwesome,
                                        contentDescription = null,
                                        tint = MaterialTheme.colorScheme.onSecondaryContainer,
                                        modifier = Modifier.size(12.dp)
                                    )
                                    Spacer(modifier = Modifier.width(4.dp))
                                    Text(
                                        text = userPrefs.themeKeyword,
                                        style = MaterialTheme.typography.labelSmall,
                                        fontWeight = FontWeight.Bold,
                                        color = MaterialTheme.colorScheme.onSecondaryContainer
                                    )
                                }
                            }
                        }
                    }

                    when (viewMode) {
                        ArticleViewMode.WEB_VIEW -> {
                            WikiWebViewReader(
                                url = articleUrl,
                                onOpenExternal = {
                                    val intent = Intent(Intent.ACTION_VIEW, Uri.parse(articleUrl))
                                    context.startActivity(intent)
                                }
                            )
                        }

                        ArticleViewMode.SUMMARY -> {
                            Column(
                                modifier = Modifier
                                    .fillMaxSize()
                                    .verticalScroll(rememberScrollState())
                                    .padding(20.dp),
                                verticalArrangement = Arrangement.spacedBy(16.dp)
                            ) {
                                // Image thumbnail
                                val imageUrl = article.thumbnail?.source ?: article.originalimage?.source
                                if (!imageUrl.isNullOrEmpty()) {
                                    Card(
                                        shape = RoundedCornerShape(20.dp),
                                        colors = CardDefaults.cardColors(containerColor = MaterialTheme.colorScheme.surfaceVariant),
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .height(220.dp)
                                    ) {
                                        AsyncImage(
                                            model = imageUrl,
                                            contentDescription = article.title,
                                            contentScale = ContentScale.Crop,
                                            modifier = Modifier.fillMaxSize()
                                        )
                                    }
                                }

                                // Title & Description
                                Column {
                                    Text(
                                        text = article.title,
                                        style = MaterialTheme.typography.headlineSmall,
                                        fontWeight = FontWeight.ExtraBold,
                                        fontFamily = FontFamily.Serif,
                                        color = MaterialTheme.colorScheme.onSurface
                                    )

                                    if (!article.description.isNullOrBlank()) {
                                        Spacer(modifier = Modifier.height(4.dp))
                                        Text(
                                            text = article.description,
                                            style = MaterialTheme.typography.titleMedium,
                                            fontWeight = FontWeight.Medium,
                                            color = MaterialTheme.colorScheme.primary
                                        )
                                    }
                                }

                                // Audio Reader & Font Bar
                                Card(
                                    shape = RoundedCornerShape(16.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surfaceVariant.copy(alpha = 0.5f)
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Row(
                                        modifier = Modifier
                                            .fillMaxWidth()
                                            .padding(12.dp),
                                        horizontalArrangement = Arrangement.SpaceBetween,
                                        verticalAlignment = Alignment.CenterVertically
                                    ) {
                                        OutlinedButton(
                                            onClick = { viewModel.toggleTts() },
                                            shape = RoundedCornerShape(12.dp),
                                            colors = ButtonDefaults.outlinedButtonColors(
                                                contentColor = if (isSpeaking) MaterialTheme.colorScheme.primary else MaterialTheme.colorScheme.onSurface
                                            ),
                                            modifier = Modifier.testTag("tts_audio_btn")
                                        ) {
                                            Icon(
                                                imageVector = Icons.Default.VolumeUp,
                                                contentDescription = "Écouter",
                                                modifier = Modifier.size(18.dp)
                                            )
                                            Spacer(modifier = Modifier.width(6.dp))
                                            Text(if (isSpeaking) "Pause" else "Écouter")
                                        }

                                        Row(verticalAlignment = Alignment.CenterVertically) {
                                            IconButton(
                                                onClick = { if (fontSizeSp > 13) fontSizeSp -= 2 },
                                                enabled = fontSizeSp > 13
                                            ) {
                                                Icon(Icons.Default.Remove, contentDescription = "Taille texte -")
                                            }
                                            Text(
                                                text = "${fontSizeSp}sp",
                                                style = MaterialTheme.typography.labelLarge,
                                                fontWeight = FontWeight.Bold
                                            )
                                            IconButton(
                                                onClick = { if (fontSizeSp < 26) fontSizeSp += 2 },
                                                enabled = fontSizeSp < 26
                                            ) {
                                                Icon(Icons.Default.Add, contentDescription = "Taille texte +")
                                            }
                                        }
                                    }
                                }

                                // Article Extract Content
                                Card(
                                    shape = RoundedCornerShape(20.dp),
                                    colors = CardDefaults.cardColors(
                                        containerColor = MaterialTheme.colorScheme.surface
                                    ),
                                    modifier = Modifier.fillMaxWidth()
                                ) {
                                    Text(
                                        text = article.extract,
                                        fontSize = fontSizeSp.sp,
                                        lineHeight = (fontSizeSp * 1.55).sp,
                                        fontFamily = FontFamily.Serif,
                                        color = MaterialTheme.colorScheme.onSurface,
                                        modifier = Modifier.padding(20.dp)
                                    )
                                }

                                Spacer(modifier = Modifier.height(20.dp))
                            }
                        }
                    }
                }
            }
        }
    }
}

@SuppressLint("SetJavaScriptEnabled")
@Composable
fun WikiWebViewReader(
    url: String,
    onOpenExternal: () -> Unit
) {
    var webViewInstance by remember { mutableStateOf<WebView?>(null) }
    var webProgress by remember { mutableIntStateOf(0) }
    var canGoBack by remember { mutableStateOf(false) }
    var canGoForward by remember { mutableStateOf(false) }

    Column(modifier = Modifier.fillMaxSize()) {
        // WebView Navigation Toolbar
        Surface(
            color = MaterialTheme.colorScheme.surface,
            tonalElevation = 2.dp,
            modifier = Modifier.fillMaxWidth()
        ) {
            Row(
                modifier = Modifier
                    .fillMaxWidth()
                    .padding(horizontal = 8.dp, vertical = 4.dp),
                verticalAlignment = Alignment.CenterVertically,
                horizontalArrangement = Arrangement.SpaceBetween
            ) {
                Row(verticalAlignment = Alignment.CenterVertically) {
                    IconButton(
                        onClick = { webViewInstance?.goBack() },
                        enabled = canGoBack
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowBack,
                            contentDescription = "Précédent",
                            tint = if (canGoBack) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.goForward() },
                        enabled = canGoForward
                    ) {
                        Icon(
                            imageVector = Icons.Default.ArrowForward,
                            contentDescription = "Suivant",
                            tint = if (canGoForward) MaterialTheme.colorScheme.onSurface else MaterialTheme.colorScheme.onSurface.copy(alpha = 0.38f)
                        )
                    }

                    IconButton(
                        onClick = { webViewInstance?.reload() }
                    ) {
                        Icon(
                            imageVector = Icons.Default.Refresh,
                            contentDescription = "Actualiser"
                        )
                    }
                }

                IconButton(
                    onClick = onOpenExternal
                ) {
                    Icon(
                        imageVector = Icons.Default.OpenInNew,
                        contentDescription = "Ouvrir dans le navigateur"
                    )
                }
            }
        }

        if (webProgress in 1..99) {
            LinearProgressIndicator(
                progress = { webProgress / 100f },
                modifier = Modifier.fillMaxWidth(),
                color = MaterialTheme.colorScheme.primary
            )
        }

        AndroidView(
            modifier = Modifier.fillMaxSize(),
            factory = { context ->
                WebView(context).apply {
                    settings.apply {
                        javaScriptEnabled = true
                        domStorageEnabled = true
                        loadWithOverviewMode = true
                        useWideViewPort = true
                        builtInZoomControls = true
                        displayZoomControls = false
                        mixedContentMode = WebSettings.MIXED_CONTENT_COMPATIBILITY_MODE
                    }

                    webViewClient = object : WebViewClient() {
                        override fun onPageFinished(view: WebView?, url: String?) {
                            super.onPageFinished(view, url)
                            canGoBack = view?.canGoBack() ?: false
                            canGoForward = view?.canGoForward() ?: false
                        }
                    }

                    webChromeClient = object : WebChromeClient() {
                        override fun onProgressChanged(view: WebView?, newProgress: Int) {
                            super.onProgressChanged(view, newProgress)
                            webProgress = newProgress
                            canGoBack = view?.canGoBack() ?: false
                            canGoForward = view?.canGoForward() ?: false
                        }
                    }

                    loadUrl(url)
                    webViewInstance = this
                }
            },
            update = { webView ->
                if (webView.url != url && !webView.url.orEmpty().contains(url)) {
                    webView.loadUrl(url)
                }
            }
        )
    }
}
