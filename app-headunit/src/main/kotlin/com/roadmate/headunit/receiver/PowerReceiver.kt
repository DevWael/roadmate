package com.roadmate.headunit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roadmate.core.location.TripRecorder
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class PowerReceiver : BroadcastReceiver() {

    @Inject lateinit var tripRecorder: TripRecorder

    private val shutdownScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_SHUTDOWN) {
            Timber.i("Shutdown received, triggering data flush")
            val pendingResult = goAsync()
            shutdownScope.launch {
                try {
                    tripRecorder.gracefulShutdown()
                } catch (e: Exception) {
                    Timber.e(e, "PowerReceiver: graceful shutdown failed")
                } finally {
                    pendingResult.finish()
                    shutdownScope.cancel()
                }
            }
}
