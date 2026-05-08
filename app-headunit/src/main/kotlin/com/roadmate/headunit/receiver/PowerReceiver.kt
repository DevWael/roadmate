package com.roadmate.headunit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import timber.log.Timber

class PowerReceiver : BroadcastReceiver() {

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SHUTDOWN) {
            Timber.i("Shutdown received, triggering data flush")
            onShutdown()
        }
    }

    internal fun onShutdown() {
        Timber.i("Data flush triggered on shutdown")
    }
}
