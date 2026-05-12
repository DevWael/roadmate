package com.roadmate.core.sync

import android.Manifest
import android.content.Context
import android.content.pm.PackageManager
import android.os.Build

object BluetoothPermissionChecker {

    val REQUIRED_PERMISSIONS: List<String>
        get() = if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.S) {
            listOf(
                Manifest.permission.BLUETOOTH_CONNECT,
                Manifest.permission.BLUETOOTH_SCAN,
            )
        } else {
            emptyList()
        }

    fun hasPermissions(context: Context): Boolean {
        return REQUIRED_PERMISSIONS.all {
            context.checkSelfPermission(it) == PackageManager.PERMISSION_GRANTED
        }
    }

    fun shouldShowRationale(activity: android.app.Activity): Boolean {
        return REQUIRED_PERMISSIONS.any {
            activity.shouldShowRequestPermissionRationale(it)
        }
    }
}
