package com.roadmate.phone.worker

import android.app.NotificationChannel
import android.app.NotificationManager
import android.content.Context
import android.os.Build

object NotificationChannels {

    const val MAINTENANCE_ALERTS = "maintenance_alerts"
    const val DOCUMENT_REMINDERS = "document_reminders"

    fun createAll(context: Context) {
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.O) return
        val manager = context.getSystemService(Context.NOTIFICATION_SERVICE) as NotificationManager

        val maintenanceChannel = NotificationChannel(
            MAINTENANCE_ALERTS,
            "Maintenance Alerts",
            NotificationManager.IMPORTANCE_HIGH,
        ).apply {
            description = "Notifications when maintenance is due or overdue"
        }

        val documentChannel = NotificationChannel(
            DOCUMENT_REMINDERS,
            "Document Reminders",
            NotificationManager.IMPORTANCE_DEFAULT,
        ).apply {
            description = "Reminders for expiring vehicle documents"
        }

        manager.createNotificationChannels(listOf(maintenanceChannel, documentChannel))
    }
}
