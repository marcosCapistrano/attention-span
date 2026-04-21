package com.example.attentionspan.service

import android.app.*
import android.content.Intent
import android.os.Binder
import android.os.Build
import android.os.IBinder
import android.os.SystemClock
import androidx.core.app.NotificationCompat
import com.example.attentionspan.MainActivity
import com.example.attentionspan.R
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.MutableStateFlow
import kotlinx.coroutines.flow.asStateFlow

class TimerService : Service() {

    private val binder = TimerBinder()
    private val serviceScope = CoroutineScope(SupervisorJob() + Dispatchers.Main)
    
    private val _elapsedTime = MutableStateFlow(0L)
    val elapsedTime = _elapsedTime.asStateFlow()
    
    private val _isRunning = MutableStateFlow(false)
    val isRunning = _isRunning.asStateFlow()

    private var startTimeMillis = 0L
    private var subjectId: Long = -1
    private var subjectName: String = ""

    companion object {
        const val CHANNEL_ID = "timer_channel"
        const val NOTIFICATION_ID = 1
        const val ACTION_STOP = "ACTION_STOP"
        const val EXTRA_SUBJECT_ID = "EXTRA_SUBJECT_ID"
        const val EXTRA_SUBJECT_NAME = "EXTRA_SUBJECT_NAME"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        when (intent?.action) {
            ACTION_STOP -> {
                stopTimer()
            }
            else -> {
                val id = intent?.getLongExtra(EXTRA_SUBJECT_ID, -1) ?: -1
                val name = intent?.getStringExtra(EXTRA_SUBJECT_NAME) ?: "Study Session"
                if (id != -1L) {
                    startTimer(id, name)
                }
            }
        }
        return START_NOT_STICKY
    }

    private fun startTimer(id: Long, name: String) {
        if (_isRunning.value) return
        
        subjectId = id
        subjectName = name
        startTimeMillis = System.currentTimeMillis()
        _isRunning.value = true
        
        startForeground(NOTIFICATION_ID, createNotification())
        
        serviceScope.launch {
            while (_isRunning.value) {
                _elapsedTime.value = System.currentTimeMillis() - startTimeMillis
                delay(1000)
            }
        }
    }

    fun stopTimer() {
        _isRunning.value = false
        stopForeground(STOP_FOREGROUND_REMOVE)
        stopSelf()
    }

    private fun createNotification(): Notification {
        val intent = Intent(this, MainActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_SINGLE_TOP
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, intent, PendingIntent.FLAG_IMMUTABLE
        )

        val stopIntent = Intent(this, TimerService::class.java).apply {
            action = ACTION_STOP
        }
        val stopPendingIntent = PendingIntent.getService(
            this, 0, stopIntent, PendingIntent.FLAG_IMMUTABLE
        )

        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("Focusing on $subjectName")
            .setSmallIcon(android.R.drawable.ic_media_play) // Use a better icon if available
            .setOngoing(true)
            .setUsesChronometer(true)
            .setWhen(System.currentTimeMillis())
            .setContentIntent(pendingIntent)
            .addAction(android.R.drawable.ic_menu_close_clear_cancel, "Stop", stopPendingIntent)
            .setCategory(NotificationCompat.CATEGORY_SERVICE)
            .setVisibility(NotificationCompat.VISIBILITY_PUBLIC)
            .build()
    }

    private fun createNotificationChannel() {
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            val channel = NotificationChannel(
                CHANNEL_ID,
                "Study Timer",
                NotificationManager.IMPORTANCE_LOW
            ).apply {
                description = "Shows the active study timer"
            }
            val manager = getSystemService(NotificationManager::class.java)
            manager.createNotificationChannel(channel)
        }
    }

    override fun onBind(intent: Intent?): IBinder = binder

    inner class TimerBinder : Binder() {
        fun getService(): TimerService = this@TimerService
    }

    override fun onDestroy() {
        super.onDestroy()
        serviceScope.cancel()
    }
}
