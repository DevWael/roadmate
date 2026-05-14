package com.roadmate.headunit.ui.permissions

import androidx.activity.compose.rememberLauncherForActivityResult
import androidx.activity.result.contract.ActivityResultContracts
import androidx.compose.material3.AlertDialog
import androidx.compose.material3.MaterialTheme
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

/**
 * Composable effect that requests Bluetooth permissions when they are missing.
 * Uses the shared [BluetoothPermissionChecker] from the core module.
 *
 * @param onResult called with `true` when all Bluetooth permissions are granted.
 */
@Composable
fun BluetoothPermissionEffect(
    onResult: (granted: Boolean) -> Unit,
) {
    val context = LocalContext.current
    var showRationale by remember { mutableStateOf(false) }

    val launcher = rememberLauncherForActivityResult(
        contract = ActivityResultContracts.RequestMultiplePermissions(),
    ) { results ->
        val allGranted = results.all { it.value }
        onResult(allGranted)
    }

    LaunchedEffect(Unit) {
        if (BluetoothPermissionChecker.REQUIRED_PERMISSIONS.isEmpty()) {
            onResult(true)
            return@LaunchedEffect
        }
        if (BluetoothPermissionChecker.hasPermissions(context)) {
            onResult(true)
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
                onResult(false)
            },
            title = {
                Text(
                    text = "Bluetooth Permission Required",
                    style = MaterialTheme.typography.titleLarge,
                )
            },
            text = {
                Text(
                    text = "RoadMate needs Bluetooth access to sync vehicle data between " +
                        "the head unit and your phone.",
                    style = MaterialTheme.typography.bodyMedium,
                )
            },
            confirmButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        launcher.launch(
                            BluetoothPermissionChecker.REQUIRED_PERMISSIONS.toTypedArray(),
                        )
                    },
                ) { Text("Continue") }
            },
            dismissButton = {
                TextButton(
                    onClick = {
                        showRationale = false
                        onResult(false)
                    },
                ) { Text("Skip") }
            },
        )
    }
}
