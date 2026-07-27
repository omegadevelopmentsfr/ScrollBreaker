package com.example.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.app.usage.UsageEvents
import android.app.usage.UsageStatsManager
import android.content.Context
import android.content.Intent
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.example.MainActivity
import com.example.R
import com.example.data.UserPreferences
import com.example.data.UserPreferencesRepository
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.Job
import kotlinx.coroutines.delay
import kotlinx.coroutines.flow.first
import kotlinx.coroutines.isActive
import kotlinx.coroutines.launch

class UsageMonitorService : Service() {

    private val serviceJob = Job()
    private val serviceScope = CoroutineScope(Dispatchers.IO + serviceJob)
    private lateinit var prefsRepository: UserPreferencesRepository
    private var lastTriggeredTime = 0L

    companion object {
        const val CHANNEL_ID = "scrollbreak_monitor_channel"
        const val BREAK_CHANNEL_ID = "scrollbreak_break_channel"
        const val NOTIFICATION_ID = 8801
        const val ACTION_START = "com.example.service.ACTION_START"
        const val ACTION_STOP = "com.example.service.ACTION_STOP"
        const val EXTRA_LAUNCH_BREAK = "EXTRA_LAUNCH_BREAK"
        private const val MONITOR_INTERVAL_MS = 3000L // 3 seconds interval for fast detection
        private const val COOLDOWN_MS = 30000L // 30 seconds cooldown between break triggers
    }

    override fun onCreate() {
        super.onCreate()
        prefsRepository = UserPreferencesRepository(applicationContext)
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val action = intent?.action
        if (action == ACTION_STOP) {
            stopForeground(STOP_FOREGROUND_REMOVE)
            stopSelf()
            return START_NOT_STICKY
        }

        startForeground(NOTIFICATION_ID, createNotification("Monitoring active app screen time"))
        startMonitoringLoop()

        return START_STICKY
    }

    private fun startMonitoringLoop() {
        serviceScope.launch {
            while (isActive) {
                try {
                    val prefs = prefsRepository.userPreferencesFlow.first()
                    if (prefs.serviceEnabled) {
                        checkUsageAndTriggerIfNeeded(prefs)
                    }
                } catch (e: Exception) {
                    e.printStackTrace()
                }
                delay(MONITOR_INTERVAL_MS)
            }
        }
    }

    private fun checkUsageAndTriggerIfNeeded(prefs: UserPreferences) {
        val now = System.currentTimeMillis()
        val dynamicCooldownMs = (prefs.usageThresholdMinutes * 60 * 1000L).coerceAtLeast(15000L)
        if (now - lastTriggeredTime < dynamicCooldownMs) {
            return // Cooldown period active based on scroll duration threshold
        }

        val usageStatsManager = getSystemService(Context.USAGE_STATS_SERVICE) as? UsageStatsManager
            ?: return

        var currentForegroundPackage = ""
        val startTime = now - 1000 * 60 * 5 // Past 5 minutes

        try {
            val usageEvents = usageStatsManager.queryEvents(startTime, now)
            val event = UsageEvents.Event()

            while (usageEvents.hasNextEvent()) {
                usageEvents.getNextEvent(event)
                if (event.eventType == UsageEvents.Event.ACTIVITY_RESUMED || event.eventType == 1 || event.eventType == 7) {
                    currentForegroundPackage = event.packageName
                }
            }
        } catch (e: Exception) {
            e.printStackTrace()
        }

        // Fallback check via queryUsageStats if currentForegroundPackage is still empty
        if (currentForegroundPackage.isEmpty()) {
            try {
                val statsList = usageStatsManager.queryUsageStats(
                    UsageStatsManager.INTERVAL_DAILY,
                    now - 1000 * 60 * 2,
                    now
                )
                if (!statsList.isNullOrEmpty()) {
                    val mostRecent = statsList.maxByOrNull { it.lastTimeUsed }
                    if (mostRecent != null && (now - mostRecent.lastTimeUsed) < 15000) {
                        currentForegroundPackage = mostRecent.packageName
                    }
                }
            } catch (e: Exception) {
                e.printStackTrace()
            }
        }

        val isMonitored = prefs.monitoredPackages.contains(currentForegroundPackage) ||
                prefs.monitoredPackages.isEmpty() ||
                prefs.monitoredPackages.contains("ALL_APPS") ||
                currentForegroundPackage.contains("instagram") ||
                currentForegroundPackage.contains("musically") ||
                currentForegroundPackage.contains("tiktok") ||
                currentForegroundPackage.contains("twitter") ||
                currentForegroundPackage.contains("youtube") ||
                currentForegroundPackage.contains("facebook") ||
                currentForegroundPackage.contains("chrome")

        // Check if currently open app is in monitored list and not ScrollBreak itself
        if (currentForegroundPackage.isNotEmpty() &&
            currentForegroundPackage != packageName &&
            isMonitored
        ) {
            lastTriggeredTime = now
            triggerBreak(prefs, currentForegroundPackage)
        }
    }

    private fun triggerBreak(prefs: com.example.data.UserPreferences, foregroundPackage: String) {
        when (prefs.interruptionStyle) {
            "notification" -> {
                sendBreakNotification(prefs, foregroundPackage)
            }
            "immersive" -> {
                launchImmersiveBreak(prefs, foregroundPackage)
            }
            else -> { // "overlay"
                if (prefs.overlayEnabled && android.provider.Settings.canDrawOverlays(this)) {
                    val overlayIntent = Intent(this, OverlayService::class.java).apply {
                        putExtra("PACKAGE_NAME", foregroundPackage)
                    }
                    startService(overlayIntent)
                } else {
                    sendBreakNotification(prefs, foregroundPackage)
                }
            }
        }
    }

    private fun launchImmersiveBreak(prefs: com.example.data.UserPreferences, foregroundPackage: String) {
        val wikiRepo = com.example.data.WikipediaRepository()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + serviceJob).launch {
            val result = wikiRepo.fetchArticle(prefs.language, prefs.themeKeyword, prefs.contentSource)
            val article = result.getOrNull()
            val breakIntent = Intent(this@UsageMonitorService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_LAUNCH_BREAK, true)
                putExtra("TRIGGERED_PACKAGE", foregroundPackage)
                if (article != null) {
                    putExtra("EXTRA_ARTICLE_TITLE", article.title)
                    putExtra("EXTRA_ARTICLE_LANG", article.lang)
                    putExtra("EXTRA_ARTICLE_EXTRACT", article.extract)
                    putExtra("EXTRA_ARTICLE_DESC", article.description)
                    putExtra("EXTRA_ARTICLE_THUMB", article.thumbnail?.source ?: article.originalimage?.source)
                    putExtra("EXTRA_ARTICLE_URL", article.getPageUrl())
                }
            }
            startActivity(breakIntent)
        }
    }

    private fun sendBreakNotification(prefs: com.example.data.UserPreferences, foregroundPackage: String) {
        val wikiRepo = com.example.data.WikipediaRepository()
        kotlinx.coroutines.CoroutineScope(kotlinx.coroutines.Dispatchers.IO + serviceJob).launch {
            val result = wikiRepo.fetchArticle(prefs.language, prefs.themeKeyword, prefs.contentSource)
            val article = result.getOrNull()

            val breakIntent = Intent(this@UsageMonitorService, MainActivity::class.java).apply {
                addFlags(Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_SINGLE_TOP)
                putExtra(EXTRA_LAUNCH_BREAK, true)
                putExtra("TRIGGERED_PACKAGE", foregroundPackage)
                if (article != null) {
                    putExtra("EXTRA_ARTICLE_TITLE", article.title)
                    putExtra("EXTRA_ARTICLE_LANG", article.lang)
                    putExtra("EXTRA_ARTICLE_EXTRACT", article.extract)
                    putExtra("EXTRA_ARTICLE_DESC", article.description)
                    putExtra("EXTRA_ARTICLE_THUMB", article.thumbnail?.source ?: article.originalimage?.source)
                    putExtra("EXTRA_ARTICLE_URL", article.getPageUrl())
                }
            }

            val pendingIntent = PendingIntent.getActivity(
                this@UsageMonitorService,
                101,
                breakIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )

            val titleText = if (article != null) "🧠 Pause Culture : ${article.title}" else "🧠 Pause ScrollBreaker !"
            val bodyText = if (article != null) article.extract else "Découvrez un article intéressant pour couper votre session."

            val notification = NotificationCompat.Builder(this@UsageMonitorService, BREAK_CHANNEL_ID)
                .setContentTitle(titleText)
                .setContentText(bodyText)
                .setStyle(NotificationCompat.BigTextStyle().bigText(bodyText))
                .setSmallIcon(android.R.drawable.ic_dialog_info)
                .setContentIntent(pendingIntent)
                .setAutoCancel(true)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
                .build()

            val manager = getSystemService(NotificationManager::class.java)
            manager?.notify(2002, notification)
        }
    }

    private fun createNotification(contentText: String): Notification {
        val pendingIntent: PendingIntent = Intent(this, MainActivity::class.java).let { notificationIntent ->
            PendingIntent.getActivity(
                this, 0, notificationIntent,
                PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
            )
        }

        val stopIntent = Intent(this, UsageMonitorService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(getString(R.string.service_notification_title))
            .setContentText(contentText)
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setContentIntent(pendingIntent)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Pause Service", stopPendingIntent)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val serviceChannel = NotificationChannel(
                CHANNEL_ID,
                getString(R.string.service_notification_channel),
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = getString(R.string.service_notification_desc)
            }
            val breakChannel = NotificationChannel(
                BREAK_CHANNEL_ID,
                "ScrollBreak Alerts",
                NotificationManager.IMPORTANCE_HIGH
            ).apply {
                description = "Notifications for Wikipedia break articles"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager?.createNotificationChannel(serviceChannel)
            manager?.createNotificationChannel(breakChannel)
        }
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceJob.cancel()
    }

    override fun onBind(intent: Intent?): IBinder? = null
}
