package com.roadmate.phone.ui.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.Text
import androidx.compose.material3.TextButton
import androidx.compose.runtime.Composable
import androidx.compose.runtime.LaunchedEffect
import androidx.compose.runtime.getValue
import androidx.compose.runtime.mutableStateOf
import androidx.compose.runtime.remember
import androidx.compose.runtime.setValue
import androidx.compose.ui.platform.LocalContext
import com.roadmate.core.sync.BluetoothPermissionChecker

@Composable
fun BluetoothPermissionEffect(
    onPermissionsResult: (granted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions()
    ) { results ->
        val allGranted = results.all { it.value }
        onPermissionsResult(allGranted)
    }

    LaunchedEffect(Unit) {
        if (BluetoothPermissionChecker.REQUIRED_PERMISSIONS.isEmpty()) {
            onPermissionsResult(true)
            return@LaunchedEffect
        }
        if (BluetoothPermissionChecker.hasPermissions(context)) {
            onPermissionsResult(true)
        } else {
            val activity = context as? android.app.Activity
            if (activity != null && BluetoothPermissionChecker.shouldShowRationale(activity)) {
                showRationale = true
            } else {
                launcher.launch(BluetoothPermissionChecker.REQUIRED_PERMISSIONS.toTypedArray())
            }
        }
    }

    if (showRationale) {
        AlertDialog(
            onDismissRequest = {
                showRationale = false
                onPermissionsResult(false)
            },
            title = { Text("Bluetooth Permission Required") },
            text = {
                Text(
                    "RoadMate needs Bluetooth access to sync data between your phone and the car head unit."
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        launcher.launch(
                            BluetoothPermissionChecker.REQUIRED_PERMISSIONS.toTypedArray()
                        )
                    }
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        onPermissionsResult(false)
                    }
                ) { Text("Cancel") }
            },
        )
    }
}
