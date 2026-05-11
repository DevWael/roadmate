package com.roadmate.phone

import android.app.Application
import androidx.work.ExistingPeriodicWorkPolicy
import androidx.work.PeriodicWorkRequestBuilder
import androidx.work.WorkManager
import com.roadmate.phone.worker.NotificationChannels
import com.roadmate.phone.worker.NotificationCheckWorker
import dagger.hilt.android.HiltAndroidApp
import timber.log.Timber
import java.util.concurrent.TimeUnit

@HiltAndroidApp
class RoadMatePhoneApplication : Application() {
    override fun onCreate() {
        super.onCreate()
        if (BuildConfig.DEBUG) {
            Timber.plant(Timber.DebugTree())
        }

        NotificationChannels.createAll(this)

        val notificationCheckWork = PeriodicWorkRequestBuilder<NotificationCheckWorker>(
            12, TimeUnit.HOURS,
            6, TimeUnit.HOURS,
        ).build()

        WorkManager.getInstance(this).enqueueUniquePeriodicWork(
            NOTIFICATION_CHECK_WORK_NAME,
            ExistingPeriodicWorkPolicy.KEEP,
            notificationCheckWork,
        )
    }

    companion object {
        const val NOTIFICATION_CHECK_WORK_NAME = "notification_check"
    }
}
