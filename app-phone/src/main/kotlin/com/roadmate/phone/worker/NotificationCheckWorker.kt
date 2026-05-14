package com.roadmate.phone.worker

import android.annotation.SuppressLint
import android.Manifest
import android.app.PendingIntent
import android.content.Context
import android.content.Intent
import android.content.pm.PackageManager
import android.net.Uri
import androidx.core.app.NotificationCompat
import androidx.core.app.NotificationManagerCompat
import androidx.work.CoroutineWorker
import androidx.work.WorkerParameters
import com.roadmate.core.database.dao.DocumentDao
import com.roadmate.core.database.dao.MaintenanceDao
import com.roadmate.core.database.dao.VehicleDao
import com.roadmate.core.database.entity.Document
import com.roadmate.core.database.entity.MaintenanceSchedule
import com.roadmate.core.database.entity.Vehicle
import com.roadmate.core.repository.ActiveVehicleRepository
import com.roadmate.core.util.AttentionLevel
import com.roadmate.core.util.MaintenancePredictionEngine
import com.roadmate.phone.MainActivity
import com.roadmate.phone.R
import dagger.hilt.EntryPoint
import dagger.hilt.InstallIn
import dagger.hilt.android.EntryPointAccessors
import dagger.hilt.components.SingletonComponent
import kotlinx.coroutines.flow.first
import timber.log.Timber
import kotlin.math.roundToInt

@SuppressLint("MissingPermission")
class NotificationCheckWorker(
    context: Context,
    params: WorkerParameters,
) : CoroutineWorker(context, params) {

    @EntryPoint
    @InstallIn(SingletonComponent::class)
    interface WorkerEntryPoint {
        fun activeVehicleRepository(): ActiveVehicleRepository
        fun vehicleDao(): VehicleDao
        fun maintenanceDao(): MaintenanceDao
        fun documentDao(): DocumentDao
    }

    override suspend fun doWork(): Result {
        return try {
            doNotificationCheck()
        } catch (e: Exception) {
            Timber.e(e, "Notification check failed")
            Result.success() // Don't retry — transient failures resolve on next periodic run
        }
    }

    private suspend fun doNotificationCheck(): Result {
        val entryPoint = EntryPointAccessors.fromApplication(
            applicationContext,
            WorkerEntryPoint::class.java,
        )
        val activeVehicleRepo = entryPoint.activeVehicleRepository()
        val vehicleDao = entryPoint.vehicleDao()
        val maintenanceDao = entryPoint.maintenanceDao()
        val documentDao = entryPoint.documentDao()

        val vehicleId = activeVehicleRepo.activeVehicleId.first()
        if (vehicleId.isNullOrBlank()) {
            Timber.d("No active vehicle — skipping notification check")
            return Result.success()
        }

        val vehicle = vehicleDao.getVehicle(vehicleId).first()
        if (vehicle == null) {
            Timber.d("Vehicle %s not found — skipping", vehicleId)
            return Result.success()
        }

        val schedules = maintenanceDao.getSchedulesForVehicle(vehicleId).first()

        var notificationCount = 0

        for (schedule in schedules) {
            val remaining = MaintenancePredictionEngine.remainingKm(
                currentOdometerKm = vehicle.odometerKm,
                lastServiceKm = schedule.lastServiceKm,
                intervalKm = schedule.intervalKm,
            )
            val band = MaintenancePredictionEngine.classifyBand(
                remainingKm = remaining,
                intervalKm = schedule.intervalKm,
            )
            if (band == AttentionLevel.WARNING || band == AttentionLevel.CRITICAL || band == AttentionLevel.OVERDUE) {
                postMaintenanceNotification(vehicle, schedule, remaining)
                notificationCount++
            }
        }

        val now = System.currentTimeMillis()
        val documents = documentDao.getDocumentsForVehicle(vehicleId).first()
        for (doc in documents) {
            val thresholdMs = doc.reminderDaysBefore.toLong() * 24 * 60 * 60 * 1000
            if (doc.expiryDate - now <= thresholdMs) {
                postDocumentNotification(vehicle, doc)
                notificationCount++
            }
        }

        Timber.d("Notification check complete: %d notifications posted", notificationCount)
        return Result.success()
    }

    private fun postMaintenanceNotification(
        vehicle: Vehicle,
        schedule: MaintenanceSchedule,
        remainingKm: Double,
    ) {
        if (!hasNotificationPermission()) return

        val remainingKmInt = remainingKm.roundToInt()
        val body = if (remainingKm <= 0) {
            "${schedule.name} is overdue"
        } else {
            "${schedule.name} is due in $remainingKmInt km"
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            data = Uri.parse("roadmate://maintenance/${schedule.id}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val notificationId = "mnt:${schedule.id}".hashCode()

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.MAINTENANCE_ALERTS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${vehicle.name} - Maintenance Due")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)
    }

    private fun postDocumentNotification(vehicle: Vehicle, document: Document) {
        if (!hasNotificationPermission()) return

        val daysRemainingRaw = (document.expiryDate - System.currentTimeMillis()) / (24.0 * 60 * 60 * 1000)
        val daysRemaining = daysRemainingRaw.roundToInt()
        val body = if (daysRemainingRaw <= 0) {
            "${document.name} has expired"
        } else {
            "${document.name} expires in $daysRemaining days"
        }

        val intent = Intent(applicationContext, MainActivity::class.java).apply {
            data = Uri.parse("roadmate://document/${document.id}")
            flags = Intent.FLAG_ACTIVITY_NEW_TASK or Intent.FLAG_ACTIVITY_CLEAR_TOP
        }

        val notificationId = "doc:${document.id}".hashCode()

        val pendingIntent = PendingIntent.getActivity(
            applicationContext,
            notificationId,
            intent,
            PendingIntent.FLAG_UPDATE_CURRENT or PendingIntent.FLAG_IMMUTABLE,
        )

        val notification = NotificationCompat.Builder(applicationContext, NotificationChannels.DOCUMENT_REMINDERS)
            .setSmallIcon(R.drawable.ic_notification)
            .setContentTitle("${vehicle.name} - Document Expiring")
            .setContentText(body)
            .setStyle(NotificationCompat.BigTextStyle().bigText(body))
            .setContentIntent(pendingIntent)
            .setAutoCancel(true)
            .build()

        NotificationManagerCompat.from(applicationContext)
            .notify(notificationId, notification)
    }

    private fun hasNotificationPermission(): Boolean {
        return if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.TIRAMISU) {
            applicationContext.checkSelfPermission(Manifest.permission.POST_NOTIFICATIONS) ==
                PackageManager.PERMISSION_GRANTED
        } else {
            true
        }
    }
}
