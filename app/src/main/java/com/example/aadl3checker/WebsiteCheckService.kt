package com.example.aadl3checker

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Intent
import android.media.RingtoneManager
import android.net.Uri
import android.os.Handler
import android.os.IBinder
import android.os.Looper
import androidx.core.app.NotificationCompat
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.launch
import kotlinx.coroutines.withContext
import kotlinx.coroutines.withTimeoutOrNull
import java.io.IOException
import java.net.HttpURLConnection
import java.net.URL

class WebsiteCheckService : Service() {
    private val channelId = "service_channel"
    private val notificationId = 2
    private var handler: Handler? = null
    private var countdownTime = 60
    private lateinit var runnable: Runnable

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        handler = Handler(Looper.getMainLooper())
        initializeRunnable()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        val notification = createNotification(getString(R.string.checking_aadl3_website_availability))
        startForeground(notificationId, notification)
        handler?.post(runnable)
        return START_STICKY
    }

    override fun onBind(intent: Intent): IBinder? = null

    private suspend fun isWebsiteAvailable(url: String): Boolean {
        return withContext(Dispatchers.IO) {
            try {
                val connection = URL(url).openConnection() as HttpURLConnection
                connection.requestMethod = "HEAD"
                connection.connectTimeout = 5000
                connection.readTimeout = 5000
                val responseCode = connection.responseCode
                responseCode in 200..399
            } catch (e: IOException) {
                false
            }
        }
    }

    private fun initializeRunnable() {
        runnable = Runnable {
            if (countdownTime > 0) {
                countdownTime--
                updateNotificationWithCountdown()
                handler?.postDelayed(runnable, 1000)
            } else {
                CoroutineScope(Dispatchers.Main).launch {
                    checkWebsiteAndNotify()
                }
            }
        }
    }

    private fun updateNotificationWithCountdown() {
        val updateText = getString(R.string.aadl3_website_is_not_available_trying_in_seconds, countdownTime.toString())
        val updatedNotification = createNotification(updateText, withAlarm = false)
        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        notificationManager.notify(notificationId, updatedNotification)
    }

    private fun createNotificationChannel() {
        val importance = NotificationManager.IMPORTANCE_HIGH
        val channel = NotificationChannel(channelId, "Service Channel", importance).apply {
            description = "Channel for AADL3 availability checks"
            setSound(
                RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM),
                Notification.AUDIO_ATTRIBUTES_DEFAULT
            )
        }
        val manager = getSystemService(NotificationManager::class.java)
        manager.createNotificationChannel(channel)
    }

    private suspend fun checkWebsiteAndNotify() {
        val websiteUrl = "https://www.aadl3inscription2024.dz/"
        val isAvailable = withTimeoutOrNull(5000L) {
            isWebsiteAvailable(websiteUrl)
        }

        val notificationManager = getSystemService(NOTIFICATION_SERVICE) as NotificationManager

        if (isAvailable == null) {
            countdownTime = 60
            handler?.postDelayed(runnable, 1000)
        } else if (!isAvailable) {
            countdownTime = 60
            handler?.postDelayed(runnable, 1000)
        } else {
            stopSelf()
            notificationManager.cancel(notificationId)
            val alertNotificationId = 3
            val updateText = getString(R.string.aadl3_website_available_go_register)
            val alertNotification = createNotification(updateText, websiteUrl, withAlarm = true, enhanceVibration = true)
            notificationManager.notify(alertNotificationId, alertNotification)
        }
    }

    private fun createNotification(text: String, url: String? = null, withAlarm: Boolean = false, enhanceVibration: Boolean = false): Notification {
        val intent = if (url != null) {
            Intent(Intent.ACTION_VIEW, Uri.parse(url))
        } else {
            Intent(this, MainActivity::class.java)
        }
        val pendingIntentFlags = PendingIntent.FLAG_IMMUTABLE or PendingIntent.FLAG_UPDATE_CURRENT
        val pendingIntent = PendingIntent.getActivity(this, 0, intent, pendingIntentFlags)

        val builder = NotificationCompat.Builder(this, channelId)
            .setContentTitle(getString(R.string.aadl3_website_checking))
            .setContentText(text)
            .setSmallIcon(R.drawable.ic_launcher_foreground)
            .setContentIntent(pendingIntent)
            .setOnlyAlertOnce(true)

        if (withAlarm) {
            val alarmSound: Uri = RingtoneManager.getDefaultUri(RingtoneManager.TYPE_ALARM)
            builder.setSound(alarmSound)
                .setPriority(NotificationCompat.PRIORITY_HIGH)
        }

        if (enhanceVibration) {
            builder.setVibrate(longArrayOf(0, 1000, 500, 1000, 500, 1000, 500, 1000))
        }

        return builder.build()
    }

    override fun onDestroy() {
        super.onDestroy()
        handler?.removeCallbacks(runnable)
    }
}
