package com.roadmate.headunit.receiver

import android.content.BroadcastReceiver
import android.content.Context
import android.content.Intent
import com.roadmate.core.util.CrashRecoveryManager
import com.roadmate.headunit.service.RoadMateService
import dagger.hilt.android.AndroidEntryPoint
import kotlinx.coroutines.CoroutineScope
import kotlinx.coroutines.Dispatchers
import kotlinx.coroutines.SupervisorJob
import kotlinx.coroutines.cancel
import kotlinx.coroutines.launch
import timber.log.Timber
import javax.inject.Inject

@AndroidEntryPoint
class BootReceiver : BroadcastReceiver() {

    @Inject lateinit var crashRecoveryManager: CrashRecoveryManager

    private val recoveryScope = CoroutineScope(SupervisorJob() + Dispatchers.IO)

    override fun onReceive(context: Context, intent: Intent) {
        if (intent.action == Intent.ACTION_BOOT_COMPLETED) {
            Timber.i("Boot completed received, starting crash recovery then RoadMateService")
            val pendingResult = goAsync()
            recoveryScope.launch {
                try {
                    crashRecoveryManager.recover()
                } catch (e: Exception) {
                    Timber.e(e, "BootReceiver: crash recovery failed")
                } finally {
                    try {
                        if (com.roadmate.headunit.ui.permissions.hasLocationPermission(context)) {
                            RoadMateService.start(context)
                        } else {
                            Timber.w("Location permission not granted, skipping service start on boot")
                        }
                    } catch (e: Exception) {
                        Timber.e(e, "Failed to start RoadMateService on boot")
                    }
                    pendingResult.finish()
                    recoveryScope.cancel()
                }
            }
        }
    }
}
