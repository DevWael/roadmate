package com.roadmate.headunit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roadmate.headunit.service.RoadMateService
import timber.log.Timber

class BootReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.i("Boot completed received, starting RoadMateService")
            try {
                RoadMateService.start(context)
            } catch (e: IllegalStateException) {
                Timber.e(e, "Failed to start RoadMateService on boot")
            }
        }
    }
}
