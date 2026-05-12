package com.roadmate.headunit.service

import android.app.Notification
import android.app.NotificationChannel
import android.app.NotificationManager
import android.app.PendingIntent
import android.app.Service
import android.content.Context
import android.content.Intent
import android.content.pm.ServiceInfo
import android.os.IBinder
import com.roadmate.core.sync.BluetoothConnectionManager
import com.roadmate.core.state.BluetoothStateManager
import com.roadmate.core.state.DrivingStateManager
import com.roadmate.core.state.LocationStateManager
import com.roadmate.headunit.MainActivity
import com.roadmate.headunit.R
import dagger.hilt.android.AndroidEntryPoint
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class RoadMateService : Service() {

    @Inject lateinit var drivingStateManager: DrivingStateManager
    @Inject lateinit var bluetoothStateManager: BluetoothStateManager
    @Inject lateinit var locationStateManager: LocationStateManager
    @Inject lateinit var connectionManager: BluetoothConnectionManager

    override fun onCreate() {
        super.onCreate()
        createNotificationChannel()
    }

    override fun onStartCommand(intent: Intent?, flags: Int, startId: Int): Int {
        Timber.i("RoadMateService starting")
        try {
            startForeground(
                NOTIFICATION_ID,
                createNotification(),
                ServiceInfo.FOREGROUND_SERVICE_TYPE_LOCATION,
            )
        } catch (e: Exception) {
            Timber.e(e, "Failed to start foreground service")
            stopSelf()
            return START_NOT_STICKY
        }
        connectionManager.startServer()
        return START_STICKY
    }

    override fun onBind(intent: Intent?): IBinder? = null

    override fun onDestroy() {
        super.onDestroy()
        connectionManager.stop()
        Timber.i("RoadMateService destroyed")
    }

    private fun createNotificationChannel() {
        val channel = NotificationChannel(
            CHANNEL_ID,
            CHANNEL_NAME,
            NotificationManager.IMPORTANCE_LOW,
        ).apply {
            description = "Shows when RoadMate tracking is active"
            setShowBadge(false)
        }
        val manager = getSystemService(NotificationManager::class.java) ?: return
        manager.createNotificationChannel(channel)
    }

    private fun createNotification(): Notification {
        val pendingIntent = PendingIntent.getActivity(
            this,
            0,
            Intent(this, MainActivity::class.java),
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )
        return Notification.Builder(this, CHANNEL_ID)
            .setContentTitle(NOTIFICATION_TITLE)
            .setContentText(NOTIFICATION_TEXT)
            .setSmallIcon(R.drawable.ic_notification)
            .setOngoing(true)
            .setContentIntent(pendingIntent)
            .build()
    }

    companion object {
        const val NOTIFICATION_ID = 1
        const val CHANNEL_ID = "roadmate_tracking"
        const val CHANNEL_NAME = "RoadMate Tracking"
        const val NOTIFICATION_TITLE = "RoadMate"
        const val NOTIFICATION_TEXT = "RoadMate tracking active"

        fun start(context: Context) {
            val intent = Intent(context, RoadMateService::class.java)
            context.startForegroundService(intent)
        }
    }
}
