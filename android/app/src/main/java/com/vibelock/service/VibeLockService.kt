package com.vibelock.service

import android.app.*
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.Build
import android.os.IBinder
import androidx.core.app.NotificationCompat
import com.vibelock.LockScreenActivity
import com.vibelock.MainActivity
import com.vibelock.data.model.MatchState
import com.vibelock.data.repository.MatchRepository
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.*
import kotlinx.coroutines.flow.collectLatest
import javax.inject.Inject

@AndroidEntryPoint
class VibeLockService : Service() {

    @Inject lateinit var matchRepo: MatchRepository

    private val scope = CoroutineScope(SupervisorJob() + Dispatchers.Main)

    companion object {
        const val CHANNEL_ID = "vibelock_service"
        const val NOTIF_ID = 1001
        const val MATCH_NOTIF_ID = 1002
        const val ACTION_STOP = "com.vibelock.STOP"
    }

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
        startForeground()
        observeMatchState()
    }

    private fun startForeground() {
        val notification = buildServiceNotification("VibeLock 실행 중", "매칭 대기 중...")
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.Q) {
            startForeground(NOTIF_ID, notification, ServiceInfo.FOREGROUND_SERVICE_TYPE_CONNECTED_DEVICE)
        } else {
            startForeground(NOTIF_ID, notification)
        }
    }

    private fun observeMatchState() {
        scope.launch {
            matchRepo.matchState.collectLatest { state ->
                when (state) {
                    is MatchState.Matched -> onMatched(state)
                    is MatchState.Searching -> updateNotification("VibeLock", "매칭 중...")
                    is MatchState.Idle -> updateNotification("VibeLock", "탭하여 매칭 시작")
                    is MatchState.Disconnected -> updateNotification("VibeLock", "연결 끊김")
                }
            }
        }
    }

    private fun onMatched(state: MatchState.Matched) {
        updateNotification("${state.partnerName} 와 매칭됨!", "탭하여 그림 그리기")

        // Show lock screen notification
        val lockIntent = Intent(this, LockScreenActivity::class.java).apply {
            flags = Intent.FLAG_ACTIVITY_NEW_TASK
            putExtra("partner_name", state.partnerName)
        }
        val pendingIntent = PendingIntent.getActivity(
            this, 0, lockIntent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notif = NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle("🎨 ${state.partnerName} 이/가 그림을 그리고 있어요!")
            .setContentText("지금 바로 참여하세요")
            .setSmallIcon(android.R.drawable.ic_dialog_info)
            .setPriority(NotificationCompat.PRIORITY_HIGH)
            .setFullScreenIntent(pendingIntent, true)
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(MATCH_NOTIF_ID, notif)
    }

    private fun updateNotification(title: String, text: String) {
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.notify(NOTIF_ID, buildServiceNotification(title, text))
    }

    private fun buildServiceNotification(title: String, text: String): Notification {
        val openIntent = PendingIntent.getActivity(
            this, 0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return NotificationCompat.Builder(this, CHANNEL_ID)
            .setContentTitle(title)
            .setContentText(text)
            .setSmallIcon(android.R.drawable.ic_menu_share)
            .setOngoing(true)
            .setPriority(NotificationCompat.PRIORITY_LOW)
            .setContentIntent(openIntent)
            .build()
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            "VibeLock 서비스",
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "VibeLock 백그라운드 연결"
        }
        val nm = getSystemService(NOTIFICATION_SERVICE) as NotificationManager
        nm.createNotificationChannel(channel)
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        if (intent?.action == ACTION_STOP) {
            stopSelf()
        }
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        scope.cancel()
        super.onDestroy()
    }
}
